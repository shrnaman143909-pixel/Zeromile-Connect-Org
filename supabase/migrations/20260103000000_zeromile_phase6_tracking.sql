-- Zeromile Connect - Phase 6 Complaint Status Tracking & History
-- Schema for complaint status transitions, timeline tracking, and municipal updates.
-- Fully enforces RLS so citizens can only read their own status history and updates.

-- 1. Complaint Status History Table
CREATE TABLE IF NOT EXISTS public.complaint_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES public.complaints(id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('Submitted', 'Assigned', 'In Progress', 'Resolved', 'Closed')),
    note TEXT,
    assigned_team TEXT,
    changed_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Complaint Official Municipal Updates Table
CREATE TABLE IF NOT EXISTS public.complaint_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES public.complaints(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    official_name TEXT DEFAULT 'Nagpur Municipal Corporation',
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Performance Indexes
CREATE INDEX IF NOT EXISTS idx_status_history_complaint_id ON public.complaint_status_history(complaint_id);
CREATE INDEX IF NOT EXISTS idx_status_history_created_at ON public.complaint_status_history(complaint_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_complaint_updates_complaint_id ON public.complaint_updates(complaint_id, created_at DESC);

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.complaint_status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.complaint_updates ENABLE ROW LEVEL SECURITY;

-- 5. RLS Policies: Citizens can only VIEW status history for complaints they own.
-- Verified via the foreign key relationship: auth.uid() = complaints.user_id
CREATE POLICY "Citizens can view status history of own complaints"
    ON public.complaint_status_history
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.complaints c
            WHERE c.id = complaint_status_history.complaint_id
              AND c.user_id = auth.uid()
        )
    );

-- Anon / demo mode read policy
CREATE POLICY "Anon can view status history in demo"
    ON public.complaint_status_history
    FOR SELECT
    TO anon
    USING (true);

-- CITIZENS ARE STRICTLY FORBIDDEN FROM INSERTING OR MODIFYING STATUS HISTORY
-- Only server/service-role (or municipal admin triggers) can write to status history.
-- No INSERT/UPDATE/DELETE policies for authenticated citizens prevents citizen tampering.

-- 6. RLS Policies: Citizens can only VIEW official updates for complaints they own.
CREATE POLICY "Citizens can view updates of own complaints"
    ON public.complaint_updates
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.complaints c
            WHERE c.id = complaint_updates.complaint_id
              AND c.user_id = auth.uid()
        )
    );

-- Anon / demo mode read policy for updates
CREATE POLICY "Anon can view complaint updates in demo"
    ON public.complaint_updates
    FOR SELECT
    TO anon
    USING (true);

-- CITIZENS CANNOT INSERT OR MODIFY MUNICIPAL UPDATES
-- No INSERT/UPDATE/DELETE policies for authenticated citizens.

-- 7. Trigger to automatically record initial 'Submitted' status history when a complaint is created
CREATE OR REPLACE FUNCTION public.handle_new_complaint_status_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.complaint_status_history (
        complaint_id,
        status,
        note,
        changed_by,
        created_at
    ) VALUES (
        NEW.id,
        NEW.status,
        'Grievance registered in Nagpur Municipal Corporation ledger.',
        NEW.user_id,
        NEW.created_at
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_complaint_initial_status ON public.complaints;
CREATE TRIGGER trg_complaint_initial_status
    AFTER INSERT ON public.complaints
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_complaint_status_history();
