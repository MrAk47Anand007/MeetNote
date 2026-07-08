package com.meetnote.shared.ai

interface TranscriptionEngine {
    suspend fun transcribe(audioPath: String): String
}
