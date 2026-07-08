package com.meetnote.android.capture

data class PlaybackCaptureSupport(
    val isPlaybackCaptureSupported: Boolean,
    val failureReason: String? = null
) {
    companion object {
        fun forSdk(sdkInt: Int): PlaybackCaptureSupport {
            return if (sdkInt >= 29) {
                PlaybackCaptureSupport(isPlaybackCaptureSupported = true)
            } else {
                PlaybackCaptureSupport(
                    isPlaybackCaptureSupported = false,
                    failureReason = "Playback capture requires Android 10+"
                )
            }
        }
    }
}
