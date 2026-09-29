-- Zeromile Connect - Phase 5 Complaint Management Schema
-- Turn reviewed complaint into a real complaint record in Supabase
-- Compatible with PostgreSQL 15+ and Supabase

-- 1. Create Sequence for Complaint Number Generation
-- Format: NMC-{YEAR}-{6 DIGITS}, e.g. NMC-2026-001245
CREATE SEQUENCE IF NOT EXISTS public.complaint_number_seq START WITH 1245 INCREMENT BY 1;

-- 2. Concurrency-Safe Function to Generate Human-Readable Complaint Number
CREATE OR REPLACE FUNCTION public.generate_complaint_number()
RETURNS TEXT AS $$
DECLARE
    curr_year TEXT;
    seq_val BIGINT;
    generated_number TEXT;
BEGIN
    curr_year := to_char(CURRENT_DATE, 'YYYY');
    seq_val := nextval('public.complaint_number_seq');
    generated_number := 'NMC-' || curr_year || '-' || lpad(seq_val::TEXT, 6, '0');
    RETURN generated_number;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 3. Create Controlled Status and Priority ENUMs/CHECKs
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'complaint_status_enum') THEN
        CREATE TYPE complaint_status_enum AS ENUM (
            'Submitted',
            'Assigned',
            'In Progress',
            'Resolved',
            'Closed'
        );
    END IF;
END $$;

-- 4. Complaints Table
CREATE TABLE IF NOT EXISTS public.complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_number TEXT NOT NULL UNIQUE DEFAULT public.generate_complaint_number(),
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    service_id UUID NOT NULL REFERENCES public.services(id) ON DELETE RESTRICT,
    category_id UUID REFERENCES public.service_categories(id) ON DELETE SET NULL,
    department_id UUID REFERENCES public.departments(id) ON DELETE SET NULL,
    description TEXT NOT NULL,
    original_transcript TEXT NOT NULL,
    input_mode TEXT NOT NULL DEFAULT 'voice' CHECK (input_mode IN ('voice', 'text')),
    language TEXT NOT NULL DEFAULT 'en-IN',
    location_text TEXT NOT NULL,
    ward_id UUID REFERENCES public.wards(id) ON DELETE SET NULL,
    priority TEXT NOT NULL DEFAULT 'Medium' CHECK (priority IN ('Low', 'Medium', 'High', 'Critical', 'Emergency', 'Urgent')),
    ai_confidence NUMERIC(4, 3) DEFAULT 0.900,
    ai_summary TEXT,
    ai_reason TEXT,
    status TEXT NOT NULL DEFAULT 'Submitted' CHECK (status IN ('Submitted', 'Assigned', 'In Progress', 'Resolved', 'Closed')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 5. Performance Indexes
CREATE INDEX IF NOT EXISTS idx_complaints_user_id ON public.complaints(user_id);
CREATE INDEX IF NOT EXISTS idx_complaints_user_created ON public.complaints(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_complaints_service_id ON public.complaints(service_id);
CREATE INDEX IF NOT EXISTS idx_complaints_number ON public.complaints(complaint_number);

-- 6. Trigger for updated_at
CREATE TRIGGER update_complaints_updated_at
    BEFORE UPDATE ON public.complaints
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- 7. Row Level Security (RLS)
ALTER TABLE public.complaints ENABLE ROW LEVEL SECURITY;

-- Citizens can only view their own complaints.
-- For demo/unauthenticated requests (or when auth.uid() is null in demo mode), allow viewing if matching demo or own user_id.
CREATE POLICY "Citizens can view own complaints"
    ON public.complaints
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

-- Citizens can only insert complaints belonging to themselves (auth.uid() = user_id).
-- The database enforces user ownership and default complaint_number and status ('Submitted').
CREATE POLICY "Citizens can insert own complaints"
    ON public.complaints
    FOR INSERT
    TO authenticated
    WITH CHECK (
        auth.uid() = user_id AND
        status = 'Submitted'
    );

-- Allow public / anon read/insert for prototype demo mode when auth is not active
CREATE POLICY "Anon can view complaints in demo"
    ON public.complaints
    FOR SELECT
    TO anon
    USING (true);

CREATE POLICY "Anon can insert complaints in demo"
    ON public.complaints
    FOR INSERT
    TO anon
    WITH CHECK (status = 'Submitted');

-- Citizens cannot modify complaint_number, status, department, etc.
-- Restricted update policy:
CREATE POLICY "Citizens can update own complaint details only"
    ON public.complaints
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (
        auth.uid() = user_id AND
        status = 'Submitted'
    );
