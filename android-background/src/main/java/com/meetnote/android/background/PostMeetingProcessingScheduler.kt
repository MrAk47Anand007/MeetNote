package com.meetnote.android.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.meetnote.shared.ai.TranscriptionEngine
import com.meetnote.shared.ai.TranscriptionRequest
import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import java.io.File
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

interface PostMeetingProcessingScheduler {
    fun enqueue(sessionId: String, audioFilePath: String)
}

class WorkManagerPostMeetingProcessingScheduler(
    private val workManager: WorkManager
) : PostMeetingProcessingScheduler {
    override fun enqueue(sessionId: String, audioFilePath: String) {
        val request = OneTimeWorkRequestBuilder<PostMeetingProcessingWorker>()
            .setInputData(
                Data.Builder()
                    .putString(PostMeetingProcessingWorker.KEY_SESSION_ID, sessionId)
                    .putString(PostMeetingProcessingWorker.KEY_AUDIO_FILE_PATH, audioFilePath)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            PostMeetingProcessingWorker.uniqueWorkName(sessionId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

class PostMeetingProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val sessionId = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()
        val audioFilePath = inputData.getString(KEY_AUDIO_FILE_PATH) ?: return Result.failure()

        val processor = GlobalContext.get().get<PostMeetingProcessingExecutor>()
        return processor.process(sessionId, audioFilePath)
    }

    companion object {
        const val KEY_SESSION_ID = "session_id"
        const val KEY_AUDIO_FILE_PATH = "audio_file_path"

        fun uniqueWorkName(sessionId: String): String = "post-meeting-processing-$sessionId"
    }
}

interface PostMeetingProcessingExecutor {
    suspend fun process(
        sessionId: String,
        audioFilePath: String
    ): androidx.work.ListenableWorker.Result
}

class DefaultPostMeetingProcessingExecutor(
    private val sessionRepository: SessionRepository,
    private val transcriptionEngine: TranscriptionEngine,
    private val artifactFileFactory: (String) -> File
) : PostMeetingProcessingExecutor {
    override suspend fun process(
        sessionId: String,
        audioFilePath: String
    ): androidx.work.ListenableWorker.Result {
        val domainSessionId = SessionId(sessionId)
        return try {
            sessionRepository.updateStatus(domainSessionId, SessionStatus.PROCESSING)
            bestEffortUpdateLastError(domainSessionId, null)

            val transcriptionResult = transcriptionEngine.transcribe(
                TranscriptionRequest(audioPath = audioFilePath)
            )
            val artifactFile = artifactFileFactory(sessionId).apply {
                parentFile?.mkdirs()
                writeText(renderTranscriptArtifact(sessionId, audioFilePath, transcriptionResult))
            }

            sessionRepository.attachProcessingArtifact(domainSessionId, artifactFile.absolutePath)
            sessionRepository.updateStatus(domainSessionId, SessionStatus.COMPLETED)
            androidx.work.ListenableWorker.Result.success()
        } catch (exception: Exception) {
            sessionRepository.updateStatus(domainSessionId, SessionStatus.FAILED)
            bestEffortUpdateLastError(
                domainSessionId,
                "Post-meeting processing failed: ${exception.message ?: "unknown error"}"
            )
            androidx.work.ListenableWorker.Result.failure()
        }
    }

    private fun renderTranscriptArtifact(
        sessionId: String,
        audioFilePath: String,
        transcriptionResult: com.meetnote.shared.ai.TranscriptionResult
    ): String = buildString {
        appendLine("MeetNote transcript artifact")
        appendLine("session_id=$sessionId")
        appendLine("audio_file=$audioFilePath")
        when (transcriptionResult) {
            is com.meetnote.shared.ai.AiProcessingResult.Completed -> {
                appendLine("status=transcription_completed")
                appendLine("processing_tier=${transcriptionResult.processingTier}")
                appendLine("processing_policy=${transcriptionResult.processingContext.processingPolicy}")
                appendLine("provider_processing_approved=${transcriptionResult.processingContext.providerProcessingApproved}")
                appendLine("transcript=")
                appendLine(transcriptionResult.value)
            }
            is com.meetnote.shared.ai.AiProcessingResult.Deferred -> {
                appendLine("status=transcription_deferred")
                appendLine("processing_policy=${transcriptionResult.processingContext.processingPolicy}")
                appendLine("provider_processing_approved=${transcriptionResult.processingContext.providerProcessingApproved}")
                appendLine("message=${transcriptionResult.message ?: "Transcription was deferred."}")
            }
            is com.meetnote.shared.ai.AiProcessingResult.RequiresProviderApproval -> {
                appendLine("status=provider_approval_required")
                appendLine("processing_policy=${transcriptionResult.processingContext.processingPolicy}")
                appendLine("provider_processing_approved=${transcriptionResult.processingContext.providerProcessingApproved}")
                appendLine("message=${transcriptionResult.message ?: "Provider processing approval is required."}")
            }
            is com.meetnote.shared.ai.AiProcessingResult.UnavailableLocally -> {
                appendLine("status=transcription_unavailable_locally")
                appendLine("processing_policy=${transcriptionResult.processingContext.processingPolicy}")
                appendLine("provider_processing_approved=${transcriptionResult.processingContext.providerProcessingApproved}")
                appendLine("message=${transcriptionResult.message ?: "No local transcription runtime is available."}")
            }
        }
    }

    private suspend fun bestEffortUpdateLastError(sessionId: SessionId, message: String?) {
        try {
            sessionRepository.updateLastError(sessionId, message)
        } catch (_: Exception) {
        }
    }
}

val androidBackgroundModule = module {
    single { WorkManager.getInstance(get()) }
    single<PostMeetingProcessingScheduler> { WorkManagerPostMeetingProcessingScheduler(get()) }
    single<PostMeetingProcessingExecutor> {
        DefaultPostMeetingProcessingExecutor(
            sessionRepository = get(),
            transcriptionEngine = get(),
            artifactFileFactory = { sessionId -> File(get<Context>().filesDir, "$sessionId-processing.txt") }
        )
    }
}
