package com.meetnote.android.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class RecorderSessionStateTest {
    @Test
    fun stopReturnsFailureForMismatchedSession() {
        val sessionState = RecorderSessionState()

        sessionState.start("session-a", "/tmp/session-a.raw")

        val result = sessionState.stop("session-b")

        assertEquals(
            RecorderResult.Failure("Recorder is active for session session-a"),
            result
        )
    }

    @Test
    fun stopClearsStateAfterSuccessfulStop() {
        val sessionState = RecorderSessionState()

        sessionState.start("session-a", "/tmp/session-a.raw")

        val firstStop = sessionState.stop("session-a")
        val secondStop = sessionState.stop("session-a")

        assertEquals(RecorderResult.Stopped("/tmp/session-a.raw"), firstStop)
        assertEquals(RecorderResult.Failure("Recorder not started"), secondStop)
    }
}
