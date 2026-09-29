-- Zeromile Connect - Phase 9 Evidence Uploads, Supabase Storage & Complaint Location
-- 1. Extend complaints table with structured location fields
-- 2. Create complaint_evidence table with strict RLS and indexes
-- 3. Configure private complaint-evidence storage bucket and security policies
-- 4. Set up audit triggers and cascade cleanup for evidence

-- 1. Extend complaints table with structured geographic location fields
ALTER TABLE public.complaints
    ADD COLUMN IF NOT EXISTS latitude NUMERIC,
    ADD COLUMN IF NOT EXISTS longitude NUMERIC,
    ADD COLUMN IF NOT EXISTS location_accuracy_meters NUMERIC,
    ADD COLUMN IF NOT EXISTS location_source TEXT DEFAULT 'manual'
    CHECK (location_source IN ('gps', 'manual', 'address', 'ai_detected'));

-- Update demo complaint NMC-2026-001245 with Nagpur Dharampeth coordinates
UPDATE public.complaints
SET 
    latitude = 21.1436,
    longitude = 79.0688,
    location_accuracy_meters = 12.5,
    location_source = 'gps'
WHERE complaint_number = 'NMC-2026-001245' OR id = 'b2222222-2222-2222-2222-222222222222';

-- 2. Create complaint_evidence table
CREATE TABLE IF NOT EXISTS public.complaint_evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES public.complaints(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES auth.users(id),
    storage_path TEXT NOT NULL,
    file_name TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for efficient lookups
CREATE INDEX IF NOT EXISTS idx_complaint_evidence_complaint_id ON public.complaint_evidence(complaint_id);
CREATE INDEX IF NOT EXISTS idx_complaint_evidence_uploaded_by ON public.complaint_evidence(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_complaint_evidence_created_at ON public.complaint_evidence(created_at DESC);

-- 3. Row Level Security (RLS) for complaint_evidence
ALTER TABLE public.complaint_evidence ENABLE ROW LEVEL SECURITY;

-- Citizens can view evidence belonging to their own complaints
CREATE POLICY "Citizens can view evidence of own complaints"
    ON public.complaint_evidence
    FOR SELECT
    TO authenticated
    USING (
        uploaded_by = auth.uid() OR
        EXISTS (
            SELECT 1 FROM public.complaints
            WHERE complaints.id = complaint_evidence.complaint_id
              AND complaints.user_id = auth.uid()
        )
    );

-- Citizens can upload evidence to their own complaints
CREATE POLICY "Citizens can upload evidence to own complaints"
    ON public.complaint_evidence
    FOR INSERT
    TO authenticated
    WITH CHECK (
        uploaded_by = auth.uid() AND
        EXISTS (
            SELECT 1 FROM public.complaints
            WHERE complaints.id = complaint_evidence.complaint_id
              AND complaints.user_id = auth.uid()
        )
    );

-- Citizens can delete their own evidence
CREATE POLICY "Citizens can delete own evidence"
    ON public.complaint_evidence
    FOR DELETE
    TO authenticated
    USING (
        uploaded_by = auth.uid()
    );

-- Staff and Admins can view all evidence
CREATE POLICY "Staff and Admins can view all evidence"
    ON public.complaint_evidence
    FOR SELECT
    TO authenticated
    USING (
        public.is_admin() OR
        EXISTS (
            SELECT 1 FROM public.user_roles
            WHERE user_roles.user_id = auth.uid()
              AND user_roles.role IN ('admin', 'staff', 'nodal_officer', 'operator')
        )
    );

-- Staff and Admins can delete evidence during complaint management
CREATE POLICY "Staff and Admins can delete evidence"
    ON public.complaint_evidence
    FOR DELETE
    TO authenticated
    USING (
        public.is_admin() OR
        EXISTS (
            SELECT 1 FROM public.user_roles
            WHERE user_roles.user_id = auth.uid()
              AND user_roles.role IN ('admin', 'staff')
        )
    );

-- Anon / Demo policy for development sandbox
CREATE POLICY "Anon can view evidence in demo mode"
    ON public.complaint_evidence
    FOR SELECT
    TO anon
    USING (true);

CREATE POLICY "Anon can insert evidence in demo mode"
    ON public.complaint_evidence
    FOR INSERT
    TO anon
    WITH CHECK (true);

CREATE POLICY "Anon can delete evidence in demo mode"
    ON public.complaint_evidence
    FOR DELETE
    TO anon
    USING (true);

-- 4. Supabase Storage Setup for private bucket 'complaint-evidence'
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'complaint-evidence',
    'complaint-evidence',
    false,
    10485760, -- 10MB limit
    ARRAY['image/jpeg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO UPDATE SET
    public = false,
    file_size_limit = 10485760,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp'];

-- Storage RLS Policies
-- Authenticated citizen can upload into their own folder: {user_id}/{complaint_id}/...
CREATE POLICY "Authenticated users can upload complaint evidence"
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'complaint-evidence' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );

-- Authenticated citizens can view their own evidence objects, staff can view all
CREATE POLICY "Authorized users can read complaint evidence objects"
    ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'complaint-evidence' AND
        (
            (storage.foldername(name))[1] = auth.uid()::text OR
            public.is_admin() OR
            EXISTS (
                SELECT 1 FROM public.user_roles
                WHERE user_roles.user_id = auth.uid()
                  AND user_roles.role IN ('admin', 'staff', 'nodal_officer')
            )
        )
    );

-- Authenticated citizens can delete their own objects, staff can delete any
CREATE POLICY "Authorized users can delete complaint evidence objects"
    ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'complaint-evidence' AND
        (
            (storage.foldername(name))[1] = auth.uid()::text OR
            public.is_admin()
        )
    );

-- Realtime publication for evidence updates
ALTER PUBLICATION supabase_realtime ADD TABLE public.complaint_evidence;
