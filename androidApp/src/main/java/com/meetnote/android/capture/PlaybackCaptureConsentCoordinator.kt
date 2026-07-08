package com.meetnote.android.capture

import android.content.Intent
import android.media.projection.MediaProjectionManager

interface PlaybackCaptureConsentCoordinator {
    fun launchPlaybackCaptureConsent(sessionId: String)
    fun onPlaybackCaptureConsentResult(sessionId: String, granted: Boolean, dataIntent: Intent?)
}

class ActivityPlaybackCaptureConsentCoordinator(
    private val mediaProjectionManager: MediaProjectionManager?,
    private val launchConsentIntent: (Intent) -> Unit,
    private val onConsentResolved: (sessionId: String, granted: Boolean, dataIntent: Intent?) -> Unit
) : PlaybackCaptureConsentCoordinator {
    private var pendingSessionId: String? = null

    override fun launchPlaybackCaptureConsent(sessionId: String) {
        pendingSessionId = sessionId
        val manager = mediaProjectionManager
        if (manager == null) {
            onPlaybackCaptureConsentResult(sessionId, granted = false, dataIntent = null)
            return
        }

        launchConsentIntent(manager.createScreenCaptureIntent())
    }

    override fun onPlaybackCaptureConsentResult(
        sessionId: String,
        granted: Boolean,
        dataIntent: Intent?
    ) {
        pendingSessionId = null
        onConsentResolved(sessionId, granted, dataIntent)
    }

    fun deliverPendingConsentResult(granted: Boolean, dataIntent: Intent?) {
        val sessionId = pendingSessionId ?: DEFAULT_PENDING_SESSION_ID
        onPlaybackCaptureConsentResult(sessionId, granted, dataIntent)
    }

    companion object {
        const val DEFAULT_PENDING_SESSION_ID = "pending-playback-capture"
    }
}
