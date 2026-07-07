package com.meetnote.android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val createdSessionId: String? = null
)

class SessionViewModel(
    private val createManualSession: CreateManualSessionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun createSession() {
        viewModelScope.launch {
            val session = createManualSession(
                title = _uiState.value.title.ifBlank { "Untitled Meeting" },
                processingMode = _uiState.value.selectedMode
            )
            _uiState.value = _uiState.value.copy(createdSessionId = session.id.value)
        }
    }
}

val androidUiModule = module {
    viewModel { SessionViewModel(get()) }
}
