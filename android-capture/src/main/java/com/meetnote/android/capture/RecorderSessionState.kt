package com.meetnote.android.capture

internal class RecorderSessionState {
    private var activeSessionId: String? = null
    private var activeFilePath: String? = null

    fun currentSessionId(): String? = activeSessionId

    fun start(sessionId: String, filePath: String): RecorderResult {
        val currentSessionId = activeSessionId
        if (currentSessionId != null) {
            return RecorderResult.Failure("Recorder is active for session $currentSessionId")
        }

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

        return try {
            onStopped(currentFilePath)
            activeSessionId = null
            activeFilePath = null
            RecorderResult.Stopped(currentFilePath)
        } catch (exception: Exception) {
            RecorderResult.Failure(
                "Failed to persist recording session: ${exception.message ?: "unknown error"}"
            )
        }
    }
}
