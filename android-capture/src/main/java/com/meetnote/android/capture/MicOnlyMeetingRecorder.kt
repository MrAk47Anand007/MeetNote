package com.meetnote.android.capture

import android.content.Context
import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import java.io.File

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

    private val sessionState = RecorderSessionState()

    override suspend fun start(sessionId: String): RecorderResult {
        val file = try {
            recordingFileFactory(sessionId).apply {
                writeBytes(byteArrayOf())
            }
        } catch (exception: Exception) {
            return RecorderResult.Failure(
                "Failed to prepare recording file: ${exception.message ?: "unknown error"}"
            )
        }

        sessionRepository.updateStatus(SessionId(sessionId), SessionStatus.CAPTURING)
        return sessionState.start(sessionId, file.absolutePath)
    }

    override suspend fun stop(sessionId: String): RecorderResult {
        return sessionState.stop(sessionId) { filePath ->
            val domainSessionId = SessionId(sessionId)
            sessionRepository.attachAudioFile(domainSessionId, filePath)
            sessionRepository.updateStatus(domainSessionId, SessionStatus.RECORDED)
        }
    }
}
