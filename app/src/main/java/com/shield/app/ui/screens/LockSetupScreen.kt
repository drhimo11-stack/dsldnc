package com.shield.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shield.app.ui.MainViewModel

private val PRESET_DAYS = listOf(1, 3, 7, 30, 90)

@Composable
fun LockSetupScreen(viewModel: MainViewModel) {
    val isLocked = viewModel.isLocked.collectAsState().value
    val remainingText = viewModel.lockRemainingText.collectAsState().value
    var customDays by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isLocked) "Currently locked: $remainingText remaining"
                    else "Not currently locked",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Text("Quick presets (days):", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESET_DAYS.forEach { days ->
                OutlinedButton(onClick = { confirming = days }) {
                    Text("$days")
                }
            }
        }

        Text("Custom (1-365 days):", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = customDays,
            onValueChange = { input -> customDays = input.filter { it.isDigit() } },
            label = { Text("Days") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val days = customDays.toIntOrNull()?.coerceIn(1, 365)
                if (days != null) confirming = days
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Custom Lock")
        }

        confirming?.let { days ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Lock the app for $days day(s)? This cannot be undone early.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            viewModel.startLock(days)
                            confirming = null
                        }) {
                            Text("Confirm")
                        }
                        OutlinedButton(onClick = { confirming = null }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}
