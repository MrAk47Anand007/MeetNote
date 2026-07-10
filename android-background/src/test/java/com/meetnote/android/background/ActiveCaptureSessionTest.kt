package com.meetnote.android.background

import com.meetnote.android.capture.CaptureSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveCaptureSessionTest {
    @Test
    fun startRetainsSessionAndSourceUntilCleared() {
        val activeCapture = ActiveCaptureSession()

        activeCapture.start("session-a", CaptureSource.PLAYBACK_AUDIO)

        assertEquals(
            ActiveCapture("session-a", CaptureSource.PLAYBACK_AUDIO),
            activeCapture.current()
        )

        activeCapture.clear()

        assertNull(activeCapture.current())
    }
}
