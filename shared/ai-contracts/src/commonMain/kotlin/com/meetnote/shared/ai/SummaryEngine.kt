package com.meetnote.shared.ai

interface SummaryEngine {
    suspend fun summarize(transcript: String): String
}
