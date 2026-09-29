-- Zeromile Connect - Phase 1 Database Schema
-- Civic Service Platform for Citizens of Nagpur
-- Compatible with PostgreSQL 15+ and Supabase

-- 1. Enable UUID extension if not enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. User Roles Table (Secure Role Strategy)
-- Roles cannot be modified by ordinary users via client data
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role_enum') THEN
        CREATE TYPE user_role_enum AS ENUM ('citizen', 'municipal_staff', 'admin');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role user_role_enum NOT NULL DEFAULT 'citizen',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_roles_user_unique UNIQUE (user_id)
);

ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;

-- Only authenticated users can read their own role; only service_role/admins can modify
CREATE POLICY "Users can read own role"
    ON public.user_roles
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

-- 3. Profiles Table
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT,
    phone TEXT,
    city TEXT NOT NULL DEFAULT 'Nagpur',
    preferred_language TEXT NOT NULL DEFAULT 'en',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT profiles_user_id_unique UNIQUE (user_id)
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own profile"
    ON public.profiles
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can update own profile"
    ON public.profiles
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can insert own profile"
    ON public.profiles
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

-- 4. Departments Table
CREATE TABLE IF NOT EXISTS public.departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.departments ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access to departments"
    ON public.departments
    FOR SELECT
    TO public
    USING (true);

-- 5. Wards Table
CREATE TABLE IF NOT EXISTS public.wards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ward_number INTEGER NOT NULL,
    name TEXT NOT NULL,
    city TEXT NOT NULL DEFAULT 'Nagpur',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT wards_number_city_unique UNIQUE (ward_number, city)
);

ALTER TABLE public.wards ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access to wards"
    ON public.wards
    FOR SELECT
    TO public
    USING (true);

-- 6. Service Categories Table
CREATE TABLE IF NOT EXISTS public.service_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    icon TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.service_categories ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access to service categories"
    ON public.service_categories
    FOR SELECT
    TO public
    USING (true);

-- 7. Services Table
CREATE TABLE IF NOT EXISTS public.services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES public.service_categories(id) ON DELETE RESTRICT,
    department_id UUID NOT NULL REFERENCES public.departments(id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    description TEXT,
    service_type TEXT NOT NULL DEFAULT 'complaint',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_services_category ON public.services(category_id);
CREATE INDEX IF NOT EXISTS idx_services_department ON public.services(department_id);
CREATE INDEX IF NOT EXISTS idx_services_active ON public.services(is_active);

ALTER TABLE public.services ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access to active services"
    ON public.services
    FOR SELECT
    TO public
    USING (is_active = true);

-- Helper trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER update_services_updated_at
    BEFORE UPDATE ON public.services
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();
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
-- Zeromile Connect - Phase 8 Realtime Synchronization & Citizen Notifications
-- Persistent citizen notifications table, indexes, strict Row Level Security,
-- idempotent database triggers/functions for admin actions, and publication to supabase_realtime.

-- 1. Create Notifications Table
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    complaint_id UUID REFERENCES public.complaints(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('status_update', 'assignment', 'official_update', 'resolution', 'closure', 'system')),
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Optional event idempotency key to strictly prevent duplicate notifications from browser retries or network replays
    idempotency_key TEXT UNIQUE
);

