package com.meetnote.android

import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.meetnote.android.capture.ActivityPlaybackCaptureConsentCoordinator
import com.meetnote.android.capture.PlaybackCaptureAuthorizationStore
import com.meetnote.android.ui.MeetNoteApp
import com.meetnote.android.ui.session.SessionViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModel()
    private val playbackCaptureAuthorizationStore: PlaybackCaptureAuthorizationStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        lateinit var consentCoordinator: ActivityPlaybackCaptureConsentCoordinator
        val consentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                consentCoordinator.deliverPendingConsentResult(
                    granted = result.resultCode == RESULT_OK,
                    dataIntent = result.data
                )
            }

        consentCoordinator = ActivityPlaybackCaptureConsentCoordinator(
            mediaProjectionManager = mediaProjectionManager,
            launchConsentIntent = consentLauncher::launch,
            onConsentResolved = { sessionId, granted, dataIntent ->
                lifecycleScope.launch {
                    if (granted && dataIntent != null) {
                        playbackCaptureAuthorizationStore.grant(sessionId, dataIntent)
                    } else {
                        playbackCaptureAuthorizationStore.clear()
                    }
                    sessionViewModel.handlePlaybackCaptureConsent(granted)
                }
            }
        )

        setContent {
            MeetNoteApp(playbackCaptureConsentCoordinator = consentCoordinator)
        }
    }
}
