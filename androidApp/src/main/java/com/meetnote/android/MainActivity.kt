package com.meetnote.android

import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.meetnote.android.capture.ActivityPlaybackCaptureConsentCoordinator
import com.meetnote.android.ui.MeetNoteApp
import com.meetnote.android.ui.session.SessionViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModel()

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
            onConsentResolved = { _, granted, _ ->
                sessionViewModel.handlePlaybackCaptureConsent(granted)
            }
        )

        setContent {
            MeetNoteApp(playbackCaptureConsentCoordinator = consentCoordinator)
        }
    }
}
