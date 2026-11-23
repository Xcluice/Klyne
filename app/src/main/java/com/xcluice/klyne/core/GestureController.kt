package com.xcluice.klyne.core

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class GestureController(private val context: Context) {

    private val dpm: DevicePolicyManager
        get() = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent: ComponentName
        get() = ComponentName(context, KlyneDeviceAdminReceiver::class.java)

    fun isDeviceAdminEnabled(): Boolean = runCatching { dpm.isAdminActive(adminComponent) }.getOrDefault(false)

    fun isAccessibilityEnabled(): Boolean = KlyneAccessibilityService.isEnabled()

    fun lockScreen(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && KlyneAccessibilityService.lockScreen()) {
            return true
        }
        if (isDeviceAdminEnabled()) {
            return runCatching {
                dpm.lockNow()
                true
            }.getOrDefault(false)
        }
        return false
    }

    @SuppressLint("WrongConstant")
    fun expandNotifications(): Boolean {
        if (KlyneAccessibilityService.expandNotifications()) return true
        return runCatching {
            val sb = context.getSystemService("statusbar") ?: return false
            val clazz = Class.forName("android.app.StatusBarManager")
            val method = clazz.getMethod("expandNotificationsPanel")
            method.invoke(sb)
            true
        }.getOrDefault(false)
    }

    @SuppressLint("WrongConstant")
    fun expandQuickSettings(): Boolean {
        if (KlyneAccessibilityService.expandQuickSettings()) return true
        return runCatching {
            val sb = context.getSystemService("statusbar") ?: return false
            val clazz = Class.forName("android.app.StatusBarManager")
            val method = clazz.getMethod("expandSettingsPanel")
            method.invoke(sb)
            true
        }.getOrDefault(false)
    }

    fun openRecents(): Boolean = KlyneAccessibilityService.openRecents()

    fun openAccessibilitySettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun requestDeviceAdmin() {
        runCatching {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Allows Klyne to lock the screen on double-tap on Android 8.0–8.1 devices."
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openWallpaperPicker() {
        runCatching {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Choose Wallpaper").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun openDefaultLauncherSettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
