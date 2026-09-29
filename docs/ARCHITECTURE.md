# Zeromile Connect — System Architecture

Zeromile Connect is an intelligent, bilingual citizen-to-municipality digital grievance and civic service coordination platform specifically engineered for the city of Nagpur, Maharashtra.

---

## 1. High-Level Architecture Overview

Zeromile Connect employs a modern, layered Android architecture powered by Jetpack Compose, Kotlin Coroutines & Flow, Supabase (PostgreSQL, PostgREST, Auth, Realtime, Storage), and Google Gemini AI.

```
+---------------------------------------------------------------------------------+
|                                USER INTERFACE LAYER                             |
|                                (Jetpack Compose)                                |
|                                                                                 |
|  +-----------------------------------+    +----------------------------------+  |
|  |           Citizen Screens         |    |         Admin / Staff UI         |  |
|  |  * Home & Quick Access            |    |  * Secure Role-Gated Login       |  |
|  |  * AI Voice / Multilingual Intake |    |  * Real-Time Metrics Dashboard   |  |
|  |  * Dynamic Complaint Form Engine  |    |  * Filterable Complaint Triage   |  |
|  |  * Real-Time Activity & Tracking  |    |  * Assignment & Workflow Modal   |  |
|  |  * Emergency & Civic Updates      |    |  * Civic Updates Publisher       |  |
|  +-----------------------------------+    +----------------------------------+  |
|                                    |                        |                   |
+------------------------------------|------------------------|-------------------+
                                     v                        v
+---------------------------------------------------------------------------------+
|                               VIEWMODEL LAYER                                   |
|  * MainViewModel: Citizen state, audio record flow, dynamic form validation     |
|  * AdminViewModel: Admin auth, complaint triage, team dispatch, audit logging   |
|  * Reactive StateFlow / SharedFlow UI state emission (UiState<T>)              |
+---------------------------------------------------------------------------------+
                                     |
                                     v
+---------------------------------------------------------------------------------+
|                               REPOSITORY LAYER                                  |
|  * CivicRepository: PostgREST queries, complaints, services, wards, emergency   |
|  * AdminRepository: Role validation, status transitions, audit logs             |
|  * NotificationRepository: Push notifications, unread tracking                  |
+---------------------------------------------------------------------------------+
                 |                                      |
                 v                                      v
+----------------------------------+  +-------------------------------------------+
|          AI ENGINE               |  |              SUPABASE BACKEND             |
|  * GeminiCivicClassifier         |  |  * PostgREST 12 (REST API via Retrofit)   |
|  * Audio & Text Intake           |  |  * Supabase Auth (GoTrue JWT)             |
|  * Marathi / Hindi / English     |  |  * Realtime Engine (WebSockets)           |
|  * Category & Ward Prediction    |  |  * Supabase Storage (complaint-evidence)  |
|  * Urgency & Priority Detection  |  |  * PostgreSQL 15 + RLS + Triggers         |
+----------------------------------+  +-------------------------------------------+
```

---

## 2. Component Architecture

### A. Presentation Layer (Jetpack Compose)
- **Single Activity Model**: `MainActivity.kt` acts as the root entry point, hosting top-level composition with Edge-to-Edge display support (`enableEdgeToEdge()`).
- **Responsive Layout System**: Uses `BoxWithConstraints` to adaptively switch layouts:
  - Mobile displays (`< 768.dp`): Vertical card stacks, optimized touch targets (minimum 44.dp), accessible font scales.
  - Wide displays (`>= 768.dp`): Side-by-side metric dashboards, horizontal data grids, dual-column panels.
- **Design System (`ZeromileComponents.kt`)**: Consistent civic design token library for buttons, cards, status badges, text inputs, headers, and modal dialogs adhering to Material Design 3 (M3).

### B. Business Logic & State Management
- **`MainViewModel`**:
  - Manages citizen active tab navigation (`Home`, `AI Assistant`, `Activity`, `Services`, `Profile`).
  - Manages voice recording states (`IDLE`, `LISTENING`, `PROCESSING`, `CLASSIFIED`).
  - Coordinates dynamic form field data entry, location capture, and evidence attachment.
  - Subscribes to real-time complaint updates and push alerts.
- **`AdminViewModel`**:
  - Manages staff session tokens, role checks (`admin`, `staff`), and audit tracking.
  - Handles administrative filtering (status, priority, search keywords, ward, date range).
  - Enforces complaint lifecycle transitions and dispatches municipal operational units.

---

## 3. Gemini AI Civic Intelligence Layer

The AI subsystem (`GeminiCivicClassifier.kt`) provides automatic understanding of citizen complaints in real time:

1. **Multilingual Processing**: Accepts transcripts or prompts in Marathi (`mr-IN`), Hindi (`hi-IN`), or Indian English (`en-IN`).
2. **Context-Aware Classification**: Matches user-reported issues against verified Nagpur Municipal Corporation (NMC) service categories and departments:
   - Roads & Potholes (`Roads`)
   - Water Supply, Drainage & Tankers (`Water`)
   - Solid Waste & Garbage Overflow (`Waste Management`)
   - Noise Pollution & Industrial Decibels (`Pollution`)
   - Traffic Signals & Obstructions (`Traffic`)
   - Food Safety & Hotel Hygiene (`Food Safety`)
   - Emergency Hazards, Fallen Trees & Flash Flooding (`Emergency Services`)
3. **Structured JSON Output**: Extracts:
   - `category_name`: Target service classification.
   - `priority`: `Low`, `Medium`, `High`, or `Critical`.
   - `summary`: Concise one-line civic grievance summary.
   - `reasoning`: Technical justification for administrative triage.
   - `confidence`: Statistical score (`0.0` to `1.0`).

---

## 4. Real-Time WebSocket Architecture

Zeromile Connect utilizes real-time PostgreSQL change capture via `SupabaseRealtimeClient.kt`:

- Connects over WSS to `/realtime/v1/websocket`.
- Subscribes to `realtime:public:complaints`, `realtime:public:complaint_updates`, and `realtime:public:notifications`.
- Heartbeat loop maintains persistent connectivity (`phx_join` protocol).
- Updates trigger immediate UI recomposition via Kotlin `SharedFlow` streams without requiring manual citizen refresh.

---

## 5. Storage & Evidence Architecture

- **Bucket**: `complaint-evidence`
- **File Types Allowed**: `image/jpeg`, `image/png`, `image/webp`
- **Maximum File Size**: 10 MB per file
- **Path Hierarchy**: `complaints/{complaint_number}/{uuid}.{ext}`
- **Security**: Strict Row-Level Security (RLS) policies prevent unauthorized overwrites; public read access is enabled for verified grievance evidence review.
