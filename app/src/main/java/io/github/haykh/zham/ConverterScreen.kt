package io.github.haykh.zham

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Scrub a time/date and read off the corresponding instant in every city.
 * Tap a city to make it the reference (highlighted); tap the date to pick a day; the
 * slider shifts the time ±12h. Each city shows a +Nd / -Nd badge when its date differs
 * from the reference, so cross-midnight timezone changes are obvious.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    cities: List<City>,
    timeFormat: String,
    showFlags: Boolean,
) {
    val base = remember { ZonedDateTime.now() }
    var dayOffset by rememberSaveable { mutableLongStateOf(0L) } // whole-day shift from today
    var offsetMinutes by remember { mutableFloatStateOf(0f) } // ±12h time scrub
    val scrubbed = base.plusDays(dayOffset).plusMinutes(offsetMinutes.toLong())
    val formatter = remember(timeFormat) { Clocks.formatterOf(timeFormat) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    var refZoneId by rememberSaveable { mutableStateOf<String?>(null) }
    val anchor = cities.firstOrNull { it.zoneIdName == refZoneId } ?: cities.firstOrNull()

    Scaffold(topBar = { TopAppBar(title = { Text("Convert") }) }) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
        ) {
            if (anchor == null) {
                Text("Add a city on the Clocks tab first.", style = MaterialTheme.typography.bodyLarge)
            } else {
                val refDate = scrubbed.withZoneSameInstant(anchor.zone).toLocalDate()
                var showDatePicker by remember { mutableStateOf(false) }

                Text(anchor.label, style = MaterialTheme.typography.titleMedium)
                Text(Clocks.format(anchor, scrubbed, formatter), style = MaterialTheme.typography.displaySmall)
                // Tappable date → date picker
                Text(
                    text = refDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { showDatePicker = true }
                            .padding(vertical = 2.dp),
                )

                Spacer(Modifier.height(8.dp))
                Text(offsetLabel(offsetMinutes.toInt()), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = offsetMinutes,
                    onValueChange = { offsetMinutes = it },
                    valueRange = -720f..720f,
                    steps = 95, // snap to 15-minute increments across ±12h
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        dayOffset = 0L
                        offsetMinutes = 0f
                    }) { Text("Now") }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(cities, key = { it.zoneIdName }) { city ->
                        val cityDate = scrubbed.withZoneSameInstant(city.zone).toLocalDate()
                        val dayDiff = ChronoUnit.DAYS.between(refDate, cityDate)
                        val selected = city.zoneIdName == anchor.zoneIdName
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        // dimmed accent tint (primary == the chosen accent)
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                                    )
                                    .clickable { refZoneId = city.zoneIdName }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (showFlags) {
                                        Text(Flags.emojiFor(city.zoneIdName), style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(city.label, style = MaterialTheme.typography.titleMedium)
                                }
                                Text(city.zone.id, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                // Always render this line (blank when no diff) so rows keep a constant height.
                                Text(
                                    text = if (dayDiff > 0) "+${dayDiff}d" else if (dayDiff < 0) "${dayDiff}d" else " ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    Clocks.format(city, scrubbed, formatter),
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                            }
                        }
                    }
                }

                if (showDatePicker) {
                    val initialMillis = refDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    val dpState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dpState.selectedDateMillis?.let { millis ->
                                    val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                                    // Nudge the day shift so the reference lands on the picked date.
                                    dayOffset += ChronoUnit.DAYS.between(refDate, picked)
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        },
                    ) {
                        DatePicker(state = dpState)
                    }
                }
            }
        }
    }
}

/** "Now" / "+2h 30m" / "-1h 15m" for the scrub offset. */
private fun offsetLabel(min: Int): String {
    if (min == 0) return "Now"
    val sign = if (min < 0) "-" else "+"
    val a = abs(min)
    val h = a / 60
    val m = a % 60
    return buildString {
        append(sign)
        if (h > 0) append("${h}h ")
        append("${m}m")
    }.trim()
}
