package io.github.haykh.zham

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorldClockViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repo = SettingsRepository(application)

    val cities: StateFlow<List<City>> = repo.cities.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Clocks.cities)

    val timeFormat: StateFlow<String> =
        repo.timeFormat.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "HH:mm:ss")

    val showFlags: StateFlow<Boolean> = repo.showFlags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<ThemeMode> =
        repo.themeMode
            .map { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val accent: StateFlow<Color> =
        repo.accent
            .map { stored -> stored?.toIntOrNull()?.let { Color(it) } ?: DefaultAccent }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultAccent)

    fun addCity(city: City) =
        viewModelScope.launch {
            val current = cities.value
            if (current.none { it.zoneIdName == city.zoneIdName }) {
                repo.setCities(current + city)
                refreshWidget()
            }
        }

    fun removeCity(city: City) =
        viewModelScope.launch {
            repo.setCities(cities.value.filterNot { it.zoneIdName == city.zoneIdName })
            refreshWidget()
        }

    fun moveCity(
        from: Int,
        to: Int,
    ) = viewModelScope.launch {
        val list = cities.value.toMutableList()
        list.add(to, list.removeAt(from))
        repo.setCities(list)
        refreshWidget()
    }

    fun saveOrder(reordered: List<City>) =
        viewModelScope.launch {
            repo.setCities(reordered)
            refreshWidget()
        }

    fun setTimeFormat(pattern: String) =
        viewModelScope.launch {
            repo.setTimeFormat(pattern)
            refreshWidget()
        }

    fun setShowFlags(enabled: Boolean) =
        viewModelScope.launch {
            repo.setShowFlags(enabled)
            refreshWidget()
        }

    /** Re-render any placed widgets so settings changes show up without waiting for the refresh cycle. */
    private suspend fun refreshWidget() {
        WorldClockWidget().updateAll(getApplication<Application>())
    }

    /** Reconcile placed widgets with the persisted state — call on app launch so any drift self-heals. */
    fun syncWidget() = viewModelScope.launch { refreshWidget() }

    fun setThemeMode(mode: ThemeMode) =
        viewModelScope.launch {
            repo.setThemeMode(mode.name)
            refreshWidget()
        }

    fun setAccent(color: Color) =
        viewModelScope.launch {
            repo.setAccent(color.toArgb().toString())
            refreshWidget()
        }
}
