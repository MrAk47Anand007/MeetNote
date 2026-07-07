package com.meetnote.android.ui.session

import com.meetnote.shared.core.SessionId
import com.meetnote.shared.domain.model.MeetingSession
import com.meetnote.shared.domain.model.ProcessingPolicy
import com.meetnote.shared.domain.model.ProcessingTier
import com.meetnote.shared.domain.model.ProcessingMode
import com.meetnote.shared.domain.model.SessionStatus
import com.meetnote.shared.domain.usecase.CreateManualSessionUseCase
import com.meetnote.shared.domain.repository.SessionRepository
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
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

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
    fun createSessionUsesFallbackTitleAndExposesCreatedSessionId() = runTest(dispatcher) {
        var createdSession: MeetingSession? = null
        val viewModel = SessionViewModel(
            createManualSession = CreateManualSessionUseCase(
                object : SessionRepository {
                    override suspend fun createSession(session: MeetingSession): MeetingSession {
                        createdSession = session
                        return session
                    }

                    override fun observeSessions(): Flow<List<MeetingSession>> = flowOf(emptyList())

                    override suspend fun updateStatus(sessionId: SessionId, status: SessionStatus) = Unit

                    override suspend fun updateProcessingConfig(
                        sessionId: SessionId,
                        processingPolicy: ProcessingPolicy,
                        processingTier: ProcessingTier
                    ) = Unit

                    override suspend fun attachAudioFile(
                        sessionId: SessionId,
                        audioFilePath: String
                    ) = Unit
                }
            )
        )

        viewModel.createSession()
        advanceUntilIdle()

        assertEquals("Untitled Meeting", createdSession?.title)
        assertEquals(ProcessingMode.RECORD_THEN_PROCESS, createdSession?.processingMode)
        assertEquals(createdSession?.id?.value, viewModel.uiState.value.createdSessionId)
    }
}
