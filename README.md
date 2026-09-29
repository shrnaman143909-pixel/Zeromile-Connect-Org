# Zeromile Connect

**A citizen-to-municipality digital service platform for Nagpur that combines AI-powered issue understanding, civic service discovery, complaint registration, real-time tracking, notifications, and municipal administration.**

---

## 1. Project Overview
Zeromile Connect bridges the communication gap between citizens of Nagpur and municipal authorities (Nagpur Municipal Corporation - NMC). By unifying multi-dialect voice input, automated Gemini AI categorization, real-time PostgreSQL replication, and responsive citizen and administrative interfaces, the platform empowers residents to report civic issues and track resolutions effortlessly.

## 2. Problem Statement
Urban civic grievances (such as potholes on Wardha Road, garbage collection delays in Dharampeth, or streetlight failures in Sitabuldi) often suffer from:
- Complex reporting procedures and confusing departmental jurisdictions.
- Language barriers for residents speaking Marathi or Hindi.
- Opaque tracking mechanisms leading to public distrust.
- Fragmented administrative backlogs without intelligent triage or accountability.

## 3. Solution
Zeromile Connect provides a cohesive digital grievance ecosystem:
- **Intelligent Intake**: Citizens speak or type naturally in Marathi, Hindi, or English.
- **Automated AI Routing**: Google Gemini models analyze complaints in real time, extract locations, classify target NMC departments, and compute urgency scores.
- **Transparent Accountability**: Unique complaint identifiers (`NMC-YYYY-XXXXXX`), live lifecycle stages, official municipal updates, and photographic evidence.
- **Integrated Administration**: Municipal staff triage issues with search, filtering, operational team assignments, and immutable audit logs.

## 4. Key Features
- **Multilingual Voice Intake**: Real-time speech input supporting Marathi (`mr-IN`), Hindi (`hi-IN`), and English (`en-IN`).
- **AI Classification Engine**: Automated department, category, and priority detection powered by Google Gemini.
- **Dynamic Form Engine**: Contextual forms tailored to specific civic categories with smart location recommendations.
- **Real-Time Lifecycle Tracking**: Live status transitions (`Submitted` -> `Assigned` -> `In Progress` -> `Resolved` -> `Closed`) synchronized over WebSockets.
- **Photo Evidence Upload**: Integrated photo attachment to Supabase Storage with client-side compression and MIME verification.
- **Nagpur Municipal Geo-Mapping**: Integrated ward resolution (including Ward 32 Dharampeth) and GPS coordinate capture.
- **Direct Emergency Directory**: Quick-dial access to verified 24x7 Nagpur emergency response desks (112, 108, NMC Disaster Cell, Fire, Police).
- **Public Civic Advisories**: Real-time broadcasts for scheduled water maintenance, road repairs, and weather alerts.
- **Secure Municipal Dashboard**: Role-based administrative portal for nodal officers with audit logging and dispute resolution.

---

## 5. Citizen Workflow
1. **Intake**: Citizen opens the app and either taps the microphone to speak or enters a text description.
2. **AI Analysis**: Gemini identifies the issue category (e.g., Pothole -> Roads Department), priority, and location.
3. **Form Refinement**: Citizen confirms details, selects local ward suggestions, and attaches photo evidence.
4. **Submission**: An official grievance ticket (`NMC-2026-XXXXXX`) is generated and logged in Supabase.
5. **Tracking**: The citizen monitors real-time progress on the **Activity** screen and receives notifications on status changes.

## 6. AI Workflow
1. Raw audio or text is captured and forwarded to `GeminiCivicClassifier`.
2. The classifier evaluates the text against verified NMC departments and service taxonomies.
3. A strictly formatted JSON response is produced:
   - `category_name`: Matched civic domain.
   - `priority`: `Low`, `Medium`, `High`, or `Critical`.
   - `summary`: Concise citizen-friendly problem statement.
   - `reasoning`: Triage justification for municipal dispatchers.
   - `confidence`: Confidence probability score.