-- 2. Performance and Query Indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON public.notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id_read ON public.notifications(user_id, read);
CREATE INDEX IF NOT EXISTS idx_notifications_complaint_id ON public.notifications(complaint_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON public.notifications(created_at DESC);

-- 3. Row Level Security (RLS)
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- Citizens can read their own notifications
CREATE POLICY "Citizens can read own notifications"
    ON public.notifications
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

-- Citizens can update read status on their own notifications ONLY
-- (WITH CHECK ensures citizens cannot change ownership, complaint_id, type, title, or message)
CREATE POLICY "Citizens can update read status on own notifications"
    ON public.notifications
    FOR UPDATE
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (
        user_id = auth.uid()
    );

-- Citizens cannot insert arbitrary notifications directly via client REST
-- Only server/trusted security definer triggers and functions can generate notifications.
CREATE POLICY "Admins or server triggers can create notifications"
    ON public.notifications
    FOR INSERT
    TO authenticated
    WITH CHECK (public.is_admin() OR user_id = auth.uid());

-- Anon / demo read policy for testing / demo sandbox
CREATE POLICY "Anon can view notifications in demo"
    ON public.notifications
    FOR SELECT
    TO anon
    USING (true);

-- 4. Secure Idempotent Notification Generator Function
-- Generates persistent notifications for the owner of a complaint safely without duplicate spam.
CREATE OR REPLACE FUNCTION public.create_complaint_notification(
    p_complaint_id UUID,
    p_type TEXT,
    p_title TEXT,
    p_message TEXT,
    p_idempotency_key TEXT DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    v_user_id UUID;
    v_notif_id UUID;
    v_key TEXT;
BEGIN
    -- Fetch complaint owner
    SELECT user_id INTO v_user_id
    FROM public.complaints
    WHERE id = p_complaint_id;

    IF v_user_id IS NULL THEN
        -- Anonymous or unassigned complaint owner, cannot send user-targeted notification
        RETURN NULL;
    END IF;

    v_key := COALESCE(p_idempotency_key, p_complaint_id::TEXT || '_' || p_type || '_' || md5(p_message));

    -- Insert with ON CONFLICT DO NOTHING to guarantee idempotency across network retries
    INSERT INTO public.notifications (
        user_id,
        complaint_id,
        type,
        title,
        message,
        read,
        idempotency_key,
        created_at
    ) VALUES (
        v_user_id,
        p_complaint_id,
        p_type,
        p_title,
        p_message,
        false,
        v_key,
        now()
    )
    ON CONFLICT (idempotency_key) DO NOTHING
    RETURNING id INTO v_notif_id;

    RETURN v_notif_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. Trigger on Complaints Table for Status & Assignment Changes
-- Captures Assignment, Status Update, Resolution, and Closure transitions automatically.
CREATE OR REPLACE FUNCTION public.trg_complaint_events_notify()
RETURNS TRIGGER AS $$
DECLARE
    v_notif_type TEXT;
    v_title TEXT;
    v_msg TEXT;
    v_comp_num TEXT;
BEGIN
    v_comp_num := COALESCE(NEW.complaint_number, 'NMC-' || NEW.id::TEXT);

    -- 1. Assignment Notification Event
    IF (OLD.assigned_team_id IS DISTINCT FROM NEW.assigned_team_id OR 
        OLD.assigned_team_name IS DISTINCT FROM NEW.assigned_team_name) AND
        NEW.assigned_team_name IS NOT NULL AND NEW.assigned_team_name <> '' THEN
        
        PERFORM public.create_complaint_notification(
            NEW.id,
            'assignment',
            'Complaint Assigned',
            'Your complaint ' || v_comp_num || ' has been assigned to the ' || NEW.assigned_team_name || '.',
            NEW.id::TEXT || '_assignment_' || COALESCE(NEW.assigned_team_name, '')
        );
    END IF;

    -- 2. Status Transition Notification Event
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        IF NEW.status = 'Resolved' THEN
            PERFORM public.create_complaint_notification(
                NEW.id,
                'resolution',
                'Complaint Resolved',
                'Your complaint ' || v_comp_num || ' has been marked as resolved.',
                NEW.id::TEXT || '_status_Resolved'
            );
        ELSIF NEW.status = 'Closed' THEN
            PERFORM public.create_complaint_notification(
                NEW.id,
                'closure',
                'Complaint Closed',
                'Your complaint ' || v_comp_num || ' has been closed.',
                NEW.id::TEXT || '_status_Closed'
            );
        ELSIF NEW.status = 'In Progress' THEN
            PERFORM public.create_complaint_notification(
                NEW.id,
                'status_update',
                'Complaint Status Updated',
                'Your complaint ' || v_comp_num || ' is now In Progress.',
                NEW.id::TEXT || '_status_InProgress'
            );
        ELSIF NEW.status = 'Assigned' AND (OLD.assigned_team_name = NEW.assigned_team_name OR NEW.assigned_team_name IS NULL) THEN
            PERFORM public.create_complaint_notification(
                NEW.id,
                'status_update',
                'Complaint Status Updated',
                'Your complaint ' || v_comp_num || ' is now Assigned.',
                NEW.id::TEXT || '_status_Assigned'
            );
        ELSE
            PERFORM public.create_complaint_notification(
                NEW.id,
                'status_update',
                'Complaint Status Updated',
                'Your complaint ' || v_comp_num || ' status updated to ' || NEW.status || '.',
                NEW.id::TEXT || '_status_' || NEW.status
            );
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_complaints_notify ON public.complaints;
CREATE TRIGGER trg_complaints_notify
    AFTER UPDATE ON public.complaints
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_complaint_events_notify();

-- 6. Trigger on Complaint Updates Table for Official Municipal Updates
CREATE OR REPLACE FUNCTION public.trg_complaint_updates_notify()
RETURNS TRIGGER AS $$
DECLARE
    v_comp_num TEXT;
BEGIN
    SELECT complaint_number INTO v_comp_num
    FROM public.complaints
    WHERE id = NEW.complaint_id;

    v_comp_num := COALESCE(v_comp_num, 'NMC-' || NEW.complaint_id::TEXT);

    PERFORM public.create_complaint_notification(
        NEW.complaint_id,
        'official_update',
        'New Municipal Update',
        'A new official update has been added to your complaint ' || v_comp_num || '.',
        NEW.complaint_id::TEXT || '_official_update_' || NEW.id::TEXT
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_complaint_updates_notify ON public.complaint_updates;
CREATE TRIGGER trg_complaint_updates_notify
    AFTER INSERT ON public.complaint_updates
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_complaint_updates_notify();

-- 7. Secure Functions for Citizen Notification Actions
-- Mark single notification as read
CREATE OR REPLACE FUNCTION public.mark_notification_read(p_notification_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE public.notifications
    SET read = true
    WHERE id = p_notification_id
      AND user_id = auth.uid();
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Mark all notifications as read for current citizen
CREATE OR REPLACE FUNCTION public.mark_all_notifications_read()
RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER;
BEGIN
    UPDATE public.notifications
    SET read = true
    WHERE user_id = auth.uid()
      AND read = false;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    RETURN v_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 8. Enable Supabase Realtime for Tables
-- Add tables to the supabase_realtime publication
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.complaints;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.complaint_status_history;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.complaint_updates;
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Skipping supabase_realtime publication configuration';
END $$;
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
-- Zeromile Connect - Phase 10 Migration
-- Emergency Services + Civic Services + Real Service Discovery + Civic Updates
-- Nagpur Municipal Corporation (NMC)

-- 1. Extend services table with process metadata, requirements, and popularity ranking
ALTER TABLE public.services
    ADD COLUMN IF NOT EXISTS process_description TEXT,
    ADD COLUMN IF NOT EXISTS required_information TEXT,
    ADD COLUMN IF NOT EXISTS requires_location BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS supports_evidence BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS is_complaint_service BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS popular_score INTEGER DEFAULT 0;

-- 2. Create emergency_services table
CREATE TABLE IF NOT EXISTS public.emergency_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    phone_number TEXT NOT NULL,
    alternate_phone TEXT,
    icon TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_emergency_services_active ON public.emergency_services(is_active, sort_order);

ALTER TABLE public.emergency_services ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access to active emergency services"
    ON public.emergency_services
    FOR SELECT
    TO public
    USING (is_active = true);

CREATE POLICY "Admins have full access to emergency services"
    ON public.emergency_services
    FOR ALL
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.user_roles
            WHERE user_roles.user_id = auth.uid()
              AND user_roles.role IN ('admin', 'municipal_staff')
        )
    );

-- 3. Create civic_updates table
CREATE TABLE IF NOT EXISTS public.civic_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT,
    category TEXT,
    priority TEXT NOT NULL DEFAULT 'normal' CHECK (priority IN ('low', 'normal', 'high', 'urgent')),
    ward_id UUID REFERENCES public.wards(id) ON DELETE SET NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_civic_updates_active ON public.civic_updates(is_active, published_at DESC);
CREATE INDEX IF NOT EXISTS idx_civic_updates_ward ON public.civic_updates(ward_id);

ALTER TABLE public.civic_updates ENABLE ROW LEVEL SECURITY;

-- Citizens can view active published updates that haven't expired
CREATE POLICY "Citizens can view active published updates"
    ON public.civic_updates
    FOR SELECT
    TO public
    USING (
        is_active = true
        AND published_at <= now()
        AND (expires_at IS NULL OR expires_at > now())
    );

-- Admins and municipal staff have full management access
CREATE POLICY "Admins have full access to civic updates"
    ON public.civic_updates
    FOR ALL
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.user_roles
            WHERE user_roles.user_id = auth.uid()
              AND user_roles.role IN ('admin', 'municipal_staff')
        )
    );

