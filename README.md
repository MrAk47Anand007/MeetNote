# MeetNote

Offline-first Android meeting notes, built for real-world meeting capture constraints.

MeetNote is an Android-first meeting assistant that aims to capture audio, preserve trust, and generate structured meeting outputs without defaulting to silent cloud usage. The current codebase is a Kotlin Multiplatform foundation with an Android shell, local session persistence, mic and playback-capture scaffolding, and a clear path toward local-first transcription and summarization.

## Why This Exists

Most meeting note tools assume a perfect integration surface: deep provider SDK access, reliable bot joins, or always-available cloud inference. MeetNote takes a different route:

- Android-first
- offline-first
- local-first processing
- explicit fallback instead of silent cloud handoff
- graceful degradation when playback capture, RAM, battery, or model capacity become limiting

The product goal is simple: help users capture and summarize meetings across common Android meeting apps while staying honest about platform limits and privacy tradeoffs.

## Current Status

This repository is in the early Android v1 foundation stage.

Implemented today:

- Kotlin Multiplatform + Gradle multi-module project bootstrap
- shared domain model for meeting sessions and processing policies
- SQLDelight-backed local session persistence
- Android app shell with Compose and Koin
- manual session creation flow
- capture source selection in the UI
- MediaProjection playback-capture consent coordination shell
- real microphone PCM capture with persistence-safe start/stop behavior
- consent-backed playback PCM capture path
- post-meeting WorkManager processing handoff
- durable transcript artifact persistence
- local transcription-engine seam with explicit unavailable-local fallback

Not implemented yet:

- physical-device validation across target meeting apps
- actual bundled ASR runtime integration
- LiteRT-LM or other on-device summary model integration
- provider fallback onboarding
- export pipelines for markdown, text, or PDF
- calendar and email meeting context ingestion

## Product Direction

MeetNote Android v1 is designed around two operating modes:

- `Live Assist`: for devices that can handle live capture and incremental processing
- `Record Then Process`: for safer, lower-power, post-meeting workflows

The preferred capture path is:

1. Request playback/system-audio capture when Android allows it
2. Fall back to microphone capture when playback capture is unavailable, denied, or blocked
3. Preserve session state and raw artifacts so later processing can still succeed

The preferred processing path is:

1. local ASR first
2. smaller local models second
3. explicit provider fallback only after user approval

## Repository Structure

```text
MeetNote/
|-- androidApp/           # Compose Android shell and Activity wiring
|-- android-core/         # Android dependency graph and SQLDelight driver setup
|-- android-capture/      # Recorder contracts and capture implementations
|-- android-background/   # Foreground service and post-meeting orchestration
|-- android-security/     # Secure storage/provider key abstractions
|-- android-ai-local/     # Local AI runtime module scaffold
|-- android-asr/          # Android ASR seam and local fallback implementation
|-- shared/core/          # Core result and identifier types
|-- shared/domain/        # Session models, policies, repository interfaces, use cases
|-- shared/storage/       # SQLDelight storage and repository implementation
|-- shared/ai-contracts/  # Transcription and summary contracts
|-- shared/providers/     # Future provider fallback integrations
|-- shared/export/        # Future export layer
|-- docs/superpowers/     # Design docs, plans, and handoffs
```

## Architecture Notes

### Shared Modules

- `shared:core` contains basic result and ID types
- `shared:domain` models meeting sessions, processing modes, policies, and use cases
- `shared:storage` stores session metadata locally via SQLDelight
- `shared:ai-contracts` defines future transcription and summarization boundaries

### Android Modules

- `androidApp` renders the current session UI and permission flow
- `android-core` wires Android-specific dependencies with Koin
- `android-capture` owns capture contracts plus microphone and playback PCM recording
- `android-background` hosts the foreground-service capture shell and transcript-artifact processing
- `android-asr` currently exposes the transcription seam and a truthful local-unavailable fallback
- `android-security` and `android-ai-local` remain scaffolds for the next phases

## Current User Flow

The app currently supports a thin but real shell for the main capture path:

1. Create a manual meeting session
2. Choose `Live Assist` or `Record Then Process`
3. Choose `Playback Audio` or `Microphone`
4. Request playback capture permission through MediaProjection
5. Fall back to microphone capture if playback capture is denied or unsupported
6. Persist session state locally and enqueue post-meeting processing for record-first sessions
7. Write a durable transcript artifact that records either transcript output or why local transcription is unavailable

## Tech Stack

- Kotlin
- Kotlin Multiplatform
- Jetpack Compose
- Koin
- SQLDelight
- Coroutines
- Android MediaProjection APIs
- Gradle Kotlin DSL

## Getting Started

### Prerequisites

- Android Studio with a recent Android SDK
- JDK 17
- Android SDK 35

### Build

```powershell
.\gradlew.bat :androidApp:compileDebugKotlin
```

### Run Unit Tests

```powershell
.\gradlew.bat :androidApp:testDebugUnitTest
.\gradlew.bat :android-capture:testDebugUnitTest
.\gradlew.bat :android-background:testDebugUnitTest
```

### Useful Focused Checks

```powershell
.\gradlew.bat :androidApp:testDebugUnitTest --tests com.meetnote.android.ui.session.SessionViewModelTest
.\gradlew.bat :android-background:testDebugUnitTest --tests com.meetnote.android.background.DefaultPostMeetingProcessingExecutorTest
.\gradlew.bat :androidApp:compileDebugKotlin
```

## Known Constraints

- Playback capture on Android is best-effort, not guaranteed across apps and devices
- Real validation needs physical Android devices, not only emulators
- The project currently emits the known Kotlin Multiplatform and AGP `8.6.1` compatibility warning
- Some Gradle tasks also emit an existing Gradle 9 deprecation warning

These warnings have not blocked the compile and test targets used so far.

## Roadmap

Near-term priorities:

- complete the playback audio recorder implementation
- pass MediaProjection state safely into the foreground service runtime
- replace the fallback transcription engine with a bundled local ASR runtime
- add local summarization integration
- add export formats and session history polish
- add calendar and email meeting context ingestion

## Design Docs

- [Android v1 design](docs/superpowers/specs/2026-07-07-meetnote-android-v1-design.md)
- [KMP foundation plan](docs/superpowers/plans/2026-07-07-meetnote-kmp-foundation-plan.md)
- [Playback capture plan](docs/superpowers/plans/2026-07-08-meetnote-playback-capture-plan.md)

## Philosophy

MeetNote is intentionally being built around trustworthy failure handling:

- keep the raw meeting session
- show the active processing mode
- degrade gracefully before failing
- never hide cloud usage behind a silent fallback

That philosophy matters as much as model quality.
