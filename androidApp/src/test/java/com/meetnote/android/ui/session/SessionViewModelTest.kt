package com.meetnote.android.ui.session

import com.meetnote.android.background.PostMeetingProcessingScheduler
import com.meetnote.android.capture.CaptureSource
import com.meetnote.android.capture.MeetingRecorder
import com.meetnote.android.capture.PlaybackCapturePermissionState
import com.meetnote.android.capture.RecorderResult
import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.model.ProcessingPolicy
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.SessionSource
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.repository.SessionRepository
import com.meetnote.shared.domain.usecase.CreateManualSessionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        advanceUntilIdle()

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
        advanceUntilIdle()

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
        advanceUntilIdle()

        viewModel.updateCaptureSource(CaptureSource.PLAYBACK_AUDIO)

        assertEquals(CaptureSource.PLAYBACK_AUDIO, viewModel.uiState.value.captureSource)
    }

    @Test
    fun storesPermissionDeniedState() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

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
        advanceUntilIdle()

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
        advanceUntilIdle()

        viewModel.onPlaybackCapturePermissionChanged(PlaybackCapturePermissionState.Denied)

        assertEquals(CaptureSource.MICROPHONE, viewModel.uiState.value.captureSource)
    }

    @Test
    fun exposesCreatedSessionsInUiState() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.createSession()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.sessions.size)
        assertEquals("Untitled Meeting", viewModel.uiState.value.sessions.single().title)
    }

    @Test
    fun startCaptureUsesMicrophoneRecorderAndMarksSessionActive() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val microphoneRecorder = FakeMeetingRecorder(
            startResult = RecorderResult.Started("demo.raw")
        )
        val viewModel = createViewModel(
            repository = repository,
            microphoneRecorder = microphoneRecorder
        )
        advanceUntilIdle()

        viewModel.updateCaptureSource(CaptureSource.MICROPHONE)
        viewModel.createSession()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.createdSessionId!!
        viewModel.startCapture(sessionId)
        advanceUntilIdle()

        assertEquals(listOf(sessionId), microphoneRecorder.startedSessions)
        assertEquals(sessionId, viewModel.uiState.value.activeCaptureSessionId)
        assertEquals(CaptureSource.MICROPHONE, viewModel.uiState.value.activeCaptureSource)
    }

    @Test
    fun stopCaptureClearsActiveSession() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val microphoneRecorder = FakeMeetingRecorder(
            startResult = RecorderResult.Started("demo.raw"),
            stopResult = RecorderResult.Stopped("demo.raw")
        )
        val viewModel = createViewModel(
            repository = repository,
            microphoneRecorder = microphoneRecorder
        )
        advanceUntilIdle()

        viewModel.updateCaptureSource(CaptureSource.MICROPHONE)
        viewModel.createSession()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.createdSessionId!!
        viewModel.startCapture(sessionId)
        advanceUntilIdle()
        viewModel.stopCapture(sessionId)
        advanceUntilIdle()

        assertEquals(listOf(sessionId), microphoneRecorder.stoppedSessions)
        assertNull(viewModel.uiState.value.activeCaptureSessionId)
        assertNull(viewModel.uiState.value.activeCaptureSource)
    }

    @Test
    fun stopCaptureQueuesPostMeetingProcessingForRecordThenProcessSessions() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val microphoneRecorder = FakeMeetingRecorder(
            startResult = RecorderResult.Started("demo.raw"),
            stopResult = RecorderResult.Stopped("demo.raw")
        )
        val scheduler = FakePostMeetingProcessingScheduler()
        val viewModel = createViewModel(
            repository = repository,
            microphoneRecorder = microphoneRecorder,
            scheduler = scheduler
        )
        advanceUntilIdle()

        viewModel.selectMode(ProcessingMode.RECORD_THEN_PROCESS)
        viewModel.updateCaptureSource(CaptureSource.MICROPHONE)
        viewModel.createSession()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.createdSessionId!!
        viewModel.startCapture(sessionId)
        advanceUntilIdle()
        viewModel.stopCapture(sessionId)
        advanceUntilIdle()

        assertEquals(listOf(sessionId to "demo.raw"), scheduler.enqueued)
        assertEquals(
            "Queued post-meeting processing scaffold for this session. AI processing is not implemented yet.",
            viewModel.uiState.value.infoMessage
        )
    }

    private fun createViewModel(
        repository: FakeSessionRepository,
        microphoneRecorder: MeetingRecorder = FakeMeetingRecorder(),
        playbackRecorder: MeetingRecorder = FakeMeetingRecorder(
            startResult = RecorderResult.Failure("Playback recorder not wired yet"),
            stopResult = RecorderResult.Failure("Playback recorder not wired yet")
        ),
        scheduler: PostMeetingProcessingScheduler = FakePostMeetingProcessingScheduler()
    ): SessionViewModel = SessionViewModel(
        createManualSession = CreateManualSessionUseCase(repository),
        sessionRepository = repository,
        microphoneRecorder = microphoneRecorder,
        playbackRecorder = playbackRecorder,
        postMeetingProcessingScheduler = scheduler
    )
}

private class FakeSessionRepository : SessionRepository {
    val createdSessions = mutableListOf<MeetingSession>()
    var failNextCreate = false
    private val sessionsFlow = MutableStateFlow<List<MeetingSession>>(emptyList())

    override suspend fun createSession(session: MeetingSession): MeetingSession {
        if (failNextCreate) {
            failNextCreate = false
            error("create failed")
        }
        createdSessions += session
        sessionsFlow.value = createdSessions.toList()
        return session
    }

    override fun observeSessions(): Flow<List<MeetingSession>> = sessionsFlow

    override suspend fun updateStatus(sessionId: SessionId, status: SessionStatus) {
        sessionsFlow.value = sessionsFlow.value.map { session ->
            if (session.id == sessionId) session.copy(status = status) else session
        }
    }

    override suspend fun updateProcessingConfig(
        sessionId: SessionId,
        processingPolicy: ProcessingPolicy,
        processingTier: ProcessingTier
    ) = Unit

    override suspend fun attachAudioFile(sessionId: SessionId, audioFilePath: String) {
        sessionsFlow.value = sessionsFlow.value.map { session ->
            if (session.id == sessionId) session.copy(audioFilePath = audioFilePath) else session
        }
    }

    override suspend fun attachProcessingArtifact(sessionId: SessionId, processingArtifactPath: String) {
        sessionsFlow.value = sessionsFlow.value.map { session ->
            if (session.id == sessionId) {
                session.copy(processingArtifactPath = processingArtifactPath)
            } else {
                session
            }
        }
    }
}

private class FakeMeetingRecorder(
    private val startResult: RecorderResult = RecorderResult.Started("fake.raw"),
    private val stopResult: RecorderResult = RecorderResult.Stopped("fake.raw")
) : MeetingRecorder {
    val startedSessions = mutableListOf<String>()
    val stoppedSessions = mutableListOf<String>()

    override suspend fun start(sessionId: String): RecorderResult {
        startedSessions += sessionId
        return startResult
    }

    override suspend fun stop(sessionId: String): RecorderResult {
        stoppedSessions += sessionId
        return stopResult
    }
}

private class FakePostMeetingProcessingScheduler : PostMeetingProcessingScheduler {
    val enqueued = mutableListOf<Pair<String, String>>()

    override fun enqueue(sessionId: String, audioFilePath: String) {
        enqueued += sessionId to audioFilePath
    }
}
