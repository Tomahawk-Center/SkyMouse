package com.skymouse.skymouseclient.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

class GyroscopeProvider(
    context: Context,
    private val onMove: (Float, Float) -> Unit
): SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var isRunning = false

    private var sensitivity = 4f
    private var acceleration = 2f
    private var smoothing = 0.12f // 0.15f // less val - more smooth

    private var currDx = 0f
    private var currDy = 0f

    private val deadZoneThresholdLow = 0.01f
    private val deadZoneThresholdHigh = 0.03f

    fun start() {
        if (isRunning) return
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            isRunning = true
        }
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        isRunning = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isRunning) return

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val axisX = event.values[0]
            val axisZ = event.values[2]

            val rawX = -axisZ * sensitivity * getDeadZoneFactor(axisZ)
            val rawY = -axisX * sensitivity * getDeadZoneFactor(axisX)

            val magnitude = sqrt(rawX * rawX + rawY * rawY)
            val accMul = 1f + (magnitude * acceleration)

            val targetDx = rawX * sensitivity * accMul
            val targetDy = rawY * sensitivity * accMul

            currDx += (targetDx - currDx) * smoothing
            currDy += (targetDy - currDy) * smoothing

            if (currDx != 0f || currDy != 0f) {
                onMove(currDx, currDy)
            } else {
                currDx = 0f
                currDy = 0f
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun getDeadZoneFactor(value: Float): Float {
        val absVal = abs(value)
        return when {
            absVal < deadZoneThresholdLow -> 0f
            absVal >= deadZoneThresholdHigh -> 1f
            else -> (absVal - deadZoneThresholdLow) / (deadZoneThresholdHigh - deadZoneThresholdLow)
        }
    }
}