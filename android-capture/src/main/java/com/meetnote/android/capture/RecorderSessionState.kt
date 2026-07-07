package com.meetnote.android.capture

internal class RecorderSessionState {
    private var activeSessionId: String? = null
    private var activeFilePath: String? = null

    fun start(sessionId: String, filePath: String): RecorderResult.Started {
        activeSessionId = sessionId
        activeFilePath = filePath
        return RecorderResult.Started(filePath)
    }

    inline fun stop(sessionId: String, onStopped: (String) -> Unit = {}): RecorderResult {
        val currentSessionId = activeSessionId ?: return RecorderResult.Failure("Recorder not started")
        val currentFilePath = activeFilePath ?: return RecorderResult.Failure("Recorder not started")

        if (currentSessionId != sessionId) {
            return RecorderResult.Failure("Recorder is active for session $currentSessionId")
        }

        onStopped(currentFilePath)
        activeSessionId = null
        activeFilePath = null
        return RecorderResult.Stopped(currentFilePath)
    }
}
