package com.meetnote.android.capture

import android.content.Context
import java.io.File

class MicOnlyMeetingRecorder(
    private val context: Context
) : MeetingRecorder {
    private var lastFile: File? = null

    override suspend fun start(sessionId: String): RecorderResult {
        val file = File(context.filesDir, "$sessionId.raw")
        file.writeBytes(byteArrayOf())
        lastFile = file
        return RecorderResult.Started(file.absolutePath)
    }

    override suspend fun stop(sessionId: String): RecorderResult {
        val file = lastFile ?: return RecorderResult.Failure("Recorder not started")
        return RecorderResult.Stopped(file.absolutePath)
    }
}
