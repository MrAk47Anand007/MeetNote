package com.meetnote.android.background

import android.content.Context
import android.content.Intent
import com.meetnote.android.capture.CaptureSource
import org.koin.dsl.module

interface MeetingCaptureServiceController {
    fun startCapture(sessionId: String, captureSource: CaptureSource)
    fun stopCapture()
}

class AndroidMeetingCaptureServiceController(
    private val context: Context
) : MeetingCaptureServiceController {
    override fun startCapture(sessionId: String, captureSource: CaptureSource) {
        context.startForegroundService(
            Intent(context, MeetingCaptureService::class.java).apply {
                action = MeetingCaptureService.ACTION_START_CAPTURE
                putExtra(MeetingCaptureService.EXTRA_SESSION_ID, sessionId)
                putExtra(MeetingCaptureService.EXTRA_CAPTURE_SOURCE, captureSource.name)
            }
        )
    }

    override fun stopCapture() {
        context.startService(
            Intent(context, MeetingCaptureService::class.java).apply {
                action = MeetingCaptureService.ACTION_STOP_CAPTURE
            }
        )
    }
}
