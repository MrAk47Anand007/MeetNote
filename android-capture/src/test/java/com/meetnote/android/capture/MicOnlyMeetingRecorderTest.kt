package com.meetnote.android.capture

import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingPolicy
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicOnlyMeetingRecorderTest {
    @Test
    fun startUpdatesSessionStatusToCapturing() = runBlocking {
        val repository = RecordingSessionRepository()
        val filesDir = Files.createTempDirectory("mic-recorder-start").toFile()
        val recorder = MicOnlyMeetingRecorder(repository) { sessionId ->
            File(filesDir, "$sessionId.raw")
        }

        val result = recorder.start("session-a")

        assertTrue(result is RecorderResult.Started)
        assertEquals(
            listOf(SessionUpdate(SessionId("session-a"), SessionStatus.CAPTURING)),
            repository.statusUpdates
        )
    }

    @Test
    fun stopAttachesAudioAndUpdatesStatusToRecorded() = runBlocking {
        val repository = RecordingSessionRepository()
        val filesDir = Files.createTempDirectory("mic-recorder-stop").toFile()
        val recorder = MicOnlyMeetingRecorder(repository) { sessionId ->
            File(filesDir, "$sessionId.raw")
        }

        val startResult = recorder.start("session-a") as RecorderResult.Started
        val stopResult = recorder.stop("session-a")

        assertEquals(RecorderResult.Stopped(startResult.filePath), stopResult)
        assertEquals(
            listOf(
                SessionAttachment(SessionId("session-a"), startResult.filePath)
            ),
            repository.attachments
        )
        assertEquals(
            listOf(
                SessionUpdate(SessionId("session-a"), SessionStatus.CAPTURING),
                SessionUpdate(SessionId("session-a"), SessionStatus.RECORDED)
            ),
            repository.statusUpdates
        )
    }

    @Test
    fun startReturnsFailureWhenRecordingFileCannotBePrepared() = runBlocking {
        val repository = RecordingSessionRepository()
        val parentFile = File.createTempFile("mic-recorder-parent", ".tmp")
        val recorder = MicOnlyMeetingRecorder(repository) { sessionId ->
            File(parentFile, "$sessionId.raw")
        }

        val result = recorder.start("session-a")

        assertTrue(result is RecorderResult.Failure)
        assertTrue((result as RecorderResult.Failure).message.startsWith("Failed to prepare recording file:"))
        assertTrue(repository.statusUpdates.isEmpty())
        assertTrue(repository.attachments.isEmpty())
    }

    private class RecordingSessionRepository : SessionRepository {
        val statusUpdates = mutableListOf<SessionUpdate>()
        val attachments = mutableListOf<SessionAttachment>()

        override suspend fun createSession(session: MeetingSession): MeetingSession = session

        override fun observeSessions(): Flow<List<MeetingSession>> = emptyFlow()

        override suspend fun updateStatus(sessionId: SessionId, status: SessionStatus) {
            statusUpdates += SessionUpdate(sessionId, status)
        }

        override suspend fun updateProcessingConfig(
            sessionId: SessionId,
            processingPolicy: ProcessingPolicy,
            processingTier: ProcessingTier
        ) = Unit

        override suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String) {
            attachments += SessionAttachment(sessionId, audioFilePath)
        }
    }

    private data class SessionUpdate(
        val sessionId: SessionId,
        val status: SessionStatus
    )

    private data class SessionAttachment(
        val sessionId: SessionId,
        val audioFilePath: String
    )
}
