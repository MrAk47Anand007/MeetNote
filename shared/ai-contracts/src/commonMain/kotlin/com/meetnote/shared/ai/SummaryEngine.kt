package com.meetnote.shared.ai

data class SummaryRequest(
    val transcript: String,
    val processingContext: AiProcessingContext = AiProcessingContext()
)

typealias SummaryResult = AiProcessingResult<String>

interface SummaryEngine {
    suspend fun summarize(request: SummaryRequest): SummaryResult
}
