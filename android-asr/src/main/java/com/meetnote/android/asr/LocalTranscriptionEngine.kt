package com.meetnote.android.asr

import com.meetnote.shared.ai.AiProcessingResult
import com.meetnote.shared.ai.TranscriptionEngine
import com.meetnote.shared.ai.TranscriptionRequest
import com.meetnote.shared.ai.TranscriptionResult
import org.koin.dsl.module

class LocalTranscriptionEngine : TranscriptionEngine {
    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult =
        AiProcessingResult.UnavailableLocally(
            processingContext = request.processingContext,
            message = "No bundled on-device transcription runtime is configured yet."
        )
}

val androidAsrModule = module {
    single<TranscriptionEngine> { LocalTranscriptionEngine() }
}