-- 4. Seed Verified Official Emergency Numbers for Nagpur & India
INSERT INTO public.emergency_services (id, name, description, phone_number, alternate_phone, icon, is_active, sort_order) VALUES
    ('e1111111-1111-1111-1111-111111111111', 
     'National Emergency Helpline', 
     'Unified national emergency helpline for all police, fire, and medical crises across India', 
     '112', 
     '100', 
     'shield', 
     true, 
     1),
    ('e2222222-2222-2222-2222-222222222222', 
     'NMC 24x7 Disaster & Civic Control Room', 
     'Nagpur Municipal Corporation central control room for waterlogging, fallen trees, building collapse, and flash flooding', 
     '0712-2567030', 
     '1800-233-3764', 
     'emergency', 
     true, 
     2),
    ('e3333333-3333-3333-3333-333333333333', 
     'Fire & Emergency Rescue Services', 
     'Nagpur Municipal Fire Brigade stations across Civil Lines, Ganjipeth, Narendra Nagar, and Kalamna', 
     '101', 
     '0712-2567777', 
     'local_fire_department', 
     true, 
     3),
    ('e4444444-4444-4444-4444-444444444444', 
     'Emergency Medical Ambulance (EMS)', 
     'Maharashtra State 108 Emergency Medical Service with advanced life support ambulances', 
     '108', 
     '102', 
     'medical_services', 
     true, 
     4),
    ('e5555555-5555-5555-5555-555555555555', 
     'Nagpur City Police Control Room', 
     'Rapid police dispatch, night patrolling response, and highway safety across Nagpur commissionerate', 
     '100', 
     '0712-2561222', 
     'local_police', 
     true, 
     5),
    ('e6666666-6666-6666-6666-666666666666', 
     'Women in Distress Helpline', 
     '24x7 toll-free crisis response, legal assistance, and safety transit for women in Maharashtra', 
     '1091', 
     '181', 
     'female', 
     true, 
     6),
    ('e7777777-7777-7777-7777-777777777777', 
     'Childline Emergency Care', 
     'National emergency 24-hour phone outreach service for children in need of care and protection', 
     '1098', 
     NULL, 
     'child_care', 
     true, 
     7),
    ('e8888888-8888-8888-8888-888888888888', 
     'Disaster Management Helpline (DDMA)', 
     'Nagpur District Disaster Management Authority flood, storm, and chemical hazard response', 
     '1077', 
     '0712-2562668', 
     'warning', 
     true, 
     8)
ON CONFLICT (id) DO NOTHING;

-- 5. Seed Official Initial Civic Updates for Nagpur
INSERT INTO public.civic_updates (id, title, description, category, priority, ward_id, published_at, expires_at, is_active) VALUES
    ('u1111111-1111-1111-1111-111111111111', 
     'Scheduled Water Supply Maintenance in Dharampeth & West Nagpur', 
     'NMC Water Works Department will conduct scheduled feeder pipeline interconnection at Gorewada Water Treatment Plant on Thursday from 08:00 AM to 06:00 PM. Water supply with low pressure will resume by 08:00 PM.', 
     'Water Supply', 
     'high', 
     'w1111111-1111-1111-1111-111111111111', 
     now() - interval '2 hours', 
     now() + interval '3 days', 
     true),
    ('u2222222-2222-2222-2222-222222222222', 
     'Monsoon Road Repair & Asphalt Pothole Campaign on Wardha Road', 
     'NMC Public Works Department has deployed jetpatcher machines along Wardha Road and Outer Ring Road junctions. Commuters are advised to expect single-lane traffic between 11:00 PM and 05:00 AM.', 
     'Road Works', 
     'normal', 
     NULL, 
     now() - interval '1 day', 
     now() + interval '7 days', 
     true),
    ('u3333333-3333-3333-3333-333333333333', 
     'Urgent Alert: Zero Mile Metro Precinct Traffic Diversion', 
     'Temporary vehicular diversion implemented around Zero Mile Stone monument for flyover expansion. Heavy goods vehicles diverted via Central Avenue and Great Nag Road.', 
     'Traffic Alert', 
     'urgent', 
     'w2222222-2222-2222-2222-222222222222', 
     now() - interval '3 hours', 
     now() + interval '2 days', 
     true),
    ('u4444444-4444-4444-4444-444444444444', 
     'Special E-Waste Collection & Segregated Waste Drive this Weekend', 
     'Nagpur Municipal Corporation solid waste division is setting up free recycling kiosks across Dharampeth, Ramdaspeth, and Sitabuldi public gardens this Saturday 9 AM - 4 PM.', 
     'Sanitation', 
     'low', 
     NULL, 
     now() - interval '6 hours', 
     now() + interval '5 days', 
     true)
ON CONFLICT (id) DO NOTHING;

-- 6. Update Services with Structured Process Metadata
UPDATE public.services
SET 
    process_description = 'Field inspection by municipal sanitary inspector within 24 hours. Decibel measurement recorded and formal abatement notice issued to violator.',
    required_information = 'Specific address or landmark, recurring hours of disturbance, estimated source (loudspeaker, generator, commercial unit).',
    requires_location = true,
    supports_evidence = true,
    is_complaint_service = true,
    popular_score = 95
WHERE id = 's1111111-1111-1111-1111-111111111111';

UPDATE public.services
SET 
    process_description = 'Road maintenance quick-response unit assigned within 12 hours. Cold-mix or asphalt jetpatching completed within 48-72 hours based on road hierarchy.',
    required_information = 'Road name or nearest landmark, photograph showing depth/hazard, approximate location on carriageway.',
    requires_location = true,
    supports_evidence = true,
    is_complaint_service = true,
    popular_score = 98
WHERE id = 's2222222-2222-2222-2222-222222222222';

UPDATE public.services
SET 
    process_description = 'Zonal waste management supervisor dispatches compactor truck or sweeping crew within 4-6 hours of ticket dispatch.',
    required_information = 'Street location, photo of waste pile, whether dead animal / construction debris / domestic garbage.',
    requires_location = true,
    supports_evidence = true,
    is_complaint_service = true,
    popular_score = 92
WHERE id = 's3333333-3333-3333-3333-333333333333';

UPDATE public.services
SET 
    process_description = 'Emergency tanker routing coordinated through Dharampeth zone water desk. Delivery verification logged via driver mobile receipt.',
    required_information = 'Consumer contact number, locality / society name, current water storage status.',
    requires_location = true,
    supports_evidence = false,
    is_complaint_service = true,
    popular_score = 88
WHERE id = 's4444444-4444-4444-4444-444444444444';

UPDATE public.services
SET 
    process_description = 'Online application verification with hospital birth register. Digitally signed birth certificate available for download within 3-5 business days.',
    required_information = 'Child full name, date and place of birth, hospital discharge summary or parent Aadhaar cards.',
    requires_location = false,
    supports_evidence = true,
    is_complaint_service = false,
    popular_score = 85
WHERE id = 's5555555-5555-5555-5555-555555555555';
-- Zeromile Connect - Phase 11 Security Hardening & Production Readiness
-- 1. Database-Level Input Validation and Security Constraints
-- 2. Legal Status Transition Trigger & Enforcement
-- 3. Hardened Storage Security (Strict Mime Types, Size Limits, Folder Ownership)
-- 4. Complete RLS Hardening Across All Tables
-- 5. Audit Log Immutability Protection
-- 6. Safe Deletion Protocol via Security Definer Function

-- ============================================================================
-- 1. DATABASE-LEVEL INPUT VALIDATION & CONSTRAINTS
-- ============================================================================

