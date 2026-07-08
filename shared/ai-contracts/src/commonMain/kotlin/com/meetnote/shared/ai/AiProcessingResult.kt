package com.meetnote.shared.ai

import com.meetnote.shared.domain.model.ProcessingPolicy
import com.meetnote.shared.domain.model.ProcessingTier

data class AiProcessingContext(
    val processingPolicy: ProcessingPolicy = ProcessingPolicy.LOCAL_ONLY,
    val providerProcessingApproved: Boolean = false
)

sealed interface AiProcessingResult<out T> {
    data class Completed<T>(
        val value: T,
        val processingContext: AiProcessingContext,
        val processingTier: ProcessingTier
    ) : AiProcessingResult<T>

    data class RequiresProviderApproval(
        val processingContext: AiProcessingContext,
        val message: String? = null
    ) : AiProcessingResult<Nothing>

    data class Deferred(
        val processingContext: AiProcessingContext,
        val message: String? = null
    ) : AiProcessingResult<Nothing>

    data class UnavailableLocally(
        val processingContext: AiProcessingContext,
        val message: String? = null
    ) : AiProcessingResult<Nothing>
}
