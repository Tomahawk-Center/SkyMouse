package com.skymouse.skymouseclient.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class GyroscopeProvider(
    context: Context,
    private val onMove: (Float, Float) -> Unit
): SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var isRunning = false

    private var sensitivity = 40f

    private val deadZoneThreshold = 0.02f

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

            val dx = if (abs(axisZ) > deadZoneThreshold) -axisZ * sensitivity else 0f
            val dy = if (abs(axisX) > deadZoneThreshold) -axisX * sensitivity else 0f

            if (dx != 0f || dy != 0f) {
                onMove(dx, dy)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}