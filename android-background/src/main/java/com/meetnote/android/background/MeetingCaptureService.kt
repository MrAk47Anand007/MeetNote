package com.meetnote.android.background

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.meetnote.android.capture.CaptureSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class MeetingCaptureService : Service() {
    private val notificationFactory by lazy { MeetingCaptureNotificationFactory(this) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coordinator by lazy { GlobalContext.get().get<CaptureCommandCoordinator>() }
    private val activeCapture = ActiveCaptureSession()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_CAPTURE) {
            stopActiveCapture()
            return START_NOT_STICKY
        }

        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID) ?: "unknown-session"
        val captureSource = CaptureSource.entries.firstOrNull {
            it.name == intent?.getStringExtra(EXTRA_CAPTURE_SOURCE)
        } ?: CaptureSource.MICROPHONE

        startForeground(
            MeetingCaptureNotificationFactory.NOTIFICATION_ID,
            notificationFactory.create(sessionId, captureSource.name)
        )
        serviceScope.launch {
            when (val result = coordinator.start(sessionId, captureSource)) {
                is CaptureCommandResult.Started -> activeCapture.start(result.sessionId, result.source)
                is CaptureCommandResult.Failed,
                is CaptureCommandResult.Stopped -> stopService()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopActiveCapture() {
        val capture = activeCapture.current() ?: return stopService()
        serviceScope.launch {
            coordinator.stop(capture.sessionId, capture.source)
            activeCapture.clear()
            stopService()
        }
    }

    private fun stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_START_CAPTURE = "com.meetnote.android.background.action.START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "com.meetnote.android.background.action.STOP_CAPTURE"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_CAPTURE_SOURCE = "extra_capture_source"
    }
}
