package io.github.haykh.zham

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val vm: WorldClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.syncWidget()
        setContent {
            val cities by vm.cities.collectAsStateWithLifecycle()
            val timeFormat by vm.timeFormat.collectAsStateWithLifecycle()
            val showFlags by vm.showFlags.collectAsStateWithLifecycle()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val pureBlack by vm.pureBlack.collectAsStateWithLifecycle()
            val accent by vm.accent.collectAsStateWithLifecycle()
            val widgetStyle by vm.widgetStyle.collectAsStateWithLifecycle()
            WorldClockTheme(themeMode = themeMode, pureBlack = pureBlack, accent = accent) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorldClockApp(
                        cities = cities,
                        onAddCity = vm::addCity,
                        onRemoveCity = vm::removeCity,
                        onRenameCity = vm::renameCity,
                        onReorder = vm::saveOrder,
                        themeMode = themeMode,
                        pureBlack = pureBlack,
                        accent = accent,
                        onSetTheme = vm::setThemeMode,
                        onSetPureBlack = vm::setPureBlack,
                        onSetAccent = vm::setAccent,
                        showFlags = showFlags,
                        timeFormat = timeFormat,
                        onSetShowFlags = vm::setShowFlags,
                        onSetTimeFormat = vm::setTimeFormat,
                        widgetStyle = widgetStyle,
                        onSetWidgetStyle = vm::setWidgetStyle,
                        onRestoreDefaults = vm::restoreDefaults,
                    )
                }
            }
        }
    }
}

