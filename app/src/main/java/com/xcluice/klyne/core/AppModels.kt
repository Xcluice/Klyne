package com.xcluice.klyne.core

import androidx.compose.ui.graphics.ImageBitmap

enum class SurfaceMode(val label: String, val subtitle: String) {
    FROSTED_GLASS("Frosted Glass", "One UI 9 translucent blur over wallpaper"),
    OLED_MATTE("OLED Matte", "Pure pitch black or crisp paper white"),
    MUTED_ADAPTIVE("Muted Adaptive", "Soft wallpaper-tinted monochrome surfaces")
}

enum class DrawerStyle(val label: String) {
    VERTICAL("Vertical List"),
    PAGED("Paged Grid")
}

enum class GestureAction(val label: String) {
    NONE("None"),
    LOCK_SCREEN("Lock Screen"),
    NOTIFICATIONS("Notification Shade"),
    QUICK_SETTINGS("Quick Settings"),
    OPEN_DRAWER("Open App Drawer"),
    OPEN_SEARCH("Finder Search"),
    HOME_EDITOR("Home Editor"),
    OPEN_SETTINGS("Klyne Settings"),
    RECENTS("Recent Apps")
}

data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: ImageBitmap?,
    val isSystem: Boolean = false
) {
    val key: String get() = "$packageName/$activityName"
}

data class FolderItem(
    val id: String,
    val name: String,
    val appKeys: List<String>
)

sealed class HomeSlot {
    data class App(val appKey: String) : HomeSlot()
    data class Folder(val folderId: String) : HomeSlot()
}

data class KlyneSettings(
    val surfaceMode: SurfaceMode = SurfaceMode.FROSTED_GLASS,
    val darkTheme: Boolean = true,
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val iconSizeDp: Int = 54,
    val showLabels: Boolean = true,
    val showDock: Boolean = true,
    val dockBlur: Boolean = true,
    val dockPaddingDp: Int = 14,
    val showHomeSearchPill: Boolean = true,
    val showClockWidget: Boolean = true,
    val monochromeIcons: Boolean = false,
    val drawerStyle: DrawerStyle = DrawerStyle.VERTICAL,
    val doubleTapAction: GestureAction = GestureAction.LOCK_SCREEN,
    val swipeDownAction: GestureAction = GestureAction.NOTIFICATIONS,
    val swipeUpAction: GestureAction = GestureAction.OPEN_DRAWER,
    val pinchInAction: GestureAction = GestureAction.HOME_EDITOR,
    val twoFingerSwipeUpAction: GestureAction = GestureAction.OPEN_SEARCH,
    val hiddenPackages: Set<String> = emptySet(),
    val dockAppKeys: List<String> = emptyList(),
    val homeAppKeys: List<String> = emptyList(),
    val folders: List<FolderItem> = emptyList()
)
