# Phase 2 Local ASR Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the ASR stub with offline Android transcription for captured WAV files.

**Architecture:** android-asr owns a whisper.cpp JNI adapter and model resolver; the existing TranscriptionEngine contract remains stable.

**Tech Stack:** Kotlin, JNI/CMake, whisper.cpp, WorkManager, JUnit4

## Global Constraints

- Input is existing 16 kHz PCM WAV.
- No network is required after model installation.
- Missing models return typed local outcomes and preserve audio.

---

### Task 1: ASR model and native bridge

**Files:**
- Create: `android-asr/src/main/java/com/meetnote/android/asr/AsrModelResolver.kt`
- Create: `android-asr/src/main/java/com/meetnote/android/asr/WhisperNativeBridge.kt`
- Create: `android-asr/src/test/java/com/meetnote/android/asr/AsrModelResolverTest.kt`

**Interfaces:**

```kotlin
data class AsrModel(val id: String, val file: File, val processingTier: ProcessingTier)
interface WhisperNativeBridge { fun transcribe(modelPath: String, wavPath: String): String }
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertTrue(missingModelResolver.resolve().isFailure); assertEquals("whisper-base.en", installedModelResolver.resolve().getOrThrow().id)
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-asr:testDebugUnitTest --tests com.meetnote.android.asr.AsrModelResolverTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
check(modelFile.isFile) { "Local ASR model is not installed" }; return Result.success(AsrModel("whisper-base.en", modelFile, ProcessingTier.PRIMARY_LOCAL))
```

- [ ] **Step 4: Run the focused verification**

Run: `./gradlew.bat :android-asr:testDebugUnitTest --tests com.meetnote.android.asr.AsrModelResolverTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-asr && git commit -m "feat: add local ASR model boundary"
```

### Task 2: Production local transcription engine

**Files:**
- Modify: `android-asr/src/main/java/com/meetnote/android/asr/LocalTranscriptionEngine.kt`
- Create: `android-asr/src/test/java/com/meetnote/android/asr/WhisperCppTranscriptionEngineTest.kt`
- Modify: `android-asr/build.gradle.kts`

**Interfaces:**

```kotlin
suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals(ProcessingTier.PRIMARY_LOCAL, completedResult.processingTier); assertTrue(missingResult is AiProcessingResult.UnavailableLocally)
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-asr:testDebugUnitTest --tests com.meetnote.android.asr.WhisperCppTranscriptionEngineTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
return AiProcessingResult.Completed(nativeBridge.transcribe(model.file.path, request.audioPath), request.processingContext, model.processingTier)
```

- [ ] **Step 4: Run the focused verification**

Run: `./gradlew.bat :android-asr:testDebugUnitTest --tests com.meetnote.android.asr.WhisperCppTranscriptionEngineTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-asr && git commit -m "feat: transcribe WAV recordings locally"
```

### Task 3: Transcript artifact and physical proof

**Files:**
- Modify: `shared/export/src/commonMain/kotlin/com/meetnote/shared/export/MeetingNoteMarkdownFormatter.kt`
- Modify: `android-background/src/test/java/com/meetnote/android/background/DefaultPostMeetingProcessingExecutorTest.kt`
- Create: `docs/validation/phase-2-local-asr.md`

**Interfaces:**

```kotlin
fun format(artifact: MeetingNoteArtifact): String
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertTrue(noteFile.readText().contains("## Transcript"))
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-background:testDebugUnitTest :android-asr:testDebugUnitTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
appendLine("## Transcript"); appendLine(completedTranscriptText)
```

- [ ] **Step 4: Run the focused verification**

Run: `./gradlew.bat :android-background:testDebugUnitTest :android-asr:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/export android-background docs/validation/phase-2-local-asr.md && git commit -m "feat: persist local transcript artifacts"
```
