package com.meetnote.android.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class MeetingCaptureNotificationFactory(
    private val context: Context
) {
    fun create(sessionId: String, captureSource: String): Notification {
        ensureChannel()

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("MeetNote capture active")
            .setContentText("Capturing $captureSource audio for session $sessionId")
            .setOngoing(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Meeting capture",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "meetnote_capture"
        const val NOTIFICATION_ID = 1001
    }
}
