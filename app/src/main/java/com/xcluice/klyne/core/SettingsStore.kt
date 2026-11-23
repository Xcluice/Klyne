package com.xcluice.klyne.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("klyne_prefs_v1", Context.MODE_PRIVATE)

    fun load(): KlyneSettings {
        val folders = parseFolders(prefs.getString("folders", "[]") ?: "[]")
        return KlyneSettings(
            surfaceMode = runCatching {
                SurfaceMode.valueOf(prefs.getString("surfaceMode", SurfaceMode.FROSTED_GLASS.name)!!)
            }.getOrDefault(SurfaceMode.FROSTED_GLASS),
            darkTheme = prefs.getBoolean("darkTheme", true),
            gridColumns = prefs.getInt("gridColumns", 4).coerceIn(4, 6),
            gridRows = prefs.getInt("gridRows", 5).coerceIn(4, 7),
            iconSizeDp = prefs.getInt("iconSizeDp", 54).coerceIn(44, 66),
            showLabels = prefs.getBoolean("showLabels", true),
            showDock = prefs.getBoolean("showDock", true),
            dockBlur = prefs.getBoolean("dockBlur", true),
            dockPaddingDp = prefs.getInt("dockPaddingDp", 14).coerceIn(6, 24),
            showHomeSearchPill = prefs.getBoolean("showHomeSearchPill", true),
            showClockWidget = prefs.getBoolean("showClockWidget", true),
            monochromeIcons = prefs.getBoolean("monochromeIcons", false),
            drawerStyle = runCatching {
                DrawerStyle.valueOf(prefs.getString("drawerStyle", DrawerStyle.VERTICAL.name)!!)
            }.getOrDefault(DrawerStyle.VERTICAL),
            doubleTapAction = loadGesture("doubleTapAction", GestureAction.LOCK_SCREEN),
            swipeDownAction = loadGesture("swipeDownAction", GestureAction.NOTIFICATIONS),
            swipeUpAction = loadGesture("swipeUpAction", GestureAction.OPEN_DRAWER),
            pinchInAction = loadGesture("pinchInAction", GestureAction.HOME_EDITOR),
            twoFingerSwipeUpAction = loadGesture("twoFingerSwipeUpAction", GestureAction.OPEN_SEARCH),
            hiddenPackages = parseStringList(prefs.getString("hiddenPackages", "[]")).toSet(),
            dockAppKeys = parseStringList(prefs.getString("dockAppKeys", "[]")),
            homeAppKeys = parseStringList(prefs.getString("homeAppKeys", "[]")),
            folders = folders
        )
    }

    fun save(s: KlyneSettings) {
        prefs.edit()
            .putString("surfaceMode", s.surfaceMode.name)
            .putBoolean("darkTheme", s.darkTheme)
            .putInt("gridColumns", s.gridColumns)
            .putInt("gridRows", s.gridRows)
            .putInt("iconSizeDp", s.iconSizeDp)
            .putBoolean("showLabels", s.showLabels)
            .putBoolean("showDock", s.showDock)
            .putBoolean("dockBlur", s.dockBlur)
            .putInt("dockPaddingDp", s.dockPaddingDp)
            .putBoolean("showHomeSearchPill", s.showHomeSearchPill)
            .putBoolean("showClockWidget", s.showClockWidget)
            .putBoolean("monochromeIcons", s.monochromeIcons)
            .putString("drawerStyle", s.drawerStyle.name)
            .putString("doubleTapAction", s.doubleTapAction.name)
            .putString("swipeDownAction", s.swipeDownAction.name)
            .putString("swipeUpAction", s.swipeUpAction.name)
            .putString("pinchInAction", s.pinchInAction.name)
            .putString("twoFingerSwipeUpAction", s.twoFingerSwipeUpAction.name)
            .putString("hiddenPackages", encodeStringList(s.hiddenPackages.toList()))
            .putString("dockAppKeys", encodeStringList(s.dockAppKeys))
            .putString("homeAppKeys", encodeStringList(s.homeAppKeys))
            .putString("folders", encodeFolders(s.folders))
            .apply()
    }

    fun isInitialized(): Boolean = prefs.getBoolean("initialized", false)
    fun markInitialized() = prefs.edit().putBoolean("initialized", true).apply()

    fun recordLaunch(appKey: String) {
        val current = prefs.getInt("cnt_$appKey", 0)
        prefs.edit()
            .putInt("cnt_$appKey", current + 1)
            .putLong("ts_$appKey", System.currentTimeMillis())
            .apply()
    }

    fun getLaunchScore(appKey: String): Long {
        val count = prefs.getInt("cnt_$appKey", 0).toLong()
        val last = prefs.getLong("ts_$appKey", 0L)
        return count * 10_000L + (last / 100_000L)
    }

    private fun loadGesture(key: String, fallback: GestureAction): GestureAction =
        runCatching { GestureAction.valueOf(prefs.getString(key, fallback.name)!!) }.getOrDefault(fallback)

    private fun parseStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    private fun encodeStringList(items: List<String>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun parseFolders(raw: String): List<FolderItem> = runCatching {
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            val keysArr = obj.optJSONArray("apps") ?: JSONArray()
            FolderItem(
                id = obj.getString("id"),
                name = obj.optString("name", "Folder"),
                appKeys = List(keysArr.length()) { keysArr.getString(it) }
            )
        }
    }.getOrDefault(emptyList())

    private fun encodeFolders(folders: List<FolderItem>): String {
        val arr = JSONArray()
        folders.forEach { folder ->
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            val apps = JSONArray()
            folder.appKeys.forEach { apps.put(it) }
            obj.put("apps", apps)
            arr.put(obj)
        }
        return arr.toString()
    }
}
