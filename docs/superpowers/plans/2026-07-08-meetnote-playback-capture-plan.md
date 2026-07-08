# MeetNote Playback Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Android MediaProjection-based playback audio capture to MeetNote, with explicit consent flow, foreground-service-safe orchestration, and graceful fallback to microphone capture when playback capture is unavailable.

**Architecture:** Keep capture Android-native. The app layer requests MediaProjection consent and stores session-level capture intent state, `android-capture` owns playback-vs-mic recorder implementations, and `android-background` owns the foreground-service entrypoint for record-then-process sessions. The first version should prioritize safe orchestration and truthful user-visible state over aggressive automation.

**Tech Stack:** Kotlin, Android MediaProjection, AudioPlaybackCaptureConfiguration, AudioRecord, Foreground Service, WorkManager, Compose, Koin, SQLDelight, Coroutines

## Global Constraints

- offline-first by default
- no silent cloud usage
- explicit user approval before any provider fallback
- degrade gracefully before failing
- preserve user trust with visible processing mode labels
- keep audio capture, transcription, and summarization isolated so each layer can fail independently
- Android v1 should support two operating modes: Live Assist Mode and Record Then Process Mode
- 8GB phones: default to smaller local models, shorter context windows, and more aggressive fallback to Record Then Process Mode
- 12GB or higher phones: allow stronger local models, richer context retention, and more live summarization
- playback capture is best-effort and microphone fallback must exist
- users can force local-only mode
- generated artifacts should indicate whether they were created using primary local, smaller local, or provider-assisted processing
- MediaProjection user consent must be requested before each capture session
- each MediaProjection instance must be used only once for `createVirtualDisplay()`
- playback capture requires `RECORD_AUDIO`, MediaProjection consent, and same-profile capture compatibility
- captured playback is limited to capturable usages and source-app capture policy; microphone fallback must remain available
- validate playback capture behavior on a real Android device, not only emulator

---

## Scope Check

This plan covers only the Android playback capture slice.

Included:

- MediaProjection consent request flow
- playback-capable audio recorder implementation
- session-level capture mode selection
- foreground-service capture launch contract
- microphone fallback when playback capture is blocked, denied, or unsupported

Explicitly deferred:

- local ASR integration
- LiteRT-LM summary generation
- provider fallback onboarding
- calendar/email meeting context ingestion
- export and sharing expansion

## Planned File Structure

### Existing files to modify

- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\AndroidManifest.xml`
  - add MediaProjection-related permissions and foreground service declaration
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\MainActivity.kt`
  - host MediaProjection consent launcher
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\session\SessionViewModel.kt`
  - manage capture source choice, consent outcome, and visible fallback state
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\session\SessionScreen.kt`
  - show playback-vs-mic capture options and capture-state messaging
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-core\src\main\java\com\meetnote\android\core\AppModules.kt`
  - bind playback recorder dependencies
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\MeetingRecorder.kt`
  - preserve interface or extend minimally if needed
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-background\src\main\java\com\meetnote\android\background\MeetingCaptureService.kt`
  - accept capture mode/session parameters and own active recorder lifecycle

### New files to create

- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\CaptureSource.kt`
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\PlaybackCapturePermissionState.kt`
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\PlaybackAudioRecorder.kt`
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\PlaybackCaptureSupport.kt`
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\test\java\com\meetnote\android\capture\PlaybackCaptureSupportTest.kt`
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\capture\PlaybackCaptureConsentCoordinator.kt`
- `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-background\src\main\java\com\meetnote\android\background\MeetingCaptureNotificationFactory.kt`

## Interfaces

Playback-capture slice interfaces should look like this:

```kotlin
enum class CaptureSource {
    PLAYBACK_AUDIO,
    MICROPHONE
}
```

```kotlin
sealed interface PlaybackCapturePermissionState {
    data object NotRequested : PlaybackCapturePermissionState
    data object Requesting : PlaybackCapturePermissionState
    data object Granted : PlaybackCapturePermissionState
    data object Denied : PlaybackCapturePermissionState
    data object Unsupported : PlaybackCapturePermissionState
}
```

```kotlin
interface PlaybackAudioRecorder : MeetingRecorder
```

```kotlin
data class PlaybackCaptureSupport(
    val isPlaybackCaptureSupported: Boolean,
    val failureReason: String? = null
)
```

```kotlin
interface PlaybackCaptureConsentCoordinator {
    fun launchPlaybackCaptureConsent(sessionId: String)
    fun onPlaybackCaptureConsentResult(sessionId: String, granted: Boolean, dataIntent: android.content.Intent?)
}
```

## Task 1: Add Playback Capture Domain For The Android Shell

**Files:**
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\CaptureSource.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\PlaybackCapturePermissionState.kt`
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\session\SessionViewModel.kt`
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\session\SessionScreen.kt`
- Test: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\test\java\com\meetnote\android\ui\session\SessionViewModelTest.kt`

**Interfaces:**
- Consumes:
  - `enum class ProcessingMode`
  - current `SessionUiState`
- Produces:
  - `enum class CaptureSource`
  - `sealed interface PlaybackCapturePermissionState`
  - `fun updateCaptureSource(source: CaptureSource): Unit`
  - `fun onPlaybackCapturePermissionChanged(state: PlaybackCapturePermissionState): Unit`

- [x] **Step 1: Write failing ViewModel tests for capture source and permission state**

```kotlin
@Test
fun updatesCaptureSourceToPlayback() = runTest {
    val viewModel = buildViewModel()
    viewModel.updateCaptureSource(CaptureSource.PLAYBACK_AUDIO)
    assertEquals(CaptureSource.PLAYBACK_AUDIO, viewModel.uiState.value.captureSource)
}

