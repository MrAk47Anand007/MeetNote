package com.meetnote.android.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SessionScreen(
    state: SessionUiState,
    onTitleChanged: (String) -> Unit,
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
            text = "Create a record-then-process session to verify Android shell wiring.",
            style = MaterialTheme.typography.bodyLarge
        )
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Meeting title") },
            singleLine = true
        )
        Button(onClick = onCreateSession) {
            Text("Create Record-Then-Process Session")
        }
        state.createdSessionId?.let { sessionId ->
            Text(
                text = "Created session: $sessionId",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
