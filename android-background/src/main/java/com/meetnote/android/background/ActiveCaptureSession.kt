package com.meetnote.android.background

import com.meetnote.android.capture.CaptureSource

data class ActiveCapture(
    val sessionId: String,
    val source: CaptureSource
)

class ActiveCaptureSession {
    private var activeCapture: ActiveCapture? = null

    fun start(sessionId: String, source: CaptureSource) {
        activeCapture = ActiveCapture(sessionId, source)
    }

    fun current(): ActiveCapture? = activeCapture

    fun clear() {
        activeCapture = null
    }
}