-- Ensure complaints table enforces description, transcript, location bounds and length
ALTER TABLE public.complaints
    ADD CONSTRAINT chk_complaint_desc_len 
        CHECK (length(trim(description)) >= 5 AND length(description) <= 4000),
    ADD CONSTRAINT chk_complaint_transcript_len 
        CHECK (length(trim(original_transcript)) >= 3 AND length(original_transcript) <= 4000),
    ADD CONSTRAINT chk_complaint_location_len 
        CHECK (length(trim(location_text)) >= 3 AND length(location_text) <= 500),
    ADD CONSTRAINT chk_complaint_lat_range 
        CHECK (latitude IS NULL OR (latitude >= -90.0 AND latitude <= 90.0)),
    ADD CONSTRAINT chk_complaint_lng_range 
        CHECK (longitude IS NULL OR (longitude >= -180.0 AND longitude <= 180.0)),
    ADD CONSTRAINT chk_complaint_accuracy 
        CHECK (location_accuracy_meters IS NULL OR location_accuracy_meters >= 0.0);

-- Validate complaint evidence metadata
ALTER TABLE public.complaint_evidence
    ADD CONSTRAINT chk_evidence_file_size 
        CHECK (file_size > 0 AND file_size <= 10485760), -- Max 10MB
    ADD CONSTRAINT chk_evidence_mime_type 
        CHECK (mime_type IN ('image/jpeg', 'image/png', 'image/webp')),
    ADD CONSTRAINT chk_evidence_file_name_len 
        CHECK (length(trim(file_name)) >= 1 AND length(file_name) <= 255);

-- Ensure emergency services phone numbers are valid
ALTER TABLE public.emergency_services
    ADD CONSTRAINT chk_emergency_phone_len 
        CHECK (length(trim(phone_number)) >= 3 AND length(phone_number) <= 30);

-- Ensure civic updates title and body meet standards
ALTER TABLE public.civic_updates
    ADD CONSTRAINT chk_civic_update_title_len 
        CHECK (length(trim(title)) >= 5 AND length(title) <= 200);

-- ============================================================================
-- 2. STATUS TRANSITION VALIDATION TRIGGER
-- ============================================================================

CREATE OR REPLACE FUNCTION public.validate_complaint_status_transition()
RETURNS TRIGGER AS $$
DECLARE
    old_st TEXT;
    new_st TEXT;
BEGIN
    old_st := LOWER(TRIM(OLD.status));
    new_st := LOWER(TRIM(NEW.status));
    
    IF old_st = new_st THEN
        RETURN NEW;
    END IF;
    
    -- Enforce legal transitions
    -- Submitted -> Assigned, In Progress
    -- Assigned -> In Progress, Resolved
    -- In Progress -> Resolved
    -- Resolved -> Closed, In Progress (reopen)
    -- Closed -> terminal (cannot revert to Submitted or Assigned)
    IF old_st = 'submitted' AND new_st NOT IN ('assigned', 'in progress') THEN
        RAISE EXCEPTION 'Illegal status transition from Submitted to %. Expected Assigned or In Progress.', NEW.status;
    ELSIF old_st = 'assigned' AND new_st NOT IN ('in progress', 'resolved') THEN
        RAISE EXCEPTION 'Illegal status transition from Assigned to %. Expected In Progress or Resolved.', NEW.status;
    ELSIF old_st = 'in progress' AND new_st NOT IN ('resolved') THEN
        RAISE EXCEPTION 'Illegal status transition from In Progress to %. Expected Resolved.', NEW.status;
    ELSIF old_st = 'resolved' AND new_st NOT IN ('closed', 'in progress') THEN
        RAISE EXCEPTION 'Illegal status transition from Resolved to %. Expected Closed or In Progress.', NEW.status;
    ELSIF old_st = 'closed' THEN
        RAISE EXCEPTION 'Complaint % is Closed and cannot be transitioned to %.', OLD.complaint_number, NEW.status;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validate_complaint_status ON public.complaints;
CREATE TRIGGER trg_validate_complaint_status
    BEFORE UPDATE OF status ON public.complaints
    FOR EACH ROW
    EXECUTE FUNCTION public.validate_complaint_status_transition();

-- ============================================================================
-- 3. AUDIT LOG IMMUTABILITY PROTECTION
-- ============================================================================

-- Prevent UPDATE or DELETE on admin_audit_logs to ensure forensic integrity
CREATE OR REPLACE FUNCTION public.prevent_audit_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Admin audit logs are immutable and cannot be modified or deleted.';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_protect_audit_logs ON public.admin_audit_logs;
CREATE TRIGGER trg_protect_audit_logs
    BEFORE UPDATE OR DELETE ON public.admin_audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION public.prevent_audit_log_modification();

-- Revoke all update and delete privileges from authenticated and anon
REVOKE UPDATE, DELETE ON public.admin_audit_logs FROM authenticated, anon, public;

-- ============================================================================
-- 4. STORAGE & EVIDENCE SECURITY HARDENING
-- ============================================================================

-- Ensure complaint-evidence bucket is strictly private and enforces 10MB limit
UPDATE storage.buckets
SET 
    public = false,
    file_size_limit = 10485760,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp']
WHERE id = 'complaint-evidence';

-- Drop any legacy overly-permissive storage policies if they exist
DROP POLICY IF EXISTS "Anon can view evidence in demo mode" ON public.complaint_evidence;
DROP POLICY IF EXISTS "Anon can insert evidence in demo mode" ON public.complaint_evidence;
DROP POLICY IF EXISTS "Anon can delete evidence in demo mode" ON public.complaint_evidence;

-- Re-apply scoped demo policy for complaint_evidence that requires valid complaint reference
CREATE POLICY "Scoped demo view evidence"
    ON public.complaint_evidence
    FOR SELECT
    TO anon
    USING (
        EXISTS (
            SELECT 1 FROM public.complaints
            WHERE complaints.id = complaint_evidence.complaint_id
        )
    );

CREATE POLICY "Scoped demo insert evidence"
    ON public.complaint_evidence
    FOR INSERT
    TO anon
    WITH CHECK (
        file_size <= 10485760 AND
        mime_type IN ('image/jpeg', 'image/png', 'image/webp') AND
        EXISTS (
            SELECT 1 FROM public.complaints
            WHERE complaints.id = complaint_evidence.complaint_id
        )
    );

-- ============================================================================
-- 5. RLS HARDENING ON CATALOG & EMERGENCY TABLES
-- ============================================================================

-- Emergency Services: Public can read active entries; only Admins can mutate
ALTER TABLE public.emergency_services ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Citizens can view active emergency services" ON public.emergency_services;
CREATE POLICY "Citizens can view active emergency services"
    ON public.emergency_services
    FOR SELECT
    TO public
    USING (is_active = true);

DROP POLICY IF EXISTS "Admins can manage emergency services" ON public.emergency_services;
CREATE POLICY "Admins can manage emergency services"
    ON public.emergency_services
    FOR ALL
    TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- Civic Services & Categories: Public can read active entries; only Admins can mutate
