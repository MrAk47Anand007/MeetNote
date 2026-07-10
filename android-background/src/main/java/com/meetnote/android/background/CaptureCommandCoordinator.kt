package com.meetnote.android.background

import com.meetnote.android.capture.CaptureSource
import com.meetnote.android.capture.MeetingRecorder
import com.meetnote.android.capture.RecorderResult

sealed interface CaptureCommandResult {
    data class Started(
        val sessionId: String,
        val source: CaptureSource
    ) : CaptureCommandResult

    data class Stopped(
        val sessionId: String,
        val filePath: String
    ) : CaptureCommandResult

    data class Failed(
        val sessionId: String,
        val message: String
    ) : CaptureCommandResult
}

class CaptureCommandCoordinator(
    private val microphoneRecorder: MeetingRecorder,
    private val playbackRecorder: MeetingRecorder
) {
    suspend fun start(sessionId: String, source: CaptureSource): CaptureCommandResult =
        when (val result = recorderFor(source).start(sessionId)) {
            is RecorderResult.Started -> CaptureCommandResult.Started(sessionId, source)
            is RecorderResult.Failure -> CaptureCommandResult.Failed(sessionId, result.message)
            is RecorderResult.Stopped -> CaptureCommandResult.Failed(
                sessionId,
                "Recorder stopped before capture could start"
            )
        }

    suspend fun stop(sessionId: String, source: CaptureSource): CaptureCommandResult =
        when (val result = recorderFor(source).stop(sessionId)) {
            is RecorderResult.Stopped -> CaptureCommandResult.Stopped(sessionId, result.filePath)
            is RecorderResult.Failure -> CaptureCommandResult.Failed(sessionId, result.message)
            is RecorderResult.Started -> CaptureCommandResult.Failed(
                sessionId,
                "Recorder remained active while stopping capture"
            )
        }

    private fun recorderFor(source: CaptureSource): MeetingRecorder = when (source) {
        CaptureSource.MICROPHONE -> microphoneRecorder
        CaptureSource.PLAYBACK_AUDIO -> playbackRecorder
    }
}
