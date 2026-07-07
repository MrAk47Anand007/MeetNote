# MeetNote Android V1 Design

Date: 2026-07-07
Scope: Android-first, offline-first meeting assistant with local-first transcription and summarization, plus explicit fallback to smaller local models and user-approved cloud providers.

## 1. Goal

MeetNote is a multiplatform product vision spanning Android, iOS, Windows, and macOS, but this design focuses only on Android v1.

Android v1 should:

- detect meeting context from calendar, email, or manual session start
- capture meeting audio from Android devices as reliably as the platform allows
- generate transcript, MoM, summary, action items, decisions, blockers, and follow-up drafts
- run fully local when the device is compatible
- fall back first to smaller local models
- ask for provider API keys only when local quality or speed is not sufficient

The primary product goal is an offline-first Android meeting assistant that works across meeting apps in a generic way rather than through deep provider-specific SDK integrations in v1.

## 2. Product Boundary

MeetNote Android v1 is an offline-first meeting assistant for phones.

Its responsibilities are:

- identify the current or upcoming meeting
- create a local meeting session
- capture meeting audio using the best available Android path
- transcribe the meeting with local-first processing
- summarize the meeting with local-first LLM inference
- store results locally
- let the user export or share results

Its non-goals for v1 are:

- deep native Zoom, Google Meet, or Microsoft Teams SDK integrations
- automatic bot joining
- desktop sync
- team collaboration or shared workspaces
- enterprise administration
- cross-platform implementation for iOS, Windows, or macOS in this phase

For v1, "support any meeting app" means MeetNote should support generic meeting capture and meeting context handling on Android, not guarantee special direct integration with every meeting provider.

## 3. Guiding Principles

- offline-first by default
- no silent cloud usage
- explicit user approval before any provider fallback
- degrade gracefully before failing
- preserve user trust with visible processing mode labels
- keep audio capture, transcription, and summarization isolated so each layer can fail independently

## 4. Core Architecture

Android v1 should be organized into the following modules.

### 4.1 Session Manager

Responsibilities:

- create sessions from calendar, email, or manual start
- manage meeting lifecycle states such as idle, scheduled, capturing, transcribing, summarizing, completed, and failed
- attach all transcript and note artifacts to a single session record

### 4.2 Context Connectors

Responsibilities:

- read calendar metadata
- read email metadata relevant to meetings
- infer meeting title, time, likely provider, and participants
- support manual override when context detection is incomplete

### 4.3 Audio Engine

Responsibilities:

- attempt playback or system-audio capture
- fall back to microphone capture when playback capture is blocked or poor
- buffer and chunk audio safely for later processing
- store raw audio locally for recovery and deferred processing

### 4.4 Transcription Engine

Responsibilities:

- run local ASR first
- switch to smaller local ASR when needed
- offer cloud transcription only after explicit approval
- merge chunk transcripts into a clean session transcript

### 4.5 LLM Engine

Responsibilities:

- run local summarization using LiteRT-LM-compatible models
- generate structured meeting outputs
- support hierarchical summarization for long meetings

Outputs:

- live bullet notes when supported
- final summary
- MoM
- action items
- decisions
- blockers or risks
- follow-up email or message drafts

### 4.6 Capability Profiler

Responsibilities:

- check RAM tier
- check storage availability
- check supported ABI and runtime compatibility
- observe thermal pressure, battery level, and latency behavior
- choose the right local model tier

### 4.7 Fallback Orchestrator

Responsibilities:

- decide whether to stay on the primary local path
- downgrade to smaller local models
- switch from live mode to post-meeting processing mode
- ask for provider fallback when the local path is not good enough

### 4.8 Local Vault

Responsibilities:

- store recordings, transcripts, summaries, exports, settings, and model metadata
- encrypt sensitive data at rest
- store provider keys securely only when the user has opted in

### 4.9 Export And Share Layer

Responsibilities:

- export notes as markdown, plain text, and PDF
- support copy or share actions
- prepare future integrations without coupling them into v1 core logic

## 5. Runtime Flow

The default Android v1 flow is:

1. The user opens MeetNote.
2. MeetNote shows upcoming meeting sessions from calendar or email, and also supports manual session start.
3. The app profiles the device and selects the primary local model tier.
4. When the meeting begins, MeetNote attempts playback or system-audio capture.
5. If playback capture is blocked or low quality, it falls back to microphone capture.
6. Audio is buffered into chunks and stored locally.
7. The transcription engine runs local ASR on the chunks.
8. If local ASR is too slow or unstable, the app switches to a smaller local ASR model.
9. If transcript quality remains poor, the app asks the user whether to enable provider transcription.
10. Transcript chunks are cleaned and merged.
11. The LLM engine generates live or final notes depending on device capacity and session mode.
12. Outputs are stored locally and can be exported or shared.

Processing mode rules:

- users can force local-only mode
- fallback must always be explicit
- generated artifacts should indicate whether they were created using primary local, smaller local, or provider-assisted processing

## 6. Operating Modes

Android v1 should support two operating modes.

### 6.1 Live Assist Mode

Use when the device can sustain live capture, local ASR, and periodic summarization.

Behavior:

- capture audio during the meeting
- produce live or near-live transcript chunks
- generate rolling bullet notes
- generate final structured output at the end

### 6.2 Record Then Process Mode

Use when the device is under RAM, thermal, battery, or latency pressure, or when the user explicitly chooses a safer low-power flow.

Behavior:

- focus on reliable full-session audio capture during the meeting
- defer heavy transcription and summarization until the meeting ends
- generate transcript cleanup, summary, MoM, and action items after capture is complete

This mode is not just a failure path. It is a first-class low-power fallback for borderline-compatible 8GB devices and long meetings.

