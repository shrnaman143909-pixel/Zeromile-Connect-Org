# Zeromile Connect — Database Documentation

The Zeromile Connect database is hosted on Supabase (PostgreSQL 15+) and structured to enforce strict data integrity, forensic audit logging, role-based authorization, and real-time subscription broadcasting.

---

## 1. Schema Entity-Relationship Overview

```
 [auth.users] (Supabase Auth)
       |
       +---> [public.profiles] (id, user_id, full_name, phone, role, preferred_language)
       |
       +---> [public.user_roles] (id, user_id, role: 'citizen'|'staff'|'admin', department_id)
       |
       +---> [public.complaints]
                 |
                 +---> [public.complaint_status_history] (timeline & audits)
                 +---> [public.complaint_updates] (official municipal messages)
                 +---> [public.complaint_evidence] (photos, geolocation proof)
                 +---> [public.notifications] (targeted alerts to citizens)
```

---

## 2. Table Definitions & Relationships

### `public.departments`
Municipal administrative departments of Nagpur Municipal Corporation (NMC).
- `id` (UUID, Primary Key)
- `name` (TEXT, Unique, NOT NULL) — e.g., 'Roads', 'Water', 'Waste Management', 'Emergency Services'
- `description` (TEXT)
- `created_at` (TIMESTAMPTZ, Default: `now()`)

### `public.wards`
Electoral and administrative wards within Nagpur.
- `id` (UUID, Primary Key)
- `ward_number` (INT, NOT NULL) — e.g., 32 (Dharampeth), 12 (Sitabuldi)
- `name` (TEXT, NOT NULL)
- `city` (TEXT, Default: 'Nagpur')
- Constraint: `UNIQUE(ward_number, city)`

### `public.service_categories`
Broad civic categories for issue grouping and icons.
- `id` (UUID, Primary Key)
- `name` (TEXT, Unique, NOT NULL) — 'Road', 'Water', 'Garbage', 'Noise', 'Pollution', 'Emergency'
- `description` (TEXT)
- `icon` (TEXT)

### `public.services`
Actionable civic services linked to a category and responsible municipal department.
- `id` (UUID, Primary Key)
- `category_id` (UUID, FK -> `service_categories.id`)
- `department_id` (UUID, FK -> `departments.id`)
- `name` (TEXT, NOT NULL)
- `description` (TEXT)
- `service_type` (TEXT) — 'complaint', 'service', 'emergency'
- `is_active` (BOOLEAN, Default: `true`)

### `public.complaints`
Core grievance ledger holding citizen reports and AI classification metadata.
- `id` (UUID, Primary Key)
- `complaint_number` (TEXT, Unique, NOT NULL) — Format: `NMC-YYYY-XXXXXX`
- `user_id` (UUID, Nullable, FK -> `auth.users.id`)
- `service_id` (UUID, Nullable, FK -> `services.id`)
- `category_id` (UUID, Nullable, FK -> `service_categories.id`)
- `department_id` (UUID, Nullable, FK -> `departments.id`)
- `description` (TEXT, NOT NULL) — Length: 5 - 4000 characters
- `original_transcript` (TEXT) — Raw citizen voice input text
- `input_mode` (TEXT, Default: 'text') — 'text' or 'voice'
- `language` (TEXT, Default: 'en') — 'en', 'mr', 'hi'
- `location_text` (TEXT, NOT NULL) — Specific address or Nagpur landmark
- `ward_id` (UUID, Nullable, FK -> `wards.id`)
- `latitude` (DOUBLE PRECISION) — Bounds: -90.0 to 90.0
- `longitude` (DOUBLE PRECISION) — Bounds: -180.0 to 180.0
- `location_accuracy_meters` (DOUBLE PRECISION)
- `priority` (TEXT, Default: 'Medium') — 'Low', 'Medium', 'High', 'Critical'
- `status` (TEXT, Default: 'Submitted') — 'Submitted', 'Assigned', 'In Progress', 'Resolved', 'Closed'
- `assigned_team` (TEXT, Nullable)
- `ai_confidence` (NUMERIC(4,3)) — Confidence score (0.000 to 1.000)
- `ai_summary` (TEXT)
- `ai_reason` (TEXT)
- `created_at` (TIMESTAMPTZ, Default: `now()`)
- `updated_at` (TIMESTAMPTZ, Default: `now()`)

