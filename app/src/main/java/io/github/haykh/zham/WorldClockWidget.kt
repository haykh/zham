package io.github.haykh.zham

import android.content.Context
import android.content.res.Configuration
import android.view.View
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

    // Glance can't read the app's MaterialTheme, so mirror it by hand: the user's background
    // colour or the theme's light/dark/pure-black surface, readable text on it, and the
    // per-widget text colour overrides.
    val systemDark =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val dark = state.themeMode.isDark(systemDark)
    val background = state.style.backgroundColor ?: widgetThemeSurface(dark, state.pureBlack)
    val onBackground = textColorOn(background)
    // Same rule as the app: a black accent in dark mode (or white in light) reads as plain text.
    val accent = readableAccent(state.accent, background, onBackground)
    val labelColor = state.style.labelColor.resolve(onBackground, accent)
    val clockColor = state.style.timeColor.resolve(onBackground, accent)
    // TextClock self-updates every minute (not per second), so strip seconds.
    val minutePattern = state.timeFormat.replace(Regex("[:.\\s]?ss"), "").ifBlank { "HH:mm" }

    val openApp = actionStartActivity<MainActivity>()

    // Back to front: the rounded background frame (tint at the user's transparency + optional
    // frosted glass), then the rows. Tapping anywhere opens the app: the outer box catches taps
    // outside the rows (the frame and inner box aren't clickable, so those fall through), and
    // each row is clickable too since list rows swallow touches otherwise.
    Box(modifier = GlanceModifier.fillMaxSize().clickable(openApp)) {
        AndroidRemoteViews(
            modifier = GlanceModifier.fillMaxSize(),
            remoteViews = widgetFrame(context, background.copy(alpha = state.style.backgroundAlpha), state.style.frost),
        )
        // The padding keeps the rows clear of the rounded corners, so they need no clipping.
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
 * The widget's rounded background (see widget_frame.xml): [tint] fill, plus the frosted-glass
 * layers (sheen, grain, rim) with their opacity scaled by [frost] (0..1; 0 hides them).
 */
private fun widgetFrame(
    context: Context,
    tint: Color,
    frost: Float,
): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_frame).apply {
        setInt(R.id.frame_tint, "setBackgroundColor", tint.toArgb())
        val glass = if (frost > 0f) View.VISIBLE else View.GONE
        val alpha = (frost.coerceIn(0f, 1f) * 255).roundToInt()
        for (layer in intArrayOf(R.id.glass_sheen, R.id.glass_grain, R.id.glass_edge)) {
            setViewVisibility(layer, glass)
            setInt(layer, "setImageAlpha", alpha)
        }
    }