## 7. Model Strategy For 8GB Plus Android Phones

Android phones with 8GB RAM should be treated as compatible but constrained. The system should not assume one large model can reliably handle both speech and language tasks live throughout long meetings.

### 7.1 Model Separation

Use separate model stacks for:

- ASR
- summarization and note generation

This allows the app to downgrade ASR or LLM behavior independently.

### 7.2 Summary Model Tiers

- Tier A: primary local summary model using LiteRT-LM
- Tier B: smaller fallback local summary model
- Tier C: provider model fallback after explicit user approval

### 7.3 ASR Tiers

- local ASR primary model
- smaller local ASR fallback model
- optional provider transcription fallback after explicit user approval

### 7.4 Device Gating

- 8GB phones: default to smaller local models, shorter context windows, and more aggressive fallback to Record Then Process Mode
- 12GB or higher phones: allow stronger local models, richer context retention, and more live summarization

### 7.5 LiteRT-LM Positioning

LiteRT-LM should be used as the preferred local LLM runtime direction for Android v1 because it aligns with the current Google AI Edge path and supports broader cross-platform growth later.

## 8. Android Platform Reality

The product must explicitly account for Android platform limits.

### 8.1 Audio Capture Limits

Playback or system-audio capture is not universally guaranteed across all devices and apps. Some meeting apps or device policies may restrict clean capture.

Design implication:

- treat playback capture as best-effort
- provide microphone fallback
- clearly surface mode and quality implications to the user

### 8.2 Real Device Requirement

Local LLM and capture behavior must be validated on real devices, not just emulators. Emulator behavior is not sufficient for performance or audio reliability claims.

## 9. V1 Feature Set

Android v1 includes:

- meeting detection from calendar and email
- manual session start
- best-effort playback or system-audio capture
- microphone fallback capture
- chunked local-first transcription
- local-first summarization and structured note generation
- MoM
- summary
- action items
- decisions
- blockers or risks
- follow-up draft generation
- automatic device capability-aware model selection
- local-only privacy mode
- explicit provider fallback approval flow
- session history
- local export in markdown, plain text, and PDF
- model management for downloading, removing, and selecting local models

Deferred beyond v1:

- deep Zoom, Teams, and Google Meet native SDK integrations
- automatic meeting bot joining
- shared team spaces
- cross-device sync
- enterprise controls
- full iOS, Windows, and macOS implementations

## 10. Failure Handling

### 10.1 System Audio Capture Failure

Problem:

- playback capture blocked or poor quality

Fallback:

- switch to mic capture
- display reduced fidelity warning

### 10.2 Thermal Or RAM Pressure

Problem:

- live inference becomes unstable over longer meetings

Fallback:

- switch to smaller local models
- reduce summarization frequency
- switch to Record Then Process Mode when needed

### 10.3 Transcript Quality Drift

Problem:

- overlap, accents, and noise reduce local ASR quality

Fallback:

- try smaller local ASR if the primary model is unstable
- ask for provider transcription approval if quality remains unacceptable

### 10.4 Battery Drain

Problem:

- continuous capture plus inference is expensive

Fallback:

- low-power mode
- end-of-meeting processing rather than continuous live assistance

### 10.5 Context Window Pressure

Problem:

- long meetings exceed practical on-device context limits

Fallback:

- rolling chunk summaries
- hierarchical final summarization

### 10.6 Trust And Privacy Risk

Problem:

- invisible cloud usage would break trust

Safeguards:

- never silently switch to cloud
- always show current processing mode
- allow users to force local-only mode

### 10.7 Partial Failure Preservation

If a session degrades or fails, MeetNote should preserve:

- raw captured audio
- partial transcript
- partial notes if available
- failure reason and processing mode metadata

## 11. Testing Strategy

Android v1 validation must happen on real phones.

### 11.1 Required Device Matrix

- one 8GB Android phone
- one 12GB or higher Android phone

### 11.2 Required Meeting Scenarios

- Google Meet
- Zoom
- Microsoft Teams
- manual audio-record session
- short meeting under 15 minutes
- long meeting over 45 minutes
- Live Assist Mode
- Record Then Process Mode
- local-only mode
- provider-fallback-approved mode

### 11.3 Success Criteria

The v1 design is successful if it demonstrates:

- reliable full-session capture in common Android meeting scenarios
- local-first transcript and summary generation on compatible phones
- graceful downgrade without losing the session
- useful MoM, summary, and action outputs
- visible and trustworthy privacy behavior
- durable storage of raw audio and results for long sessions

## 12. Recommended Build Direction

The recommended build direction for Android v1 is a hybrid local-first architecture:

- local capture first
- local transcription first
- smaller local model fallback second
- explicit provider fallback third

This is the best fit for the product vision because it keeps privacy and offline capability central without depending on perfect on-device performance across every Android phone and meeting app.

## 13. Open Implementation Notes

The next planning phase should decide:

- exact Android app stack and UI architecture
- specific ASR runtime and model candidates
- exact LiteRT-LM-compatible local LLM candidates
- session storage schema
- background processing and foreground service strategy
- export format implementation details
- provider fallback onboarding UX and key storage flow

## 14. Source Notes For Technical Direction

This design is informed by the current official Android and Google AI Edge direction as of 2026-07-07:

- LiteRT-LM is Google AI Edge's production-ready orchestration layer for on-device LLM execution across Android and other platforms
- LiteRT-LM has an Android guide and is the preferred local LLM direction for this project
- Android system and playback audio capture is possible through MediaProjection and playback capture APIs, but compatibility varies by app and device

These source notes guide technical feasibility, but exact implementation details still need to be validated through prototype work on real Android devices.
