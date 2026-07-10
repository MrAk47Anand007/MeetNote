# MeetNote Local Meeting Intelligence Design

Date: 2026-07-10  
Scope: Six sequential Android implementation phases from the current capture foundation to local transcription and LiteRT-backed meeting minutes.

## Goal

Build an Android-first, offline-first flow that captures a meeting from external meeting apps where Android permits it, transcribes the saved audio locally, and generates a structured summary and minutes of meeting (MOM) using a local LiteRT-compatible LLM.

## Current Baseline

MeetNote already provides the foundation required to begin this work:

- manual meeting sessions persisted with SQLDelight
- microphone WAV capture and best-effort playback WAV capture
- MediaProjection consent coordination for playback capture
- microphone fallback when playback capture is denied or unsupported
- foreground-service shell, WorkManager post-capture scheduling, and durable markdown artifacts
- shared `TranscriptionEngine` and `SummaryEngine` contracts
- persisted session status, artifact paths, and last-failure messages

The current transcription implementation deliberately returns `UnavailableLocally`. The current summary implementation is deterministic extraction, not LLM inference. No model-management, capability-profiling, transcript-chunking, structured MOM, or real-device validation capability exists yet.

## Product Boundary

"Any meeting app" means generic Android capture support, not provider SDK integration or a guarantee that every app exposes capturable playback audio. MeetNote must request MediaProjection consent for each playback-capture session, respond when projection access is revoked, and fall back to microphone capture when playback capture is unavailable.

The first shippable target is Record Then Process: capture during the meeting, then run local ASR and local LLM processing after capture stops. Live Assist is deferred until this reliable end-to-end path is proven on physical devices.

## Architecture

The pipeline remains deliberately separated so capture, ASR, and LLM processing fail independently:

```text
External meeting app
  -> Playback capture when permitted; microphone fallback otherwise
  -> Durable WAV recording and meeting session
  -> WorkManager post-meeting orchestration
  -> Local ASR chunks and merged transcript
  -> LiteRT local LLM structured meeting analysis
  -> Summary, MOM, actions, decisions, risks, and markdown artifact
```

Each stage records processing tier, progress, and the most actionable failure reason. Raw audio and partial transcript/notes are preserved whenever a later stage fails.

## Six Phases

### Phase 1: Capture Reliability and Device Validation

Make capture production-safe before adding models. Move recorder and projection-token lifetime ownership behind a foreground-service-safe runtime boundary, stop safely when MediaProjection is revoked, validate WAV output and session transitions, and create a physical-device test matrix for Google Meet, Zoom, and Microsoft Teams on an 8GB and a 12GB-or-higher device.

Exit condition: a recorded meeting remains durable and correctly labeled regardless of playback success, microphone fallback, or capture interruption.

### Phase 2: Local ASR Vertical Slice

Replace the unavailable ASR stub with one bundled, on-device ASR runtime for post-meeting processing. The phase chooses and proves a supported Android runtime/model combination, records model provenance, writes transcript output, and reports real model/read/decode failures without losing audio.

Exit condition: a short recorded WAV file produces a persisted local transcript on a physical device with no network requirement.

### Phase 3: Resumable Transcript Processing

Support long recordings through bounded audio chunks, transcript segments with timing metadata, resume-safe WorkManager processing, merge and cleanup rules, and partial-result preservation. This phase prepares stable, bounded LLM input rather than putting a full meeting transcript into one prompt.

Exit condition: an interrupted long-meeting job resumes from its persisted checkpoint and produces an ordered transcript artifact.

### Phase 4: LiteRT Local LLM and Structured MOM

Replace deterministic summaries with a LiteRT-compatible local LLM adapter behind `SummaryEngine`. Use a schema-constrained, validated result for summary, MOM, action items, decisions, risks, and follow-ups; render it to markdown only after validation. Long meetings use map-reduce or hierarchical summarization over transcript segments.

Exit condition: a local model on a real device converts a persisted transcript into a valid, durable structured MOM artifact without network access.

### Phase 5: Model and Capability Management

Add model manifest metadata, user-controlled download/removal, storage checks, compatibility probes, RAM/thermal/battery-aware model selection, and explicit primary-versus-smaller local processing tiers. Local-only mode remains a first-class setting.

Exit condition: compatible devices select a usable local model tier before processing, while incompatible or resource-constrained devices receive a truthful, actionable local-only outcome.

### Phase 6: Product Hardening and Controlled Expansion

Add export/share flows, session-history polish, privacy and retention controls, observability that stays on device, and the explicit user-approved provider fallback contract. Live Assist is evaluated only after the Record Then Process path passes the device matrix; it remains a separate follow-on plan if it cannot meet reliability and battery targets.

Exit condition: users can capture, transcribe, review, export, and recover a local meeting result through a trustworthy end-to-end flow.

## Global Constraints

- Offline-first is the default; no provider call or upload may occur silently.
- Playback capture is best-effort. Microphone fallback and user-visible fidelity messaging are mandatory.
- A MediaProjection consent token is single-session and must not be reused.
- Local ASR and LiteRT LLM model/runtime choices are locked by a focused real-device spike before product integration.
- Tests cover contracts, persistence, orchestration, and schema validation; capture and model performance claims require real-device evidence.
- Every generated artifact records the actual processing tier: primary local, smaller local, or approved provider-assisted.
- No deep Zoom, Google Meet, or Teams SDK integration, bot joining, shared workspaces, or cross-device sync is in these six phases.

## Risks and Decisions

The highest technical unknowns are the bundled ASR runtime/model, LiteRT-compatible model packaging and memory behavior, and real playback-capture compatibility across apps. Therefore Phase 2 and Phase 4 each begin with a narrow device spike and have explicit exit criteria. The roadmap does not promise universal external-app audio capture; it promises a graceful and transparent fallback path.

