# Task 7 Report: Add AI And Security Scaffolding Interfaces

## Summary

Created the requested scaffolding-only modules and shared AI/security interfaces:

- `shared/ai-contracts`
- `android-security`
- `android-ai-local`
- `android-asr`

The new shared module exposes the two KMP contracts:

- `TranscriptionEngine`
- `SummaryEngine`

The Android security module exposes:

- `ProviderKeyStore`

I kept the implementation strictly to shells and interfaces. No LiteRT-LM integration, no ASR runtime, no provider API client, and no Android app wiring were added.

## Verification

I ran the requested module compile set with one repo-specific correction:

- `.\gradlew.bat :shared:ai-contracts:compileDebugKotlinAndroid :android-security:compileDebugKotlin :android-ai-local:compileDebugKotlin :android-asr:compileDebugKotlin`

Result: pass.

Notes:

- The brief’s shorthand `:shared:ai-contracts:compileKotlinAndroid` was ambiguous in this repo, so I used the concrete task name `compileDebugKotlinAndroid`.
- Gradle emitted the existing Kotlin Multiplatform vs AGP 8.6.1 compatibility warning, but the build completed successfully.

## Files Added

- `shared/ai-contracts/build.gradle.kts`
- `shared/ai-contracts/src/commonMain/kotlin/com/meetnote/shared/ai/TranscriptionEngine.kt`
- `shared/ai-contracts/src/commonMain/kotlin/com/meetnote/shared/ai/SummaryEngine.kt`
- `android-security/build.gradle.kts`
- `android-security/src/main/java/com/meetnote/android/security/ProviderKeyStore.kt`
- `android-ai-local/build.gradle.kts`
- `android-asr/build.gradle.kts`

## Outcome

Task 7 is complete.

---

## Fix Pass: Review 86edda3..49d0cf8

### Review-Driven Scope

Kept this pass strictly within the Task 7 ownership boundary:

- `shared/ai-contracts`
- Android shell modules already created for Task 7
- this Task 7 report

No LiteRT, ASR runtime, provider client, application wiring, or non-Task-7 module changes were added.

### What Was Missing

The initial scaffolding contracts only exposed raw string input/output:

- `TranscriptionEngine.transcribe(audioPath: String): String`
- `SummaryEngine.summarize(transcript: String): String`

That shape could not carry:

- the requested execution policy boundary (`LOCAL_ONLY` vs `PROVIDER_ALLOWED`)
- explicit approval context for provider-capable processing
- the actual processing-tier provenance returned by an implementation

### Fix Applied

I upgraded the shared AI contract surface with a minimal request/result model while reusing the existing shared-domain enums:

- `ProcessingPolicy`
- `ProcessingTier`

Added:

- `AiProcessingContext`
  - `processingPolicy: ProcessingPolicy`
  - `providerProcessingApproved: Boolean`
- `TranscriptionRequest`
- `TranscriptionResult`
- `SummaryRequest`
- `SummaryResult`

Updated interfaces:

- `TranscriptionEngine`
  - from `suspend fun transcribe(audioPath: String): String`
  - to `suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult`
- `SummaryEngine`
  - from `suspend fun summarize(transcript: String): String`
  - to `suspend fun summarize(request: SummaryRequest): SummaryResult`

### Why This Addresses The Findings

The revised contracts now support the required scaffolding semantics without introducing runtime behavior:

- requests can declare `ProcessingPolicy.LOCAL_ONLY` or `ProcessingPolicy.PROVIDER_ALLOWED`
- requests can carry explicit provider approval context through `providerProcessingApproved`
- results can report provenance with `ProcessingTier.PRIMARY_LOCAL`, `ProcessingTier.SMALLER_LOCAL`, or `ProcessingTier.PROVIDER_ASSISTED`
- results also retain the originating processing context so later layers can preserve policy/approval metadata without parallel side channels

This keeps Task 7 at the contract/scaffolding layer only.

### Files Updated In This Fix Pass

