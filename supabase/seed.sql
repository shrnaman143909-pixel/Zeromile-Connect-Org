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


