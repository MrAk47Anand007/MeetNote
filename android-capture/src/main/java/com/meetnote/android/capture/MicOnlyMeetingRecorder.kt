package com.meetnote.android.capture

import android.content.Context
import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MicOnlyMeetingRecorder internal constructor(
    private val sessionRepository: SessionRepository,
    private val recordingFileFactory: (String) -> File,
    private val audioCaptureSessionFactory: AudioCaptureSessionFactory
) : MeetingRecorder {
    constructor(
        context: Context,
        sessionRepository: SessionRepository
    ) : this(
        sessionRepository = sessionRepository,
        recordingFileFactory = { sessionId -> File(context.filesDir, "$sessionId.raw") },
        audioCaptureSessionFactory = AudioCaptureSessionFactory { AndroidMicrophoneAudioCaptureSession() }
    )

    private val recorderMutex = Mutex()
    private val sessionState = RecorderSessionState()
    private var activeAudioCaptureSession: AudioCaptureSession? = null

    override suspend fun start(sessionId: String): RecorderResult {
        return recorderMutex.withLock {
            val currentSessionId = sessionState.currentSessionId()
            if (currentSessionId != null) {
                return@withLock RecorderResult.Failure("Recorder is active for session $currentSessionId")
            }

            val file = try {
                recordingFileFactory(sessionId).apply {
                    parentFile?.let { parent ->
                        if (parent.exists() && !parent.isDirectory) {
                            error("Parent path is not a directory")
                        }
                        if (!parent.exists() && !parent.mkdirs()) {
                            error("Unable to create recording directory")
                        }
                    }
                }
            } catch (exception: Exception) {
                return@withLock sessionFailure(
                    sessionId = sessionId,
                    message = "Failed to prepare recording file: ${exception.message ?: "unknown error"}"
                )
            }

            val audioCaptureSession = try {
                audioCaptureSessionFactory.create().also { session ->
                    session.start(file)
                }
            } catch (exception: Exception) {
                return@withLock sessionFailure(
                    sessionId = sessionId,
                    message = "Failed to start microphone capture: ${exception.message ?: "unknown error"}"
                )
            }

            try {
                sessionRepository.updateStatus(SessionId(sessionId), SessionStatus.CAPTURING)
                bestEffortUpdateLastError(SessionId(sessionId), null)
            } catch (cancellation: CancellationException) {
                audioCaptureSession.stop()
                throw cancellation
            } catch (exception: Exception) {
                audioCaptureSession.stop()
                return@withLock sessionFailure(
                    sessionId = sessionId,
                    message = "Failed to persist recording session: ${exception.message ?: "unknown error"}"
                )
            }

            activeAudioCaptureSession = audioCaptureSession
            sessionState.start(sessionId, file.absolutePath)
        }
    }

    override suspend fun stop(sessionId: String): RecorderResult {
        return recorderMutex.withLock {
            sessionState.stop(sessionId) { filePath ->
                activeAudioCaptureSession?.stop()
                val domainSessionId = SessionId(sessionId)
                sessionRepository.attachAudioFile(domainSessionId, filePath)
                try {
                    sessionRepository.updateStatus(domainSessionId, SessionStatus.RECORDED)
                    bestEffortUpdateLastError(domainSessionId, null)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    bestEffortUpdateLastError(
                        domainSessionId,
                        "Failed to persist recording session: ${exception.message ?: "unknown error"}"
                    )
                    throw exception
                }
                activeAudioCaptureSession = null
            }
        }
    }

    private suspend fun sessionFailure(sessionId: String, message: String): RecorderResult {
        val domainSessionId = SessionId(sessionId)
        bestEffortUpdateStatus(domainSessionId, SessionStatus.FAILED)
        bestEffortUpdateLastError(domainSessionId, message)
        return RecorderResult.Failure(message)
    }

    private suspend fun bestEffortUpdateStatus(sessionId: SessionId, status: SessionStatus) {
        try {
            sessionRepository.updateStatus(sessionId, status)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
        }
    }

    private suspend fun bestEffortUpdateLastError(sessionId: SessionId, message: String?) {
        try {
            sessionRepository.updateLastError(sessionId, message)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
        }
    }
}
