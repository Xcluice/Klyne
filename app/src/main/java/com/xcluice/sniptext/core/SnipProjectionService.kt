package com.xcluice.sniptext.core

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.xcluice.sniptext.MainActivity
import com.xcluice.sniptext.R

class SnipProjectionService : Service() {
    private var mediaProjection: MediaProjection? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopProjection()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_AND_CAPTURE -> {
                ensureForeground()
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                @Suppress("DEPRECATION")
                val data: Intent? = intent.getParcelableExtra(EXTRA_DATA)
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    mediaProjection?.stop()
                    mediaProjection = mgr.getMediaProjection(resultCode, data)
                    handler.postDelayed({ captureSingleFrame() }, 220L)
                }
            }
            ACTION_CAPTURE_AGAIN -> {
                if (mediaProjection != null) {
                    handler.postDelayed({ captureSingleFrame() }, 80L)
                } else {
                    val permIntent = Intent(this, ProjectionPermissionActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(permIntent)
                }
            }
        }
        return START_STICKY
    }

    private fun ensureForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SnipText Screen Capture",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)

        val openPending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopPending = PendingIntent.getService(
            this,
            1,
            Intent(this, SnipProjectionService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("SnipText ready")
            .setContentText("Press Vol Up + Vol Down anytime to grab text")
            .setSmallIcon(R.drawable.ic_tile_snip)
            .setContentIntent(openPending)
            .addAction(Notification.Action.Builder(null, "Stop", stopPending).build())
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun captureSingleFrame() {
        val projection = mediaProjection ?: return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var virtualDisplay: VirtualDisplay? = null
        var captured = false

        reader.setOnImageAvailableListener({ r ->
            if (captured) return@setOnImageAvailableListener
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            captured = true
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val rawBitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                rawBitmap.copyPixelsFromBuffer(buffer)
                val finalBitmap = if (rawBitmap.width != width) {
                    Bitmap.createBitmap(rawBitmap, 0, 0, width, height).also {
                        rawBitmap.recycle()
                    }
                } else {
                    rawBitmap
                }
                handler.post {
                    SnipAccessibilityService.instance?.showSelectionOverlay(finalBitmap)
                }
            } catch (_: Exception) {
            } finally {
                image.close()
                virtualDisplay?.release()
                reader.close()
            }
        }, handler)

        virtualDisplay = projection.createVirtualDisplay(
            "SnipTextCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )
    }

    private fun stopProjection() {
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        isRunning = false
        stopProjection()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_AND_CAPTURE = "com.xcluice.sniptext.START_CAPTURE"
        const val ACTION_CAPTURE_AGAIN = "com.xcluice.sniptext.CAPTURE_AGAIN"
        const val ACTION_STOP = "com.xcluice.sniptext.STOP_PROJECTION"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        private const val CHANNEL_ID = "sniptext_projection"
        private const val NOTIF_ID = 4102

        @Volatile
        var isRunning: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }
}
