package com.meetnote.android.ui.session

import com.meetnote.android.capture.CaptureSource
import com.meetnote.android.capture.PlaybackCapturePermissionState
import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.model.ProcessingPolicy
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import com.meetnote.shared.domain.usecase.CreateManualSessionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createSessionUsesFallbackTitleAndSelectedMode() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val viewModel = createViewModel(repository)

        viewModel.selectMode(ProcessingMode.LIVE_ASSIST)
        viewModel.createSession()
        advanceUntilIdle()

        assertEquals("Untitled Meeting", repository.createdSessions.single().title)
        assertEquals(ProcessingMode.LIVE_ASSIST, repository.createdSessions.single().processingMode)
        assertEquals(
            repository.createdSessions.single().id.value,
            viewModel.uiState.value.createdSessionId
        )
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun createSessionExposesFailureAndAllowsRetry() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        repository.failNextCreate = true
        val viewModel = createViewModel(repository)

        viewModel.createSession()
        advanceUntilIdle()

        assertEquals("Unable to create session. Please try again.", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.createdSessionId)
        assertEquals(false, viewModel.uiState.value.isCreating)

        viewModel.selectMode(ProcessingMode.RECORD_THEN_PROCESS)
        viewModel.createSession()
        advanceUntilIdle()

        assertEquals(1, repository.createdSessions.size)
        assertEquals(
            repository.createdSessions.single().id.value,
            viewModel.uiState.value.createdSessionId
        )
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isCreating)
    }

    @Test
    fun updatesCaptureSourceToPlayback() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val viewModel = createViewModel(repository)

        viewModel.updateCaptureSource(CaptureSource.PLAYBACK_AUDIO)

        assertEquals(CaptureSource.PLAYBACK_AUDIO, viewModel.uiState.value.captureSource)
    }

    @Test
    fun storesPermissionDeniedState() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val viewModel = createViewModel(repository)

        viewModel.onPlaybackCapturePermissionChanged(PlaybackCapturePermissionState.Denied)

        assertEquals(
            PlaybackCapturePermissionState.Denied,
            viewModel.uiState.value.playbackPermissionState
        )
    }

    @Test
    fun marksPermissionRequestingBeforeLaunch() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val viewModel = createViewModel(repository)

        viewModel.requestPlaybackCaptureConsent()

        assertEquals(
            PlaybackCapturePermissionState.Requesting,
            viewModel.uiState.value.playbackPermissionState
        )
    }

    @Test
    fun switchesToMicWhenPlaybackDenied() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val viewModel = createViewModel(repository)

        viewModel.onPlaybackCapturePermissionChanged(PlaybackCapturePermissionState.Denied)

        assertEquals(CaptureSource.MICROPHONE, viewModel.uiState.value.captureSource)
    }

    private fun createViewModel(repository: FakeSessionRepository): SessionViewModel =
        SessionViewModel(CreateManualSessionUseCase(repository))
}

private class FakeSessionRepository : SessionRepository {
    val createdSessions = mutableListOf<MeetingSession>()
    var failNextCreate = false

    override suspend fun createSession(session: MeetingSession): MeetingSession {
        if (failNextCreate) {
            failNextCreate = false
            error("create failed")
        }
        createdSessions += session
        return session
    }

    override fun observeSessions(): Flow<List<MeetingSession>> = flowOf(emptyList())

    override suspend fun updateStatus(sessionId: SessionId, status: SessionStatus) = Unit

    override suspend fun updateProcessingConfig(
        sessionId: SessionId,
        processingPolicy: ProcessingPolicy,
        processingTier: ProcessingTier
    ) = Unit

    override suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String) = Unit
}
