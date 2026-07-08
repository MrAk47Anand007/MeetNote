package com.meetnote.android.capture

import android.content.Intent
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

class PlaybackAudioRecorderTest {
    @Test
    fun startFailsWithoutGrantedPermission() = runBlocking {
        val repository = ConfigurableSessionRepository()
        val authorizationStore = FakePlaybackAuthorizationStore()
        val filesDir = Files.createTempDirectory("playback-recorder-no-permission").toFile()
        val recorder = PlaybackAudioRecorder(
            sessionRepository = repository,
            authorizationStore = authorizationStore,
            recordingFileFactory = { sessionId -> File(filesDir, "$sessionId.raw") },
            playbackCaptureSessionFactory = PlaybackCaptureSessionFactory { FakePlaybackCaptureSession() },
            sdkIntProvider = { 29 }
        )

        val result = recorder.start("session-a")

        assertEquals(
            RecorderResult.Failure("Playback capture permission is not available for this session"),
            result
        )
        assertTrue(repository.statusUpdates.isEmpty())
        assertEquals(
            listOf(SessionUpdate(SessionId("session-a"), SessionStatus.FAILED)),
            repository.failedStatusUpdates
        )
        assertEquals(
            listOf(SessionError(SessionId("session-a"), "Playback capture permission is not available for this session")),
            repository.lastErrors
        )
    }

    @Test
    fun startAndStopPersistPlaybackCaptureSession() = runBlocking {
        val repository = ConfigurableSessionRepository()
        val authorizationStore = FakePlaybackAuthorizationStore()
        authorizationStore.grant("session-a", Intent("playback"))
        val filesDir = Files.createTempDirectory("playback-recorder-success").toFile()
        val captureSession = FakePlaybackCaptureSession()
        val recorder = PlaybackAudioRecorder(
            sessionRepository = repository,
            authorizationStore = authorizationStore,
            recordingFileFactory = { sessionId -> File(filesDir, "$sessionId.raw") },
            playbackCaptureSessionFactory = PlaybackCaptureSessionFactory { captureSession },
            sdkIntProvider = { 29 }
        )

        val startResult = recorder.start("session-a") as RecorderResult.Started
        val stopResult = recorder.stop("session-a")

        assertEquals(1, captureSession.startCalls.size)
        assertEquals(1, captureSession.stopCalls)
        assertEquals(RecorderResult.Stopped(startResult.filePath), stopResult)
        assertEquals(
            listOf(
                SessionUpdate(SessionId("session-a"), SessionStatus.CAPTURING),
                SessionUpdate(SessionId("session-a"), SessionStatus.RECORDED)
            ),
            repository.statusUpdates
        )
        assertEquals(
            listOf(SessionAttachment(SessionId("session-a"), startResult.filePath)),
            repository.attachments
        )
    }

    @Test
    fun startFailsBelowApi29() = runBlocking {
        val repository = ConfigurableSessionRepository()
        val authorizationStore = FakePlaybackAuthorizationStore()
        authorizationStore.grant("session-a", Intent("playback"))
        val filesDir = Files.createTempDirectory("playback-recorder-sdk").toFile()
        val recorder = PlaybackAudioRecorder(
            sessionRepository = repository,
            authorizationStore = authorizationStore,
            recordingFileFactory = { sessionId -> File(filesDir, "$sessionId.raw") },
            playbackCaptureSessionFactory = PlaybackCaptureSessionFactory { FakePlaybackCaptureSession() },
            sdkIntProvider = { 28 }
        )

        val result = recorder.start("session-a")

        assertEquals(
            RecorderResult.Failure("Playback capture requires Android 10+"),
            result
        )
    }

    private class FakePlaybackAuthorizationStore : PlaybackCaptureAuthorizationStore {
        private var grantedSessionId: String? = null
        private var grantedIntent: Intent? = null

        override suspend fun grant(sessionId: String, dataIntent: Intent) {
            grantedSessionId = sessionId
            grantedIntent = dataIntent
        }

        override suspend fun consume(sessionId: String): Intent? {
            if (grantedSessionId != sessionId) return null
            val intent = grantedIntent
            grantedSessionId = null
            grantedIntent = null
            return intent
        }

        override suspend fun clear() {
            grantedSessionId = null
            grantedIntent = null
        }
    }

    private class FakePlaybackCaptureSession : PlaybackAudioCaptureSession {
        val startCalls = mutableListOf<Pair<Intent, File>>()
        var stopCalls = 0

        override fun start(permissionIntent: Intent, outputFile: File) {
            startCalls += permissionIntent to outputFile
            outputFile.writeText("playback")
        }

        override fun stop() {
            stopCalls += 1
        }
    }

    private class ConfigurableSessionRepository : SessionRepository {
        val statusUpdates = mutableListOf<SessionUpdate>()
        val failedStatusUpdates = mutableListOf<SessionUpdate>()
        val attachments = mutableListOf<SessionAttachment>()
        val lastErrors = mutableListOf<SessionError>()

        override suspend fun createSession(session: MeetingSession): MeetingSession = session

        override fun observeSessions(): Flow<List<MeetingSession>> = emptyFlow()

        override suspend fun updateStatus(sessionId: SessionId, status: SessionStatus) {
            if (status == SessionStatus.FAILED) {
                failedStatusUpdates += SessionUpdate(sessionId, status)
            } else {
                statusUpdates += SessionUpdate(sessionId, status)
            }
        }

        override suspend fun updateProcessingConfig(
            sessionId: SessionId,
            processingPolicy: ProcessingPolicy,
            processingTier: ProcessingTier
        ) = Unit

        override suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String) {
            attachments += SessionAttachment(sessionId, audioFilePath)
        }

        override suspend fun attachProcessingArtifact(sessionId: SessionId, processingArtifactPath: String) = Unit

        override suspend fun updateLastError(sessionId: SessionId, lastErrorMessage: String?) {
            lastErrors += SessionError(sessionId, lastErrorMessage)
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

    private data class SessionError(
        val sessionId: SessionId,
        val lastErrorMessage: String?
    )
}
