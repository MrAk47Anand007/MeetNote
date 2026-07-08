package com.meetnote.shared.ai

data class TranscriptionRequest(
    val audioPath: String,
    val processingContext: AiProcessingContext = AiProcessingContext()
)

typealias TranscriptionResult = AiProcessingResult<String>

interface TranscriptionEngine {
    suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult
}
