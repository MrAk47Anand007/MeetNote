package com.meetnote.android.background

import com.meetnote.android.capture.CaptureSource
import com.meetnote.android.capture.MeetingRecorder
import com.meetnote.android.capture.RecorderResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureCommandCoordinatorTest {
    @Test
    fun playbackStartMapsToStarted() = runBlocking {
        val coordinator = CaptureCommandCoordinator(
            microphoneRecorder = FakeRecorder(),
            playbackRecorder = FakeRecorder(startResult = RecorderResult.Started("/tmp/session-a.wav"))
        )

        assertEquals(
            CaptureCommandResult.Started("session-a", CaptureSource.PLAYBACK_AUDIO),
            coordinator.start("session-a", CaptureSource.PLAYBACK_AUDIO)
        )
    }

    @Test
    fun startFailureRetainsSessionIdAndMessage() = runBlocking {
        val coordinator = CaptureCommandCoordinator(
            microphoneRecorder = FakeRecorder(startResult = RecorderResult.Failure("microphone unavailable")),
            playbackRecorder = FakeRecorder()
        )

        assertEquals(
            CaptureCommandResult.Failed("session-a", "microphone unavailable"),
            coordinator.start("session-a", CaptureSource.MICROPHONE)
        )
    }

    @Test
    fun stopMapsRecorderFilePath() = runBlocking {
        val coordinator = CaptureCommandCoordinator(
            microphoneRecorder = FakeRecorder(stopResult = RecorderResult.Stopped("/tmp/session-a.wav")),
            playbackRecorder = FakeRecorder()
        )

        assertEquals(
            CaptureCommandResult.Stopped("session-a", "/tmp/session-a.wav"),
            coordinator.stop("session-a", CaptureSource.MICROPHONE)
        )
    }

    private class FakeRecorder(
        private val startResult: RecorderResult = RecorderResult.Started("fake.wav"),
        private val stopResult: RecorderResult = RecorderResult.Stopped("fake.wav")
    ) : MeetingRecorder {
        override suspend fun start(sessionId: String): RecorderResult = startResult

        override suspend fun stop(sessionId: String): RecorderResult = stopResult
    }
}
