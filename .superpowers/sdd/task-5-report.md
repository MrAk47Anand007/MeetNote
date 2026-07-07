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
