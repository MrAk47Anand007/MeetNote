package com.meetnote.android.capture

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal interface AudioCaptureSession {
    fun start(outputFile: File)
    fun stop()
}

internal fun interface AudioCaptureSessionFactory {
    fun create(): AudioCaptureSession
}

internal class AndroidMicrophoneAudioCaptureSession : AudioCaptureSession {
    private val isCapturing = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null

    override fun start(outputFile: File) {
        if (!isCapturing.compareAndSet(false, true)) {
            throw IllegalStateException("Microphone capture already active")
        }

        try {
            val sampleRate = SAMPLE_RATE_HZ
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (bufferSize <= 0) {
                throw IllegalStateException("Unable to determine microphone buffer size")
            }

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                throw IllegalStateException("Microphone recorder failed to initialize")
            }

            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            recorder.startRecording()
            audioRecord = recorder
            captureThread = Thread(
                AudioRecordPcmWriter(
                    recorder = recorder,
                    outputFile = outputFile,
                    isCapturing = isCapturing,
                    bufferSize = bufferSize,
                    sampleRateHz = sampleRate,
                    channelCount = 1
                ),
                "meetnote-mic-capture"
            ).apply { start() }
        } catch (exception: Exception) {
            isCapturing.set(false)
            cleanupRecorder()
            throw exception
        }
    }

    override fun stop() {
        if (!isCapturing.compareAndSet(true, false)) {
            return
        }

        val recorder = audioRecord
        try {
            recorder?.stop()
        } catch (_: IllegalStateException) {
            // If start failed mid-flight or stop is retried after the recorder is already stopped,
            // the persisted file is still the source of truth for subsequent handling.
        }

        captureThread?.join(2_000)
        cleanupRecorder()
    }

    private fun cleanupRecorder() {
        captureThread = null
        audioRecord?.release()
        audioRecord = null
    }

    private class AudioRecordPcmWriter(
        private val recorder: AudioRecord,
        private val outputFile: File,
        private val isCapturing: AtomicBoolean,
        private val bufferSize: Int,
        private val sampleRateHz: Int,
        private val channelCount: Int
    ) : Runnable {
        override fun run() {
            val buffer = ByteArray(bufferSize)
            WavFileWriter(
                outputFile = outputFile,
                sampleRateHz = sampleRateHz,
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
