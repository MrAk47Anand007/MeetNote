# Phase 1 Capture Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make microphone and playback capture service-owned, interruption-safe, and ready for physical-device validation.

**Architecture:** MeetingCaptureService owns the active recorder after a command. A coordinator maps recorder outcomes and the UI observes persisted session state.

**Tech Stack:** Kotlin, foreground services, MediaProjection, AudioRecord, Koin, JUnit4

## Global Constraints

- Playback capture is best-effort; microphone fallback is mandatory.
- Never reuse a MediaProjection token.
- Preserve WAV output and session failure details after interruption.

---

### Task 1: Capture command coordination

**Files:**
- Create: `android-background/src/main/java/com/meetnote/android/background/CaptureCommandCoordinator.kt`
- Create: `android-background/src/test/java/com/meetnote/android/background/CaptureCommandCoordinatorTest.kt`

**Interfaces:**

```kotlin
sealed interface CaptureCommandResult { data class Started(val sessionId: String, val source: CaptureSource) : CaptureCommandResult; data class Stopped(val sessionId: String, val filePath: String) : CaptureCommandResult; data class Failed(val sessionId: String, val message: String) : CaptureCommandResult }
```

- [ ] **Step 1: Write the failing test**

```kotlin
assertEquals(CaptureCommandResult.Started("s1", CaptureSource.PLAYBACK_AUDIO), coordinator.start("s1", CaptureSource.PLAYBACK_AUDIO))
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-background:testDebugUnitTest --tests com.meetnote.android.background.CaptureCommandCoordinatorTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
private fun recorderFor(source: CaptureSource) = if (source == CaptureSource.MICROPHONE) microphoneRecorder else playbackRecorder
```

- [ ] **Step 4: Run the focused verification**

Run: `./gradlew.bat :android-background:testDebugUnitTest --tests com.meetnote.android.background.CaptureCommandCoordinatorTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-background && git commit -m "feat: centralize capture command coordination"
```

### Task 2: Service-owned recorder lifecycle

**Files:**
- Modify: `android-background/src/main/java/com/meetnote/android/background/MeetingCaptureService.kt`
- Modify: `android-background/src/main/java/com/meetnote/android/background/MeetingCaptureServiceController.kt`
- Modify: `androidApp/src/main/java/com/meetnote/android/ui/session/SessionViewModel.kt`

**Interfaces:**

```kotlin
suspend fun CaptureCommandCoordinator.start(sessionId: String, source: CaptureSource): CaptureCommandResult
suspend fun CaptureCommandCoordinator.stop(sessionId: String, source: CaptureSource): CaptureCommandResult
```

- [ ] **Step 1: Write the failing test**

```kotlin
coordinator.start("s1", CaptureSource.PLAYBACK_AUDIO); assertEquals(CaptureCommandResult.Stopped("s1", "/tmp/s1.wav"), coordinator.stop("s1", CaptureSource.PLAYBACK_AUDIO))
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-background:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:compileDebugKotlin`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
serviceScope.launch { if (action == ACTION_START_CAPTURE) coordinator.start(sessionId, source) else coordinator.stop(sessionId, activeSource) }
```

- [ ] **Step 4: Run the focused verification**

Run: `./gradlew.bat :android-background:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:compileDebugKotlin`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-background androidApp && git commit -m "fix: make foreground service own capture lifecycle"
```

### Task 3: Projection revocation and device matrix

**Files:**
- Modify: `android-capture/src/main/java/com/meetnote/android/capture/PlaybackAudioRecorder.kt`
- Modify: `android-capture/src/test/java/com/meetnote/android/capture/PlaybackAudioRecorderTest.kt`
- Create: `docs/validation/phase-1-capture-device-matrix.md`

**Interfaces:**

```kotlin
interface ProjectionStopListener { fun onProjectionStopped() }
```

- [ ] **Step 1: Write the failing test**

```kotlin
captureSession.onProjectionStopped(); assertEquals("Playback capture permission was revoked", repository.lastError("s1"))
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat :android-capture:testDebugUnitTest :android-background:testDebugUnitTest`
Expected: FAIL because the described production behavior is absent.

- [ ] **Step 3: Implement the minimal behavior**

```kotlin
projection.registerCallback(object : MediaProjection.Callback() { override fun onStop() = listener.onProjectionStopped() }, mainHandler)
```

- [ ] **Step 4: Run the focused verification**

Run: `./gradlew.bat :android-capture:testDebugUnitTest :android-background:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android-capture docs/validation/phase-1-capture-device-matrix.md && git commit -m "fix: recover from revoked playback capture"
```
