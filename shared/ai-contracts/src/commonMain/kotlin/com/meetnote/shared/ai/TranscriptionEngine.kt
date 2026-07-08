package com.meetnote.shared.ai

import com.meetnote.shared.domain.model.ProcessingPolicy
import com.meetnote.shared.domain.model.ProcessingTier

data class AiProcessingContext(
    val processingPolicy: ProcessingPolicy = ProcessingPolicy.LOCAL_ONLY,
    val providerProcessingApproved: Boolean = false
)

data class TranscriptionRequest(
    val audioPath: String,
    val processingContext: AiProcessingContext = AiProcessingContext()
)

data class TranscriptionResult(
    val transcript: String,
    val processingContext: AiProcessingContext,
    val processingTier: ProcessingTier
)

interface TranscriptionEngine {
    suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult
}
