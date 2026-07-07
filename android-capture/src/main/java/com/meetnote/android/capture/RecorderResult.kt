package com.meetnote.android.capture

sealed interface RecorderResult {
    data class Started(val filePath: String) : RecorderResult
    data class Stopped(val filePath: String) : RecorderResult
    data class Failure(val message: String) : RecorderResult
}
