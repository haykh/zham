package io.github.haykh.zham

import android.content.Context
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import kotlinx.coroutines.flow.first

class WorldClockWidget : GlanceAppWidget() {
    // Re-compose to the widget's actual size when the user resizes it on the home screen.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val repo = SettingsRepository(context)
        val cities = repo.cities.first()
        val showFlags = repo.showFlags.first()
        val pattern = repo.timeFormat.first()
        // Glance can't read the app's MaterialTheme, so resolve the theme ourselves.
        val mode = runCatching { ThemeMode.valueOf(repo.themeMode.first()) }.getOrDefault(ThemeMode.SYSTEM)
        val accent = repo.accent.first()?.toIntOrNull()?.let { Color(it) } ?: DefaultAccent
        val dark = isDark(context, mode)
        provideContent { WidgetContent(cities, showFlags, pattern, dark, accent) }
    }

    private fun isDark(
        context: Context,
        mode: ThemeMode,
    ): Boolean =
        when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> {
                val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                night == Configuration.UI_MODE_NIGHT_YES
            }
        }
}

@Composable
private fun WidgetContent(
    cities: List<City>,
    showFlags: Boolean,
    pattern: String,
    dark: Boolean,
    accent: Color,
) {
    val context = LocalContext.current

    // Mirror the app's theme: dark/light surface + onSurface, accent on the time.
    val background = if (dark) Color(0xFF1C1B1F) else Color(0xFFFDFCFF)
    val onBackground = (if (dark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)).toArgb()
    val clockColor = accent.toArgb()
    // TextClock self-updates every minute (not per second), so strip seconds.
    val minutePattern = pattern.replace(Regex("[:.\\s]?ss"), "").ifBlank { "HH:mm" }

    // LazyColumn scrolls when the cities don't fit the (resizable) widget height.
    LazyColumn(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(background)
                .padding(12.dp),
    ) {
        items(cities) { city ->
            val label = if (showFlags) "${Flags.emojiFor(city.zoneIdName)} ${city.label}" else city.label
            // Whole row as one RemoteViews: label + a self-ticking TextClock.
            AndroidRemoteViews(
                modifier = GlanceModifier.fillMaxWidth(),
                remoteViews =
                    RemoteViews(context.packageName, R.layout.widget_row).apply {
                        setTextViewText(R.id.row_label, label)
                        setTextColor(R.id.row_label, onBackground)
                        setString(R.id.row_clock, "setTimeZone", city.zoneIdName)
                        setCharSequence(R.id.row_clock, "setFormat12Hour", minutePattern)
                        setCharSequence(R.id.row_clock, "setFormat24Hour", minutePattern)
                        setTextColor(R.id.row_clock, clockColor)
                    },
            )
        }
    }
}
