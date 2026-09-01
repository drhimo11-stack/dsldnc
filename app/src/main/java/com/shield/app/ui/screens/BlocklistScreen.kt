package com.shield.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shield.app.ui.MainViewModel

@Composable
fun BlocklistScreen(viewModel: MainViewModel) {
    val items = viewModel.blocklistItems.collectAsState().value
    var newPattern by remember { mutableStateOf("") }

    // Informational only — Keyword already falls back to matching a bad
    // pattern as plain text rather than crashing, so this never blocks
    // adding it, it just tells the user what will actually happen.
    val isValidRegex by remember(newPattern) {
        derivedStateOf {
            if (newPattern.isBlank()) true
            else try {
                Regex(newPattern)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add a custom pattern (regex or plain text). It will be matched against any visible page text.")

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = newPattern,
                onValueChange = { newPattern = it },
                label = { Text("Pattern") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (newPattern.isNotBlank()) {
                    viewModel.addBlockPattern(newPattern.trim())
                    newPattern = ""
                }
            }) {
                Text("Add")
            }
        }

        if (!isValidRegex) {
            Text(
                text = "Not valid regex \u2014 will be matched as plain text instead.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(item.pattern, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removeBlockPattern(item.pattern) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}
