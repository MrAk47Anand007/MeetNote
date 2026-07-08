package com.meetnote.android.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meetnote.android.capture.CaptureSource
import com.meetnote.android.capture.PlaybackCapturePermissionState
import com.meetnote.shared.domain.model.ProcessingMode

@Composable
fun SessionScreen(
    state: SessionUiState,
    onTitleChanged: (String) -> Unit,
    onModeSelected: (ProcessingMode) -> Unit,
    onCaptureSourceSelected: (CaptureSource) -> Unit,
    onRequestPlaybackCaptureConsent: () -> Unit,
    onCreateSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "MeetNote",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Create a manual session to verify Android shell wiring.",
            style = MaterialTheme.typography.bodyLarge
        )
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Meeting title") },
            singleLine = true
        )
        Text(
            text = "Processing mode",
            style = MaterialTheme.typography.titleMedium
        )
        ProcessingModeOption(
            label = "Live Assist",
            selected = state.selectedMode == ProcessingMode.LIVE_ASSIST,
            onSelected = { onModeSelected(ProcessingMode.LIVE_ASSIST) }
        )
        ProcessingModeOption(
            label = "Record Then Process",
            selected = state.selectedMode == ProcessingMode.RECORD_THEN_PROCESS,
            onSelected = { onModeSelected(ProcessingMode.RECORD_THEN_PROCESS) }
        )
        Text(
            text = "Capture source",
            style = MaterialTheme.typography.titleMedium
        )
        CaptureSourceOption(
            label = "Playback Audio (Preferred)",
            selected = state.captureSource == CaptureSource.PLAYBACK_AUDIO,
            onSelected = { onCaptureSourceSelected(CaptureSource.PLAYBACK_AUDIO) }
        )
        CaptureSourceOption(
            label = "Microphone",
            selected = state.captureSource == CaptureSource.MICROPHONE,
            onSelected = { onCaptureSourceSelected(CaptureSource.MICROPHONE) }
        )
        if (state.playbackPermissionState != PlaybackCapturePermissionState.NotRequested) {
            Text(
                text = "Playback capture permission: ${state.playbackPermissionState.label()}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (state.captureSource == CaptureSource.PLAYBACK_AUDIO) {
            Button(
                onClick = onRequestPlaybackCaptureConsent,
                enabled = state.playbackPermissionState != PlaybackCapturePermissionState.Requesting
            ) {
                Text("Request Playback Permission")
            }
        }
        Button(
            onClick = onCreateSession,
            enabled = !state.isCreating
        ) {
            Text(if (state.isCreating) "Creating Session..." else "Create Session")
        }
        state.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        state.createdSessionId?.let { sessionId ->
            Text(
                text = "Created session: $sessionId",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ProcessingModeOption(
    label: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        RadioButton(
            selected = selected,
            onClick = onSelected
        )
        Text(
            text = label,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun CaptureSourceOption(
    label: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        RadioButton(
            selected = selected,
            onClick = onSelected
        )
        Text(
            text = label,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

private fun PlaybackCapturePermissionState.label(): String = when (this) {
    PlaybackCapturePermissionState.NotRequested -> "Not requested"
    PlaybackCapturePermissionState.Requesting -> "Requesting"
    PlaybackCapturePermissionState.Granted -> "Granted"
    PlaybackCapturePermissionState.Denied -> "Denied"
    PlaybackCapturePermissionState.Unsupported -> "Unsupported"
}
