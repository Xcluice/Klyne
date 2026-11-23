package com.xcluice.klyne

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xcluice.klyne.core.AppEntry
import com.xcluice.klyne.core.AppRepository
import com.xcluice.klyne.core.FolderItem
import com.xcluice.klyne.core.ForceUpdater
import com.xcluice.klyne.core.GestureAction
import com.xcluice.klyne.core.GestureController
import com.xcluice.klyne.core.KlyneSettings
import com.xcluice.klyne.core.ReleaseInfo
import com.xcluice.klyne.core.SettingsStore
import com.xcluice.klyne.core.SurfaceMode
import com.xcluice.klyne.ui.AppDrawerOverlay
import com.xcluice.klyne.ui.CloseIcon
import com.xcluice.klyne.ui.FinderSearchPill
import com.xcluice.klyne.ui.ForceUpdateScreen
import com.xcluice.klyne.ui.KlynePalette
import com.xcluice.klyne.ui.OneUiClockWidget
import com.xcluice.klyne.ui.PlusIcon
import com.xcluice.klyne.ui.SettingsScreen
import com.xcluice.klyne.ui.SquircleAppTile
import com.xcluice.klyne.ui.SquircleFolderTile
import com.xcluice.klyne.ui.rememberKlynePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = SettingsStore(this)
        val repo = AppRepository(this)
        val gestures = GestureController(this)

        setContent {
            KlyneRoot(store = store, repo = repo, gestures = gestures)
        }
    }
}

