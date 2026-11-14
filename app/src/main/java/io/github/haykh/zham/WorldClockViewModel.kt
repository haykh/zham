package io.github.haykh.zham

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
        repo.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val pureBlack: StateFlow<Boolean> = repo.pureBlack.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val accent: StateFlow<Color> = repo.accent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultAccent)

    val widgetStyle: StateFlow<WidgetStyle> =
        repo.widgetStyle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WidgetStyle())

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

    /** Rename a city; a blank name restores the default derived from its zone id. */
    fun renameCity(
        city: City,
        label: String,
    ) = viewModelScope.launch {
        val name = label.trim().ifEmpty { City.defaultLabel(city.zoneIdName) }
        repo.setCities(cities.value.map { if (it.zoneIdName == city.zoneIdName) it.copy(label = name) else it })
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
            repo.setThemeMode(mode)
            refreshWidget()
        }

    fun setPureBlack(enabled: Boolean) =
        viewModelScope.launch {
            repo.setPureBlack(enabled)
            refreshWidget()
        }

    fun setAccent(color: Color) =
        viewModelScope.launch {
            repo.setAccent(color)
            refreshWidget()
        }

    fun restoreDefaults() =
        viewModelScope.launch {
            repo.resetToDefaults()
            refreshWidget()
        }

    fun setWidgetStyle(style: WidgetStyle) =
        viewModelScope.launch {
            repo.setWidgetStyle(style)
            refreshWidget()
        }
}
