package com.example.sheshield.service

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/** Triggers after 3 strong shakes within 2 seconds. */
class ShakeDetector(
    private val getSensitivity: () -> Float,
    private val onShake: () -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "SheShieldShake"
    }

    private var count = 0
    private var windowStart = 0L
    private var lastShake = 0L

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate g-force magnitude
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

        // Calibrated threshold: 1.8g (least sensitive, sensitivity = 0.0) to 1.2g (most sensitive, sensitivity = 1.0)
        // Default sensitivity = 0.5 -> threshold = 1.5g
        val sensitivity = getSensitivity()
        val threshold = 1.8f - (sensitivity * 0.6f)

        if (g > threshold) {
            val now = System.currentTimeMillis()

            // Debounce: allow rapid back-and-forth shakes spaced by at least 150ms
            if (now - lastShake < 150) {
                return
            }

            Log.d(TAG, "Shake spike detected! g=%.2f, threshold=%.2f, count=%d".format(g, threshold, count + 1))
            lastShake = now

            // Reset window if more than 2 seconds have passed since the first shake in the sequence
            if (now - windowStart > 2000) {
                count = 0
                windowStart = now
            }

            count++
            if (count >= 3) {
                Log.i(TAG, "SUCCESS: 3 shakes detected within window. Triggering SOS!")
                count = 0
                windowStart = 0
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
