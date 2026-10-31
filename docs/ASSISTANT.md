# Zeromile Assistant — conversational chat

The `feat/assistant-chat` change adds a multi-turn assistant to the citizen app. It is
deliberately separate from the existing AI intake pipeline: the AI tab turns one utterance into
one complaint ticket, while the assistant holds a conversation and can look things up.

## Entry points

- Home → **"Ask Zeromile AI a question"** (under the primary CTA).
- `NavTab.ASSISTANT` (not in the bottom bar, so the five primary tabs keep their space).

## Layers

| File | Role |
| :--- | :--- |
| `data/model/ChatModels.kt` | `ChatMessage`, `ChatRole`, `ChatAction`, `ChatUiState` |
| `ai/GeminiChatClient.kt` | `generateContent` transport: system instruction, multi-turn `contents`, function declarations, response parsing |
| `ai/ChatToolExecutor.kt` | Executes tool calls against `CivicRepository`; all read-only except a hand-off |
| `ui/viewmodel/ChatViewModel.kt` | Conversation state, tool loop, offline behaviour |
| `ui/screens/ChatScreen.kt` | Compose transcript, suggestion prompts, voice input, action buttons |

The ViewModel keeps two transcripts on purpose: render bubbles in `ChatUiState` and the Gemini
wire turns in a private `history` list (including `functionCall` / `functionResponse` pairs).

## Tools (function calling)

| Tool | Backing call | Effect |
| :--- | :--- | :--- |
| `find_service` | `CivicRepository.getServices()` | Ranks the NMC catalogue against a free-text issue, with Marathi/Hindi synonym expansion |
| `check_complaint_status` | `getComplaintTrackingDetails(number)` | Current status, department, assigned team, recent history |
| `list_my_complaints` | `getComplaints(userId)` | The citizen's five most recent complaints |
| `start_complaint` | none | Returns a `ChatAction` that opens the existing intake form pre-filled |

`start_complaint` never writes to Supabase. The chat cannot create, edit or resolve anything —
submission still happens through the audited `submitComplaint` flow.

## Model and key

`GeminiChatClient.DEFAULT_MODEL = "gemini-2.5-flash"`. The key comes from `BuildConfig.GEMINI_API_KEY`
(Gradle Secrets plugin → `.env`). If the key is unset the chat degrades to *limited mode*:
complaint numbers typed in the conversation are still resolved locally, and the UI says so
instead of pretending.

> Before this ships to real citizens, move the call behind a Supabase Edge Function so the key
> is not compiled into the APK. `GeminiChatClient` is the only class that would change.

## Verified

- `gradle :app:compileDebugKotlin` — clean.
- `gradle :app:assembleDebug` — debug APK builds.
- `gradle :app:testDebugUnitTest` — 44 tests pass, including 11 new ones in
  `ChatAssistantUnitTests` (ranking, request shaping, response parsing, complaint serialisation).

## Known gaps / next

1. Chat history is in-memory only (`ChatViewModel`). Persisting it needs a `chat_messages` table
   with owner-only RLS, or DataStore for a single-device transcript.
2. The assistant answers but cannot attach a photo or GPS fix itself — `start_complaint` hands
   those steps to the existing dynamic form, which is where they belong.
3. Voice input reuses `AndroidSpeechManager`; the assistant does not speak replies yet.
