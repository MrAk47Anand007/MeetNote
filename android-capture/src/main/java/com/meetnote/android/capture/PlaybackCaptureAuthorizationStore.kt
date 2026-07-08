package com.meetnote.android.capture

import android.content.Intent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface PlaybackCaptureAuthorizationStore {
    suspend fun grant(sessionId: String, dataIntent: Intent)
    suspend fun consume(sessionId: String): Intent?
    suspend fun clear()
}

class InMemoryPlaybackCaptureAuthorizationStore : PlaybackCaptureAuthorizationStore {
    private val mutex = Mutex()
    private var grantedSessionId: String? = null
    private var grantedIntent: Intent? = null

    override suspend fun grant(sessionId: String, dataIntent: Intent) {
        mutex.withLock {
            grantedSessionId = sessionId
            grantedIntent = Intent(dataIntent)
        }
    }

    override suspend fun consume(sessionId: String): Intent? {
        return mutex.withLock {
            if (grantedSessionId != sessionId) {
                return@withLock null
            }
            val intent = grantedIntent?.let(::Intent)
            grantedSessionId = null
            grantedIntent = null
            intent
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            grantedSessionId = null
            grantedIntent = null
        }
    }
}