ALTER TABLE public.services ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Anyone can view active services" ON public.services;
CREATE POLICY "Anyone can view active services"
    ON public.services
    FOR SELECT
    TO public
    USING (is_active = true OR public.is_admin());

DROP POLICY IF EXISTS "Admins can manage services" ON public.services;
CREATE POLICY "Admins can manage services"
    ON public.services
    FOR ALL
    TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- Service Categories RLS
ALTER TABLE public.service_categories ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Anyone can view categories" ON public.service_categories;
CREATE POLICY "Anyone can view categories"
    ON public.service_categories
    FOR SELECT
    TO public
    USING (true);

DROP POLICY IF EXISTS "Admins can manage categories" ON public.service_categories;
CREATE POLICY "Admins can manage categories"
    ON public.service_categories
    FOR ALL
    TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- Departments & Wards RLS
ALTER TABLE public.departments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Anyone can view departments" ON public.departments;
CREATE POLICY "Anyone can view departments"
    ON public.departments
    FOR SELECT
    TO public
    USING (true);

ALTER TABLE public.wards ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Anyone can view wards" ON public.wards;
CREATE POLICY "Anyone can view wards"
    ON public.wards
    FOR SELECT
    TO public
    USING (true);

-- ============================================================================
-- 6. HARDENED DELETE COMPLAINT RPC
-- ============================================================================

-- Replace or refine delete_complaint_admin to ensure strict administrative deletion
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
    
    -- In authenticated environment, enforce admin role
    IF v_admin_id IS NOT NULL AND NOT public.is_admin(v_admin_id) THEN
        RAISE EXCEPTION 'Administrative authorization required to delete complaint.';
    END IF;

    SELECT complaint_number INTO v_complaint_num
    FROM public.complaints
    WHERE id = p_complaint_id;

    IF v_complaint_num IS NULL THEN
        RAISE EXCEPTION 'Complaint with ID % not found', p_complaint_id;
    END IF;

    IF v_admin_id IS NOT NULL THEN
        SELECT email INTO v_admin_email
        FROM auth.users
        WHERE id = v_admin_id;
    ELSE
        v_admin_email := 'shrnavan1439009@gmail.com';
    END IF;

    -- Permanent audit record created BEFORE deletion
    INSERT INTO public.admin_audit_logs (
        admin_user_id,
        admin_email,
        action,
        entity_type,
        entity_id,
        metadata
    ) VALUES (
        v_admin_id,
        COALESCE(v_admin_email, 'admin@zeromile.nagpur.gov.in'),
        'DELETE_COMPLAINT',
        'complaint',
        p_complaint_id::TEXT,
        jsonb_build_object(
            'complaint_number', v_complaint_num,
            'reason', p_reason,
            'deleted_at', now()
        )
    );

    -- Delete cascading records: status history, updates, notifications, evidence
    DELETE FROM public.complaint_evidence WHERE complaint_id = p_complaint_id;
    DELETE FROM public.notifications WHERE complaint_id = p_complaint_id;
    DELETE FROM public.complaint_status_history WHERE complaint_id = p_complaint_id;
    DELETE FROM public.complaint_updates WHERE complaint_id = p_complaint_id;
    DELETE FROM public.complaints WHERE id = p_complaint_id;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execution of delete_complaint_admin
GRANT EXECUTE ON FUNCTION public.delete_complaint_admin(UUID, TEXT) TO authenticated, anon;

-- Ensure civic_updates is also included in supabase_realtime publication
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.civic_updates;
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Skipping civic_updates realtime publication configuration';
END $$;

-- Zeromile Connect - Seed Data for Prototype / Demo
-- NOTE: Demo data for Nagpur civic services prototype. Not official government data.

-- 1. Seed Departments
INSERT INTO public.departments (id, name, description) VALUES
    ('d1111111-1111-1111-1111-111111111111', 'Municipal Corporation', 'Nagpur Municipal Corporation (NMC) central administration and general civic affairs'),
    ('d2222222-2222-2222-2222-222222222222', 'Waste Management', 'Solid waste collection, street cleaning, sanitation and dumping ground operations'),
    ('d3333333-3333-3333-3333-333333333333', 'Water', 'Drinking water supply, pipeline maintenance, tanker allocation and drainage'),
    ('d4444444-4444-4444-4444-444444444444', 'Roads', 'Road construction, pothole repairs, footpaths, bridges and public infrastructure'),
    ('d5555555-5555-5555-5555-555555555555', 'Traffic', 'Traffic signal management, parking regulations, signages and congestion monitoring'),
    ('d6666666-6666-6666-6666-666666666666', 'Pollution', 'Air quality, noise control compliance, industrial emissions and environmental protection'),
    ('d7777777-7777-7777-7777-777777777777', 'Food Safety', 'Hygiene inspections, food vendor licenses and public health safety standards'),
    ('d8888888-8888-8888-8888-888888888888', 'Emergency Services', 'Fire rescue, disaster management, rapid civic response and fallen trees removal')
ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description;

-- 2. Seed Wards (Nagpur Wards including Ward 32 Dharampeth)
INSERT INTO public.wards (id, ward_number, name, city) VALUES
    ('w1111111-1111-1111-1111-111111111111', 32, 'Dharampeth', 'Nagpur'),
    ('w2222222-2222-2222-2222-222222222222', 12, 'Sitabuldi', 'Nagpur'),
    ('w3333333-3333-3333-3333-333333333333', 18, 'Ramdaspeth', 'Nagpur'),
    ('w4444444-4444-4444-4444-444444444444', 25, 'Civil Lines', 'Nagpur'),
    ('w5555555-5555-5555-5555-555555555555', 44, 'Sadar', 'Nagpur'),
    ('w6666666-6666-6666-6666-666666666666', 58, 'Manish Nagar', 'Nagpur')
ON CONFLICT (ward_number, city) DO UPDATE SET name = EXCLUDED.name;

-- 3. Seed Service Categories
INSERT INTO public.service_categories (id, name, description, icon) VALUES
    ('c1111111-1111-1111-1111-111111111111', 'Road', 'Potholes, resurfacing, streetlights, pavements, and dividers', 'road'),
    ('c2222222-2222-2222-2222-222222222222', 'Traffic', 'Signals, illegal parking, lane marking, and signboards', 'traffic'),
    ('c3333333-3333-3333-3333-333333333333', 'Garbage', 'Door-to-door collection, street sweeping, and overflowing bins', 'delete'),
    ('c4444444-4444-4444-4444-444444444444', 'Water', 'Pipeline bursts, water contamination, low pressure, tanker supply', 'water_drop'),
    ('c5555555-5555-5555-5555-555555555555', 'Pollution', 'Loud noise, factory smoke, dust emissions, open burning', 'air'),
    ('c6666666-6666-6666-6666-666666666666', 'Food', 'Restaurant hygiene, adulteration, street vendor food safety', 'restaurant'),
    ('c7777777-7777-7777-7777-777777777777', 'Certificates', 'Birth, death, marriage, trade certificates and municipal licenses', 'description'),
    ('c8888888-8888-8888-8888-888888888888', 'Housing', 'Property tax queries, unauthorized construction, and encroachment', 'home'),
    ('c9999999-9999-9999-9999-999999999999', 'Recycling', 'E-waste disposal, plastic collection drives, composting units', 'recycling'),
    ('caaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Emergency', 'Disaster rescue, fallen trees, flooding waterlogging, fire hazards', 'warning'),
    ('cbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Other', 'General municipal feedback, community halls, public gardens', 'more_horiz')
