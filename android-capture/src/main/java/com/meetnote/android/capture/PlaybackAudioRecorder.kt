package com.meetnote.android.capture

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaybackAudioRecorder internal constructor(
    private val sessionRepository: SessionRepository,
    private val authorizationStore: PlaybackCaptureAuthorizationStore,
    private val recordingFileFactory: (String) -> File,
    private val playbackCaptureSessionFactory: PlaybackCaptureSessionFactory,
    private val sdkIntProvider: () -> Int
) : MeetingRecorder {
    constructor(
        context: Context,
        sessionRepository: SessionRepository,
        authorizationStore: PlaybackCaptureAuthorizationStore
    ) : this(
        sessionRepository = sessionRepository,
        authorizationStore = authorizationStore,
        recordingFileFactory = { sessionId -> File(context.filesDir, "$sessionId-playback.wav") },
        playbackCaptureSessionFactory = PlaybackCaptureSessionFactory {
            AndroidPlaybackAudioCaptureSession(
                mediaProjectionManager = context.getSystemService(MediaProjectionManager::class.java)
            )
        },
        sdkIntProvider = { Build.VERSION.SDK_INT }
    )

    private val recorderMutex = Mutex()
    private val sessionState = RecorderSessionState()
    private var activeCaptureSession: PlaybackAudioCaptureSession? = null

    override suspend fun start(sessionId: String): RecorderResult {
        return recorderMutex.withLock {
            val currentSessionId = sessionState.currentSessionId()
            if (currentSessionId != null) {
                return@withLock RecorderResult.Failure("Recorder is active for session $currentSessionId")
            }

            val support = PlaybackCaptureSupport.forSdk(sdkIntProvider())
            if (!support.isPlaybackCaptureSupported) {
                return@withLock sessionFailure(
                    sessionId = sessionId,
                    message = support.failureReason ?: "Playback capture is not supported on this device"
                )
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

            val permissionIntent = authorizationStore.consume(sessionId)
                ?: return@withLock sessionFailure(
                    sessionId = sessionId,
                    message = "Playback capture permission is not available for this session"
                )

            val captureSession = try {
                playbackCaptureSessionFactory.create().also { session ->
                    session.start(permissionIntent, file)
                }
            } catch (exception: Exception) {
                return@withLock sessionFailure(
                    sessionId = sessionId,
                    message = "Failed to start playback capture: ${exception.message ?: "unknown error"}"
                )
            }

            try {
                sessionRepository.updateStatus(SessionId(sessionId), SessionStatus.CAPTURING)
                bestEffortUpdateLastError(SessionId(sessionId), null)
            } catch (cancellation: CancellationException) {
                captureSession.stop()
                throw cancellation
            } catch (exception: Exception) {
                captureSession.stop()
                return@withLock sessionFailure(
                    sessionId = sessionId,
                    message = "Failed to persist recording session: ${exception.message ?: "unknown error"}"
                )
            }

            activeCaptureSession = captureSession
            sessionState.start(sessionId, file.absolutePath)
        }
    }

    override suspend fun stop(sessionId: String): RecorderResult {
        return recorderMutex.withLock {
            sessionState.stop(sessionId) { filePath ->
                activeCaptureSession?.stop()
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
                activeCaptureSession = null
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

internal interface PlaybackAudioCaptureSession {
    fun start(permissionIntent: Intent, outputFile: File)
    fun stop()
}

internal fun interface PlaybackCaptureSessionFactory {
    fun create(): PlaybackAudioCaptureSession
}

internal class AndroidPlaybackAudioCaptureSession(
    private val mediaProjectionManager: MediaProjectionManager?
) : PlaybackAudioCaptureSession {
    private val isCapturing = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    private var mediaProjection: MediaProjection? = null

    override fun start(permissionIntent: Intent, outputFile: File) {
        if (!isCapturing.compareAndSet(false, true)) {
            throw IllegalStateException("Playback capture already active")
        }

        try {
            val manager = mediaProjectionManager
                ?: throw IllegalStateException("MediaProjectionManager unavailable")
            val projection = manager.getMediaProjection(android.app.Activity.RESULT_OK, permissionIntent)
                ?: throw IllegalStateException("Unable to create MediaProjection")
            mediaProjection = projection

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE_HZ)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()

            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (bufferSize <= 0) {
                throw IllegalStateException("Unable to determine playback buffer size")
            }

            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val recorder = AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(captureConfig)
                .build()
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                throw IllegalStateException("Playback AudioRecord failed to initialize")
            }

            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            recorder.startRecording()
            audioRecord = recorder
            captureThread = Thread(
                PlaybackPcmWriter(
                    recorder = recorder,
                    outputFile = outputFile,
                    isCapturing = isCapturing,
                    bufferSize = bufferSize,
                    channelCount = 2
                ),
                "meetnote-playback-capture"
            ).apply { start() }
        } catch (exception: Exception) {
            isCapturing.set(false)
            cleanup()
            throw exception
        }
    }

    override fun stop() {
        if (!isCapturing.compareAndSet(true, false)) {
            return
        }

        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
        }

        captureThread?.join(2_000)
        cleanup()
    }

    private fun cleanup() {
        captureThread = null
        audioRecord?.release()
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private class PlaybackPcmWriter(
        private val recorder: AudioRecord,
        private val outputFile: File,
        private val isCapturing: AtomicBoolean,
        private val bufferSize: Int,
        private val channelCount: Int
    ) : Runnable {
        override fun run() {
            val buffer = ByteArray(bufferSize)
            WavFileWriter(
                outputFile = outputFile,
                sampleRateHz = SAMPLE_RATE_HZ,
                channelCount = channelCount,
                bitsPerSample = 16
            ).use { output ->
                while (isCapturing.get()) {
                    val bytesRead = recorder.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        output.write(buffer, bytesRead)
                    }
                }
            }
        }
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 16_000
    }
}
