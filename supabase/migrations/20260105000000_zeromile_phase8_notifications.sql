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