## 7. Complaint Lifecycle
- **Submitted**: Grievance officially registered in the civic ledger.
- **Assigned**: Transferred to the relevant zonal municipal operational team (e.g., *Zone Road Repair Squad*).
- **In Progress**: Field workers or contractors actively deployed on-site.
- **Resolved**: Civic repair complete; verification note logged.
- **Closed**: Final citizen satisfaction confirmed; terminal status.

## 8. Municipal / Admin Workflow
1. **Authentication**: Administrative staff log in using verified Supabase credentials.
2. **Triage Dashboard**: View live breakdown charts, urgency counters, and departmental workloads.
3. **Filtering & Search**: Filter grievances by status, priority level, ward, or search keywords.
4. **Actionable Management**: Assign operational teams, record internal and public status updates, and resolve grievances.
5. **Audit Trail**: Every state modification generates an immutable audit record in `admin_audit_logs`.

---

## 9. Technology Stack
- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **Asynchronous Architecture**: Kotlin Coroutines & StateFlow
- **Backend as a Service**: Supabase (PostgreSQL 15, PostgREST 12, GoTrue Auth, Realtime, Storage)
- **AI / LLM**: Google Gemini API (`models/gemini-2.5-flash`)
- **Networking**: Retrofit 2, OkHttp 4, Kotlinx Serialization
- **Image Loading**: Coil 3
- **Testing**: JUnit 4, Robolectric, Kotlinx Coroutines Test

---

