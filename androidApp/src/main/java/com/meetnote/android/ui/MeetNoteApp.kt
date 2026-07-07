package com.meetnote.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meetnote.android.ui.session.SessionScreen
import com.meetnote.android.ui.session.SessionViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MeetNoteApp(viewModel: SessionViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SessionScreen(
        state = state,
        onTitleChanged = viewModel::updateTitle,
        onCreateSession = viewModel::createSession
    )
}
