package com.meetnote.android.background

import com.meetnote.shared.ai.AiProcessingContext
import com.meetnote.shared.ai.AiProcessingResult
import com.meetnote.shared.ai.TranscriptionEngine
import com.meetnote.shared.ai.TranscriptionRequest
import com.meetnote.shared.ai.TranscriptionResult
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

class DefaultPostMeetingProcessingExecutorTest {
    @Test
    fun processWritesUnavailableArtifactAndMarksSessionCompleted() = runBlocking {
        val tempDir = Files.createTempDirectory("meetnote-processing-artifact").toFile()
        val repository = FakeSessionRepository()
        val transcriptionEngine = FakeTranscriptionEngine(
            result = AiProcessingResult.UnavailableLocally(
                processingContext = AiProcessingContext(),
                message = "No local model is installed."
            )
        )
        val sessionId = "session-a"
        val audioFile = File(tempDir, "session-a.raw").apply { writeText("pcm") }

        val result = DefaultPostMeetingProcessingExecutor(
            sessionRepository = repository,
            transcriptionEngine = transcriptionEngine,
            artifactFileFactory = { id -> File(tempDir, "$id-processing.txt") }
        )
            .process(sessionId, audioFile.absolutePath)

        assertEquals(androidx.work.ListenableWorker.Result.success()::class, result::class)
        assertEquals(
            listOf(
                SessionUpdate(SessionId(sessionId), SessionStatus.PROCESSING),
                SessionUpdate(SessionId(sessionId), SessionStatus.COMPLETED)
            ),
            repository.statusUpdates
        )
        assertEquals(1, repository.processingArtifacts.size)
        val artifactFile = File(repository.processingArtifacts.single().processingArtifactPath)
        assertTrue(artifactFile.exists())
        val artifactContents = artifactFile.readText()
        assertTrue(artifactContents.contains("status=transcription_unavailable_locally"))
        assertTrue(artifactContents.contains("message=No local model is installed."))
        assertEquals(listOf(audioFile.absolutePath), transcriptionEngine.requestedAudioPaths)
    }

    @Test
    fun processWritesCompletedTranscriptArtifact() = runBlocking {
        val tempDir = Files.createTempDirectory("meetnote-processing-transcript").toFile()
        val repository = FakeSessionRepository()
        val transcriptionEngine = FakeTranscriptionEngine(
            result = AiProcessingResult.Completed(
                value = "Hello from the meeting transcript.",
                processingContext = AiProcessingContext(processingPolicy = ProcessingPolicy.LOCAL_ONLY),
                processingTier = ProcessingTier.PRIMARY_LOCAL
            )
        )
        val sessionId = "session-b"
        val audioFile = File(tempDir, "session-b.raw").apply { writeText("pcm") }

        val result = DefaultPostMeetingProcessingExecutor(
            sessionRepository = repository,
            transcriptionEngine = transcriptionEngine,
            artifactFileFactory = { id -> File(tempDir, "$id-processing.txt") }
        )
            .process(sessionId, audioFile.absolutePath)

        assertEquals(androidx.work.ListenableWorker.Result.success()::class, result::class)
        val artifactContents = File(repository.processingArtifacts.single().processingArtifactPath).readText()
        assertTrue(artifactContents.contains("status=transcription_completed"))
        assertTrue(artifactContents.contains("processing_tier=PRIMARY_LOCAL"))
        assertTrue(artifactContents.contains("Hello from the meeting transcript."))
    }

    private class FakeSessionRepository : SessionRepository {
        val statusUpdates = mutableListOf<SessionUpdate>()
        val processingArtifacts = mutableListOf<ProcessingArtifactUpdate>()

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

        override suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String) = Unit

        override suspend fun attachProcessingArtifact(sessionId: SessionId, processingArtifactPath: String) {
            processingArtifacts += ProcessingArtifactUpdate(sessionId, processingArtifactPath)
        }
    }

    private data class SessionUpdate(
        val sessionId: SessionId,
        val status: SessionStatus
    )

    private data class ProcessingArtifactUpdate(
        val sessionId: SessionId,
        val processingArtifactPath: String
    )

    private class FakeTranscriptionEngine(
        private val result: TranscriptionResult
    ) : TranscriptionEngine {
        val requestedAudioPaths = mutableListOf<String>()

        override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
            requestedAudioPaths += request.audioPath
            return result
        }
    }
}
