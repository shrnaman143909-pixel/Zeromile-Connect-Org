# Zeromile Connect — Developer Setup Guide

This guide describes how to configure, build, and deploy Zeromile Connect from a fresh clone of the repository.

---

## 1. Prerequisites

Ensure you have the following installed on your development workstation:

- **Java Development Kit (JDK)**: OpenJDK 17 or 21
- **Android SDK**: Build Tools 34.0.0+, Android API 34 SDK platform
- **Gradle**: 8.7+ (or use system Gradle)
- **Supabase Account**: A free Supabase Cloud project or local Supabase CLI instance
- **Google Gemini API Key**: From Google AI Studio

---

## 2. Environment Configuration

1. Copy `.env.example` to `.env` in the root directory:
   ```bash
   cp .env.example .env
   ```
2. Edit `.env` and fill in your actual credentials:
   ```properties
   # Gemini API Key for AI speech understanding & classification
   GEMINI_API_KEY=your_actual_gemini_api_key

   # Supabase Project Configuration
   SUPABASE_URL=https://your-project-id.supabase.co
   SUPABASE_PUBLISHABLE_KEY=your_supabase_anon_public_key
   ```

---

## 3. Supabase Database Provisioning

You can set up the database using either the modular migrations or the unified setup script.

### Method A: Modular Migrations (Recommended)
Apply the SQL migrations located in `supabase/migrations/` sequentially using the Supabase CLI or SQL Editor:
1. `20260101000000_zeromile_phase1_schema.sql` (Core tables, wards, services, RLS)
2. `20260102000000_zeromile_phase5_complaints.sql` (Complaints ledger, number generation)
3. `20260103000000_zeromile_phase6_tracking.sql` (Status history, public updates)
4. `20260104000000_zeromile_phase7_admin.sql` (Admin audit logs, teams, views)
5. `20260105000000_zeromile_phase8_notifications.sql` (Notification engine)
6. `20260106000000_zeromile_phase9_evidence_storage_location.sql` (Storage & coordinates)
7. `20260107000000_zeromile_phase10_emergency_services_updates.sql` (Emergency directory & broadcasts)
8. `20260108000000_zeromile_phase11_security_hardening.sql` (Integrity triggers & RLS hardening)

### Method B: Unified SQL Script
Alternatively, run the complete idempotent setup script:
- Copy contents of `supabase/FULL_SETUP.sql` into the Supabase Dashboard SQL Editor and execute.

### Seed Reference Data
Load Nagpur departments, wards, and services by running:
- `supabase/seed/seed.sql`

---

## 4. Supabase Storage Configuration

1. In the Supabase Dashboard, navigate to **Storage** -> **New Bucket**.
2. Name the bucket: `complaint-evidence`.
3. Set **Public Bucket** to `true` (or configure signed URL access).
4. Restrict allowed MIME types: `image/jpeg, image/png, image/webp`.
5. Restrict maximum file upload size: `10MB`.

---

## 5. Build & Test Commands

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```

### Build Debug APK
```bash
gradle :app:assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Lint Check
```bash
gradle :app:lintDebug
```