/** Bottom-nav shell: a "Clocks" tab (the list) and a "Convert" tab (the time scrubber). */
@Composable
fun WorldClockApp(
    cities: List<City>,
    onAddCity: (City) -> Unit,
    onRemoveCity: (City) -> Unit,
    onRenameCity: (City, String) -> Unit,
    onReorder: (List<City>) -> Unit,
    themeMode: ThemeMode,
    pureBlack: Boolean,
    accent: Color,
    onSetTheme: (ThemeMode) -> Unit,
    onSetPureBlack: (Boolean) -> Unit,
    onSetAccent: (Color) -> Unit,
    showFlags: Boolean,
    timeFormat: String,
    onSetShowFlags: (Boolean) -> Unit,
    onSetTimeFormat: (String) -> Unit,
    widgetStyle: WidgetStyle,
    onSetWidgetStyle: (WidgetStyle) -> Unit,
    onRestoreDefaults: () -> Unit,
) {
    val nav = rememberNavController()
    Scaffold(
        // Only reserve space for the bottom bar here; each screen's own Scaffold/TopAppBar
        // handles the status-bar inset, so we zero system insets to avoid double padding.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                val backStackEntry by nav.currentBackStackEntryAsState()
                val route = backStackEntry?.destination?.route
                NavigationBarItem(
                    selected = route == "clocks",
                    onClick = {
                        nav.navigate("clocks") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(ZhamIcons.Clock, contentDescription = null) },
                    label = { Text("Clocks") },
                )
                NavigationBarItem(
                    selected = route == "convert",
                    onClick = {
                        nav.navigate("convert") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(ZhamIcons.Convert, contentDescription = null) },
                    label = { Text("Convert") },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "clocks",
            modifier = Modifier.padding(padding),
        ) {
            composable("clocks") {
                WorldClockScreen(
                    cities = cities,
                    onAddCity = onAddCity,
                    onRemoveCity = onRemoveCity,
                    onRenameCity = onRenameCity,
                    onReorder = onReorder,
                    themeMode = themeMode,
                    pureBlack = pureBlack,
                    accent = accent,
                    onSetTheme = onSetTheme,
                    onSetPureBlack = onSetPureBlack,
                    onSetAccent = onSetAccent,
                    showFlags = showFlags,
                    timeFormat = timeFormat,
                    onSetShowFlags = onSetShowFlags,
                    onSetTimeFormat = onSetTimeFormat,
                    widgetStyle = widgetStyle,
                    onSetWidgetStyle = onSetWidgetStyle,
                    onRestoreDefaults = onRestoreDefaults,
                )
            }
            composable("convert") {
                ConverterScreen(cities = cities, timeFormat = timeFormat, showFlags = showFlags)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(
    cities: List<City>,
    onAddCity: (City) -> Unit,
    onRemoveCity: (City) -> Unit,
    onRenameCity: (City, String) -> Unit,
    onReorder: (List<City>) -> Unit,
    themeMode: ThemeMode,
    pureBlack: Boolean,
    accent: Color,
    onSetTheme: (ThemeMode) -> Unit,
    onSetPureBlack: (Boolean) -> Unit,
    onSetAccent: (Color) -> Unit,
    showFlags: Boolean,
    timeFormat: String,
    onSetShowFlags: (Boolean) -> Unit,
    onSetTimeFormat: (String) -> Unit,
    widgetStyle: WidgetStyle,
    onSetWidgetStyle: (WidgetStyle) -> Unit,
    onRestoreDefaults: () -> Unit,
) {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000)
        }
    }

    var items by remember(cities) { mutableStateOf(cities) }

    val lazyListState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromIndex = items.indexOfFirst { it.zoneIdName == from.key }
            val toIndex = items.indexOfFirst { it.zoneIdName == to.key }
            if (fromIndex != -1 && toIndex != -1) {
                items = items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            }
        }

    val formatter = remember(timeFormat) { Clocks.formatterOf(timeFormat) }

    var showPicker by remember { mutableStateOf(false) }

    var showSettings by remember { mutableStateOf(false) }

    var renaming by remember { mutableStateOf<City?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("World Clock") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(ZhamIcons.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.zoneIdName }) { city ->
                ReorderableItem(reorderableState, key = city.zoneIdName) { isDragging ->
                    SwipeableClockRow(
                        city = city,
                        now = now,
                        formatter = formatter,
                        showFlags = showFlags,
                        onRemove = { onRemoveCity(city) },
                        onEdit = { renaming = city },
                        handleModifier = Modifier.draggableHandle(onDragStopped = { onReorder(items) }),
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(ZhamIcons.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add city")
                }
            }
        }
    }

    if (showPicker) {
        CityPickerDialog(
            existing = cities,
            onPick = { city ->
                onAddCity(city)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    renaming?.let { city ->
        RenameCityDialog(
            city = city,
            onRename = { label ->
                onRenameCity(city, label)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    if (showSettings) {
        SettingsDialog(
            themeMode = themeMode,
            pureBlack = pureBlack,
            accent = accent,
            showFlags = showFlags,
            timeFormat = timeFormat,
            widgetStyle = widgetStyle,
            onSetTheme = onSetTheme,
            onSetPureBlack = onSetPureBlack,
            onSetAccent = onSetAccent,
            onSetShowFlags = onSetShowFlags,
            onSetTimeFormat = onSetTimeFormat,
            onSetWidgetStyle = onSetWidgetStyle,
            onRestoreDefaults = onRestoreDefaults,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
fun ClockRow(
    city: City,
    now: ZonedDateTime,
    formatter: DateTimeFormatter,
    showFlags: Boolean,
    onClick: () -> Unit,
    handleModifier: Modifier,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                ZhamIcons.DragHandle,
                contentDescription = "Drag to reorder ${city.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = handleModifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showFlags) {
                        val flag = remember(city.zoneIdName) { Flags.emojiFor(city.zoneIdName) }
                        Text(flag, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(city.label, style = MaterialTheme.typography.titleMedium)
                }
                Text(city.zone.id, style = MaterialTheme.typography.bodySmall)
            }
            Text(Clocks.format(city, now, formatter), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun SwipeableClockRow(
    city: City,
    now: ZonedDateTime,
    formatter: DateTimeFormatter,
    showFlags: Boolean,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    handleModifier: Modifier,
) {
    val density = LocalDensity.current
    val actionWidth = 96.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxWidth()) {
        // Revealed delete action, sitting behind the row and pinned to the right.
        Row(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    onRemove()
                    scope.launch { offsetX.animateTo(0f) }
                },
                modifier = Modifier.width(actionWidth),
            ) {
                Icon(
                    ZhamIcons.Delete,
                    contentDescription = "Delete ${city.label}",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        // Foreground row: drag horizontally to reveal; it snaps fully open or shut
        // (it does NOT delete on swipe — you tap the revealed button).
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                scope.launch {
                                    offsetX.snapTo((offsetX.value + delta).coerceIn(-actionWidthPx, 0f))
                                }
                            },
                        onDragStopped = {
                            val target = if (offsetX.value <= -actionWidthPx / 2f) -actionWidthPx else 0f
                            scope.launch { offsetX.animateTo(target) }
                        },
                    ),
        ) {
            ClockRow(
                city = city,
                now = now,
                formatter = formatter,
                showFlags = showFlags,
                // Tap renames; if the delete action is showing, a tap just closes it.
                onClick = {
                    if (offsetX.value != 0f) scope.launch { offsetX.animateTo(0f) } else onEdit()
                },
                handleModifier = handleModifier,
            )
        }
    }
}

@Composable
fun CityPickerDialog(
    existing: List<City>,
    onPick: (City) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val allZones = remember { ZoneId.getAvailableZoneIds().sorted() }
    val existingIds = remember(existing) { existing.map { it.zoneIdName }.toSet() }
    val filtered =
        remember(query, allZones) {
            if (query.isBlank()) {
                allZones
            } else {
                allZones.filter { it.contains(query, ignoreCase = true) }
            }
        }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search zones") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(filtered, key = { it }) { zoneId ->
                        val already = zoneId in existingIds
                        Text(
                            text = zoneId,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !already) {
                                        onPick(
                                            City(label = City.defaultLabel(zoneId), zoneIdName = zoneId),
                                        )
                                    }.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Rename a city (e.g. "Tokyo" instead of "Japan", or "NYC" for a narrower widget). */
@Composable
fun RenameCityDialog(
    city: City,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultLabel = remember(city.zoneIdName) { City.defaultLabel(city.zoneIdName) }
    // Start with the current name selected, so typing replaces it outright.
    var name by remember { mutableStateOf(TextFieldValue(city.label, TextRange(0, city.label.length))) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename city") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text(defaultLabel) },
                supportingText = { Text(city.zoneIdName) },
                trailingIcon = {
                    if (name.text != defaultLabel) {
                        IconButton(onClick = { name = TextFieldValue(defaultLabel, TextRange(defaultLabel.length)) }) {
                            Icon(ZhamIcons.Restore, contentDescription = "Restore default name")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onRename(name.text) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        confirmButton = { TextButton(onClick = { onRename(name.text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    themeMode: ThemeMode,
    pureBlack: Boolean,
    accent: Color,
    showFlags: Boolean,
    timeFormat: String,
    widgetStyle: WidgetStyle,
    onSetTheme: (ThemeMode) -> Unit,
    onSetPureBlack: (Boolean) -> Unit,
    onSetAccent: (Color) -> Unit,
    onSetShowFlags: (Boolean) -> Unit,
    onSetTimeFormat: (String) -> Unit,
    onSetWidgetStyle: (WidgetStyle) -> Unit,
    onRestoreDefaults: () -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { ZonedDateTime.now() }
    var showLicense by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    // Hoisted so restoring defaults can close the custom pickers, whose wheel and hex
    // field are seeded from the colour only when opened.
    var showPicker by remember { mutableStateOf(false) }
    var showBgPicker by remember { mutableStateOf(false) }
    val autoWidgetBackground = widgetThemeSurface(themeMode.isDark(isSystemInDarkTheme()), pureBlack)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, density.fontScale / 1.2f),
            ) {
                Column(
                    modifier =
                        Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Show flags", style = MaterialTheme.typography.titleLarge)
                        Switch(checked = showFlags, onCheckedChange = onSetShowFlags)
                    }

                    Spacer(Modifier.height(20.dp))

                    Text("Time format", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    val isCustom = timeFormat !in timeFormatPresets
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = if (isCustom) "Custom" else now.format(Clocks.formatterOf(timeFormat)).lowercase(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Preset") },
                            trailingIcon = {
                                Icon(
                                    ZhamIcons.ChevronDown,
                                    contentDescription = null,
                                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                                )
                            },
                            modifier =
                                Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            timeFormatPresets.forEach { pattern ->
                                DropdownMenuItem(
                                    text = { Text(now.format(Clocks.formatterOf(pattern)).lowercase()) },
                                    onClick = {
                                        onSetTimeFormat(pattern)
                                        expanded = false
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Custom…") },
                                onClick = {
                                    onSetTimeFormat("EEE HH:mm") // a non-preset seed; edit it below
                                    expanded = false
                                },
                            )
                        }
                    }
                    if (isCustom) {
                        val valid = runCatching { DateTimeFormatter.ofPattern(timeFormat) }.isSuccess
                        OutlinedTextField(
                            value = timeFormat,
                            onValueChange = onSetTimeFormat,
                            label = { Text("Custom pattern") },
                            singleLine = true,
                            isError = !valid,
                            supportingText = {
                                Text(
                                    if (valid) {
                                        "Preview: " + now.format(Clocks.formatterOf(timeFormat)).lowercase()
                                    } else {
                                        "Invalid pattern"
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // --- Theme: a single button that cycles System → Light → Dark ---
                    Text("Theme", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onSetTheme(themeMode.next()) }) {
                        Icon(themeMode.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(themeMode.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pure black", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "AMOLED black background in dark mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = pureBlack,
                            onCheckedChange = onSetPureBlack,
                            enabled = themeMode != ThemeMode.LIGHT,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // --- Accent: preset swatches + a custom RGB picker ---
                    Text("Accent", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    ColorChooser(
                        selected = accent,
                        showPicker = showPicker,
                        onShowPickerChange = { showPicker = it },
                        onSelect = { it?.let(onSetAccent) },
                    )

                    Spacer(Modifier.height(20.dp))

                    // --- Widget: background colour, transparency, glass, text colours ---
                    Text("Widget", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    WidgetStyleSettings(
                        style = widgetStyle,
                        onChange = onSetWidgetStyle,
                        autoBackground = autoWidgetBackground,
                        showBackgroundPicker = showBgPicker,
                        onShowBackgroundPickerChange = { showBgPicker = it },
                    )

                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            onClick = { showLicense = true },
                            contentPadding = PaddingValues(0.dp),
                        ) { Text("License") }
                        TextButton(
                            onClick = { confirmRestore = true },
                            contentPadding = PaddingValues(0.dp),
                        ) { Text("Restore defaults") }
                    }
                }
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore defaults?") },
            text = {
                Text(
                    "Theme, accent, time format, flags and widget appearance go back to their " +
                        "defaults, and every city gets its original name back. " +
                        "Your cities and their order are kept.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    showBgPicker = false
                    onRestoreDefaults()
                    confirmRestore = false
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Cancel") }
            },
        )
    }

    if (showLicense) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            confirmButton = {
                TextButton(onClick = { showLicense = false }) { Text("Close") }
            },
            title = { Text("License") },
            text = {
                Text(
                    text = LICENSE_TEXT,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
        )
    }
}

// Paragraphs are single logical lines so the Text wraps them to the dialog width;
// only the header/copyright line break and the paragraph gaps are kept.
private val LICENSE_TEXT =
    "Zham — a world clock and time-zone converter for Android.\n" +
        "Copyright (C) 2026  @haykh\n" +
        "\n" +
        "This program is free software: you can redistribute it and/or modify " +
        "it under the terms of the GNU General Public License as published by " +
        "the Free Software Foundation, either version 3 of the License, or " +
        "(at your option) any later version.\n" +
        "\n" +
        "This program is distributed in the hope that it will be useful, " +
        "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the " +
        "GNU General Public License for more details.\n" +
        "\n" +
        "You should have received a copy of the GNU General Public License " +
        "along with this program.  If not, see <http://www.gnu.org/licenses/>."

@Composable
private fun WidgetStyleSettings(
    style: WidgetStyle,
    onChange: (WidgetStyle) -> Unit,
    autoBackground: Color,
    showBackgroundPicker: Boolean,
    onShowBackgroundPickerChange: (Boolean) -> Unit,
) {
    Column {
        Text("Background color", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ColorChooser(
            selected = style.backgroundColor,
            autoColor = autoBackground,
            showPicker = showBackgroundPicker,
            onShowPickerChange = onShowBackgroundPickerChange,
            onSelect = { onChange(style.copy(backgroundColor = it)) },
        )

        Spacer(Modifier.height(12.dp))
        // Stored as opacity; shown as transparency (0% = solid, 100% = see-through).
        PercentSlider(
            title = "Background transparency",
            value = 1f - style.backgroundAlpha,
            onValueChangeFinished = { onChange(style.copy(backgroundAlpha = 1f - it)) },
        )
        Spacer(Modifier.height(4.dp))
        PercentSlider(
            title = "Frosted glass",
            subtitle = "Glass rim, sheen and grain. Best with some transparency.",
            value = style.frost,
            onValueChangeFinished = { onChange(style.copy(frost = it)) },
        )

        Spacer(Modifier.height(4.dp))
        Text("City names", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        WidgetTextColorPicker(selected = style.labelColor) { onChange(style.copy(labelColor = it)) }

        Spacer(Modifier.height(12.dp))
        Text("Times", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        WidgetTextColorPicker(selected = style.timeColor) { onChange(style.copy(timeColor = it)) }
    }
}

/**
 * A 0–100% slider in 10% steps. It moves locally while dragging and reports once on release,
 * so the widget is re-rendered once rather than on every drag frame.
 */
@Composable
private fun PercentSlider(
    title: String,
    value: Float,
    onValueChangeFinished: (Float) -> Unit,
    subtitle: String? = null,
) {
    var current by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("${(current * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onValueChangeFinished(current) },
            steps = 9,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetTextColorPicker(
    selected: WidgetTextColor,
    onSelect: (WidgetTextColor) -> Unit,
) {
    val options = WidgetTextColor.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(inactiveContainerColor = Color.Transparent),
                icon = {}, // no checkmark: four labels need the width
                label = { Text(option.label, maxLines = 1) },
            )
        }
    }
}

/**
 * Colour swatches: an optional leading "Auto" swatch (follow the theme, reported as null and
 * drawn in [autoColor]), the presets, and a "+" swatch that toggles a wheel + hex picker.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorChooser(
    selected: Color?,
    showPicker: Boolean,
    onShowPickerChange: (Boolean) -> Unit,
    onSelect: (Color?) -> Unit,
    autoColor: Color? = null,
) {
    // What the "+" swatch shows and what the picker starts from.
    val current = selected ?: autoColor ?: DefaultAccent
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (autoColor != null) {
            ColorSwatch(
                color = autoColor,
                selected = selected == null && !showPicker,
                onClick = {
                    onShowPickerChange(false)
                    onSelect(null)
                },
                icon = ZhamIcons.ThemeSystem,
                iconDescription = "Follow theme",
            )
        }
        accentPresets.forEach { preset ->
            ColorSwatch(
                color = preset,
                selected = preset == selected && !showPicker,
                onClick = {
                    onShowPickerChange(false)
                    onSelect(preset)
                },
            )
        }
        ColorSwatch(
            color = current,
            selected = showPicker,
            onClick = { onShowPickerChange(!showPicker) },
            icon = ZhamIcons.Add,
            iconDescription = "Custom color",
        )
    }
    if (showPicker) {
        Spacer(Modifier.height(12.dp))
        ColorSlidersPicker(color = current, onColorChange = onSelect)
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconDescription: String? = null,
) {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            // Readable on any swatch colour (e.g. a white or black accent).
            Icon(icon, contentDescription = iconDescription, tint = textColorOn(color), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ColorSlidersPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    val controller = rememberColorPickerController()
    // Hex digits as typed (no '#'). Kept in sync synchronously from the wheel callback
    // rather than from `color`: that value round-trips through DataStore and would land
    // late, clobbering text the user is still typing. (Picking a preset closes this picker,
    // and reopening it starts fresh from the current colour.)
    var hex by remember { mutableStateOf(color.toHex()) }
    Column {
        HsvColorPicker(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            controller = controller,
            initialColor = color,
            onColorChanged = { envelope ->
                // fromUser guards against the initial/programmatic callback re-writing state.
                if (envelope.fromUser) {
                    hex = envelope.color.toHex()
                    onColorChange(envelope.color)
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        BrightnessSlider(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            controller = controller,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = hex,
            onValueChange = { input ->
                hex = input.filter { it.isHexDigit() }.take(6).uppercase()
                parseHexColor(hex)?.let { parsed ->
                    // Move the wheel to match; fromUser=false so its callback doesn't echo back.
                    controller.selectByColor(parsed, fromUser = false)
                    onColorChange(parsed)
                }
            },
            label = { Text("Hex") },
            prefix = { Text("#") },
            isError = parseHexColor(hex) == null,
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
