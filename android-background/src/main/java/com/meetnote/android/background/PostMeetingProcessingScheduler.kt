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
import com.meetnote.shared.ai.AiProcessingResult
import com.meetnote.shared.ai.SummaryEngine
import com.meetnote.shared.ai.SummaryRequest
import com.meetnote.shared.ai.TranscriptionEngine
import com.meetnote.shared.ai.TranscriptionRequest
import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import com.meetnote.shared.export.MeetingNoteArtifact
import com.meetnote.shared.export.MeetingNoteMarkdownFormatter
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
    private val summaryEngine: SummaryEngine,
    private val markdownFormatter: MeetingNoteMarkdownFormatter,
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
            val summaryResult = when (transcriptionResult) {
                is AiProcessingResult.Completed -> summaryEngine.summarize(
                    SummaryRequest(
                        transcript = transcriptionResult.value,
                        processingContext = transcriptionResult.processingContext
                    )
                )
                else -> null
            }
            val artifactFile = artifactFileFactory(sessionId).apply {
                parentFile?.mkdirs()
                writeText(
                    markdownFormatter.format(
                        MeetingNoteArtifact(
                            sessionId = sessionId,
                            audioFilePath = audioFilePath,
                            transcriptionResult = transcriptionResult,
                            summaryResult = summaryResult
                        )
                    )
                )
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

    private suspend fun bestEffortUpdateLastError(sessionId: SessionId, message: String?) {
        try {
            sessionRepository.updateLastError(sessionId, message)
        } catch (_: Exception) {
        }
    }
}

val androidBackgroundModule = module {
    single { WorkManager.getInstance(get()) }
    single<MeetingCaptureServiceController> { AndroidMeetingCaptureServiceController(get()) }
    single<PostMeetingProcessingScheduler> { WorkManagerPostMeetingProcessingScheduler(get()) }
    single { MeetingNoteMarkdownFormatter() }
    single<PostMeetingProcessingExecutor> {
        DefaultPostMeetingProcessingExecutor(
            sessionRepository = get(),
            transcriptionEngine = get(),
            summaryEngine = get(),
            markdownFormatter = get(),
            artifactFileFactory = { sessionId -> File(get<Context>().filesDir, "$sessionId-meeting-note.md") }
        )
    }
}