## 10. Architecture Overview
Refer to [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for full architectural diagrams and component hierarchies.

## 11. Supabase Architecture
Zeromile Connect uses Supabase as its primary backend:
- PostgREST for high-performance direct database queries.
- Supabase Auth for JWT-backed staff and citizen identity.
- Realtime PostgreSQL replication via WebSocket channels.
- Supabase Storage for secure grievance photo attachments.

## 12. Gemini / AI Architecture
- Direct client-to-API and server-side ready execution.
- Deterministic structured output parsing with strict fallback safety.
- Multi-language prompt engineering tailored to Marathi and Indian civic vernacular.

## 13. Authentication and Authorization
- **Citizens**: Seamless guest submission with optional authenticated profile linking.
- **Staff / Admins**: Dedicated password-based authentication via Supabase Auth with server-enforced role checks in `public.user_roles`.

## 14. Realtime Architecture
- Persistent WebSocket connection to Supabase Realtime (`realtime:public:complaints`, `realtime:public:complaint_updates`).
- Instantaneous UI updates without pull-to-refresh or polling overhead.

## 15. Evidence & Storage Architecture
- Bucket: `complaint-evidence`.
- File restrictions: `image/jpeg`, `image/png`, `image/webp` up to 10MB per file.
- Path structure: `complaints/{complaint_number}/{uuid}.{ext}`.

## 16. Location Support
- Real-time GPS coordinate capture with accuracy radius reporting.
- Localized Nagpur area suggestions (Dharampeth, Sitabuldi, Ramdaspeth, Civil Lines, Sadar, VIP Road).
- Direct linking to external map coordinates for municipal field crews.

## 17. Multilingual Support
- First-class support for English, Marathi (`mr-IN`), and Hindi (`hi-IN`).
- Language-specific audio transcription prompts and UI strings.

## 18. Emergency Services
- 24x7 directory featuring direct one-tap calling to:
  - National Emergency (112)
  - NMC Disaster & Civic Control Room (0712-2567030 / 1800-233-3764)
  - Fire Brigade (101)
  - Emergency Ambulance EMS (108)
  - City Police Control (100)

## 19. Civic Updates
- Dynamic broadcast notices published by NMC for planned water disruptions, monsoon road works, and local traffic diversions.

## 20. Responsive & Mobile Support
- Fluid adaptation across smartphone screens (`< 768.dp`) and tablet/desktop views (`>= 768.dp`).
- Fully accessible 48.dp minimum touch targets.

---

## 21. Local Development Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/zeromile-connect.git
   cd zeromile-connect
   ```
2. Configure environment credentials:
   ```bash
   cp .env.example .env
   ```
3. Set your credentials in `.env`:
   - `GEMINI_API_KEY`
   - `SUPABASE_URL`
   - `SUPABASE_PUBLISHABLE_KEY`

## 22. Environment Variables
See [.env.example](.env.example) for placeholder definitions.

## 23. Supabase Setup
Follow [docs/SETUP.md](docs/SETUP.md) for full database provisioning instructions.

## 24. Database Migrations
Migrations are tracked sequentially in `supabase/migrations/`:
- `20260101000000_zeromile_phase1_schema.sql`
- `20260102000000_zeromile_phase5_complaints.sql`
- `20260103000000_zeromile_phase6_tracking.sql`
- `20260104000000_zeromile_phase7_admin.sql`
- `20260105000000_zeromile_phase8_notifications.sql`
- `20260106000000_zeromile_phase9_evidence_storage_location.sql`
- `20260107000000_zeromile_phase10_emergency_services_updates.sql`
- `20260108000000_zeromile_phase11_security_hardening.sql`

## 25. Seed Data
Reference Nagpur municipal data is available at:
- `supabase/seed/seed.sql`

## 26. Development Commands
- **Run Unit Tests**: `gradle :app:testDebugUnitTest`
- **Build APK**: `gradle :app:assembleDebug`
- **Lint Check**: `gradle :app:lintDebug`

## 27. Production Build
To generate a release build:
```bash
gradle :app:assembleRelease
```

## 28. Security Notes
- Row-Level Security is strictly enforced across all database tables.
- Audit logs in `admin_audit_logs` are protected by immutability triggers.
- For complete security specifications, see [docs/SECURITY.md](docs/SECURITY.md).

## 29. Project Structure
```
zeromile-connect/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/zeromile/
│       │   │   ├── ai/               # Gemini AI Classifier
│       │   │   ├── data/             # Repositories, models & remote clients
│       │   │   ├── ui/               # Jetpack Compose UI screens & components
│       │   │   └── MainActivity.kt   # Root application activity
│       │   └── res/                  # Drawables, layouts, strings, values
│       └── test/                     # Local JVM unit & Robolectric tests
├── docs/                             # In-depth architectural & security docs
│   ├── ARCHITECTURE.md
│   ├── DATABASE.md
│   ├── SECURITY.md
│   └── SETUP.md
├── supabase/
│   ├── migrations/                   # Sequential PostgreSQL migrations
│   ├── seed/                         # Reference seed data for Nagpur
│   └── FULL_SETUP.sql                # Complete idempotent setup script
├── .env.example                      # Template environment variables
├── .gitignore                        # Git exclusion rules
├── CONTRIBUTING.md                   # Contribution guidelines
└── README.md                         # Main project documentation
```

## 30. Demo Workflow
1. Launch app -> Tap microphone icon on **AI Assistant** tab.
2. Speak in Marathi: *"Dharampeth madhe rastyavar mothe khadde padlet"* or English: *"Large pothole on Wardha Road near VNIT gate"*.
3. Gemini automatically categorizes the grievance under **Roads**, selects **High** priority, and generates a concise summary.
4. Tap **Review & Submit** -> Ticket `NMC-2026-XXXXXX` is generated.
5. Switch to **Admin Portal** -> Log in -> Triage ticket, assign **Zone Road Repair Squad**, and mark **In Progress**.
6. Switch back to **Citizen Activity** -> Notice real-time status update to **In Progress** with official dispatch note.

## 31. Future Enhancements
- Offline synchronization via Room database queue with opportunistic cloud syncing.
- Drone / GIS satellite imagery integration for rapid flood and road surface assessment.
- WhatsApp Business API chatbot integration for automated complaint intake.
