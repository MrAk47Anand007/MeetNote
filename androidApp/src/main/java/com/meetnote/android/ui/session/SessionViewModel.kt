package com.meetnote.android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetnote.android.capture.CaptureSource
import com.meetnote.android.capture.PlaybackCapturePermissionState
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.usecase.CreateManualSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

data class SessionUiState(
    val title: String = "",
    val selectedMode: ProcessingMode = ProcessingMode.RECORD_THEN_PROCESS,
    val captureSource: CaptureSource = CaptureSource.PLAYBACK_AUDIO,
    val playbackPermissionState: PlaybackCapturePermissionState = PlaybackCapturePermissionState.NotRequested,
    val createdSessionId: String? = null,
    val errorMessage: String? = null,
    val isCreating: Boolean = false
)

class SessionViewModel(
    private val createManualSession: CreateManualSessionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun selectMode(mode: ProcessingMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun updateCaptureSource(source: CaptureSource) {
        _uiState.value = _uiState.value.copy(captureSource = source)
    }

    fun onPlaybackCapturePermissionChanged(state: PlaybackCapturePermissionState) {
        _uiState.value = when (state) {
            PlaybackCapturePermissionState.Denied,
            PlaybackCapturePermissionState.Unsupported -> _uiState.value.copy(
                playbackPermissionState = state,
                captureSource = CaptureSource.MICROPHONE,
                errorMessage = "Playback capture unavailable. Falling back to microphone capture."
            )

            else -> _uiState.value.copy(
                playbackPermissionState = state,
                errorMessage = null
            )
        }
    }

    fun requestPlaybackCaptureConsent() {
        _uiState.value = _uiState.value.copy(
            playbackPermissionState = PlaybackCapturePermissionState.Requesting
        )
    }

    fun handlePlaybackCaptureConsent(granted: Boolean) {
        onPlaybackCapturePermissionChanged(
            if (granted) {
                PlaybackCapturePermissionState.Granted
            } else {
                PlaybackCapturePermissionState.Denied
            }
        )
    }

    fun createSession() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            createdSessionId = null,
            errorMessage = null,
            isCreating = true
        )

        viewModelScope.launch {
            runCatching {
                createManualSession(
                    title = currentState.title.ifBlank { "Untitled Meeting" },
                    processingMode = currentState.selectedMode
                )
            }.onSuccess { session ->
                _uiState.value = _uiState.value.copy(
                    createdSessionId = session.id.value,
                    errorMessage = null,
                    isCreating = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Unable to create session. Please try again.",
                    isCreating = false
                )
            }
        }
    }
}

val androidUiModule = module {
    viewModel { SessionViewModel(get()) }
}
