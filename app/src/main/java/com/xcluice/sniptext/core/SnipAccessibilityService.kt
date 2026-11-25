package com.xcluice.sniptext.core

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Display
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat

class SnipAccessibilityService : AccessibilityService() {

    private var overlayManager: SelectionOverlayManager? = null
    private var store: SettingsStore? = null
    private var shakeDetector: ShakeDetector? = null
    private var screenReceiver: BroadcastReceiver? = null

    private var volUpPressed = false
    private var volDownPressed = false
    private var volUpDownAt = 0L
    private var volDownDownAt = 0L
    private var comboActive = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        store = SettingsStore(this)
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayManager = SelectionOverlayManager(this, wm)

        shakeDetector = ShakeDetector(this) {
            onGestureTriggered()
        }

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> syncShakeListener()
                    Intent.ACTION_SCREEN_OFF -> shakeDetector?.stop()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }

        reloadSettings()
    }

    fun reloadSettings() {
        val settings = store?.loadSettings() ?: return
        shakeDetector?.sensitivity = settings.shakeSensitivity
        syncShakeListener()
    }

    private fun syncShakeListener() {
        val settings = store?.loadSettings() ?: return
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val screenOn = pm?.isInteractive ?: true
        if (settings.shakeEnabled && screenOn) {
            shakeDetector?.start()
        } else {
            shakeDetector?.stop()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        overlayManager?.dismiss()
    }

    override fun onDestroy() {
        shakeDetector?.stop()
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        screenReceiver = null
        overlayManager?.dismiss()
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val settings = store?.loadSettings()
        if (settings?.volumeComboEnabled == false) {
            return super.onKeyEvent(event)
        }

        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyEvent(event)
        }

        val chordWindow = (settings?.chordWindowMs ?: 220).toLong()
        val now = SystemClock.uptimeMillis()

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) {
                    return comboActive
                }
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    volUpPressed = true
                    volUpDownAt = now
                    if (volDownPressed && (now - volDownDownAt) <= chordWindow) {
                        comboActive = true
                        onGestureTriggered()
                        return true
                    }
                } else {
                    volDownPressed = true
                    volDownDownAt = now
                    if (volUpPressed && (now - volUpDownAt) <= chordWindow) {
                        comboActive = true
                        onGestureTriggered()
                        return true
                    }
                }
            }

            KeyEvent.ACTION_UP -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    volUpPressed = false
                } else {
                    volDownPressed = false
                }
                if (comboActive) {
                    if (!volUpPressed && !volDownPressed) {
                        comboActive = false
                    }
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun onGestureTriggered() {
        val haptic = store?.loadSettings()?.hapticEnabled ?: true
        if (haptic) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(28L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        if (overlayManager?.isShowing == true) {
            overlayManager?.dismiss()
            return
        }
        triggerCapture()
    }

    fun triggerCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        try {
                            val hwBitmap = Bitmap.wrapHardwareBuffer(
                                screenshot.hardwareBuffer,
                                screenshot.colorSpace
                            )
                            val softBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                            hwBitmap?.recycle()
                            screenshot.hardwareBuffer.close()
                            if (softBitmap != null) {
                                showSelectionOverlay(softBitmap)
                            } else {
                                Toast.makeText(
                                    this@SnipAccessibilityService,
                                    "Could not read screen frame",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@SnipAccessibilityService,
                                "Capture failed: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Toast.makeText(
                            this@SnipAccessibilityService,
                            "Screenshot blocked on this screen (code $errorCode)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        } else if (SnipProjectionService.isRunning) {
            val intent = Intent(this, SnipProjectionService::class.java).apply {
                action = SnipProjectionService.ACTION_CAPTURE_AGAIN
            }
            ContextCompat.startForegroundService(this, intent)
        } else {
            val permIntent = Intent(this, ProjectionPermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(permIntent)
        }
    }

    fun showSelectionOverlay(bitmap: Bitmap) {
        overlayManager?.show(bitmap)
    }

    companion object {
        @Volatile
        var instance: SnipAccessibilityService? = null
    }
}
