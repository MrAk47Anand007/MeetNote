package com.meetnote.android.ailocal

import com.meetnote.shared.ai.AiProcessingResult
import com.meetnote.shared.ai.SummaryEngine
import com.meetnote.shared.ai.SummaryRequest
import com.meetnote.shared.ai.SummaryResult
import com.meetnote.shared.domain.model.ProcessingTier
import org.koin.dsl.module

class LocalSummaryEngine : SummaryEngine {
    override suspend fun summarize(request: SummaryRequest): SummaryResult {
        val transcript = request.transcript.trim()
        if (transcript.isBlank()) {
            return AiProcessingResult.Deferred(
                processingContext = request.processingContext,
                message = "Transcript is empty, so there is nothing to summarize yet."
            )
        }

        val normalizedLines = transcript
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val sentenceCandidates = normalizedLines
            .flatMap(::splitIntoSentences)
            .map { it.trim() }
            .filter { it.length >= MIN_SENTENCE_LENGTH }
        if (sentenceCandidates.isEmpty()) {
            return AiProcessingResult.Deferred(
                processingContext = request.processingContext,
                message = "Transcript does not contain enough structured content to summarize yet."
            )
        }

        val keyPoints = sentenceCandidates
            .distinct()
            .take(MAX_KEY_POINTS)
        val actionItems = sentenceCandidates
            .filter(::looksLikeActionItem)
            .distinct()
            .take(MAX_ACTION_ITEMS)

        val summaryText = buildString {
            appendLine("Summary")
            keyPoints.forEachIndexed { index, sentence ->
                appendLine("${index + 1}. $sentence")
            }
            if (actionItems.isNotEmpty()) {
                appendLine()
                appendLine("Action Items")
                actionItems.forEachIndexed { index, sentence ->
                    appendLine("${index + 1}. $sentence")
                }
            }
        }.trim()

        return AiProcessingResult.Completed(
            value = summaryText,
            processingContext = request.processingContext,
            processingTier = ProcessingTier.SMALLER_LOCAL
        )
    }

    private fun splitIntoSentences(line: String): List<String> {
        return line
            .split(SENTENCE_BOUNDARY_REGEX)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun looksLikeActionItem(sentence: String): Boolean {
        val lowercase = sentence.lowercase()
        return ACTION_ITEM_HINTS.any(lowercase::contains)
    }

    private companion object {
        const val MIN_SENTENCE_LENGTH = 20
        const val MAX_KEY_POINTS = 3
        const val MAX_ACTION_ITEMS = 3
        val SENTENCE_BOUNDARY_REGEX = Regex("(?<=[.!?])\\s+")
        val ACTION_ITEM_HINTS = listOf(
            "will ",
            "need to",
            "follow up",
            "todo",
            "action",
            "next step",
            "send ",
            "prepare ",
            "share ",
            "review ",
            "schedule "
        )
    }
}

val androidAiLocalModule = module {
    single<SummaryEngine> { LocalSummaryEngine() }
}
