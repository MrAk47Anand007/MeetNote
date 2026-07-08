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
