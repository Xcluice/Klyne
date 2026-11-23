package com.xcluice.klyne.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xcluice.klyne.BuildConfig
import com.xcluice.klyne.core.AppEntry
import com.xcluice.klyne.core.DrawerStyle
import com.xcluice.klyne.core.GestureAction
import com.xcluice.klyne.core.GestureController
import com.xcluice.klyne.core.KlyneSettings
import com.xcluice.klyne.core.SurfaceMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: KlyneSettings,
    allApps: List<AppEntry>,
    palette: KlynePalette,
    gestureController: GestureController,
    onUpdate: (KlyneSettings) -> Unit,
    onCheckUpdate: () -> Unit,
    onClose: () -> Unit
) {
    var showHiddenPicker by remember { mutableStateOf(false) }
    var editingGesture by remember { mutableStateOf<Pair<String, (GestureAction) -> Unit>?>(null) }

    val statusPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .padding(top = statusPad + 6.dp, bottom = navPad)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(palette.pillSurface)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    BackIcon(tint = palette.onSurface, size = 18.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Klyne Settings",
                        color = palette.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} • One UI 9 Edition",
                        color = palette.onSurfaceMuted,
                        fontSize = 12.sp
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 1. Surface Mode Switcher (all 3 modes)
                item {
                    SectionCard("SURFACE MODE", palette) {
                        SurfaceMode.entries.forEach { mode ->
                            val selected = settings.surfaceMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) palette.accent.copy(alpha = 0.14f) else palette.pillSurface.copy(alpha = 0.4f))
                                    .border(
                                        width = if (selected) 1.2.dp else 0.6.dp,
                                        color = if (selected) palette.accent else palette.borderSubtle,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onUpdate(settings.copy(surfaceMode = mode)) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mode.label, color = palette.onSurface, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                                    Text(mode.subtitle, color = palette.onSurfaceMuted, fontSize = 12.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, if (selected) palette.accent else palette.onSurfaceMuted, CircleShape)
                                        .padding(3.dp)
                                ) {
                                    if (selected) {
                                        Box(Modifier.fillMaxSize().clip(CircleShape).background(palette.accent))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        ToggleRow("Dark Surface", settings.darkTheme, palette) {
                            onUpdate(settings.copy(darkTheme = it))
                        }
                        ToggleRow("Monochrome Icons", settings.monochromeIcons, palette) {
                            onUpdate(settings.copy(monochromeIcons = it))
                        }
                    }
                }

                // 2. Gesture Engine
                item {
                    SectionCard("GESTURE ENGINE", palette) {
                        GestureRow("Double Tap Home", settings.doubleTapAction, palette) {
                            editingGesture = "Double Tap Home" to { act -> onUpdate(settings.copy(doubleTapAction = act)) }
                        }
                        GestureRow("Swipe Down", settings.swipeDownAction, palette) {
                            editingGesture = "Swipe Down" to { act -> onUpdate(settings.copy(swipeDownAction = act)) }
                        }
                        GestureRow("Swipe Up", settings.swipeUpAction, palette) {
                            editingGesture = "Swipe Up" to { act -> onUpdate(settings.copy(swipeUpAction = act)) }
                        }
                        GestureRow("Pinch In", settings.pinchInAction, palette) {
                            editingGesture = "Pinch In" to { act -> onUpdate(settings.copy(pinchInAction = act)) }
                        }
                        GestureRow("Two-Finger Swipe Up", settings.twoFingerSwipeUpAction, palette) {
                            editingGesture = "Two-Finger Swipe Up" to { act -> onUpdate(settings.copy(twoFingerSwipeUpAction = act)) }
                        }

                        Spacer(Modifier.height(8.dp))
                        val accOn = gestureController.isAccessibilityEnabled()
                        ActionButtonRow(
                            title = if (accOn) "Gesture Accessibility Active" else "Enable Gesture Accessibility",
                            subtitle = "Required for fingerprint-safe double-tap lock (Android 9+)",
                            palette = palette,
                            onClick = { gestureController.openAccessibilitySettings() }
                        )
                        Spacer(Modifier.height(6.dp))
                        val admOn = gestureController.isDeviceAdminEnabled()
                        ActionButtonRow(
                            title = if (admOn) "Oreo Screen Lock Active" else "Enable Oreo Screen Lock",
                            subtitle = "Fallback for double-tap lock on Android 8.0–8.1",
                            palette = palette,
                            onClick = { gestureController.requestDeviceAdmin() }
                        )
                    }
                }

                // 3. Home Up Layout & Dock
                item {
                    SectionCard("HOME UP LAYOUT & DOCK", palette) {
                        SliderRow("Grid Columns", settings.gridColumns, 4..6, palette) {
                            onUpdate(settings.copy(gridColumns = it))
                        }
                        SliderRow("Grid Rows", settings.gridRows, 4..7, palette) {
                            onUpdate(settings.copy(gridRows = it))
                        }
                        SliderRow("Icon Size (${settings.iconSizeDp} dp)", settings.iconSizeDp, 44..66, palette) {
                            onUpdate(settings.copy(iconSizeDp = it))
                        }
                        SliderRow("Dock Padding (${settings.dockPaddingDp} dp)", settings.dockPaddingDp, 6..24, palette) {
                            onUpdate(settings.copy(dockPaddingDp = it))
                        }
                        ToggleRow("Show Floating Dock", settings.showDock, palette) {
                            onUpdate(settings.copy(showDock = it))
                        }
                        ToggleRow("One UI 9 Dock Blur", settings.dockBlur, palette) {
                            onUpdate(settings.copy(dockBlur = it))
                        }
                        ToggleRow("Home Screen Search Pill", settings.showHomeSearchPill, palette) {
                            onUpdate(settings.copy(showHomeSearchPill = it))
                        }
                        ToggleRow("Top Clock & Battery Widget", settings.showClockWidget, palette) {
                            onUpdate(settings.copy(showClockWidget = it))
                        }
                        ToggleRow("App Labels", settings.showLabels, palette) {
                            onUpdate(settings.copy(showLabels = it))
                        }
                        ToggleRow(
                            label = "Paged App Drawer (off = Vertical)",
                            checked = settings.drawerStyle == DrawerStyle.PAGED,
                            palette = palette
                        ) { paged ->
                            onUpdate(settings.copy(drawerStyle = if (paged) DrawerStyle.PAGED else DrawerStyle.VERTICAL))
                        }
                    }
                }

                // 4. Privacy & System
                item {
                    SectionCard("APPS & SYSTEM", palette) {
                        ActionButtonRow(
                            title = "Hidden Apps (${settings.hiddenPackages.size})",
                            subtitle = "Choose apps to hide from the drawer",
                            palette = palette,
                            onClick = { showHiddenPicker = true }
                        )
                        Spacer(Modifier.height(6.dp))
                        ActionButtonRow(
                            title = "Set Klyne as Default Launcher",
                            subtitle = "Open Android Home app chooser",
                            palette = palette,
                            onClick = { gestureController.openDefaultLauncherSettings() }
                        )
                        Spacer(Modifier.height(6.dp))
                        ActionButtonRow(
                            title = "Check for Updates",
                            subtitle = "Checks GitHub Releases for newer signed APKs",
                            palette = palette,
                            onClick = onCheckUpdate
                        )
                    }
                }

                item { Spacer(Modifier.height(18.dp)) }
            }
        }

        // Gesture picker dialog
        editingGesture?.let { (title, onPick) ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.scrimBackground)
                    .clickable { editingGesture = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(palette.cardSurface)
                        .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(26.dp))
                        .padding(18.dp)
                ) {
                    Text(title, color = palette.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    GestureAction.entries.forEach { act ->
                        Text(
                            text = act.label,
                            color = palette.onSurface,
                            fontSize = 14.5.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onPick(act)
                                    editingGesture = null
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // Hidden apps picker dialog
        if (showHiddenPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.scrimBackground)
                    .clickable { showHiddenPicker = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(520.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(palette.cardSurface)
                        .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(26.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hidden Apps", color = palette.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { showHiddenPicker = false },
                            contentAlignment = Alignment.Center
                        ) { CloseIcon(palette.onSurfaceMuted, 16.dp) }
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(allApps, key = { it.key }) { app ->
                            val isHidden = app.packageName in settings.hiddenPackages
                            ToggleRow(app.label, isHidden, palette) { hide ->
                                val next = if (hide) settings.hiddenPackages + app.packageName
                                else settings.hiddenPackages - app.packageName
                                onUpdate(settings.copy(hiddenPackages = next))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, palette: KlynePalette, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.cardSurface)
            .border(0.8.dp, palette.borderSubtle, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = palette.onSurfaceMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, palette: KlynePalette, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = palette.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onAccent,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.onSurfaceMuted,
                uncheckedTrackColor = palette.pillSurface
            )
        )
    }
}

@Composable
private fun SliderRow(label: String, value: Int, range: IntRange, palette: KlynePalette, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = palette.onSurface, fontSize = 14.sp)
            Text(value.toString(), color = palette.onSurfaceMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.pillSurface
            )
        )
    }
}

@Composable
private fun GestureRow(label: String, action: GestureAction, palette: KlynePalette, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.onSurface, fontSize = 14.sp)
        Text(action.label, color = palette.onSurfaceMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionButtonRow(title: String, subtitle: String, palette: KlynePalette, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.pillSurface.copy(alpha = 0.7f))
            .border(0.6.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(title, color = palette.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = palette.onSurfaceMuted, fontSize = 12.sp)
    }
}