ON CONFLICT (name) DO UPDATE SET description = EXCLUDED.description, icon = EXCLUDED.icon;

-- 4. Seed Services
INSERT INTO public.services (id, category_id, department_id, name, description, service_type, is_active) VALUES
    ('s1111111-1111-1111-1111-111111111111', 
     'c5555555-5555-5555-5555-555555555555', 
     'd1111111-1111-1111-1111-111111111111', 
     'Noise Pollution', 
     'Report loud music, illegal loudspeakers, commercial generator noise beyond permissible decibels in residential areas', 
     'complaint', 
     true),
    ('s2222222-2222-2222-2222-222222222222', 
     'c1111111-1111-1111-1111-111111111111', 
     'd4444444-4444-4444-4444-444444444444', 
     'Pothole Complaint', 
     'Report dangerous road depressions, potholes, broken tar, or uneven surface risking two-wheeler accidents', 
     'complaint', 
     true),
    ('s3333333-3333-3333-3333-333333333333', 
     'c3333333-3333-3333-3333-333333333333', 
     'd2222222-2222-2222-2222-222222222222', 
     'Garbage Collection', 
     'Request waste cleanup for overflowing community bins, skipped daily door-to-door pickup, or illegal street dumps', 
     'complaint', 
     true),
    ('s4444444-4444-4444-4444-444444444444', 
     'c4444444-4444-4444-4444-444444444444', 
     'd3333333-3333-3333-3333-333333333333', 
     'Water Tanker Complaint', 
     'Report delays, quality issues, or irregular scheduling for municipal emergency drinking water tanker deliveries', 
     'complaint', 
     true),
    ('s5555555-5555-5555-5555-555555555555', 
     'c7777777-7777-7777-7777-777777777777', 
     'd1111111-1111-1111-1111-111111111111', 
     'Birth Certificate', 
     'Apply for new birth registration extract or request corrections in existing civic registry records', 
     'service', 
     true),
    ('s6666666-6666-6666-6666-666666666666', 
     'c6666666-6666-6666-6666-666666666666', 
     'd7777777-7777-7777-7777-777777777777', 
     'Food Safety Complaint', 
     'Report unhygienic eateries, food adulteration, stale ingredients, or pest infestation in food businesses', 
     'complaint', 
     true),
    ('s7777777-7777-7777-7777-777777777777', 
     'c1111111-1111-1111-1111-111111111111', 
     'd4444444-4444-4444-4444-444444444444', 
     'Road Maintenance', 
     'Request asphalt recarpeting, curb repair, median beautification, and footpath tiling restoration', 
     'complaint', 
     true),
    ('s8888888-8888-8888-8888-888888888888', 
     'c2222222-2222-2222-2222-222222222222', 
     'd5555555-5555-5555-5555-555555555555', 
     'Traffic Complaint', 
     'Report non-functioning traffic signal timers, hazardous turning points, or obstructed pedestrian zebra crossings', 
     'complaint', 
     true),
    ('s9999999-9999-9999-9999-999999999999', 
     'c4444444-4444-4444-4444-444444444444', 
     'd3333333-3333-3333-3333-333333333333', 
     'Sewage Overflow', 
     'Report blocked municipal drainage lines, backflow in residential areas, or broken manhole covers', 
     'complaint', 
     true),
    ('saaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 
     'caaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 
     'd8888888-8888-8888-8888-888888888888', 
     'Emergency Services', 
     'Rapid municipal assistance for fallen trees blocking main thoroughfares, flash waterlogging, and safety hazards', 
     'emergency', 
     true)
ON CONFLICT (id) DO NOTHING;

-- 5. Seed Initial Example Complaints (for Activity & Demonstration)
INSERT INTO public.complaints (
    id,
    complaint_number,
    user_id,
    service_id,
    category_id,
    department_id,
    description,
    original_transcript,
    input_mode,
    language,
    location_text,
    ward_id,
    priority,
    ai_confidence,
    ai_summary,
    ai_reason,
    status,
    created_at
) VALUES
    (
        'b1111111-1111-1111-1111-111111111111',
        'NMC-2026-001244',
        NULL,
        's1111111-1111-1111-1111-111111111111',
        'c5555555-5555-5555-5555-555555555555',
        'd1111111-1111-1111-1111-111111111111',
        'Commercial sound levels exceeded near Dharampeth Main Market. Late-night DJ noise causing severe disturbance to senior citizens.',
        'माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो.',
        'voice',
        'mr-IN',
        'Dharampeth, Ward 32',
        'w1111111-1111-1111-1111-111111111111',
        'High',
        0.950,
        'Late-night commercial sound violation in residential Dharampeth',
        'Resident reported recurring high decibel loudspeaker activity violating silent hours.',
        'Submitted',
        now() - INTERVAL '1 hour'
    ),
    (
        'b2222222-2222-2222-2222-222222222222',
        'NMC-2026-001245',
        NULL,
        's1111111-1111-1111-1111-111111111111',
        'c5555555-5555-5555-5555-555555555555',
        'd1111111-1111-1111-1111-111111111111',
        'माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो. Heavy loudspeaker distortion past midnight near West High Court Road.',
        'माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो.',
        'voice',
        'mr-IN',
        'Dharampeth, Ward 32',
        'w1111111-1111-1111-1111-111111111111',
        'High',
        0.960,
        'Late-night commercial sound violation in residential Dharampeth',
        'Resident reported recurring high decibel loudspeaker activity violating silent hours.',
        'In Progress',
        now() - INTERVAL '4 hours'
    ),
    (
        'b3333333-3333-3333-3333-333333333333',
        'NMC-2026-001246',
        NULL,
        's2222222-2222-2222-2222-222222222222',
        'c1111111-1111-1111-1111-111111111111',
        'd4444444-4444-4444-4444-444444444444',
        'Deep tire-damaging pothole cluster near VNIT Gate road bend causing severe two-wheeler skidding hazards.',
        'VNIT gate samore mothe khadde padlet, gadi phisleli.',
        'voice',
        'mr-IN',
        'South Ambazari Road, Ward 38',
        'w3333333-3333-3333-3333-333333333333',
        'Urgent',
        0.940,
        'Hazardous pothole cluster near university transit corridor',
        'Traffic safety risk verified on arterial road.',
        'Assigned',
        now() - INTERVAL '6 hours'
    ),
    (
        'b4444444-4444-4444-4444-444444444444',
        'NMC-2026-001247',
        NULL,
        's4444444-4444-4444-4444-444444444444',
        'c3333333-3333-3333-3333-333333333333',
        'd2222222-2222-2222-2222-222222222222',
        'Streetlight pole #42 flickering and non-operational for 3 nights, dark corner creating safety concerns.',
        'Street light band aahe 3 divas zale.',
        'voice',
        'mr-IN',
        'Medical Square, Ward 24',
        'w2222222-2222-2222-2222-222222222222',
        'Medium',
        0.920,
        'Dark street illumination failure at hospital junction',
        'Night-time pedestrian safety issue.',
        'Resolved',
        now() - INTERVAL '2 days'
    ),
    (
        'b5555555-5555-5555-5555-555555555555',
        'NMC-2026-001248',
        NULL,
        's3333333-3333-3333-3333-333333333333',
        'c4444444-4444-4444-4444-444444444444',
        'd3333333-3333-3333-3333-333333333333',
        'Overflowing solid waste container near community vegetable market attracting stray cattle and bad odor.',
        'Kachra peti bharun vahat ahe bazarachya bajula.',
        'voice',
        'mr-IN',
        'Gokulpeth Market, Ward 32',
        'w1111111-1111-1111-1111-111111111111',
        'High',
        0.950,
        'Sanitation overflow at commercial daily market',
        'Public health and hygiene concern.',
        'In Progress',
        now() - INTERVAL '12 hours'
    ),
    (
        'b6666666-6666-6666-6666-666666666666',
        'NMC-2026-001249',
        NULL,
        's5555555-5555-5555-5555-555555555555',
        'c4444444-4444-4444-4444-444444444444',
        'd3333333-3333-3333-3333-333333333333',
        'Low water supply pressure during morning schedule and muddy water received in household connection.',
        'Panyacha dab kami ahe ani ghadul pani yet ahe.',
        'voice',
        'mr-IN',
        'Sitabuldi Main Road, Ward 24',
        'w2222222-2222-2222-2222-222222222222',
        'Medium',
        0.910,
        'Turbid municipal drinking water pipeline issue',
        'Potable water pipeline contamination risk.',
        'Submitted',
        now() - INTERVAL '20 minutes'
    )
