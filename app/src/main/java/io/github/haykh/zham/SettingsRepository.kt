package io.github.haykh.zham

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// One DataStore instance for the whole app, attached to the application Context.
// `by preferencesDataStore(...)` is a property delegate: it lazily creates and
// then always returns the same DataStore, backed by a file called settings.preferences_pb.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** The typed keys we read/write. Like the string keys you'd use in localStorage,
 *  but each one carries its value type so reads/writes are type-checked. */
private object Keys {
    val CITIES = stringPreferencesKey("cities_json")
    val TIME_FORMAT = stringPreferencesKey("time_format")
    val SHOW_FLAGS = booleanPreferencesKey("show_flags")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val PURE_BLACK = booleanPreferencesKey("pure_black")
    val ACCENT = stringPreferencesKey("accent")
    val WIDGET_BG_COLOR = intPreferencesKey("widget_bg_color")
    val WIDGET_BG_ALPHA = floatPreferencesKey("widget_bg_alpha")
    val WIDGET_FROST = floatPreferencesKey("widget_frost")
    val WIDGET_LABEL_COLOR = stringPreferencesKey("widget_label_color")
    val WIDGET_TIME_COLOR = stringPreferencesKey("widget_time_color")
}

private inline fun <reified E : Enum<E>> enumOrDefault(
    name: String?,
    default: E,
): E = name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

// Decoding lives here, once, so the app and the widget always agree on defaults.
private fun Preferences.cities(): List<City> = this[Keys.CITIES]?.let { Json.decodeFromString(it) } ?: Clocks.cities

private fun Preferences.timeFormat(): String = this[Keys.TIME_FORMAT] ?: "HH:mm:ss"

private fun Preferences.showFlags(): Boolean = this[Keys.SHOW_FLAGS] ?: true

private fun Preferences.themeMode(): ThemeMode = enumOrDefault(this[Keys.THEME_MODE], ThemeMode.SYSTEM)

private fun Preferences.pureBlack(): Boolean = this[Keys.PURE_BLACK] ?: false

// accent — stored as an ARGB int (as a string); absent until the user picks one
private fun Preferences.accent(): Color = this[Keys.ACCENT]?.toIntOrNull()?.let { Color(it) } ?: DefaultAccent

private fun Preferences.widgetStyle(): WidgetStyle {
    val defaults = WidgetStyle()
    return WidgetStyle(
        backgroundColor = this[Keys.WIDGET_BG_COLOR]?.let { Color(it) } ?: defaults.backgroundColor,
        backgroundAlpha = this[Keys.WIDGET_BG_ALPHA] ?: defaults.backgroundAlpha,
        frost = this[Keys.WIDGET_FROST] ?: defaults.frost,
        labelColor = enumOrDefault(this[Keys.WIDGET_LABEL_COLOR], defaults.labelColor),
        timeColor = enumOrDefault(this[Keys.WIDGET_TIME_COLOR], defaults.timeColor),
    )
}

/** Everything the home-screen widget renders, decoded from a single DataStore snapshot. */
data class WidgetState(
    val cities: List<City>,
    val showFlags: Boolean,
    val timeFormat: String,
    val themeMode: ThemeMode,
    val pureBlack: Boolean,
    val accent: Color,
    val style: WidgetStyle,
)

/**
 * Persisted app settings. Reads are exposed as [Flow]s (streams that re-emit on every
 * change); writes are `suspend` functions (async, must be called from a coroutine).
 */
class SettingsRepository(
    context: Context,
) {
    private val dataStore = context.dataStore

    val cities: Flow<List<City>> = dataStore.data.map { it.cities() }

    suspend fun setCities(cities: List<City>) {
        dataStore.edit { prefs ->
            prefs[Keys.CITIES] = Json.encodeToString(cities)
        }
    }

    // time format — stored as a raw DateTimeFormatter pattern (preset or custom)
    val timeFormat: Flow<String> = dataStore.data.map { it.timeFormat() }

    suspend fun setTimeFormat(pattern: String) {
        dataStore.edit { prefs -> prefs[Keys.TIME_FORMAT] = pattern }
    }

    // theme & ui
    val showFlags: Flow<Boolean> = dataStore.data.map { it.showFlags() }

    suspend fun setShowFlags(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_FLAGS] = enabled }
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { it.themeMode() }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    /** Use true black instead of dark grey whenever the dark theme is active (AMOLED). */
    val pureBlack: Flow<Boolean> = dataStore.data.map { it.pureBlack() }

    suspend fun setPureBlack(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PURE_BLACK] = enabled }
    }

    val accent: Flow<Color> = dataStore.data.map { it.accent() }

    suspend fun setAccent(color: Color) {
        dataStore.edit { prefs -> prefs[Keys.ACCENT] = color.toArgb().toString() }
    }

    val widgetStyle: Flow<WidgetStyle> = dataStore.data.map { it.widgetStyle() }

    suspend fun setWidgetStyle(style: WidgetStyle) {
        dataStore.edit { prefs ->
            val bg = style.backgroundColor
            if (bg == null) prefs.remove(Keys.WIDGET_BG_COLOR) else prefs[Keys.WIDGET_BG_COLOR] = bg.toArgb()
            prefs[Keys.WIDGET_BG_ALPHA] = style.backgroundAlpha
            prefs[Keys.WIDGET_FROST] = style.frost
            prefs[Keys.WIDGET_LABEL_COLOR] = style.labelColor.name
            prefs[Keys.WIDGET_TIME_COLOR] = style.timeColor.name
        }
    }

    /**
     * Put every setting back to its default and give each city its original name.
     * The cities themselves (and their order) are kept: they're the user's data, not a setting.
     */
    suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            val cities = prefs.cities().map { it.copy(label = City.defaultLabel(it.zoneIdName)) }
            prefs.clear()
            prefs[Keys.CITIES] = Json.encodeToString(cities)
        }
    }

    val widgetState: Flow<WidgetState> =
        dataStore.data.map { prefs ->
            WidgetState(
                cities = prefs.cities(),
                showFlags = prefs.showFlags(),
                timeFormat = prefs.timeFormat(),
                themeMode = prefs.themeMode(),
                pureBlack = prefs.pureBlack(),
                accent = prefs.accent(),
                style = prefs.widgetStyle(),
            )
        }
}
