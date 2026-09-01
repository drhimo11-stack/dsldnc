package com.shield.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shield.app.accessibility.InstalledAppInfo
import com.shield.app.blocklist.ManagedAppItem
import com.shield.app.ui.MainViewModel

@Composable
fun ManagedAppsScreen(viewModel: MainViewModel) {
    val managedApps = viewModel.managedApps.collectAsState().value

    var pickerExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var loadingCandidates by remember { mutableStateOf(false) }

    LaunchedEffect(pickerExpanded) {
        if (pickerExpanded && candidates.isEmpty()) {
            loadingCandidates = true
            candidates = viewModel.listInstallableApps()
            loadingCandidates = false
        }
    }

    val managedPackageNames = managedApps.map { it.packageName }.toSet()
    val filteredCandidates = candidates
        .filter { it.packageName !in managedPackageNames }
        .filter { searchQuery.isBlank() || it.label.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Any browser or downloader added here is scanned and blocked the " +
                "same way as a built-in supported browser \u2014 and this list stays " +
                "editable even while a lock is active.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FilledTonalButton(
            onClick = { pickerExpanded = !pickerExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (pickerExpanded) Icons.Filled.ExpandLess else Icons.Filled.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (pickerExpanded) "Hide App Picker" else "Add App")
        }

        if (pickerExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search installed apps") },
                modifier = Modifier.fillMaxWidth()
            )

            when {
                loadingCandidates -> Text(
                    text = "Scanning installed apps\u2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                filteredCandidates.isEmpty() -> Text(
                    text = "No matching apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(filteredCandidates, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(app.label, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = if (app.isBrowserCapable) "Browser-capable" else app.packageName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(onClick = {
                                    viewModel.addManagedApp(app.packageName, app.label)
                                    searchQuery = ""
                                }) {
                                    Text("Add")
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        if (managedApps.isEmpty()) {
            Text(
                text = "No apps added yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(managedApps, key = { it.packageName }) { app ->
                    ManagedAppRow(
                        app = app,
                        onToggle = { viewModel.setManagedAppBlocked(app.packageName, it) },
                        onRemove = { viewModel.removeManagedApp(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagedAppRow(
    app: ManagedAppItem,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (app.autoDetected) "Auto-detected \u2022 ${app.packageName}" else app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = app.blocked, onCheckedChange = onToggle)
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove")
            }
        }
    }
}