@Composable
private fun KlyneRoot(
    store: SettingsStore,
    repo: AppRepository,
    gestures: GestureController
) {
    val scope = rememberCoroutineScope()
    val allApps by repo.apps.collectAsState()
    val wallpaperSeed by repo.wallpaperSeed.collectAsState()

    var settings by remember { mutableStateOf(store.load()) }
    var drawerOpen by remember { mutableStateOf(false) }
    var drawerFocusSearch by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var homeEditorOpen by remember { mutableStateOf(false) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var contextMenuApp by remember { mutableStateOf<AppEntry?>(null) }
    var lockPermissionPrompt by remember { mutableStateOf(false) }
    var pendingRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    val palette = rememberKlynePalette(
        mode = settings.surfaceMode,
        darkTheme = settings.darkTheme,
        wallpaperSeed = wallpaperSeed
    )

    fun saveSettings(next: KlyneSettings) {
        settings = next
        store.save(next)
    }

    LaunchedEffect(Unit) {
        val loaded = repo.reloadApps()
        if (!store.isInitialized() && loaded.isNotEmpty()) {
            val dock = repo.pickDefaultDock(loaded)
            val homeSeed = loaded.filter { it.key !in dock }.take(8).map { it.key }
            saveSettings(settings.copy(dockAppKeys = dock, homeAppKeys = homeSeed))
            store.markInitialized()
        }
        pendingRelease = ForceUpdater.checkForUpdate()
    }

    DisposableEffect(Unit) {
        repo.registerCallback {
            scope.launch { repo.reloadApps() }
        }
        onDispose { repo.unregisterCallback() }
    }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(2400)
            toastMsg = null
        }
    }

    val appMap = remember(allApps) { allApps.associateBy { it.key } }
    val frequentApps = remember(allApps, settings.hiddenPackages) {
        allApps
            .filter { it.packageName !in settings.hiddenPackages }
            .sortedByDescending { store.getLaunchScore(it.key) }
            .take(6)
    }

    fun launch(app: AppEntry) {
        store.recordLaunch(app.key)
        drawerOpen = false
        openFolderId = null
        repo.launchApp(app)
    }

    fun runGesture(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.LOCK_SCREEN -> {
                if (!gestures.lockScreen()) lockPermissionPrompt = true
            }
            GestureAction.NOTIFICATIONS -> gestures.expandNotifications()
            GestureAction.QUICK_SETTINGS -> gestures.expandQuickSettings()
            GestureAction.OPEN_DRAWER -> {
                drawerFocusSearch = false
                drawerOpen = true
            }
            GestureAction.OPEN_SEARCH -> {
                drawerFocusSearch = true
                drawerOpen = true
            }
            GestureAction.HOME_EDITOR -> homeEditorOpen = true
            GestureAction.OPEN_SETTINGS -> settingsOpen = true
            GestureAction.RECENTS -> gestures.openRecents()
        }
    }

    BackHandler(enabled = drawerOpen || settingsOpen || homeEditorOpen || openFolderId != null || contextMenuApp != null) {
        when {
            contextMenuApp != null -> contextMenuApp = null
            openFolderId != null -> openFolderId = null
            settingsOpen -> settingsOpen = false
            homeEditorOpen -> homeEditorOpen = false
            drawerOpen -> drawerOpen = false
        }
    }

    // Force Update Gate
    val releaseToInstall = pendingRelease
    if (releaseToInstall != null) {
        ForceUpdateScreen(release = releaseToInstall, palette = palette)
        return
    }

    val homeScale by animateFloatAsState(
        targetValue = when {
            homeEditorOpen -> 0.86f
            drawerOpen -> 0.93f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "homeScale"
    )

    val statusPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val rootBg = if (settings.surfaceMode == SurfaceMode.OLED_MATTE) palette.drawerBackground else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(rootBg)
    ) {
        val homeApps = remember(settings.homeAppKeys, appMap, settings.hiddenPackages) {
            settings.homeAppKeys.mapNotNull { appMap[it] }.filter { it.packageName !in settings.hiddenPackages }
        }
        val dockApps = remember(settings.dockAppKeys, appMap) {
            settings.dockAppKeys.mapNotNull { appMap[it] }.take(5)
        }

        val pageCapacity = (settings.gridColumns * (settings.gridRows - 1)).coerceAtLeast(8)
        val totalHomeEntries = settings.folders.size + homeApps.size
        val pageCount = ((totalHomeEntries + pageCapacity - 1) / pageCapacity).coerceAtLeast(1)
        val pagerState = rememberPagerState(pageCount = { pageCount })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = homeScale
                    scaleY = homeScale
                }
                .pointerInput(settings.doubleTapAction, settings.pinchInAction) {
                    detectTapGestures(
                        onDoubleTap = { runGesture(settings.doubleTapAction) },
                        onLongPress = { homeEditorOpen = true }
                    )
                }
                .pointerInput(settings.pinchInAction) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom < 0.86f && !homeEditorOpen) {
                            runGesture(settings.pinchInAction)
                        }
                    }
                }
                .pointerInput(settings.swipeUpAction, settings.swipeDownAction) {
                    var accumY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { accumY = 0f },
                        onDragEnd = {
                            if (accumY < -75f) runGesture(settings.swipeUpAction)
                            else if (accumY > 75f) runGesture(settings.swipeDownAction)
                            accumY = 0f
                        }
                    ) { _, dragAmount ->
                        accumY += dragAmount
                    }
                }
                .padding(top = statusPad + 6.dp, bottom = navPad + 10.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { pageIndex ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    if (pageIndex == 0 && settings.showClockWidget) {
                        OneUiClockWidget(
                            palette = palette,
                            onClick = { repo.openClockApp() }
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    val start = pageIndex * pageCapacity
                    val pageFolders = if (pageIndex == 0) settings.folders else emptyList()
                    val appStart = if (pageIndex == 0) 0 else (start - settings.folders.size).coerceAtLeast(0)
                    val appLimit = (pageCapacity - pageFolders.size).coerceAtLeast(4)
                    val pageApps = homeApps.drop(appStart).take(appLimit)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings.gridColumns),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(pageFolders, key = { "folder_${it.id}" }) { folder ->
                            val preview = folder.appKeys.mapNotNull { appMap[it] }
                            SquircleFolderTile(
                                name = folder.name,
                                previewApps = preview,
                                iconSize = settings.iconSizeDp.dp,
                                showLabel = settings.showLabels,
                                monochromeIcons = settings.monochromeIcons,
                                palette = palette,
                                onClick = { openFolderId = folder.id },
                                onLongClick = { homeEditorOpen = true }
                            )
                        }
                        items(pageApps, key = { "home_${it.key}" }) { app ->
                            SquircleAppTile(
                                app = app,
                                iconSize = settings.iconSizeDp.dp,
                                showLabel = settings.showLabels,
                                monochromeIcons = settings.monochromeIcons,
                                labelColor = palette.onSurface,
                                shadowColor = palette.homeTextShadow,
                                onClick = { launch(app) },
                                onLongClick = { contextMenuApp = app }
                            )
                        }
                    }
                }
            }

            if (pageCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pageCount) { idx ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (pagerState.currentPage == idx) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == idx) palette.onSurface
                                    else palette.onSurfaceMuted.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }

            if (settings.showHomeSearchPill) {
                FinderSearchPill(
                    palette = palette,
                    onClick = {
                        drawerFocusSearch = true
                        drawerOpen = true
                    },
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }

            if (settings.showDock && dockApps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = settings.dockPaddingDp.dp, vertical = 6.dp)
                ) {
                    if (settings.dockBlur) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(30.dp))
                                .then(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && palette.useGlassBlur) {
                                        Modifier.blur(14.dp)
                                    } else Modifier
                                )
                                .background(palette.dockSurface)
                                .border(0.9.dp, palette.dockBorder, RoundedCornerShape(30.dp))
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dockApps.forEach { app ->
                            SquircleAppTile(
                                app = app,
                                iconSize = (settings.iconSizeDp - 2).coerceAtLeast(44).dp,
                                showLabel = false,
                                monochromeIcons = settings.monochromeIcons,
                                labelColor = palette.onSurface,
                                shadowColor = palette.homeTextShadow,
                                onClick = { launch(app) },
                                onLongClick = { contextMenuApp = app }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium)
            ) + fadeOut()
        ) {
            AppDrawerOverlay(
                apps = allApps,
                frequentApps = frequentApps,
                settings = settings,
                palette = palette,
                autoFocusSearch = drawerFocusSearch,
                onLaunchApp = { launch(it) },
                onLongClickApp = { contextMenuApp = it },
                onOpenSettings = {
                    drawerOpen = false
                    settingsOpen = true
                },
                onClose = { drawerOpen = false }
            )
        }

        val currentFolder = settings.folders.firstOrNull { it.id == openFolderId }
        AnimatedVisibility(
            visible = currentFolder != null,
            enter = scaleIn(initialScale = 0.86f, animationSpec = spring(dampingRatio = 0.72f)) + fadeIn(),
            exit = scaleOut(targetScale = 0.88f) + fadeOut()
        ) {
            currentFolder?.let { folder ->
                FolderPopupOverlay(
                    folder = folder,
                    appMap = appMap,
                    allApps = allApps,
                    settings = settings,
                    palette = palette,
                    onLaunch = { launch(it) },
                    onUpdateFolder = { updated ->
                        saveSettings(settings.copy(folders = settings.folders.map { if (it.id == updated.id) updated else it }))
                    },
                    onDeleteFolder = {
                        saveSettings(settings.copy(folders = settings.folders.filterNot { it.id == folder.id }))
                        openFolderId = null
                    },
                    onDismiss = { openFolderId = null }
                )
            }
        }

        AnimatedVisibility(
            visible = homeEditorOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            HomeEditorBar(
                settings = settings,
                palette = palette,
                onWallpaper = {
                    homeEditorOpen = false
                    gestures.openWallpaperPicker()
                },
                onCycleSurface = {
                    val modes = SurfaceMode.entries
                    val next = modes[(modes.indexOf(settings.surfaceMode) + 1) % modes.size]
                    saveSettings(settings.copy(surfaceMode = next))
                    toastMsg = "Surface: ${next.label}"
                },
                onNewFolder = {
                    val id = UUID.randomUUID().toString().take(8)
                    val nextFolders = settings.folders + FolderItem(id = id, name = "Folder", appKeys = emptyList())
                    saveSettings(settings.copy(folders = nextFolders))
                    homeEditorOpen = false
                    openFolderId = id
                },
                onOpenSettings = {
                    homeEditorOpen = false
                    settingsOpen = true
                },
                onDismiss = { homeEditorOpen = false }
            )
        }

        contextMenuApp?.let { app ->
            AppActionPopup(
                app = app,
                isOnHome = app.key in settings.homeAppKeys,
                isInDock = app.key in settings.dockAppKeys,
                palette = palette,
                onToggleHome = {
                    val next = if (app.key in settings.homeAppKeys) settings.homeAppKeys - app.key
                    else settings.homeAppKeys + app.key
                    saveSettings(settings.copy(homeAppKeys = next))
                    contextMenuApp = null
                },
                onToggleDock = {
                    val next = if (app.key in settings.dockAppKeys) settings.dockAppKeys - app.key
                    else (settings.dockAppKeys + app.key).takeLast(5)
                    saveSettings(settings.copy(dockAppKeys = next))
                    contextMenuApp = null
                },
                onHide = {
                    saveSettings(settings.copy(hiddenPackages = settings.hiddenPackages + app.packageName))
                    contextMenuApp = null
                },
                onAppInfo = {
                    repo.openAppInfo(app.packageName)
                    contextMenuApp = null
                },
                onUninstall = {
                    repo.uninstallApp(app.packageName)
                    contextMenuApp = null
                },
                onDismiss = { contextMenuApp = null }
            )
        }

        if (lockPermissionPrompt) {
            LockPermissionDialog(
                palette = palette,
                onEnableAccessibility = {
                    lockPermissionPrompt = false
                    gestures.openAccessibilitySettings()
                },
                onEnableDeviceAdmin = {
                    lockPermissionPrompt = false
                    gestures.requestDeviceAdmin()
                },
                onDismiss = { lockPermissionPrompt = false }
            )
        }

        AnimatedVisibility(
            visible = settingsOpen,
            enter = slideInVertically(initialOffsetY = { it / 4 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut()
        ) {
            SettingsScreen(
                settings = settings,
                allApps = allApps,
                palette = palette,
                gestureController = gestures,
                onUpdate = { saveSettings(it) },
                onCheckUpdate = {
                    scope.launch {
                        val rel = ForceUpdater.checkForUpdate()
                        if (rel != null) pendingRelease = rel
                        else toastMsg = "Klyne is up to date (v${BuildConfig.VERSION_NAME})"
                    }
                },
                onClose = { settingsOpen = false }
            )
        }

        AnimatedVisibility(
            visible = toastMsg != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusPad + 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.accent)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(toastMsg ?: "", color = palette.onAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HomeEditorBar(
    settings: KlyneSettings,
    palette: KlynePalette,
    onWallpaper: () -> Unit,
    onCycleSurface: () -> Unit,
    onNewFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrimBackground.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
            .padding(bottom = navPad + 18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(palette.cardSurface)
                .border(0.8.dp, palette.dockBorder, RoundedCornerShape(28.dp))
                .clickable(enabled = false) {}
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EditorPill("Wallpaper", palette, onWallpaper)
            EditorPill(settings.surfaceMode.label.substringBefore(" "), palette, onCycleSurface)
            EditorPill("+ Folder", palette, onNewFolder)
            EditorPill("Settings", palette, onOpenSettings)
        }
    }
}

@Composable
private fun EditorPill(label: String, palette: KlynePalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.pillSurface)
            .border(0.6.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Text(label, color = palette.onSurface, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FolderPopupOverlay(
    folder: FolderItem,
    appMap: Map<String, AppEntry>,
    allApps: List<AppEntry>,
    settings: KlyneSettings,
    palette: KlynePalette,
    onLaunch: (AppEntry) -> Unit,
    onUpdateFolder: (FolderItem) -> Unit,
    onDeleteFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(folder.id) { mutableStateOf(folder.name) }
    var addingApps by remember { mutableStateOf(false) }
    val folderApps = folder.appKeys.mapNotNull { appMap[it] }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrimBackground)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(32.dp))
                .background(palette.cardSurface)
                .border(0.9.dp, palette.dockBorder, RoundedCornerShape(32.dp))
                .clickable(enabled = false) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdateFolder(folder.copy(name = it.ifBlank { "Folder" }))
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = palette.onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush = SolidColor(palette.onSurface),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.pillSurface)
                        .clickable { addingApps = !addingApps },
                    contentAlignment = Alignment.Center
                ) {
                    if (addingApps) CloseIcon(palette.onSurface, 14.dp) else PlusIcon(palette.onSurface, 16.dp)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (addingApps) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allApps, key = { it.key }) { app ->
                        val inFolder = app.key in folder.appKeys
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (inFolder) palette.accent.copy(alpha = 0.16f) else Color.Transparent)
                        ) {
                            SquircleAppTile(
                                app = app,
                                iconSize = 46.dp,
                                showLabel = true,
                                monochromeIcons = settings.monochromeIcons,
                                labelColor = palette.onSurface,
                                shadowColor = palette.homeTextShadow,
                                onClick = {
                                    val next = if (inFolder) folder.appKeys - app.key else folder.appKeys + app.key
                                    onUpdateFolder(folder.copy(appKeys = next))
                                },
                                onLongClick = {}
                            )
                        }
                    }
                }
            } else if (folderApps.isEmpty()) {
                Text(
                    text = "Tap + to add apps to this folder",
                    color = palette.onSurfaceMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 36.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(folderApps, key = { it.key }) { app ->
                        SquircleAppTile(
                            app = app,
                            iconSize = 50.dp,
                            showLabel = true,
                            monochromeIcons = settings.monochromeIcons,
                            labelColor = palette.onSurface,
                            shadowColor = palette.homeTextShadow,
                            onClick = { onLaunch(app) },
                            onLongClick = {
                                onUpdateFolder(folder.copy(appKeys = folder.appKeys - app.key))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Delete Folder",
                color = palette.onSurfaceMuted,
                fontSize = 12.5.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDeleteFolder)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun AppActionPopup(
    app: AppEntry,
    isOnHome: Boolean,
    isInDock: Boolean,
    palette: KlynePalette,
    onToggleHome: () -> Unit,
    onToggleDock: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrimBackground)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(26.dp))
                .background(palette.cardSurface)
                .border(0.8.dp, palette.dockBorder, RoundedCornerShape(26.dp))
                .clickable(enabled = false) {}
                .padding(18.dp)
        ) {
            Text(app.label, color = palette.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            PopupRow(if (isOnHome) "Remove from Home" else "Pin to Home", palette, onToggleHome)
            PopupRow(if (isInDock) "Remove from Dock" else "Add to Dock", palette, onToggleDock)
            PopupRow("Hide App", palette, onHide)
            PopupRow("App Info", palette, onAppInfo)
            if (!app.isSystem) {
                PopupRow("Uninstall", palette, onUninstall)
            }
        }
    }
}

@Composable
private fun PopupRow(label: String, palette: KlynePalette, onClick: () -> Unit) {
    Text(
        text = label,
        color = palette.onSurface,
        fontSize = 14.5.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp)
    )
}

@Composable
private fun LockPermissionDialog(
    palette: KlynePalette,
    onEnableAccessibility: () -> Unit,
    onEnableDeviceAdmin: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrimBackground)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(26.dp))
                .background(palette.cardSurface)
                .border(0.8.dp, palette.dockBorder, RoundedCornerShape(26.dp))
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Text("Enable Double-Tap Lock", color = palette.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Turn on Klyne Gestures in Accessibility for fingerprint-friendly screen locking (Android 9+), or Device Admin on Android 8.0–8.1.",
                color = palette.onSurfaceMuted,
                fontSize = 13.5.sp
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.accent)
                    .clickable(onClick = onEnableAccessibility)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Open Accessibility Settings", color = palette.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.pillSurface)
                    .clickable(onClick = onEnableDeviceAdmin)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Enable Oreo Device Admin", color = palette.onSurface, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
