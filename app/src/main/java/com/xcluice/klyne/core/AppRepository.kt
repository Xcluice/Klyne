package com.xcluice.klyne.core

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val iconCache = LruCache<String, ImageBitmap>(256)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val _wallpaperSeed = MutableStateFlow(Color(0xFF7B8392))
    val wallpaperSeed: StateFlow<Color> = _wallpaperSeed

    private var callbackRegistered = false
    private var onChangeListener: (() -> Unit)? = null

    private val launcherCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) { onChangeListener?.invoke() }
        override fun onPackageAdded(packageName: String?, user: UserHandle?) { onChangeListener?.invoke() }
        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            if (packageName != null) {
                iconCache.snapshot().keys.filter { it.startsWith("$packageName/") }.forEach { iconCache.remove(it) }
            }
            onChangeListener?.invoke()
        }
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            onChangeListener?.invoke()
        }
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            onChangeListener?.invoke()
        }
    }

    fun registerCallback(onChanged: () -> Unit) {
        onChangeListener = onChanged
        if (!callbackRegistered) {
            runCatching {
                launcherApps.registerCallback(launcherCallback)
                callbackRegistered = true
            }
        }
    }

    fun unregisterCallback() {
        if (callbackRegistered) {
            runCatching { launcherApps.unregisterCallback(launcherCallback) }
            callbackRegistered = false
        }
    }

    suspend fun reloadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val selfPkg = context.packageName
        val infos = runCatching {
            launcherApps.getActivityList(null, Process.myUserHandle())
        }.getOrDefault(emptyList())

        val loaded = infos
            .filter { it.applicationInfo.packageName != selfPkg }
            .map { info ->
                val pkg = info.applicationInfo.packageName
                val act = info.componentName.className
                val key = "$pkg/$act"
                val icon = iconCache.get(key) ?: run {
                    val drawable = runCatching { info.getBadgedIcon(0) }.getOrNull()
                    val bmp = drawable?.let { drawableToImageBitmap(it, 128) }
                    if (bmp != null) iconCache.put(key, bmp)
                    bmp
                }
                val isSys = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                AppEntry(
                    packageName = pkg,
                    activityName = act,
                    label = info.label?.toString()?.trim().orEmpty().ifEmpty { pkg },
                    icon = icon,
                    isSystem = isSys
                )
            }
            .sortedBy { it.label.lowercase() }

        _apps.value = loaded
        extractWallpaperAccent()
        loaded
    }

    private fun extractWallpaperAccent() {
        runCatching {
            val wm = WallpaperManager.getInstance(context)
            val drawable = wm.peekDrawable() ?: return
            val bmp = drawableToBitmap(drawable, 72)
            val palette = Palette.from(bmp).generate()
            val muted = palette.mutedSwatch?.rgb
                ?: palette.darkMutedSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
            if (muted != null) {
                _wallpaperSeed.value = Color(muted)
            }
        }
    }

    fun launchApp(app: AppEntry): Boolean {
        return runCatching {
            val component = ComponentName(app.packageName, app.activityName)
            launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
            true
        }.getOrElse {
            runCatching {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    true
                } else false
            }.getOrDefault(false)
        }
    }

    fun openAppInfo(packageName: String) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun uninstallApp(packageName: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openClockApp() {
        val candidates = listOf(
            "com.sec.android.app.clockpackage",
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.coloros.alarmclock"
        )
        for (pkg in candidates) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }
    }

    fun pickDefaultDock(allApps: List<AppEntry>): List<String> {
        val hints = listOf(
            listOf("dialer", "phone", "contacts"),
            listOf("messaging", "mms", "whatsapp", "telegram", "messages"),
            listOf("chrome", "browser", "firefox", "brave", "sbrowser"),
            listOf("camera"),
            listOf("settings", "gallery", "photos")
        )
        val picked = mutableListOf<String>()
        for (group in hints) {
            val match = allApps.firstOrNull { app ->
                app.key !in picked && group.any { h ->
                    app.packageName.lowercase().contains(h) || app.label.lowercase().contains(h)
                }
            }
            if (match != null) picked.add(match.key)
        }
        if (picked.size < 4) {
            allApps.forEach { app ->
                if (picked.size < 4 && app.key !in picked) picked.add(app.key)
            }
        }
        return picked.take(5)
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width <= sizePx && src.height <= sizePx) return src
            return Bitmap.createScaledBitmap(src, sizePx, sizePx, true)
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }

    private fun drawableToImageBitmap(drawable: Drawable, sizePx: Int): ImageBitmap {
        return drawableToBitmap(drawable, sizePx).asImageBitmap()
    }
}
