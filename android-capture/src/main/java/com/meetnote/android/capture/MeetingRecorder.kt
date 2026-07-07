package com.meetnote.android.capture

interface MeetingRecorder {
    suspend fun start(sessionId: String): RecorderResult

    suspend fun stop(sessionId: String): RecorderResult
}
