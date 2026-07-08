package com.meetnote.shared.ai

import com.meetnote.shared.domain.model.ProcessingTier

data class SummaryRequest(
    val transcript: String,
    val processingContext: AiProcessingContext = AiProcessingContext()
)

data class SummaryResult(
    val summary: String,
    val processingContext: AiProcessingContext,
    val processingTier: ProcessingTier
)

interface SummaryEngine {
    suspend fun summarize(request: SummaryRequest): SummaryResult
}