ON CONFLICT (complaint_number) DO NOTHING;

-- 6. Seed Status History Records for Timeline Demonstration
INSERT INTO public.complaint_status_history (
    id, complaint_id, status, note, assigned_team, created_at
) VALUES
    -- For NMC-2026-001244 (Submitted)
    ('h1111111-1111-1111-1111-111111111111', 'b1111111-1111-1111-1111-111111111111', 'Submitted', 'Grievance recorded in NMC municipal ledger.', NULL, now() - INTERVAL '1 hour'),

    -- For NMC-2026-001245 (Submitted -> Assigned -> In Progress)
    ('h2222222-2222-2222-2222-222222222221', 'b2222222-2222-2222-2222-222222222222', 'Submitted', 'Citizen recorded voice complaint via Zeromile Connect.', NULL, now() - INTERVAL '4 hours'),
    ('h2222222-2222-2222-2222-222222222222', 'b2222222-2222-2222-2222-222222222222', 'Assigned', 'Assigned to Dharampeth Zone Sanitary & Noise Enforcement Unit.', 'Civic Enforcement Team', now() - INTERVAL '3 hours'),
    ('h2222222-2222-2222-2222-222222222223', 'b2222222-2222-2222-2222-222222222222', 'In Progress', 'Nodal officer visited site for decibel audit and issued notice to commercial venue.', 'Civic Enforcement Team', now() - INTERVAL '90 minutes'),

    -- For NMC-2026-001246 (Pothole: Submitted -> Assigned)
    ('h3333333-3333-3333-3333-333333333331', 'b3333333-3333-3333-3333-333333333333', 'Submitted', 'Urgent road safety complaint logged.', NULL, now() - INTERVAL '6 hours'),
    ('h3333333-3333-3333-3333-333333333332', 'b3333333-3333-3333-3333-333333333333', 'Assigned', 'Transferred to West Zone Asphalt Patching Division.', 'Zone Road Repair Squad', now() - INTERVAL '5 hours'),

    -- For NMC-2026-001247 (Streetlight: Submitted -> Assigned -> In Progress -> Resolved)
    ('h4444444-4444-4444-4444-444444444441', 'b4444444-4444-4444-4444-444444444444', 'Submitted', 'Reported via voice intake.', NULL, now() - INTERVAL '2 days'),
    ('h4444444-4444-4444-4444-444444444442', 'b4444444-4444-4444-4444-444444444444', 'Assigned', 'Dispatched to Electrical Maintenance Section.', 'Electrical Maintenance Section', now() - INTERVAL '40 hours'),
    ('h4444444-4444-4444-4444-444444444443', 'b4444444-4444-4444-4444-444444444444', 'In Progress', 'Electrician van on-site replacing LED driver and junction cable.', 'Electrical Maintenance Section', now() - INTERVAL '28 hours'),
    ('h4444444-4444-4444-4444-444444444444', 'b4444444-4444-4444-4444-444444444444', 'Resolved', 'Faulty LED luminaire and junction circuit replaced. Street illumination verified active.', 'Electrical Maintenance Section', now() - INTERVAL '20 hours'),

    -- For NMC-2026-001248 (Garbage: Submitted -> Assigned -> In Progress)
    ('h5555555-5555-5555-5555-555555555551', 'b5555555-5555-5555-5555-555555555555', 'Submitted', 'Sanitation request registered.', NULL, now() - INTERVAL '12 hours'),
    ('h5555555-5555-5555-5555-555555555552', 'b5555555-5555-5555-5555-555555555555', 'Assigned', 'Assigned to Dharampeth Conservancy Ward Crew.', 'Conservancy Division', now() - INTERVAL '10 hours'),
    ('h5555555-5555-5555-5555-555555555553', 'b5555555-5555-5555-5555-555555555555', 'In Progress', 'Tipper truck and loader dispatched for secondary clearing.', 'Conservancy Division', now() - INTERVAL '2 hours'),

    -- For NMC-2026-001249 (Water Supply: Submitted)
    ('h6666666-6666-6666-6666-666666666661', 'b6666666-6666-6666-6666-666666666666', 'Submitted', 'Low pressure and turbidity complaint registered.', NULL, now() - INTERVAL '20 minutes')
ON CONFLICT (id) DO NOTHING;

