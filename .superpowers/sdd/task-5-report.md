Task 5 report

Implemented the Android capture module scaffold in `android-capture` only:
- Added `android-capture/build.gradle.kts`
- Added `RecorderResult`, `MeetingRecorder`, and `MicOnlyMeetingRecorder`
- Kept the implementation mic-only and limited to the compile path as requested

Verification:
- `.\gradlew.bat :android-capture:compileDebugKotlin` passed

Notes:
- The build emitted the existing Kotlin/AGP compatibility warning from the workspace, but it did not block compilation.
