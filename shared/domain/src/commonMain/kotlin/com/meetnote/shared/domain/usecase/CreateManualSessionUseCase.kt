package com.meetnote.shared.domain.usecase

import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionSource
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateManualSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(title: String, processingMode: ProcessingMode): MeetingSession {
        val session = MeetingSession(
            id = SessionId(Uuid.random().toString()),
            title = title,
            status = SessionStatus.SCHEDULED,
            processingMode = processingMode,
            processingTier = ProcessingTier.PRIMARY_LOCAL,
            source = SessionSource.MANUAL
        )
        return sessionRepository.createSession(session)
    }
}
