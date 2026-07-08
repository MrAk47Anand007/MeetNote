package com.meetnote.android.capture

import org.junit.Assert.assertFalse
import org.junit.Test

class PlaybackCaptureSupportTest {
    @Test
    fun reportsUnsupportedBelowApi29() {
        val result = PlaybackCaptureSupport.forSdk(28)

        assertFalse(result.isPlaybackCaptureSupported)
    }
}