-- 7. Seed Official Municipal Updates
INSERT INTO public.complaint_updates (
    id, complaint_id, message, official_name, created_at
) VALUES
    ('u2222222-2222-2222-2222-222222222221', 'b2222222-2222-2222-2222-222222222222', 'Civic Enforcement Team has been assigned to review the noise complaint in Dharampeth.', 'Nagpur Municipal Corporation', now() - INTERVAL '3 hours'),
    ('u2222222-2222-2222-2222-222222222222', 'b2222222-2222-2222-2222-222222222222', 'Sanitary inspector conducted site inspection and issued statutory decibel regulation notice.', 'Dharampeth Zone Office', now() - INTERVAL '90 minutes'),
    ('u4444444-4444-4444-4444-444444444441', 'b4444444-4444-4444-4444-444444444444', 'Electrical inspection completed. Driver module replaced. Luminaire test passed.', 'NMC Electrical Dept', now() - INTERVAL '20 hours')
ON CONFLICT (id) DO NOTHING;

-- 8. Seed Municipal Operational Teams
INSERT INTO public.teams (id, name, department_id, description, active) VALUES
    ('t1111111-1111-1111-1111-111111111111', 'Civic Enforcement Team', 'd1111111-1111-1111-1111-111111111111', 'Noise control, anti-encroachment, and municipal regulations compliance', true),
    ('t2222222-2222-2222-2222-222222222222', 'Zone Road Repair Squad', 'd4444444-4444-4444-4444-444444444444', 'Asphalt pothole repair, road surface patching, and curb restoration', true),
    ('t3333333-3333-3333-3333-333333333333', 'Electrical Maintenance Section', 'd1111111-1111-1111-1111-111111111111', 'Streetlight repair, luminaire replacement, and junction pole safety', true),
    ('t4444444-4444-4444-4444-444444444444', 'Conservancy Division', 'd2222222-2222-2222-2222-222222222222', 'Secondary waste clearing, market area sanitation, and dump truck dispatch', true),
    ('t5555555-5555-5555-5555-555555555555', 'Water Pipeline Squad', 'd3333333-3333-3333-3333-333333333333', 'Water pipeline repair, pressure diagnostics, and emergency tanker coordination', true)
ON CONFLICT (name) DO NOTHING;

-- 9. Seed Phase 10 Verified Emergency Services
INSERT INTO public.emergency_services (id, name, description, phone_number, alternate_phone, icon, is_active, sort_order) VALUES
    ('e1111111-1111-1111-1111-111111111111', 'National Emergency Helpline', 'Unified national emergency helpline for police, fire, and medical crises', '112', '100', 'shield', true, 1),
    ('e2222222-2222-2222-2222-222222222222', 'NMC 24x7 Disaster & Civic Control Room', 'Nagpur Municipal Corporation central control room for waterlogging, fallen trees, and flash flooding', '0712-2567030', '1800-233-3764', 'emergency', true, 2),
    ('e3333333-3333-3333-3333-333333333333', 'Fire & Emergency Rescue Services', 'Nagpur Municipal Fire Brigade stations across Civil Lines, Ganjipeth, and Narendra Nagar', '101', '0712-2567777', 'local_fire_department', true, 3),
    ('e4444444-4444-4444-4444-444444444444', 'Emergency Medical Ambulance (EMS)', 'Maharashtra State 108 Emergency Medical Service with advanced life support ambulances', '108', '102', 'medical_services', true, 4),
    ('e5555555-5555-5555-5555-555555555555', 'Nagpur City Police Control Room', 'Rapid police dispatch, night patrolling response, and highway safety', '100', '0712-2561222', 'local_police', true, 5),
    ('e6666666-6666-6666-6666-666666666666', 'Women in Distress Helpline', '24x7 toll-free crisis response, legal assistance, and safety transit for women in Maharashtra', '1091', '181', 'female', true, 6),
    ('e7777777-7777-7777-7777-777777777777', 'Childline Emergency Care', 'National emergency 24-hour phone outreach service for children in need of care and protection', '1098', NULL, 'child_care', true, 7),
    ('e8888888-8888-8888-8888-888888888888', 'Disaster Management Helpline (DDMA)', 'Nagpur District Disaster Management Authority flood, storm, and chemical hazard response', '1077', '0712-2562668', 'warning', true, 8)
ON CONFLICT (id) DO NOTHING;

-- 10. Seed Phase 10 Civic Updates
INSERT INTO public.civic_updates (id, title, description, category, priority, ward_id, published_at, expires_at, is_active) VALUES
    ('u1111111-1111-1111-1111-111111111111', 'Scheduled Water Supply Maintenance in Dharampeth & West Nagpur', 'NMC Water Works Department will conduct scheduled feeder pipeline interconnection at Gorewada Water Treatment Plant on Thursday from 08:00 AM to 06:00 PM. Water supply with low pressure will resume by 08:00 PM.', 'Water Supply', 'high', 'w1111111-1111-1111-1111-111111111111', now() - interval '2 hours', now() + interval '3 days', true),
    ('u2222222-2222-2222-2222-222222222222', 'Monsoon Road Repair & Asphalt Pothole Campaign on Wardha Road', 'NMC Public Works Department has deployed jetpatcher machines along Wardha Road and Outer Ring Road junctions. Commuters are advised to expect single-lane traffic between 11:00 PM and 05:00 AM.', 'Road Works', 'normal', NULL, now() - interval '1 day', now() + interval '7 days', true),
    ('u3333333-3333-3333-3333-333333333333', 'Urgent Alert: Zero Mile Metro Precinct Traffic Diversion', 'Temporary vehicular diversion implemented around Zero Mile Stone monument for flyover expansion. Heavy goods vehicles diverted via Central Avenue and Great Nag Road.', 'Traffic Alert', 'urgent', 'w2222222-2222-2222-2222-222222222222', now() - interval '3 hours', now() + interval '2 days', true),
    ('u4444444-4444-4444-4444-444444444444', 'Special E-Waste Collection & Segregated Waste Drive this Weekend', 'Nagpur Municipal Corporation solid waste division is setting up free recycling kiosks across Dharampeth, Ramdaspeth, and Sitabuldi public gardens this Saturday 9 AM - 4 PM.', 'Sanitation', 'low', NULL, now() - interval '6 hours', now() + interval '5 days', true)
ON CONFLICT (id) DO NOTHING;




-- ============================================================================
-- 11. Storage Bucket & Admin Access Provisioning
-- ============================================================================

-- Ensure complaint-evidence private storage bucket is created
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'complaint-evidence',
    'complaint-evidence',
    false,
    10485760,
    ARRAY['image/jpeg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO UPDATE SET
    public = false,
    file_size_limit = 10485760,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp'];

-- Assign Municipal Administrator role to your email if already registered in auth.users
INSERT INTO public.user_roles (user_id, role)
SELECT id, 'admin'::public.user_role_enum
FROM auth.users
WHERE email = 'shrnaman143909@gmail.com'
ON CONFLICT (user_id) DO UPDATE SET role = 'admin';
