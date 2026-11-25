package com.xcluice.sniptext.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onDoubleShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var registered = false
    private val gravity = FloatArray(3)
    private var gravityInitialized = false

    private var firstShakeAt = 0L
    private var lastPulseAt = 0L
    private var lastTriggerAt = 0L

    @Volatile
    var sensitivity: Int = 6

    fun start() {
        if (registered || accelerometer == null) return
        gravityInitialized = false
        firstShakeAt = 0L
        lastPulseAt = 0L
        registered = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stop() {
        if (!registered) return
        sensorManager.unregisterListener(this)
        registered = false
        firstShakeAt = 0L
        lastPulseAt = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (!gravityInitialized) {
            gravity[0] = x
            gravity[1] = y
            gravity[2] = z
            gravityInitialized = true
            return
        }

        val alpha = 0.8f
        gravity[0] = alpha * gravity[0] + (1f - alpha) * x
        gravity[1] = alpha * gravity[1] + (1f - alpha) * y
        gravity[2] = alpha * gravity[2] + (1f - alpha) * z

        val lx = x - gravity[0]
        val ly = y - gravity[1]
        val lz = z - gravity[2]
        val linearAccel = sqrt(lx * lx + ly * ly + lz * lz)

        val clamped = sensitivity.coerceIn(1, 10)
        val threshold = 19.0f - (clamped - 1) * 1.28f

        if (linearAccel < threshold) return

        val now = SystemClock.uptimeMillis()
        if (now - lastTriggerAt < COOLDOWN_MS) return
        if (now - lastPulseAt < PULSE_DEBOUNCE_MS) return

        lastPulseAt = now

        if (firstShakeAt == 0L || (now - firstShakeAt) > DOUBLE_SHAKE_WINDOW_MS) {
            firstShakeAt = now
        } else {
            firstShakeAt = 0L
            lastTriggerAt = now
            onDoubleShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val PULSE_DEBOUNCE_MS = 170L
        private const val DOUBLE_SHAKE_WINDOW_MS = 780L
        private const val COOLDOWN_MS = 1500L
    }
}
