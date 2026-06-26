package io.github.haykh.zham

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    val ACCENT = stringPreferencesKey("accent")
}

/**
 * Persisted app settings. Reads are exposed as [Flow]s (streams that re-emit on every
 * change); writes are `suspend` functions (async, must be called from a coroutine).
 */
class SettingsRepository(
    context: Context,
) {
    private val dataStore = context.dataStore

    val cities: Flow<List<City>> =
        dataStore.data.map { prefs ->
            val json = prefs[Keys.CITIES]
            if (json == null) Clocks.cities else Json.decodeFromString(json)
        }

    suspend fun setCities(cities: List<City>) {
        dataStore.edit { prefs ->
            prefs[Keys.CITIES] = Json.encodeToString(cities)
        }
    }

    // time format — stored as a raw DateTimeFormatter pattern (preset or custom)
    val timeFormat: Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.TIME_FORMAT] ?: "HH:mm:ss" }

    suspend fun setTimeFormat(pattern: String) {
        dataStore.edit { prefs -> prefs[Keys.TIME_FORMAT] = pattern }
    }

    // theme & ui
    val showFlags: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.SHOW_FLAGS] ?: true }

    suspend fun setShowFlags(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_FLAGS] = enabled }
    }

    val themeMode: Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.THEME_MODE] ?: "SYSTEM" }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode }
    }

    // accent — stored as an ARGB int (as a string); null until the user picks one
    val accent: Flow<String?> =
        dataStore.data.map { prefs -> prefs[Keys.ACCENT] }

    suspend fun setAccent(argb: String) {
        dataStore.edit { prefs -> prefs[Keys.ACCENT] = argb }
    }
}