@Test
fun storesPermissionDeniedState() = runTest {
    val viewModel = buildViewModel()
    viewModel.onPlaybackCapturePermissionChanged(PlaybackCapturePermissionState.Denied)
    assertEquals(PlaybackCapturePermissionState.Denied, viewModel.uiState.value.playbackPermissionState)
}
```

- [x] **Step 2: Run focused tests to verify failure**

Run: `.\gradlew.bat :androidApp:testDebugUnitTest --tests com.meetnote.android.ui.session.SessionViewModelTest`
Expected: FAIL because capture-source and permission-state APIs do not exist yet

- [x] **Step 3: Add minimal capture-source state and UI**

```kotlin
enum class CaptureSource {
    PLAYBACK_AUDIO,
    MICROPHONE
}
```

```kotlin
sealed interface PlaybackCapturePermissionState {
    data object NotRequested : PlaybackCapturePermissionState
    data object Requesting : PlaybackCapturePermissionState
    data object Granted : PlaybackCapturePermissionState
    data object Denied : PlaybackCapturePermissionState
    data object Unsupported : PlaybackCapturePermissionState
}
```

```kotlin
data class SessionUiState(
    val title: String = "",
    val selectedMode: ProcessingMode = ProcessingMode.RECORD_THEN_PROCESS,
    val captureSource: CaptureSource = CaptureSource.PLAYBACK_AUDIO,
    val playbackPermissionState: PlaybackCapturePermissionState = PlaybackCapturePermissionState.NotRequested,
    val createdSessionId: String? = null,
    val errorMessage: String? = null
)
```

- [x] **Step 4: Run tests and compile**

Run: `.\gradlew.bat :androidApp:testDebugUnitTest --tests com.meetnote.android.ui.session.SessionViewModelTest :androidApp:compileDebugKotlin`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/java/com/meetnote/android/ui/session android-capture/src/main/java/com/meetnote/android/capture/CaptureSource.kt android-capture/src/main/java/com/meetnote/android/capture/PlaybackCapturePermissionState.kt androidApp/src/test/java/com/meetnote/android/ui/session/SessionViewModelTest.kt
git commit -m "feat: add playback capture state to Android shell"
```

## Task 2: Add MediaProjection Consent Coordination

