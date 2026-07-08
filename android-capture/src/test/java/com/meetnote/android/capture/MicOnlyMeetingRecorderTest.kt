package com.meetnote.android.capture

import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingPolicy
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicOnlyMeetingRecorderTest {
    @Test
    fun startUpdatesSessionStatusToCapturing() = runBlocking {
        val repository = ConfigurableSessionRepository()
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
        val repository = ConfigurableSessionRepository()
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
    fun startReturnsFailureWhenRecordingIsAlreadyActive() = runBlocking {
        val repository = ConfigurableSessionRepository()
        val filesDir = Files.createTempDirectory("mic-recorder-overlap").toFile()
        val recorder = MicOnlyMeetingRecorder(repository) { sessionId ->
            File(filesDir, "$sessionId.raw")
        }

        val firstStart = recorder.start("session-a")
        val overlappingStart = recorder.start("session-b")

        assertTrue(firstStart is RecorderResult.Started)
        assertEquals(
            RecorderResult.Failure("Recorder is active for session session-a"),
            overlappingStart
        )
        assertEquals(
            listOf(SessionUpdate(SessionId("session-a"), SessionStatus.CAPTURING)),
            repository.statusUpdates
        )
        assertTrue(recorder.stop("session-a") is RecorderResult.Stopped)
    }

    @Test
    fun startReturnsFailureWhenSessionStatusCannotBePersisted() = runBlocking {
        val repository = ConfigurableSessionRepository(
            updateStatusFailure = { status ->
                if (status == SessionStatus.CAPTURING) RuntimeException("boom") else null
            }
        )
        val filesDir = Files.createTempDirectory("mic-recorder-status-failure").toFile()
        val recorder = MicOnlyMeetingRecorder(repository) { sessionId ->
            File(filesDir, "$sessionId.raw")
        }

        val result = recorder.start("session-a")

        assertEquals(
            RecorderResult.Failure("Failed to persist recording session: boom"),
            result
        )
        assertTrue(repository.statusUpdates.isEmpty())
        assertTrue(repository.attachments.isEmpty())
    }

    @Test
    fun startPropagatesCancellationFromSessionStatusPersistence() = runBlocking {
        val repository = ConfigurableSessionRepository(
            updateStatusFailure = { status ->
                if (status == SessionStatus.CAPTURING) CancellationException("cancelled") else null
            }
        )
        val filesDir = Files.createTempDirectory("mic-recorder-cancel-start").toFile()
        val recorder = MicOnlyMeetingRecorder(repository) { sessionId ->
            File(filesDir, "$sessionId.raw")
        }

        try {
            recorder.start("session-a")
            throw AssertionError("Expected CancellationException")
        } catch (exception: CancellationException) {
            assertEquals("cancelled", exception.message)
        }
    }

    @Test
    fun stopReturnsFailureWhenRepositoryWriteFailsAndRetainsActiveSession() = runBlocking {
        val repository = ConfigurableSessionRepository(attachAudioFileFailure = RuntimeException("attach failed"))
        val filesDir = Files.createTempDirectory("mic-recorder-stop-failure").toFile()
        val recorder = MicOnlyMeetingRecorder(repository) { sessionId ->
            File(filesDir, "$sessionId.raw")
        }

        val startResult = recorder.start("session-a")
        val failedStop = recorder.stop("session-a")

        repository.attachAudioFileFailure = null

        val successfulStop = recorder.stop("session-a")

        assertTrue(startResult is RecorderResult.Started)
        assertEquals(
            RecorderResult.Failure("Failed to persist recording session: attach failed"),
            failedStop
        )
        assertEquals(
            RecorderResult.Stopped((startResult as RecorderResult.Started).filePath),
            successfulStop
        )
        assertEquals(
            listOf(
                SessionUpdate(SessionId("session-a"), SessionStatus.CAPTURING),
                SessionUpdate(SessionId("session-a"), SessionStatus.RECORDED)
            ),
            repository.statusUpdates
        )
        assertEquals(
            listOf(
                SessionAttachment(SessionId("session-a"), startResult.filePath)
            ),
            repository.attachments
        )
    }

    @Test
    fun stopReturnsFailureWhenRecordedStatusCannotBePersistedAfterAttachment() = runBlocking {
        val repository = ConfigurableSessionRepository(
            updateStatusFailure = { status ->
                if (status == SessionStatus.RECORDED) RuntimeException("recorded failed") else null
            }
        )
        val filesDir = Files.createTempDirectory("mic-recorder-recorded-status-failure").toFile()
        val recorder = MicOnlyMeetingRecorder(repository) { sessionId ->
            File(filesDir, "$sessionId.raw")
        }

        val startResult = recorder.start("session-a") as RecorderResult.Started
        val failedStop = recorder.stop("session-a")

        repository.updateStatusFailure = null
        val successfulStop = recorder.stop("session-a")

        assertEquals(
            RecorderResult.Failure("Failed to persist recording session: recorded failed"),
            failedStop
        )
        assertEquals(RecorderResult.Stopped(startResult.filePath), successfulStop)
        assertEquals(
            listOf(
                SessionAttachment(SessionId("session-a"), startResult.filePath),
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
    fun stopPropagatesCancellationWhenRecordedStatusCannotBePersistedAfterAttachment() = runBlocking {
        val repository = ConfigurableSessionRepository(
            updateStatusFailure = { status ->
                if (status == SessionStatus.RECORDED) CancellationException("cancelled") else null
            }
        )
        val filesDir = Files.createTempDirectory("mic-recorder-recorded-status-cancel").toFile()
        val recorder = MicOnlyMeetingRecorder(repository) { sessionId ->
            File(filesDir, "$sessionId.raw")
        }

        val startResult = recorder.start("session-a") as RecorderResult.Started

        try {
            recorder.stop("session-a")
            throw AssertionError("Expected CancellationException")
        } catch (exception: CancellationException) {
            assertEquals("cancelled", exception.message)
        }

        repository.updateStatusFailure = null
        val successfulStop = recorder.stop("session-a")

        assertEquals(RecorderResult.Stopped(startResult.filePath), successfulStop)
        assertEquals(
            listOf(
                SessionAttachment(SessionId("session-a"), startResult.filePath),
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
    fun stopPropagatesCancellationFromRepositoryWrites() = runBlocking {
        val repository = ConfigurableSessionRepository(attachAudioFileFailure = CancellationException("cancelled"))
        val filesDir = Files.createTempDirectory("mic-recorder-cancel-stop").toFile()
        val recorder = MicOnlyMeetingRecorder(repository) { sessionId ->
            File(filesDir, "$sessionId.raw")
        }

        recorder.start("session-a")

        try {
            recorder.stop("session-a")
            throw AssertionError("Expected CancellationException")
        } catch (exception: CancellationException) {
            assertEquals("cancelled", exception.message)
        }
    }

    @Test
    fun startReturnsFailureWhenRecordingFileCannotBePrepared() = runBlocking {
        val repository = ConfigurableSessionRepository()
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

    private class ConfigurableSessionRepository(
        var updateStatusFailure: ((SessionStatus) -> Throwable?)? = null,
        var attachAudioFileFailure: Throwable? = null
    ) : SessionRepository {
        val statusUpdates = mutableListOf<SessionUpdate>()
        val attachments = mutableListOf<SessionAttachment>()

        override suspend fun createSession(session: MeetingSession): MeetingSession = session

        override fun observeSessions(): Flow<List<MeetingSession>> = emptyFlow()

        override suspend fun updateStatus(sessionId: SessionId, status: SessionStatus) {
            updateStatusFailure?.invoke(status)?.let { throw it }
            statusUpdates += SessionUpdate(sessionId, status)
        }

        override suspend fun updateProcessingConfig(
            sessionId: SessionId,
            processingPolicy: ProcessingPolicy,
            processingTier: ProcessingTier
        ) = Unit

        override suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String) {
            attachAudioFileFailure?.let { throw it }
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
