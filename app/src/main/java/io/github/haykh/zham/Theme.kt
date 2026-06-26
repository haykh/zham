package io.github.haykh.zham

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Cycle order for the single theme button: System → Light → Dark → System. */
fun ThemeMode.next(): ThemeMode =
    when (this) {
        ThemeMode.SYSTEM -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.DARK
        ThemeMode.DARK -> ThemeMode.SYSTEM
    }

/** Emoji shown on the cycling theme button (no icon assets needed). */
fun ThemeMode.emoji(): String =
    when (this) {
        ThemeMode.SYSTEM -> "🌗"
        ThemeMode.LIGHT -> "☀️"
        ThemeMode.DARK -> "🌙"
    }

/** Preset accent swatches offered in settings; the accent itself can be any Color. */
val accentPresets: List<Color> =
    listOf(
        Color(0xFF5C6BC0), // indigo
        Color(0xFF1E88E5), // blue
        Color(0xFF00897B), // teal
        Color(0xFF43A047), // green
        Color(0xFFFFB300), // amber
        Color(0xFFF4511E), // deep orange
        Color(0xFFE91E63), // rose
        Color(0xFF8E24AA), // violet
    )

val DefaultAccent: Color = accentPresets.first()

@Composable
fun WorldClockTheme(
    themeMode: ThemeMode,
    accent: Color,
    content: @Composable () -> Unit,
) {
    val dark =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val colorScheme =
        if (dark) {
            darkColorScheme(primary = accent)
        } else {
            lightColorScheme(primary = accent)
        }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
