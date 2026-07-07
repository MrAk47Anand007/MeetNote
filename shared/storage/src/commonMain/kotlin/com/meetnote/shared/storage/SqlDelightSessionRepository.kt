package com.meetnote.shared.storage

import app.cash.sqldelight.Query
import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.model.ProcessingPolicy
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionSource
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlDelightSessionRepository(
    private val storage: SessionStorage,
    private val queryToFlow: QueryToFlow
) : SessionRepository {
    override suspend fun createSession(session: MeetingSession): MeetingSession {
        storage.database.meetNoteDatabaseQueries.insertSession(
            id = session.id.value,
            title = session.title,
            status = session.status.name,
            processing_mode = session.processingMode.name,
            processing_policy = session.processingPolicy.name,
            processing_tier = session.processingTier.name,
            source = session.source.name,
            audio_file_path = session.audioFilePath
        )
        return session
    }

    override fun observeSessions(): Flow<List<MeetingSession>> {
        return queryToFlow
            .asFlow(storage.database.meetNoteDatabaseQueries.selectAll())
            .map { rows ->
                rows.map { row ->
                    MeetingSession(
                        id = SessionId(row.id),
                        title = row.title,
                        status = SessionStatus.valueOf(row.status),
                        processingMode = ProcessingMode.valueOf(row.processing_mode),
                        processingPolicy = ProcessingPolicy.valueOf(row.processing_policy),
                        processingTier = ProcessingTier.valueOf(row.processing_tier),
                        source = SessionSource.valueOf(row.source),
                        audioFilePath = row.audio_file_path
                    )
                }
            }
    }

    override suspend fun updateStatus(sessionId: SessionId, status: SessionStatus) {
        storage.database.meetNoteDatabaseQueries.updateStatus(
            status = status.name,
            id = sessionId.value
        )
    }

    override suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String) {
        storage.database.meetNoteDatabaseQueries.attachAudioFile(
            audio_file_path = audioFilePath,
            id = sessionId.value
        )
    }
}

interface QueryToFlow {
    fun <T : Any> asFlow(query: Query<T>): Flow<List<T>>
}
