package com.meetnote.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meetnote.android.capture.ActivityPlaybackCaptureConsentCoordinator
import com.meetnote.android.capture.CaptureSource
import com.meetnote.android.capture.PlaybackCaptureConsentCoordinator
import com.meetnote.android.ui.session.SessionScreen
import com.meetnote.android.ui.session.SessionViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MeetNoteApp(
    playbackCaptureConsentCoordinator: PlaybackCaptureConsentCoordinator? = null,
    viewModel: SessionViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SessionScreen(
        state = state,
        onTitleChanged = viewModel::updateTitle,
        onModeSelected = viewModel::selectMode,
        onCaptureSourceSelected = viewModel::updateCaptureSource,
        onRequestPlaybackCaptureConsent = {
            viewModel.requestPlaybackCaptureConsent()
            if (state.captureSource == CaptureSource.PLAYBACK_AUDIO) {
                playbackCaptureConsentCoordinator?.launchPlaybackCaptureConsent(
                    state.createdSessionId
                        ?: ActivityPlaybackCaptureConsentCoordinator.DEFAULT_PENDING_SESSION_ID
                )
            }
        },
        onCreateSession = viewModel::createSession,
        onStartCapture = viewModel::startCapture,
        onStopCapture = viewModel::stopCapture
    )
}
