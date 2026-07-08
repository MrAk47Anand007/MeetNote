package com.meetnote.android.capture

import com.meetnote.shared.domain.repository.SessionRepository

class PlaybackAudioRecorder(
    @Suppress("unused")
    private val sessionRepository: SessionRepository
) : MeetingRecorder {
    override suspend fun start(sessionId: String): RecorderResult {
        return RecorderResult.Failure("Playback recorder not wired yet")
    }

    override suspend fun stop(sessionId: String): RecorderResult {
        return RecorderResult.Failure("Playback recorder not wired yet")
    }
}
