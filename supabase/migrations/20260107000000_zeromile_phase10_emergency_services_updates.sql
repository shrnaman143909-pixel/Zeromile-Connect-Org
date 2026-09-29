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
