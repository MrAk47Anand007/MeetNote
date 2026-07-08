package com.meetnote.android.background

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.meetnote.android.capture.CaptureSource

class MeetingCaptureService : Service() {
    private val notificationFactory by lazy { MeetingCaptureNotificationFactory(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID) ?: "unknown-session"
        val captureSource = intent?.getStringExtra(EXTRA_CAPTURE_SOURCE) ?: CaptureSource.MICROPHONE.name

        startForeground(
            MeetingCaptureNotificationFactory.NOTIFICATION_ID,
            notificationFactory.create(sessionId, captureSource)
        )

        return START_NOT_STICKY
    }

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_CAPTURE_SOURCE = "extra_capture_source"
    }
}
