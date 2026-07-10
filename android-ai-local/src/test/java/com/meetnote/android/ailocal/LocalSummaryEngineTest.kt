package com.meetnote.android.ailocal

import com.meetnote.shared.ai.AiProcessingContext
import com.meetnote.shared.ai.AiProcessingResult
import com.meetnote.shared.ai.SummaryRequest
import com.meetnote.shared.domain.model.ProcessingPolicy
import com.meetnote.shared.domain.model.ProcessingTier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSummaryEngineTest {
    @Test
    fun summarizeBuildsLocalSummaryAndActionItems() = runBlocking {
        val engine = LocalSummaryEngine()

        val result = engine.summarize(
            SummaryRequest(
                transcript = """
                    We agreed to ship the Android beta next Friday after the playback validation pass.
                    Anand will prepare the release checklist and share it with the team tomorrow.
                    The group also decided to keep provider fallback disabled for the first beta release.
                """.trimIndent(),
                processingContext = AiProcessingContext(processingPolicy = ProcessingPolicy.LOCAL_ONLY)
            )
        )

        assertTrue(result is AiProcessingResult.Completed)
        result as AiProcessingResult.Completed
        assertEquals(ProcessingTier.SMALLER_LOCAL, result.processingTier)
        assertTrue(result.value.contains("Summary"))
        assertTrue(result.value.contains("Action Items"))
        assertTrue(result.value.contains("Anand will prepare the release checklist"))
    }

    @Test
    fun summarizeDefersWhenTranscriptIsBlank() = runBlocking {
        val engine = LocalSummaryEngine()

        val result = engine.summarize(SummaryRequest(transcript = "   "))

        assertEquals(
            AiProcessingResult.Deferred(
                processingContext = AiProcessingContext(),
                message = "Transcript is empty, so there is nothing to summarize yet."
            ),
            result
        )
    }
}