### `public.complaint_status_history`
Chronological timeline of lifecycle transitions for transparency.
- `id` (UUID, Primary Key)
- `complaint_id` (UUID, NOT NULL, FK -> `complaints.id` ON DELETE CASCADE)
- `status` (TEXT, NOT NULL)
- `note` (TEXT)
- `assigned_team` (TEXT)
- `created_at` (TIMESTAMPTZ, Default: `now()`)

### `public.complaint_updates`
Official broadcast updates published by municipal officers to citizens.
- `id` (UUID, Primary Key)
- `complaint_id` (UUID, NOT NULL, FK -> `complaints.id` ON DELETE CASCADE)
- `message` (TEXT, NOT NULL)
- `official_name` (TEXT, NOT NULL) — Department or designated nodal officer
- `created_at` (TIMESTAMPTZ, Default: `now()`)

### `public.complaint_evidence`
Media attachments uploaded by citizens or inspectors.
- `id` (UUID, Primary Key)
- `complaint_id` (UUID, NOT NULL, FK -> `complaints.id` ON DELETE CASCADE)
- `file_url` (TEXT, NOT NULL) — Public or signed URL in `complaint-evidence`
- `file_name` (TEXT, NOT NULL)
- `file_size` (BIGINT, NOT NULL) — Max 10,485,760 bytes (10MB)
- `mime_type` (TEXT, NOT NULL) — 'image/jpeg', 'image/png', 'image/webp'
- `created_at` (TIMESTAMPTZ, Default: `now()`)

### `public.notifications`
Citizen and staff alerts for status milestones.
- `id` (UUID, Primary Key)
- `user_id` (UUID, Nullable, FK -> `auth.users.id`)
- `complaint_id` (UUID, Nullable, FK -> `complaints.id` ON DELETE SET NULL)
- `title` (TEXT, NOT NULL)
- `message` (TEXT, NOT NULL)
- `type` (TEXT, Default: 'status_update')
- `is_read` (BOOLEAN, Default: `false`)
- `created_at` (TIMESTAMPTZ, Default: `now()`)

### `public.emergency_services`
Official 24x7 emergency and rapid civic response contacts.
- `id` (UUID, Primary Key)
- `name` (TEXT, NOT NULL)
- `description` (TEXT)
- `phone_number` (TEXT, NOT NULL)
- `alternate_phone` (TEXT)
- `icon` (TEXT)
- `is_active` (BOOLEAN, Default: `true`)
- `sort_order` (INT, Default: 0)

### `public.civic_updates`
Public announcements and civic advisories for Nagpur residents.
- `id` (UUID, Primary Key)
- `title` (TEXT, NOT NULL)
- `description` (TEXT, NOT NULL)
- `category` (TEXT, NOT NULL)
- `priority` (TEXT, Default: 'normal') — 'low', 'normal', 'high', 'urgent'
- `ward_id` (UUID, Nullable, FK -> `wards.id`)
- `published_at` (TIMESTAMPTZ, Default: `now()`)
- `expires_at` (TIMESTAMPTZ)
- `is_active` (BOOLEAN, Default: `true`)

### `public.admin_audit_logs`
Immutable forensic audit trail of all staff administrative interventions.
- `id` (UUID, Primary Key)
- `admin_id` (UUID, Nullable, FK -> `auth.users.id`)
- `admin_email` (TEXT, NOT NULL)
- `action` (TEXT, NOT NULL) — 'STATUS_CHANGE', 'ASSIGN_TEAM', 'PRIORITY_UPDATE', 'DELETE_COMPLAINT'
- `complaint_id` (UUID, Nullable)
- `details` (JSONB)
- `ip_address` (TEXT)
- `created_at` (TIMESTAMPTZ, Default: `now()`)

---

## 3. Database Constraints & Triggers

1. **Status Transition Enforcement (`validate_complaint_status_transition`)**:
   - `Submitted` -> `Assigned`, `In Progress`
   - `Assigned` -> `In Progress`, `Resolved`
   - `In Progress` -> `Resolved`
   - `Resolved` -> `Closed`, `In Progress` (reopening)
   - `Closed` is terminal and cannot transition backwards.

2. **Audit Log Immutability (`prevent_audit_log_modification`)**:
   - Blocks any SQL `UPDATE` or `DELETE` on `public.admin_audit_logs`.

3. **Input Range & Length Validation**:
   - Complaint text length constraints (5 - 4000 characters).
   - Evidence file size (<= 10MB) and allowed image MIME types.
   - Latitude (-90 to +90) and Longitude (-180 to +180) checks.