- `shared/ai-contracts/src/commonMain/kotlin/com/meetnote/shared/ai/TranscriptionEngine.kt`
- `shared/ai-contracts/src/commonMain/kotlin/com/meetnote/shared/ai/SummaryEngine.kt`
- `.superpowers/sdd/task-7-report.md`

Android shell modules did not require behavioral changes for this review fix pass.

### Verification

Re-ran the focused Task 7 compile check:

- `.\gradlew.bat :shared:ai-contracts:compileDebugKotlinAndroid :android-security:compileDebugKotlin :android-ai-local:compileDebugKotlin :android-asr:compileDebugKotlin`

Result:

- `BUILD SUCCESSFUL in 1m 42s`

Observed warnings/non-blockers:

- existing Kotlin Multiplatform vs AGP 8.6.1 compatibility warning
- existing Gradle 9 deprecation warning

Neither warning blocked the Task 7 compile targets.

---

## Second Fix Pass: Review 86edda3..97c7bfa

### Scope Kept Narrow

This second pass stayed inside the Task 7 ownership boundary only:

- `shared/ai-contracts`
- existing Android shell modules from Task 7
- this report file

No app wiring, runtime behavior, local-model integration, provider integration, or non-Task-7 module edits were introduced.

### Review Finding Addressed

The first fix pass made the contracts policy-aware and provenance-aware, but the result types still modeled only a completed success payload:

- `TranscriptionResult` always required a transcript
- `SummaryResult` always required a summary

That meant callers still had no typed, in-band way to represent graceful non-fatal outcomes such as:

- provider approval required before continuing
- work intentionally deferred for later processing
- local processing currently unavailable

### Contract Adjustment

I replaced the happy-path-only result payloads with a lightweight sealed outcome shape that follows the repo's existing simple result-style pattern without adding runtime behavior.

Added:

- `AiProcessingResult<T>`
  - `Completed<T>`
  - `RequiresProviderApproval`
  - `Deferred`
  - `UnavailableLocally`

Kept and reused:

- `AiProcessingContext`
  - `processingPolicy: ProcessingPolicy`
  - `providerProcessingApproved: Boolean`
- `ProcessingTier` on completed outcomes for provenance reporting

Updated aliases:

- `TranscriptionResult = AiProcessingResult<String>`
- `SummaryResult = AiProcessingResult<String>`

Requests remain unchanged:

- `TranscriptionRequest`
- `SummaryRequest`

### Why This Fix Is The Right Shape

This keeps Task 7 scaffolding-only while making the shared interface expressive enough for later implementations to return non-throwing fallback states in-band:

- `Completed` carries the produced value plus `processingContext` and `processingTier`
- `RequiresProviderApproval` preserves policy/approval context when provider escalation is blocked pending consent
- `Deferred` allows later-stage scheduling or model-readiness flows to stay typed without pretending work already succeeded
- `UnavailableLocally` allows local-only flows to terminate gracefully when no local path is currently available

This also stays consistent with the repo's existing lightweight sealed result approach instead of inventing a heavier runtime abstraction.

### Files Updated In This Pass

- `shared/ai-contracts/src/commonMain/kotlin/com/meetnote/shared/ai/AiProcessingResult.kt`
- `shared/ai-contracts/src/commonMain/kotlin/com/meetnote/shared/ai/TranscriptionEngine.kt`
- `shared/ai-contracts/src/commonMain/kotlin/com/meetnote/shared/ai/SummaryEngine.kt`
- `.superpowers/sdd/task-7-report.md`

Android shell modules did not need code changes in this pass.

### Verification

Re-ran the focused Task 7 compile targets:

- `.\gradlew.bat :shared:ai-contracts:compileDebugKotlinAndroid :android-security:compileDebugKotlin :android-ai-local:compileDebugKotlin :android-asr:compileDebugKotlin`

Result:

- `BUILD SUCCESSFUL in 33s`
- `38 actionable tasks: 1 executed, 37 up-to-date`

Observed warnings/non-blockers:

- existing Kotlin Multiplatform vs AGP 8.6.1 compatibility warning
- existing Gradle 9 deprecation warning

Neither warning blocked the Task 7 targets.
