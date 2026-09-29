-- Zeromile Connect - Phase 7 Administrative Dashboard Schema
-- Role-based access control, municipal teams, admin audit logging, and complaint management.
-- Compatible with PostgreSQL 15+ and Supabase.

-- 1. Create Teams Table
CREATE TABLE IF NOT EXISTS public.teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,
    department_id UUID REFERENCES public.departments(id) ON DELETE SET NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Alter Complaints Table to Support Administrative Assignment, Resolution, and Closure
ALTER TABLE public.complaints
    ADD COLUMN IF NOT EXISTS assigned_team_id UUID REFERENCES public.teams(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS assigned_team_name TEXT,
    ADD COLUMN IF NOT EXISTS assigned_staff_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS assigned_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS resolution_text TEXT,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS resolved_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS closed_by UUID REFERENCES auth.users(id) ON DELETE SET NULL;

-- 3. Create Admin Audit Logs Table (Permanent Privileged Record)
CREATE TABLE IF NOT EXISTS public.admin_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    admin_email TEXT,
    action TEXT NOT NULL, -- ASSIGN_COMPLAINT, UPDATE_STATUS, ADD_UPDATE, RESOLVE_COMPLAINT, CLOSE_COMPLAINT, DELETE_COMPLAINT
    entity_type TEXT NOT NULL DEFAULT 'complaint',
    entity_id TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4. Create Performance Indexes for Admin Queries (Item 43)
CREATE INDEX IF NOT EXISTS idx_complaints_status ON public.complaints(status);
CREATE INDEX IF NOT EXISTS idx_complaints_priority ON public.complaints(priority);
CREATE INDEX IF NOT EXISTS idx_complaints_service_id ON public.complaints(service_id);
CREATE INDEX IF NOT EXISTS idx_complaints_department_id ON public.complaints(department_id);
CREATE INDEX IF NOT EXISTS idx_complaints_ward_id ON public.complaints(ward_id);
CREATE INDEX IF NOT EXISTS idx_complaints_created_at ON public.complaints(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_complaints_updated_at ON public.complaints(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_complaints_admin_filter ON public.complaints(status, priority, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON public.admin_audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON public.admin_audit_logs(action);

-- 5. Helper Function: Check If User Is Admin
CREATE OR REPLACE FUNCTION public.is_admin(check_user_id UUID DEFAULT auth.uid())
RETURNS BOOLEAN AS $$
BEGIN
    IF check_user_id IS NULL THEN
        RETURN FALSE;
    END IF;
    RETURN EXISTS (
        SELECT 1 FROM public.user_roles
        WHERE user_id = check_user_id AND role = 'admin'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. Helper Function: Check If User Is Municipal Staff or Admin
CREATE OR REPLACE FUNCTION public.is_staff_or_admin(check_user_id UUID DEFAULT auth.uid())
RETURNS BOOLEAN AS $$
BEGIN
    IF check_user_id IS NULL THEN
        RETURN FALSE;
    END IF;
    RETURN EXISTS (
        SELECT 1 FROM public.user_roles
        WHERE user_id = check_user_id AND role IN ('municipal_staff', 'admin')
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 7. Admin Role Bootstrapping for Administrator Email: shrnavan1439009@gmail.com
CREATE OR REPLACE FUNCTION public.sync_admin_role_for_user()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.email IN ('shrnavan1439009@gmail.com', 'shrnaman143909@gmail.com') THEN
        INSERT INTO public.user_roles (user_id, role)
        VALUES (NEW.id, 'admin')
        ON CONFLICT (user_id) DO UPDATE SET role = 'admin';
    ELSE
        INSERT INTO public.user_roles (user_id, role)
        VALUES (NEW.id, 'citizen')
        ON CONFLICT (user_id) DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Attach trigger to auth.users if possible
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'auth' AND table_name = 'users') THEN
        DROP TRIGGER IF EXISTS trigger_sync_admin_role ON auth.users;
        CREATE TRIGGER trigger_sync_admin_role
            AFTER INSERT OR UPDATE OF email ON auth.users
            FOR EACH ROW
            EXECUTE FUNCTION public.sync_admin_role_for_user();

        -- Retroactively grant admin role to existing auth user if present
        INSERT INTO public.user_roles (user_id, role)
        SELECT id, 'admin'::user_role_enum
        FROM auth.users
        WHERE email IN ('shrnavan1439009@gmail.com', 'shrnaman143909@gmail.com')
        ON CONFLICT (user_id) DO UPDATE SET role = 'admin';
    END IF;
EXCEPTION WHEN OTHERS THEN
    -- In environments without direct auth trigger permissions, fail gracefully
    RAISE NOTICE 'Skipping direct auth.users trigger';
END $$;

-- 8. Row Level Security Policies

-- Teams RLS
ALTER TABLE public.teams ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow authenticated to view active teams"
    ON public.teams
    FOR SELECT
    TO authenticated
    USING (active = true OR public.is_admin());

CREATE POLICY "Allow anon to view active teams in demo"
    ON public.teams
    FOR SELECT
    TO anon
    USING (active = true);

CREATE POLICY "Admins can manage teams"
    ON public.teams
    FOR ALL
    TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- Complaints Admin Policies
CREATE POLICY "Admins can view all complaints"
    ON public.complaints
    FOR SELECT
    TO authenticated
    USING (public.is_admin());

CREATE POLICY "Admins can update complaints"
    ON public.complaints
    FOR UPDATE
    TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

CREATE POLICY "Admins can delete complaints"
    ON public.complaints
    FOR DELETE
    TO authenticated
    USING (public.is_admin());

-- Status History Admin Policies
CREATE POLICY "Admins can view all status history"
    ON public.complaint_status_history
    FOR SELECT
    TO authenticated
    USING (public.is_admin());

CREATE POLICY "Admins can insert status history"
    ON public.complaint_status_history
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_admin());

-- Municipal Updates Admin Policies
CREATE POLICY "Admins can view all complaint updates"
    ON public.complaint_updates
    FOR SELECT
    TO authenticated
    USING (public.is_admin());

CREATE POLICY "Admins can insert complaint updates"
    ON public.complaint_updates
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_admin());

-- Audit Logs Policies (Strictly Admin Only)
ALTER TABLE public.admin_audit_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Only admins can view audit logs"
    ON public.admin_audit_logs
    FOR SELECT
    TO authenticated
    USING (public.is_admin());

CREATE POLICY "Only admins can insert audit logs"
    ON public.admin_audit_logs
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_admin());

-- 9. Secure Server-Side Function for Deleting Complaint with Audit Log
CREATE OR REPLACE FUNCTION public.delete_complaint_admin(
    p_complaint_id UUID,
    p_reason TEXT DEFAULT 'Administrative deletion'
)
RETURNS BOOLEAN AS $$
DECLARE
    v_admin_id UUID;
    v_admin_email TEXT;
    v_complaint_num TEXT;
BEGIN
    v_admin_id := auth.uid();
    IF NOT public.is_admin(v_admin_id) THEN
        RAISE EXCEPTION 'Admin authorization required';
    END IF;

    SELECT complaint_number INTO v_complaint_num
    FROM public.complaints
    WHERE id = p_complaint_id;

    IF v_complaint_num IS NULL THEN
        RAISE EXCEPTION 'Complaint not found';
    END IF;

    SELECT email INTO v_admin_email
    FROM auth.users
    WHERE id = v_admin_id;

    -- Record in audit log BEFORE deletion
    INSERT INTO public.admin_audit_logs (
        admin_user_id,
        admin_email,
        action,
        entity_type,
        entity_id,
        metadata
    ) VALUES (
        v_admin_id,
        v_admin_email,
        'DELETE_COMPLAINT',
        'complaint',
        p_complaint_id::TEXT,
        jsonb_build_object(
            'complaint_number', v_complaint_num,
            'reason', p_reason,
            'timestamp', now()
        )
    );

    -- Delete complaint; CASCADE handles status history and updates safely
    DELETE FROM public.complaints WHERE id = p_complaint_id;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
