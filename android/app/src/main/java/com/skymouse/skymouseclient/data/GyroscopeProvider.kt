package com.skymouse.skymouseclient.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

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

    private val _isGyroActive = MutableStateFlow(false)
    val isGyroActive: StateFlow<Boolean> = _isGyroActive.asStateFlow()

    private val providerScope = CoroutineScope(Dispatchers.Default)
    private var timeoutJob: Job? = null
    private val activeTimeoutMs = 20_000L

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

        timeoutJob?.cancel()
        _isGyroActive.value = false
    }

    fun updateSettings(newSensitivity: Float, newAcceleration: Float) {
        this.sensitivity = newSensitivity
        this.acceleration = newAcceleration
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
                if (abs(currDx) > 0.01f || abs(currDy) > 0.01f) {
                    notifyMotion()
                }

                onMove(currDx, currDy)
            } else {
                currDx = 0f
                currDy = 0f
            }
        }
    }

    private fun notifyMotion() {
        if (!_isGyroActive.value) {
            _isGyroActive.value = true
        }
        resetTimeoutTimer()
    }

    private fun resetTimeoutTimer() {
        timeoutJob?.cancel()
        timeoutJob = providerScope.launch {
            delay(activeTimeoutMs.milliseconds)
            _isGyroActive.value = false
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