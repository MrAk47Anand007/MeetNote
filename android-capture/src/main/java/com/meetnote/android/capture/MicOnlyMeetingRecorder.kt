package com.meetnote.android.capture

import android.content.Context
import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MicOnlyMeetingRecorder internal constructor(
    private val sessionRepository: SessionRepository,
    private val recordingFileFactory: (String) -> File
) : MeetingRecorder {
    constructor(
        context: Context,
        sessionRepository: SessionRepository
    ) : this(
        sessionRepository = sessionRepository,
        recordingFileFactory = { sessionId -> File(context.filesDir, "$sessionId.raw") }
    )

    private val recorderMutex = Mutex()
    private val sessionState = RecorderSessionState()

    override suspend fun start(sessionId: String): RecorderResult {
        return recorderMutex.withLock {
            val currentSessionId = sessionState.currentSessionId()
            if (currentSessionId != null) {
                return@withLock RecorderResult.Failure("Recorder is active for session $currentSessionId")
            }

            val file = try {
                recordingFileFactory(sessionId).apply {
                    writeBytes(byteArrayOf())
                }
            } catch (exception: Exception) {
                return@withLock RecorderResult.Failure(
                    "Failed to prepare recording file: ${exception.message ?: "unknown error"}"
                )
            }

            try {
                sessionRepository.updateStatus(SessionId(sessionId), SessionStatus.CAPTURING)
            } catch (exception: Exception) {
                return@withLock RecorderResult.Failure(
                    "Failed to persist recording session: ${exception.message ?: "unknown error"}"
                )
            }

            sessionState.start(sessionId, file.absolutePath)
        }
    }

    override suspend fun stop(sessionId: String): RecorderResult {
        return recorderMutex.withLock {
            sessionState.stop(sessionId) { filePath ->
                val domainSessionId = SessionId(sessionId)
                sessionRepository.attachAudioFile(domainSessionId, filePath)
                sessionRepository.updateStatus(domainSessionId, SessionStatus.RECORDED)
            }
        }
    }
}
