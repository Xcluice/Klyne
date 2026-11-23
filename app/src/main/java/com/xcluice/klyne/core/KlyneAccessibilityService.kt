package com.xcluice.klyne.core

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class KlyneAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: KlyneAccessibilityService? = null
            private set

        fun isEnabled(): Boolean = instance != null

        fun lockScreen(): Boolean {
            val svc = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                svc.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
        }

        fun expandNotifications(): Boolean {
            val svc = instance ?: return false
            return svc.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }

        fun expandQuickSettings(): Boolean {
            val svc = instance ?: return false
            return svc.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        }

        fun openRecents(): Boolean {
            val svc = instance ?: return false
            return svc.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }
    }
}
