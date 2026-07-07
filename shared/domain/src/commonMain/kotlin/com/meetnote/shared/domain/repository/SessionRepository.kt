package com.meetnote.shared.domain.repository

import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun createSession(session: MeetingSession): MeetingSession
    fun observeSessions(): Flow<List<MeetingSession>>
    suspend fun updateStatus(sessionId: SessionId, status: SessionStatus)
    suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String)
}
