package com.meetnote.shared.export

import com.meetnote.shared.ai.AiProcessingResult
import com.meetnote.shared.ai.SummaryResult
import com.meetnote.shared.ai.TranscriptionResult

data class MeetingNoteArtifact(
    val sessionId: String,
    val audioFilePath: String,
    val transcriptionResult: TranscriptionResult,
    val summaryResult: SummaryResult?
)

class MeetingNoteMarkdownFormatter {
    fun format(artifact: MeetingNoteArtifact): String = buildString {
        appendLine("# MeetNote Session Artifact")
        appendLine()
        appendLine("- Session ID: `${artifact.sessionId}`")
        appendLine("- Audio File: `${artifact.audioFilePath}`")
        appendLine()
        appendLine("## Transcript")
        append(renderSection(artifact.transcriptionResult))
        appendLine()
        appendLine("## Summary")
        append(
            artifact.summaryResult?.let(::renderSection)
                ?: "Summary was not attempted because no transcript text was available.\n"
        )
    }

    private fun renderSection(result: AiProcessingResult<String>): String = buildString {
        when (result) {
            is AiProcessingResult.Completed -> {
                appendLine("- Status: completed")
                appendLine("- Processing Tier: `${result.processingTier}`")
                appendLine("- Processing Policy: `${result.processingContext.processingPolicy}`")
                appendLine("- Provider Approved: `${result.processingContext.providerProcessingApproved}`")
                appendLine()
                appendLine("```text")
                appendLine(result.value)
                appendLine("```")
            }
            is AiProcessingResult.Deferred -> {
                appendLine("- Status: deferred")
                appendLine("- Message: ${result.message ?: "Processing was deferred."}")
            }
            is AiProcessingResult.RequiresProviderApproval -> {
                appendLine("- Status: provider-approval-required")
                appendLine("- Message: ${result.message ?: "Provider processing approval is required."}")
            }
            is AiProcessingResult.UnavailableLocally -> {
                appendLine("- Status: unavailable-locally")
                appendLine("- Message: ${result.message ?: "No local runtime is available."}")
            }
        }
    }
}
