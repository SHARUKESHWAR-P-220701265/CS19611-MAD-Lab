package com.example.gotg

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastShakeTime: Long = 0
    
    companion object {
        private const val SHAKE_THRESHOLD = 800
        private const val UPDATE_INTERVAL = 100
        private const val COOLDOWN_DURATION = 10000 // 10 seconds cooldown
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        
        if ((currentTime - lastUpdate) > UPDATE_INTERVAL) {
            val diffTime = currentTime - lastUpdate
            lastUpdate = currentTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val speed = sqrt(
                ((x - lastX) * (x - lastX) +
                        (y - lastY) * (y - lastY) +
                        (z - lastZ) * (z - lastZ)) / diffTime * 10000
            )

            if (speed > SHAKE_THRESHOLD && (currentTime - lastShakeTime) > COOLDOWN_DURATION) {
                lastShakeTime = currentTime
                onShake()
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
} 