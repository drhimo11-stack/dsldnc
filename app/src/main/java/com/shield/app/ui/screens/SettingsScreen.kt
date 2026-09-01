package com.shield.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shield.app.ui.MainViewModel
import kotlinx.coroutines.delay

// How long to wait after the user stops typing before autosaving a field,
// so we're not writing to disk on every keystroke.
private const val AUTOSAVE_DEBOUNCE_MS = 600L

private val DAY_LABELS = listOf("S", "M", "T", "W", "T", "F", "S") // Sun..Sat, matches bit i

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings = viewModel.settings.collectAsState().value

    var message by remember { mutableStateOf(settings.customMessage) }
    var countdown by remember { mutableStateOf(settings.countdownSeconds.toString()) }
    var redirectUrl by remember { mutableStateOf(settings.redirectUrl) }
    var blockUnsupported by remember { mutableStateOf(settings.blockUnsupportedBrowsers) }
    var scheduleEnabled by remember { mutableStateOf(settings.scheduleEnabled) }
    var scheduleDaysMask by remember { mutableStateOf(settings.scheduleDaysMask) }
    var startTimeText by remember { mutableStateOf(minuteToTimeText(settings.scheduleStartMinute)) }
    var endTimeText by remember { mutableStateOf(minuteToTimeText(settings.scheduleEndMinute)) }

    // Track whether the field value currently on screen came from the
    // user typing (needs saving) or from a fresh load from settings
    // (already saved) so autosave doesn't re-save on first composition.
    var messageEdited by remember { mutableStateOf(false) }
    var countdownEdited by remember { mutableStateOf(false) }
    var redirectUrlEdited by remember { mutableStateOf(false) }
    var startTimeEdited by remember { mutableStateOf(false) }
    var endTimeEdited by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        message = settings.customMessage
        countdown = settings.countdownSeconds.toString()
        redirectUrl = settings.redirectUrl
        blockUnsupported = settings.blockUnsupportedBrowsers
        scheduleEnabled = settings.scheduleEnabled
        scheduleDaysMask = settings.scheduleDaysMask
        startTimeText = minuteToTimeText(settings.scheduleStartMinute)
        endTimeText = minuteToTimeText(settings.scheduleEndMinute)
        messageEdited = false
        countdownEdited = false
        redirectUrlEdited = false
        startTimeEdited = false
        endTimeEdited = false
    }

    // Autosave: persist a field a short moment after the user stops
    // typing, so navigating away (or the back button) never loses what
    // was entered — the explicit "Save" buttons below just make it
    // happen immediately instead of waiting for the debounce.
    LaunchedEffect(message) {
        if (!messageEdited) return@LaunchedEffect
        delay(AUTOSAVE_DEBOUNCE_MS)
        viewModel.setCustomMessage(message)
    }
    LaunchedEffect(countdown) {
        if (!countdownEdited) return@LaunchedEffect
        delay(AUTOSAVE_DEBOUNCE_MS)
        viewModel.setCountdown(countdown.toIntOrNull()?.coerceIn(0, 30) ?: 3)
    }
    LaunchedEffect(redirectUrl) {
        if (!redirectUrlEdited) return@LaunchedEffect
        delay(AUTOSAVE_DEBOUNCE_MS)
        viewModel.setRedirectUrl(redirectUrl)
    }
    LaunchedEffect(startTimeText, endTimeText) {
        if (!startTimeEdited && !endTimeEdited) return@LaunchedEffect
        delay(AUTOSAVE_DEBOUNCE_MS)
        val start = parseTimeText(startTimeText)
        val end = parseTimeText(endTimeText)
        if (start != null && end != null) {
            viewModel.setScheduleWindow(start, end)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Overlay Message", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = message,
            onValueChange = { message = it; messageEdited = true },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            messageEdited = false
            viewModel.setCustomMessage(message)
        }) {
            Text("Save Message")
        }

        Text("Close Button Countdown (0-30 seconds)", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = countdown,
            onValueChange = { countdown = it.filter { c -> c.isDigit() }; countdownEdited = true },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            countdownEdited = false
            val seconds = countdown.toIntOrNull()?.coerceIn(0, 30) ?: 3
            viewModel.setCountdown(seconds)
        }) {
            Text("Save Countdown")
        }

        Column {
            Text("Redirect URL (optional, leave empty for Home)", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "e.g. example.com \u2014 https:// is added automatically if you leave it out",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = redirectUrl,
            onValueChange = { redirectUrl = it; redirectUrlEdited = true },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            redirectUrlEdited = false
            viewModel.setRedirectUrl(redirectUrl)
        }) {
            Text("Save Redirect URL")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Block unsupported browsers", style = MaterialTheme.typography.labelLarge)
            Switch(
                checked = blockUnsupported,
                onCheckedChange = {
                    blockUnsupported = it
                    viewModel.setBlockUnsupportedBrowsers(it)
                }
            )
        }

        HorizontalDivider()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Scheduled Blocking", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Only enforce blocking during chosen days/hours.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = scheduleEnabled,
                        onCheckedChange = {
                            scheduleEnabled = it
                            viewModel.setScheduleEnabled(it)
                        }
                    )
                }

                if (scheduleEnabled) {
                    Text("Active Days", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DAY_LABELS.forEachIndexed { index, label ->
                            val bit = 1 shl index
                            val selected = (scheduleDaysMask and bit) != 0
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val newMask = scheduleDaysMask xor bit
                                    scheduleDaysMask = newMask
                                    viewModel.setScheduleDaysMask(newMask)
                                },
                                label = { Text(label, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start (24h HH:mm)", style = MaterialTheme.typography.labelLarge)
                            OutlinedTextField(
                                value = startTimeText,
                                onValueChange = { startTimeText = it; startTimeEdited = true },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("08:00") }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("End (24h HH:mm)", style = MaterialTheme.typography.labelLarge)
                            OutlinedTextField(
                                value = endTimeText,
                                onValueChange = { endTimeText = it; endTimeEdited = true },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("15:00") }
                            )
                        }
                    }
                    Text(
                        text = "An end time earlier than the start time is treated as " +
                            "overnight (e.g. 22:00 \u2192 06:00).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun minuteToTimeText(minute: Int): String {
    if (minute >= 1440) return "23:59"
    val clamped = minute.coerceIn(0, 1439)
    val hh = clamped / 60
    val mm = clamped % 60
    return "%02d:%02d".format(hh, mm)
}

private fun parseTimeText(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val hh = parts[0].toIntOrNull() ?: return null
    val mm = parts[1].toIntOrNull() ?: return null
    if (hh !in 0..23 || mm !in 0..59) return null
    return hh * 60 + mm
}
