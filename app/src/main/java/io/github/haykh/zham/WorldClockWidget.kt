package io.github.haykh.zham

import android.content.Context
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

class WorldClockWidget : GlanceAppWidget() {
    // Re-compose to the widget's actual size when the user resizes it on the home screen.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val repo = SettingsRepository(context)
        // Read once up front so the first frame is already correct...
        val initial = repo.widgetState.first()
        provideContent {
            // ...then keep collecting, so a settings change re-renders a live session too.
            val state by repo.widgetState.collectAsState(initial)
            WidgetContent(state)
        }
    }
}

@Composable
private fun WidgetContent(state: WidgetState) {
    val context = LocalContext.current

    // Glance can't read the app's MaterialTheme, so mirror it by hand:
    // light/dark/pure-black surface + onSurface, with per-widget text colour overrides.
    val systemDark =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val dark = state.themeMode.isDark(systemDark)
    val background =
        when {
            !dark -> Color(0xFFFDFCFF)
            state.pureBlack -> Color.Black
            else -> Color(0xFF1C1B1F)
        }
    val onBackground = if (dark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
    // Same rule as the app: a black accent in dark mode (or white in light) reads as plain text.
    val accent = readableAccent(state.accent, background, onBackground)
    val labelColor = state.style.labelColor.resolve(onBackground, accent)
    val clockColor = state.style.timeColor.resolve(onBackground, accent)
    // TextClock self-updates every minute (not per second), so strip seconds.
    val minutePattern = state.timeFormat.replace(Regex("[:.\\s]?ss"), "").ifBlank { "HH:mm" }
    val openApp = actionStartActivity<MainActivity>()

    // Layers, back to front: tint (with the user's transparency), optional frosted glass,
    // then the rows. Tapping anywhere opens the app: the outer box catches taps on the
    // padding (the inner boxes aren't clickable, so those fall through), and each row is
    // clickable too since list rows swallow touches otherwise.
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(background.copy(alpha = state.style.backgroundAlpha))
                // Clip every layer (incl. the glass) to the launcher's rounded widget shape.
                .cornerRadius(R.dimen.widget_corner_radius)
                .clickable(openApp),
    ) {
        if (state.style.frost > 0f) {
            AndroidRemoteViews(
                modifier = GlanceModifier.fillMaxSize(),
                remoteViews = glassLayers(context, state.style.frost),
            )
        }
        Box(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
            // LazyColumn scrolls when the cities don't fit the (resizable) widget height.
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(state.cities) { city ->
                    val label = if (state.showFlags) "${Flags.emojiFor(city.zoneIdName)} ${city.label}" else city.label
                    // Whole row as one RemoteViews: label + a self-ticking TextClock.
                    AndroidRemoteViews(
                        modifier = GlanceModifier.fillMaxWidth().clickable(openApp),
                        remoteViews =
                            RemoteViews(context.packageName, R.layout.widget_row).apply {
                                setTextViewText(R.id.row_label, label)
                                setTextColor(R.id.row_label, labelColor.toArgb())
                                setString(R.id.row_clock, "setTimeZone", city.zoneIdName)
                                setCharSequence(R.id.row_clock, "setFormat12Hour", minutePattern)
                                setCharSequence(R.id.row_clock, "setFormat24Hour", minutePattern)
                                setTextColor(R.id.row_clock, clockColor.toArgb())
                            },
                    )
                }
            }
        }
    }
}

/**
 * The frosted-glass layers (sheen, grain, rim) with their opacity scaled by [frost] (0..1).
 * A launcher can't blur what's behind a widget, so this is the closest available look.
 */
private fun glassLayers(
    context: Context,
    frost: Float,
): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_glass).apply {
        val alpha = (frost.coerceIn(0f, 1f) * 255).roundToInt()
        setInt(R.id.glass_sheen, "setImageAlpha", alpha)
        setInt(R.id.glass_grain, "setImageAlpha", alpha)
        setInt(R.id.glass_edge, "setImageAlpha", alpha)
    }
