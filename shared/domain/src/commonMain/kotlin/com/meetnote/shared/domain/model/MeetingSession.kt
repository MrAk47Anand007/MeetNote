package com.meetnote.shared.domain.model

import com.meetnote.shared.core.SessionId

data class MeetingSession(
    val id: SessionId,
    val title: String,
    val status: SessionStatus,
    val processingMode: ProcessingMode,
    val processingPolicy: ProcessingPolicy,
    val processingTier: ProcessingTier,
    val source: SessionSource,
    val audioFilePath: String? = null
)

enum class SessionStatus {
    SCHEDULED,
    CAPTURING,
    RECORDED,
    PROCESSING,
    COMPLETED,
    FAILED
}

enum class SessionSource {
    MANUAL,
    CALENDAR,
    EMAIL
}
