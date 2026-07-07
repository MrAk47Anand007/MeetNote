package com.meetnote.android.capture

import android.content.Context
import java.io.File

class MicOnlyMeetingRecorder(
    private val context: Context
) : MeetingRecorder {
    private val sessionState = RecorderSessionState()

    override suspend fun start(sessionId: String): RecorderResult {
        val file = File(context.filesDir, "$sessionId.raw")
        file.writeBytes(byteArrayOf())
        return sessionState.start(sessionId, file.absolutePath)
    }

    override suspend fun stop(sessionId: String): RecorderResult {
        return sessionState.stop(sessionId)
    }
}
