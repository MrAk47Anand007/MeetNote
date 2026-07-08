Task 5 report

Implemented the Android capture module scaffold in `android-capture` only:
- Added `android-capture/build.gradle.kts`
- Added `RecorderResult`, `MeetingRecorder`, and `MicOnlyMeetingRecorder`
- Kept the implementation mic-only and limited to the compile path as requested

Verification:
- `.\gradlew.bat :android-capture:compileDebugKotlin` passed

Notes:
- The build emitted the existing Kotlin/AGP compatibility warning from the workspace, but it did not block compilation.

Fix pass:
- Made `MicOnlyMeetingRecorder` session-aware on stop by delegating state handling to a small internal `RecorderSessionState` helper.
- `stop(sessionId)` now returns `Failure` when called for a session that does not match the active session instead of reusing the last file path.
- Successful stop now clears the active session/file state, so repeated stop calls return `Failure("Recorder not started")` instead of stale success.
- Kept the change inside `android-capture` only; no app/background wiring was added.

Verification:
- `.\gradlew.bat :android-capture:compileDebugKotlin` passed
- `.\gradlew.bat :android-capture:testDebugUnitTest` passed

Tests added:
- `android-capture/src/test/java/com/meetnote/android/capture/RecorderSessionStateTest.kt`
- Covered mismatched-session stop failure and stale-stop reset behavior

Second fix pass:
- Injected `SessionRepository` into `MicOnlyMeetingRecorder` and used the shared domain `SessionStatus` lifecycle directly inside the capture contract.
- `start(sessionId)` now writes the placeholder recording file inside a local `try/catch`, returns `RecorderResult.Failure(...)` on file-preparation errors, and persists `SessionStatus.CAPTURING` through `SessionRepository.updateStatus(...)` before marking the recorder state active.
- `stop(sessionId)` now persists the captured file path through `SessionRepository.attachAudioFile(...)` and then updates the session to `SessionStatus.RECORDED` before clearing recorder state.
- Preserved the earlier mismatched-session and stale-stop protections by keeping session validation inside `RecorderSessionState`, while changing stop-state clearing to happen only after the repository writes succeed.
- Added the minimal direct `:shared:core` dependency required because `SessionRepository` exposes `SessionId` in its public API.
- Kept the scope limited to `android-capture` and this report; no app/background DI wiring was added.

Verification:
- `.\gradlew.bat :android-capture:compileDebugKotlin` passed
- `.\gradlew.bat :android-capture:testDebugUnitTest` passed

Tests added:
- `android-capture/src/test/java/com/meetnote/android/capture/MicOnlyMeetingRecorderTest.kt`
- Covered capture-status persistence on start, audio attachment plus recorded-status persistence on stop, and graceful `RecorderResult.Failure` behavior when the output file cannot be prepared

Third fix pass:
- Guarded `MicOnlyMeetingRecorder.start(sessionId)` with a coroutine mutex so a second start cannot replace an active session while the first one is still live.
- Kept the existing session-identity checks and stop-state clearing behavior in `RecorderSessionState`, but now repository write failures during both `start()` and `stop()` return `RecorderResult.Failure` instead of escaping.
- Preserved the previous persistence flow: successful `start()` still marks `CAPTURING`, and successful `stop()` still attaches the audio file and marks `RECORDED` before the active session is cleared.
- Kept the scope inside `android-capture` only; no app/background wiring was added.

Verification:
- `.\gradlew.bat :android-capture:compileDebugKotlin` passed
- `.\gradlew.bat :android-capture:testDebugUnitTest` passed

Tests added or updated:
- `android-capture/src/test/java/com/meetnote/android/capture/MicOnlyMeetingRecorderTest.kt`
- `android-capture/src/test/java/com/meetnote/android/capture/RecorderSessionStateTest.kt`
- Covered overlapping `start()` rejection, start-status persistence failures, stop write failures that retain the active session, and the existing mismatch/stale-stop checks

Notes:
- The build still prints the pre-existing Kotlin/AGP compatibility warning from the workspace, but it does not block compilation or tests.

Fifth fix pass:
- Added explicit stop-path coverage for the branch where `stop(sessionId)` reaches `SessionRepository.attachAudioFile(...)` successfully and then `SessionRepository.updateStatus(..., SessionStatus.RECORDED)` fails.
- Added a matching cancellation test for the same branch so coroutine cancellation is still propagated after the attachment write has succeeded.
- Kept the implementation unchanged; the fix pass was limited to test coverage and the task report only.

Verification:
- `.\gradlew.bat :android-capture:compileDebugKotlin` passed
- `.\gradlew.bat :android-capture:testDebugUnitTest` passed

Tests updated:
- `android-capture/src/test/java/com/meetnote/android/capture/MicOnlyMeetingRecorderTest.kt`
- Added explicit coverage for `RECORDED` status failure after attachment and `CancellationException` propagation after attachment

Fourth fix pass:
- Added explicit `CancellationException` passthrough around the suspend repository calls in `MicOnlyMeetingRecorder.start(sessionId)` and the `stop(sessionId)` persistence path so coroutine cancellation is no longer converted into `RecorderResult.Failure`.
- Kept the existing failure mapping for real repository/storage problems intact, including start-status persistence failures and stop attachment/status write failures.
- Preserved the earlier Task 5 behavior that was already verified: overlapping start protection, session-identity checks, state clearing after successful stop, repository persistence, and graceful file-preparation failure.
- Kept the scope inside `android-capture` only; no app/background wiring was added.

Verification:
- `.\gradlew.bat :android-capture:compileDebugKotlin` passed
- `.\gradlew.bat :android-capture:testDebugUnitTest` passed

Tests updated:
- `android-capture/src/test/java/com/meetnote/android/capture/MicOnlyMeetingRecorderTest.kt`
- Added cancellation propagation coverage for `start()` and `stop()` alongside the existing failure-mapping tests
