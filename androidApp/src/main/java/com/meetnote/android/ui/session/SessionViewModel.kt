package com.meetnote.android.ui.session

import com.meetnote.android.background.MeetingCaptureServiceController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetnote.android.background.PostMeetingProcessingScheduler
import com.meetnote.android.capture.CaptureSource
import com.meetnote.android.capture.MeetingRecorder
import com.meetnote.android.capture.PlaybackCapturePermissionState
import com.meetnote.android.capture.RecorderResult
import com.meetnote.android.core.MICROPHONE_RECORDER_QUALIFIER
import com.meetnote.android.core.PLAYBACK_RECORDER_QUALIFIER
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.repository.SessionRepository
import com.meetnote.shared.domain.usecase.CreateManualSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

data class SessionUiState(
    val title: String = "",
    val selectedMode: ProcessingMode = ProcessingMode.RECORD_THEN_PROCESS,
    val captureSource: CaptureSource = CaptureSource.PLAYBACK_AUDIO,
    val playbackPermissionState: PlaybackCapturePermissionState = PlaybackCapturePermissionState.NotRequested,
    val sessions: List<MeetingSession> = emptyList(),
    val createdSessionId: String? = null,
    val activeCaptureSessionId: String? = null,
    val activeCaptureSource: CaptureSource? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isCreating: Boolean = false
)

class SessionViewModel(
    private val createManualSession: CreateManualSessionUseCase,
    private val sessionRepository: SessionRepository,
    private val microphoneRecorder: MeetingRecorder,
    private val playbackRecorder: MeetingRecorder,
    private val captureServiceController: MeetingCaptureServiceController,
    private val postMeetingProcessingScheduler: PostMeetingProcessingScheduler
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.observeSessions().collect { sessions ->
                _uiState.value = _uiState.value.copy(sessions = sessions)
            }
        }
    }

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
                infoMessage = null,
                errorMessage = "Playback capture unavailable. Falling back to microphone capture."
            )

            else -> _uiState.value.copy(
                playbackPermissionState = state,
                infoMessage = null,
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
            infoMessage = null,
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
                    title = "",
                    createdSessionId = session.id.value,
                    infoMessage = null,
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

    fun startCapture(sessionId: String) {
        val currentState = _uiState.value
        if (currentState.activeCaptureSessionId != null) {
            _uiState.value = currentState.copy(
                infoMessage = null,
                errorMessage = "A capture session is already active."
            )
            return
        }

        val selectedSource = currentState.captureSource
        if (selectedSource == CaptureSource.PLAYBACK_AUDIO &&
            currentState.playbackPermissionState != PlaybackCapturePermissionState.Granted
        ) {
            _uiState.value = currentState.copy(
                infoMessage = null,
                errorMessage = "Grant playback capture permission first, or switch to microphone capture."
            )
            return
        }

        viewModelScope.launch {
            captureServiceController.startCapture(sessionId, selectedSource)
            when (val result = recorderFor(selectedSource).start(sessionId)) {
                is RecorderResult.Started -> {
                    _uiState.value = _uiState.value.copy(
                        activeCaptureSessionId = sessionId,
                        activeCaptureSource = selectedSource,
                        infoMessage = null,
                        errorMessage = null
                    )
                }

                is RecorderResult.Failure -> {
                    captureServiceController.stopCapture()
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }

                is RecorderResult.Stopped -> Unit
            }
        }
    }

    fun stopCapture(sessionId: String) {
        val currentState = _uiState.value
        val captureSource = currentState.activeCaptureSource ?: currentState.captureSource

        viewModelScope.launch {
            when (val result = recorderFor(captureSource).stop(sessionId)) {
                is RecorderResult.Stopped -> {
                    captureServiceController.stopCapture()
                    val session = _uiState.value.sessions.firstOrNull { it.id.value == sessionId }
                    val queuedProcessing = session?.processingMode == ProcessingMode.RECORD_THEN_PROCESS
                    if (queuedProcessing) {
                        postMeetingProcessingScheduler.enqueue(sessionId, result.filePath)
                    }
                    _uiState.value = _uiState.value.copy(
                        activeCaptureSessionId = null,
                        activeCaptureSource = null,
                        infoMessage = if (queuedProcessing) {
                            "Queued post-meeting processing for this session. Local AI runtimes may still be unavailable."
                        } else {
                            "Capture stopped. Session is ready for the next live-assist step."
                        },
                        errorMessage = null
                    )
                }

                is RecorderResult.Failure -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }

                is RecorderResult.Started -> Unit
            }
        }
    }

    private fun recorderFor(source: CaptureSource): MeetingRecorder {
        return when (source) {
            CaptureSource.PLAYBACK_AUDIO -> playbackRecorder
            CaptureSource.MICROPHONE -> microphoneRecorder
        }
    }
}

val androidUiModule = module {
    viewModel {
        SessionViewModel(
            createManualSession = get(),
            sessionRepository = get(),
            microphoneRecorder = get(named(MICROPHONE_RECORDER_QUALIFIER)),
            playbackRecorder = get(named(PLAYBACK_RECORDER_QUALIFIER)),
            captureServiceController = get(),
            postMeetingProcessingScheduler = get()
        )
    }
}