**Files:**
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\capture\PlaybackCaptureConsentCoordinator.kt`
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\MainActivity.kt`
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\MeetNoteApp.kt`
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\session\SessionViewModel.kt`
- Test: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\test\java\com\meetnote\android\ui\session\SessionViewModelTest.kt`

**Interfaces:**
- Consumes:
  - `PlaybackCapturePermissionState`
  - `MediaProjectionManager.createScreenCaptureIntent()`
- Produces:
  - `interface PlaybackCaptureConsentCoordinator`
  - `fun requestPlaybackCaptureConsent(): Unit`
  - `fun handlePlaybackCaptureConsent(granted: Boolean): Unit`

- [x] **Step 1: Write failing test for request flow**

```kotlin
@Test
fun marksPermissionRequestingBeforeLaunch() = runTest {
    val viewModel = buildViewModel()
    viewModel.requestPlaybackCaptureConsent()
    assertEquals(PlaybackCapturePermissionState.Requesting, viewModel.uiState.value.playbackPermissionState)
}
```

- [x] **Step 2: Run focused test**

Run: `.\gradlew.bat :androidApp:testDebugUnitTest --tests com.meetnote.android.ui.session.SessionViewModelTest`
Expected: FAIL because request API does not exist yet

- [x] **Step 3: Add coordinator and activity result launcher**

```kotlin
interface PlaybackCaptureConsentCoordinator {
    fun launchPlaybackCaptureConsent(sessionId: String)
    fun onPlaybackCaptureConsentResult(sessionId: String, granted: Boolean, dataIntent: Intent?)
}
```

```kotlin
val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
```

- [x] **Step 4: Run tests and compile**

Run: `.\gradlew.bat :androidApp:testDebugUnitTest --tests com.meetnote.android.ui.session.SessionViewModelTest :androidApp:compileDebugKotlin`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/java/com/meetnote/android/MainActivity.kt androidApp/src/main/java/com/meetnote/android/ui/MeetNoteApp.kt androidApp/src/main/java/com/meetnote/android/ui/session/SessionViewModel.kt androidApp/src/main/java/com/meetnote/android/capture/PlaybackCaptureConsentCoordinator.kt androidApp/src/test/java/com/meetnote/android/ui/session/SessionViewModelTest.kt
git commit -m "feat: add MediaProjection consent coordination"
```

## Task 3: Implement Playback Recorder Support Checks And Recorder Shell

**Files:**
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\PlaybackCaptureSupport.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\main\java\com\meetnote\android\capture\PlaybackAudioRecorder.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\src\test\java\com\meetnote\android\capture\PlaybackCaptureSupportTest.kt`
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-capture\build.gradle.kts`

**Interfaces:**
- Consumes:
  - `MeetingRecorder`
  - `SessionRepository`
  - `AudioPlaybackCaptureConfiguration`
  - `AudioRecord.Builder.setAudioPlaybackCaptureConfig(...)`
- Produces:
  - `data class PlaybackCaptureSupport(...)`
  - `class PlaybackAudioRecorder(...) : MeetingRecorder`

- [x] **Step 1: Write failing support test**

```kotlin
@Test
fun reportsUnsupportedBelowApi29() {
    val result = PlaybackCaptureSupport.forSdk(28)
    assertFalse(result.isPlaybackCaptureSupported)
}
```

- [x] **Step 2: Run focused test**

Run: `.\gradlew.bat :android-capture:testDebugUnitTest --tests com.meetnote.android.capture.PlaybackCaptureSupportTest`
Expected: FAIL because support helper does not exist yet

- [x] **Step 3: Add support helper and recorder shell**

```kotlin
data class PlaybackCaptureSupport(
    val isPlaybackCaptureSupported: Boolean,
    val failureReason: String? = null
) {
    companion object {
        fun forSdk(sdkInt: Int): PlaybackCaptureSupport =
            if (sdkInt >= 29) PlaybackCaptureSupport(true) else PlaybackCaptureSupport(false, "Playback capture requires Android 10+")
    }
}
```

```kotlin
class PlaybackAudioRecorder(
    private val sessionRepository: SessionRepository
) : MeetingRecorder {
    override suspend fun start(sessionId: String): RecorderResult = RecorderResult.Failure("Playback recorder not wired yet")
    override suspend fun stop(sessionId: String): RecorderResult = RecorderResult.Failure("Playback recorder not wired yet")
}
```

- [x] **Step 4: Run tests and compile**

Run: `.\gradlew.bat :android-capture:testDebugUnitTest :android-capture:compileDebugKotlin`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add android-capture/build.gradle.kts android-capture/src/main/java/com/meetnote/android/capture/PlaybackCaptureSupport.kt android-capture/src/main/java/com/meetnote/android/capture/PlaybackAudioRecorder.kt android-capture/src/test/java/com/meetnote/android/capture/PlaybackCaptureSupportTest.kt
git commit -m "feat: add playback capture support and recorder shell"
```

## Task 4: Wire Foreground Service And Manifest For Playback Capture

**Files:**
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\AndroidManifest.xml`
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-background\src\main\java\com\meetnote\android\background\MeetingCaptureService.kt`
- Create: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-background\src\main\java\com\meetnote\android\background\MeetingCaptureNotificationFactory.kt`
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\android-background\build.gradle.kts`

**Interfaces:**
- Consumes:
  - `MeetingRecorder`
  - `CaptureSource`
- Produces:
  - `const val EXTRA_SESSION_ID: String`
  - `const val EXTRA_CAPTURE_SOURCE: String`
  - foreground service declaration for media projection capture

- [ ] **Step 1: Add manifest/service tests by compile target**

Run: `.\gradlew.bat :android-background:compileDebugKotlin`
Expected: PASS before change, used as baseline

- [x] **Step 2: Add manifest and service skeleton**

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```

