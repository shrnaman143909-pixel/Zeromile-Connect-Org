# Zeromile Connect — Security Architecture & Guidelines

Zeromile Connect is designed with a defense-in-depth security model to safeguard municipal operations, protect citizen privacy, and enforce tamper-proof audit trails.

---

## 1. Row-Level Security (RLS) Policies

All PostgreSQL tables have Row-Level Security explicitly enabled (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY`).

| Table | SELECT Policy | INSERT Policy | UPDATE / DELETE Policy |
| :--- | :--- | :--- | :--- |
| `profiles` | Own profile or Admin/Staff | Authenticated user for own ID | Own profile or Admin |
| `user_roles` | Own role or Admin | Service role / Admin only | Service role / Admin only |
| `departments` | Public (Read-only) | Admin only | Admin only |
| `wards` | Public (Read-only) | Admin only | Admin only |
| `services` | Public (Read-only) | Admin only | Admin only |
| `complaints` | Public / Citizen own / Admin | Any user or citizen | Admin or Staff only |
| `complaint_status_history` | Public / Citizen / Admin | Admin or Staff only | Read-only (Immutable) |
| `complaint_updates` | Public / Citizen / Admin | Admin or Staff only | Admin or Staff only |
| `complaint_evidence` | Public / Citizen / Admin | Complaint owner or Admin | Admin only |
| `notifications` | Targeted citizen (`user_id`) | System / Admin | Citizen (marks `is_read`) |
| `admin_audit_logs` | Admin only | Admin / System triggers | **IMMUTABLE** (No UPDATE/DELETE) |
| `emergency_services` | Public (Read-only) | Admin only | Admin only |
| `civic_updates` | Public (Read-only) | Admin only | Admin only |

---

## 2. Secrets & Credential Management

- **Public Anon Key**: The mobile client interacts exclusively using Supabase's publishable `anon` key (`SUPABASE_PUBLISHABLE_KEY`). This key respects all Row-Level Security policies.
- **Service Role Key**: The Supabase `service_role` secret key is **never bundled** or exposed in the client application or APK.
- **Environment Ingestion**: API keys and backend URLs are managed via `.env` files and injected into the build system via `BuildConfig` using Gradle Secrets.
- **Zero Hardcoded Passwords**: Municipal staff and administrator accounts are authenticated exclusively through Supabase Auth (GoTrue), verifying credentials server-side and returning signed JWTs.

---

## 3. Storage Security (`complaint-evidence`)

- **Strict MIME Type Filtering**: The bucket rejects any uploaded objects that are not `image/jpeg`, `image/png`, or `image/webp`.
- **Payload Size Restriction**: Hard limit of 10 MB per image to prevent denial-of-service or storage exhaustion attacks.
- **Partitioned File Paths**: Images are organized in folders matching the complaint ID: `complaints/{complaint_number}/{uuid}.{ext}`.

---

## 4. Audit Log Immutability & Forensic Integrity

- The `public.admin_audit_logs` table records every administrative state mutation (status progression, team assignment, priority change, resolution note, or complaint deletion).
- A PostgreSQL database trigger (`prevent_audit_log_modification`) intercepts and aborts any attempt to modify or delete audit log rows, guaranteeing non-repudiation and regulatory compliance.

---

## 5. Citizen Privacy & Data Protection

- **Anonymity Support**: Citizens may register grievances as guests or without linking persistent identity. Complaints default to `user_id = NULL` when submitted anonymously.
- **Location Precision**: GPS coordinates are optional; citizens may submit landmark-based descriptions without revealing real-time location.
- **Sanitized Seed Data**: Public repository releases and testing fixtures contain only fictional Nagpur landmarks and mock incident numbers (`NMC-2026-XXXXXX`).
