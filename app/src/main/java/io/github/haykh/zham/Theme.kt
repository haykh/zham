package io.github.haykh.zham

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.max
import kotlin.math.min

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Cycle order for the single theme button: System → Light → Dark → System. */
fun ThemeMode.next(): ThemeMode =
    when (this) {
        ThemeMode.SYSTEM -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.DARK
        ThemeMode.DARK -> ThemeMode.SYSTEM
    }

/** Icon shown on the cycling theme button. */
fun ThemeMode.icon(): ImageVector =
    when (this) {
        ThemeMode.SYSTEM -> ZhamIcons.ThemeSystem
        ThemeMode.LIGHT -> ZhamIcons.ThemeLight
        ThemeMode.DARK -> ZhamIcons.ThemeDark
    }

fun ThemeMode.isDark(systemDark: Boolean): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
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
        Color.White,
        Color.Black,
    )

val DefaultAccent: Color = accentPresets.first()

/** Text colour choices for the widget's city names and times. */
enum class WidgetTextColor(
    val label: String,
) {
    AUTO("Auto"),
    WHITE("White"),
    BLACK("Black"),
    ACCENT("Accent"),
    ;

    /** [themed] is the theme's on-surface colour, used for [AUTO]. */
    fun resolve(
        themed: Color,
        accent: Color,
    ): Color =
        when (this) {
            AUTO -> themed
            WHITE -> Color.White
            BLACK -> Color.Black
            ACCENT -> accent
        }
}

/** Widget-only appearance settings. Defaults reproduce the original widget look. */
data class WidgetStyle(
    /** Background colour; null = follow the theme (see [widgetThemeSurface]). */
    val backgroundColor: Color? = null,
    /** Background opacity, 0 (fully transparent) to 1 (opaque). */
    val backgroundAlpha: Float = 1f,
    /** Frosted-glass strength (rim + sheen + grain), 0 (off) to 1. */
    val frost: Float = 0f,
    val labelColor: WidgetTextColor = WidgetTextColor.AUTO,
    val timeColor: WidgetTextColor = WidgetTextColor.ACCENT,
)

/** The widget's "Auto" background: the app's light, dark or pure-black surface. */
fun widgetThemeSurface(
    dark: Boolean,
    pureBlack: Boolean,
): Color =
    when {
        !dark -> Color(0xFFFDFCFF)
        pureBlack -> Color.Black
        else -> Color(0xFF1C1B1F)
    }

/** Default ("Auto") text colour for [background]: the theme's light or dark text, whichever reads better. */
fun textColorOn(background: Color): Color {
    val darkText = Color(0xFF1C1B1F)
    val lightText = Color(0xFFE6E1E5)
    return if (contrastRatio(darkText, background) >= contrastRatio(lightText, background)) darkText else lightText
}

/** WCAG contrast ratio between two colours, from 1 (identical) to 21 (black on white). */
fun contrastRatio(
    a: Color,
    b: Color,
): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

/**
 * The accent as it should be drawn on [surface]. A near-white accent on a light surface
 * (or near-black on a dark one) would be invisible as text or controls, so fall back to
 * the regular text colour [onSurface] in that case.
 */
fun readableAccent(
    accent: Color,
    surface: Color,
    onSurface: Color,
): Color = if (contrastRatio(accent, surface) < 1.5f) onSurface else accent

/** "#RRGGBB" without the '#', upper-case; alpha is dropped. */
fun Color.toHex(): String =
    Integer
        .toHexString(toArgb() and 0xFFFFFF)
        .padStart(6, '0')
        .uppercase()

fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

/** Parses "RRGGBB" (optionally '#'-prefixed) into an opaque colour, or null if invalid. */
fun parseHexColor(input: String): Color? {
    val hex = input.removePrefix("#")
    if (hex.length != 6 || !hex.all { it.isHexDigit() }) return null
    return Color(0xFF000000L or hex.toLong(16))
}

/** Dark scheme with true-black backgrounds for AMOLED screens; cards and dialogs stay faintly raised. */
private val PureBlackColorScheme =
    darkColorScheme(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color.Black, // navigation bar
        surfaceContainerHigh = Color(0xFF161616), // dialogs
        surfaceContainerHighest = Color(0xFF1E1E1E), // cards, text fields
    )

@Composable
fun WorldClockTheme(
    themeMode: ThemeMode,
    pureBlack: Boolean,
    accent: Color,
    content: @Composable () -> Unit,
) {
    val dark = themeMode.isDark(isSystemInDarkTheme())
    val base =
        when {
            dark && pureBlack -> PureBlackColorScheme
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
    // The accent doubles as the colour of buttons, links and switches; keep those visible.
    val primary = readableAccent(accent, base.surface, base.onSurface)
    val onPrimary = if (contrastRatio(Color.White, primary) >= 3f) Color.White else Color.Black
    val colorScheme =
        base.copy(
            primary = primary,
            onPrimary = onPrimary,
            // Tonal elevation tints surfaces with this; keep pure black truly black.
            surfaceTint = if (dark && pureBlack) Color.Black else primary,
        )

    MaterialTheme(colorScheme = colorScheme, content = content)
}
