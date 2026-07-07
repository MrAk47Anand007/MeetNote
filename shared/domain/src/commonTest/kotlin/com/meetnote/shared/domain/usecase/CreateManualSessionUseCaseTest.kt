package com.meetnote.shared.domain.usecase

import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionSource
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateManualSessionUseCaseTest {
    @Test
    fun createsManualRecordThenProcessSession() = runTest {
        val repository = object : SessionRepository {
            override suspend fun createSession(session: MeetingSession): MeetingSession = session
            override fun observeSessions(): Flow<List<MeetingSession>> = flowOf(emptyList())
            override suspend fun updateStatus(sessionId: SessionId, status: SessionStatus) = Unit
            override suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String) = Unit
        }

        val useCase = CreateManualSessionUseCase(repository)
        val result = useCase("Demo Meeting", ProcessingMode.RECORD_THEN_PROCESS)

        assertEquals("Demo Meeting", result.title)
        assertEquals(ProcessingMode.RECORD_THEN_PROCESS, result.processingMode)
        assertEquals(ProcessingTier.PRIMARY_LOCAL, result.processingTier)
        assertEquals(SessionSource.MANUAL, result.source)
    }
}
