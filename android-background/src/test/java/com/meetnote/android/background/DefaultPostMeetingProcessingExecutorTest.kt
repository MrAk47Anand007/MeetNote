package com.meetnote.android.background

import com.meetnote.shared.ai.AiProcessingContext
import com.meetnote.shared.ai.AiProcessingResult
import com.meetnote.shared.ai.SummaryEngine
import com.meetnote.shared.ai.SummaryRequest
import com.meetnote.shared.ai.SummaryResult
import com.meetnote.shared.ai.TranscriptionEngine
import com.meetnote.shared.ai.TranscriptionRequest
import com.meetnote.shared.ai.TranscriptionResult
import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingPolicy
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import com.meetnote.shared.export.MeetingNoteMarkdownFormatter
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
            summaryEngine = FakeSummaryEngine(
                result = AiProcessingResult.UnavailableLocally(
                    processingContext = AiProcessingContext(),
                    message = "No local summary model is installed."
                )
            ),
            markdownFormatter = MeetingNoteMarkdownFormatter(),
            artifactFileFactory = { id -> File(tempDir, "$id-meeting-note.md") }
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
        assertTrue(artifactContents.contains("## Transcript"))
        assertTrue(artifactContents.contains("unavailable-locally"))
        assertTrue(artifactContents.contains("No local model is installed."))
        assertTrue(artifactContents.contains("Summary was not attempted"))
        assertEquals(listOf(audioFile.absolutePath), transcriptionEngine.requestedAudioPaths)
        assertEquals(listOf(ProcessingErrorUpdate(SessionId(sessionId), null)), repository.processingErrors)
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
        val summaryEngine = FakeSummaryEngine(
            result = AiProcessingResult.Completed(
                value = "Meeting summary output.",
                processingContext = AiProcessingContext(processingPolicy = ProcessingPolicy.LOCAL_ONLY),
                processingTier = ProcessingTier.SMALLER_LOCAL
            )
        )
        val sessionId = "session-b"
        val audioFile = File(tempDir, "session-b.raw").apply { writeText("pcm") }

        val result = DefaultPostMeetingProcessingExecutor(
            sessionRepository = repository,
            transcriptionEngine = transcriptionEngine,
            summaryEngine = summaryEngine,
            markdownFormatter = MeetingNoteMarkdownFormatter(),
            artifactFileFactory = { id -> File(tempDir, "$id-meeting-note.md") }
        )
            .process(sessionId, audioFile.absolutePath)

        assertEquals(androidx.work.ListenableWorker.Result.success()::class, result::class)
        val artifactContents = File(repository.processingArtifacts.single().processingArtifactPath).readText()
        assertTrue(artifactContents.contains("Status: completed"))
        assertTrue(artifactContents.contains("Processing Tier: `PRIMARY_LOCAL`"))
        assertTrue(artifactContents.contains("Hello from the meeting transcript."))
        assertTrue(artifactContents.contains("Meeting summary output."))
        assertEquals(listOf("Hello from the meeting transcript."), summaryEngine.requestedTranscripts)
    }

    @Test
    fun processPersistsFailureReasonWhenExecutorThrows() = runBlocking {
        val repository = FakeSessionRepository()
        val result = DefaultPostMeetingProcessingExecutor(
            sessionRepository = repository,
            transcriptionEngine = FakeTranscriptionEngine(result = AiProcessingResult.Deferred(AiProcessingContext())),
            summaryEngine = FakeSummaryEngine(result = AiProcessingResult.Deferred(AiProcessingContext())),
            markdownFormatter = MeetingNoteMarkdownFormatter(),
            artifactFileFactory = { error("artifact creation failed") }
        ).process("session-c", "missing.raw")

        assertEquals(androidx.work.ListenableWorker.Result.failure()::class, result::class)
        assertEquals(
            listOf(
                SessionUpdate(SessionId("session-c"), SessionStatus.PROCESSING),
                SessionUpdate(SessionId("session-c"), SessionStatus.FAILED)
            ),
            repository.statusUpdates
        )
        assertEquals(
            listOf(
                ProcessingErrorUpdate(
                    sessionId = SessionId("session-c"),
                    lastErrorMessage = null
                ),
                ProcessingErrorUpdate(
                    sessionId = SessionId("session-c"),
                    lastErrorMessage = "Post-meeting processing failed: artifact creation failed"
                )
            ),
            repository.processingErrors
        )
    }

    private class FakeSessionRepository : SessionRepository {
        val statusUpdates = mutableListOf<SessionUpdate>()
        val processingArtifacts = mutableListOf<ProcessingArtifactUpdate>()
        val processingErrors = mutableListOf<ProcessingErrorUpdate>()

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

        override suspend fun updateLastError(sessionId: SessionId, lastErrorMessage: String?) {
            processingErrors += ProcessingErrorUpdate(sessionId, lastErrorMessage)
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

    private data class ProcessingErrorUpdate(
        val sessionId: SessionId,
        val lastErrorMessage: String?
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

    private class FakeSummaryEngine(
        private val result: SummaryResult
    ) : SummaryEngine {
        val requestedTranscripts = mutableListOf<String>()

        override suspend fun summarize(request: SummaryRequest): SummaryResult {
            requestedTranscripts += request.transcript
            return result
        }
    }
}