```xml
<service
    android:name="com.meetnote.android.background.MeetingCaptureService"
    android:exported="false"
    android:foregroundServiceType="mediaProjection|microphone" />
```

```kotlin
const val EXTRA_SESSION_ID = "extra_session_id"
const val EXTRA_CAPTURE_SOURCE = "extra_capture_source"
```

- [x] **Step 3: Run compile verification**

Run: `.\gradlew.bat :android-background:compileDebugKotlin :androidApp:compileDebugKotlin`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/AndroidManifest.xml android-background/build.gradle.kts android-background/src/main/java/com/meetnote/android/background/MeetingCaptureService.kt android-background/src/main/java/com/meetnote/android/background/MeetingCaptureNotificationFactory.kt
git commit -m "feat: add foreground service playback capture shell"
```

## Task 5: Connect Playback Request Flow To Mic Fallback Messaging

**Files:**
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\session\SessionViewModel.kt`
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\main\java\com\meetnote\android\ui\session\SessionScreen.kt`
- Modify: `C:\Users\Anand\OneDrive - Xalta Technology Services Pvt Ltd\Desktop\SelfProjects\MeetNote\androidApp\src\test\java\com\meetnote\android\ui\session\SessionViewModelTest.kt`

**Interfaces:**
- Consumes:
  - `CaptureSource`
  - `PlaybackCapturePermissionState`
  - `PlaybackCaptureSupport`
- Produces:
  - visible fallback message when playback capture is denied or unsupported
  - mic fallback source selection path

- [x] **Step 1: Write failing fallback tests**

```kotlin
@Test
fun switchesToMicWhenPlaybackDenied() = runTest {
    val viewModel = buildViewModel()
    viewModel.onPlaybackCapturePermissionChanged(PlaybackCapturePermissionState.Denied)
    assertEquals(CaptureSource.MICROPHONE, viewModel.uiState.value.captureSource)
}
```

- [x] **Step 2: Run focused tests**

Run: `.\gradlew.bat :androidApp:testDebugUnitTest --tests com.meetnote.android.ui.session.SessionViewModelTest`
Expected: FAIL because fallback switch logic is missing

- [x] **Step 3: Add minimal fallback behavior**

```kotlin
fun onPlaybackCapturePermissionChanged(state: PlaybackCapturePermissionState) {
    _uiState.value = when (state) {
        PlaybackCapturePermissionState.Denied,
        PlaybackCapturePermissionState.Unsupported -> _uiState.value.copy(
            playbackPermissionState = state,
            captureSource = CaptureSource.MICROPHONE,
            errorMessage = "Playback capture unavailable. Falling back to microphone capture."
        )
        else -> _uiState.value.copy(playbackPermissionState = state)
    }
}
```

- [x] **Step 4: Run tests and compile**

Run: `.\gradlew.bat :androidApp:testDebugUnitTest --tests com.meetnote.android.ui.session.SessionViewModelTest :androidApp:compileDebugKotlin`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/java/com/meetnote/android/ui/session/SessionViewModel.kt androidApp/src/main/java/com/meetnote/android/ui/session/SessionScreen.kt androidApp/src/test/java/com/meetnote/android/ui/session/SessionViewModelTest.kt
git commit -m "feat: add playback capture fallback messaging"
```

## Self-Review

### Spec Coverage

- playback/system-audio capture path is covered by Tasks 1 through 4
- microphone fallback behavior is covered by Task 5
- explicit MediaProjection consent handling is covered by Task 2
- record-then-process foreground orchestration shell is covered by Task 4
- Android best-effort platform constraints are reflected in support checks and fallback messaging

Deferred to later plans:

- full `AudioRecord` PCM pipeline and file writing implementation
- real `MediaProjection` token storage/transfer into service runtime
- local ASR integration
- LiteRT-LM integration
- provider fallback onboarding

### Placeholder Scan

No placeholder markers remain. Deferred items are explicitly named as out of scope.

### Type Consistency

- `CaptureSource` is used consistently across app and background layers
- `PlaybackCapturePermissionState` is the single permission-state type
- `MeetingRecorder` remains the common recorder contract

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-08-meetnote-playback-capture-plan.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Status update: Inline execution is complete through the implementation and verification steps. Commit checkpoints remain intentionally open.

**Which approach?**
