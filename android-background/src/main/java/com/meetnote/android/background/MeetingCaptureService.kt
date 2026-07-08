package com.meetnote.android.background

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MeetingCaptureService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
