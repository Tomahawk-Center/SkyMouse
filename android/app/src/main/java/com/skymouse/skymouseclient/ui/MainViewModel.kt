package com.skymouse.skymouseclient.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.skymouse.skymouseclient.data.GyroscopeProvider
import com.skymouse.skymouseclient.data.SettingsState
import com.skymouse.skymouseclient.data.TcpClientManager
import com.skymouse.skymouseclient.data.TcpConnectionState
import com.skymouse.skymouseclient.data.UdpClientManager
import com.skymouse.skymouseclient.data.UdpConnectionState
import com.skymouse.skymouseclient.data.util.VersionVerificationResult
import com.skymouse.skymouseclient.data.util.VersionVerifier
import com.skymouse.skymouseclient.proto.HapticEventType
import com.skymouse.skymouseclient.proto.MouseButton
import com.skymouse.skymouseclient.proto.ServerEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver {

    private var shouldAutoReconnect = false

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (isGyroEnabled) {
            gyroscopeProvider.start()
        }

        if (shouldAutoReconnect && tcpConnectionState.value !is TcpConnectionState.Connected) {
            onConnectClicked()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        gyroscopeProvider.stop()

        if (tcpConnectionState.value is TcpConnectionState.Connected) {
            if (settingsState.value.autoReconnect) {
                shouldAutoReconnect = true
            }

            disconnect()
        }
    }

    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState

    fun onSensitivityChange(newValue: Float) {
        _settingsState.update { currentState ->
            currentState.copy(gyroSensitivity = newValue)
        }
    }

    fun onAccelerationChange(newValue: Float) {
        _settingsState.update { currentState ->
            currentState.copy(gyroAcceleration = newValue)
        }
    }

    fun onTouchpadSensitivityChange(newValue: Float) {
        _settingsState.update { it.copy(touchpadSensitivity = newValue) }
    }

    fun onTouchpadAccelerationChange(newValue: Float) {
        _settingsState.update { it.copy(touchpadAcceleration = newValue) }
    }

    fun onLongPressVibrationLevelChange(newValue: Int) {
        _settingsState.update { it.copy(longPressVibrationLevel = newValue) }
    }

    fun onScrollMultiplierChange(newValue: Int) {
        _settingsState.update { it.copy(scrollMultiplier = newValue) }
    }

    fun onAutoReconnectChange(newValue: Boolean) {
        _settingsState.update { it.copy(autoReconnect = newValue) }
    }

    var isGyroEnabled by mutableStateOf(prefs.getBoolean("gyro_enabled", false))
        private set

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val pressedButtons = mutableSetOf<MouseButton>()
    private var holdVibrationJob: Job? = null

    private fun startReceivingServerEvents() {
        viewModelScope.launch {
            while (udpClientManager.connectionState.value is UdpConnectionState.Connected) {
                val msg = udpClientManager.receiveProto() ?: break
                if (msg.hasServerEvent()) {
                    triggerHaptic(msg.serverEvent)
                }
            }
        }
    }

    private fun triggerHaptic(event: ServerEvent) {
        val effect = when (event.type) {
            HapticEventType.EVENT_BORDER_CROSSING -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                        .compose()
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                }
            }

            HapticEventType.EVENT_EDGE_HIT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                        .compose()
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                }
            }
            else -> null
        }
        effect?.let { vibrator.vibrate(it) }
    }

    private fun vibrateLocal(effectId: Int, primitiveId: Int, intensity: Float = 0.6f) {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(primitiveId)) {
            VibrationEffect.startComposition()
                .addPrimitive(primitiveId, intensity)
                .compose()
        } else {
            VibrationEffect.createPredefined(effectId)
        }
        vibrator.vibrate(effect)
    }

    private fun startHoldVibration() {
        val timings = longArrayOf(0, 100)
        val amplitudes = intArrayOf(0, settingsState.value.longPressVibrationLevel)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, 1)
        vibrator.vibrate(effect)
    }


    fun toggleControlMode() {
        isGyroEnabled = !isGyroEnabled
        prefs.edit {
            putBoolean("gyro_enabled", isGyroEnabled)
        }

        if (isGyroEnabled) {
            gyroscopeProvider.start()
        } else {
            gyroscopeProvider.stop()
        }
    }

    override fun onCleared() {
        gyroscopeProvider.stop()
    }

    private val udpClientManager = UdpClientManager()

    val udpConnectionState = udpClientManager.connectionState

    var ipAddress by mutableStateOf(prefs.getString("ip_address", "") ?: "")
    var port by mutableStateOf(prefs.getString("port", "10000") ?: "10000")


    fun saveSettings() {
        val state = _settingsState.value
        prefs.edit {
            putFloat("gyro_sensitivity", state.gyroSensitivity)
            putFloat("gyro_acceleration", state.gyroAcceleration)
            putFloat("touchpad_sensitivity", state.touchpadSensitivity)
            putFloat("touchpad_acceleration", state.touchpadAcceleration)
            putInt("long_press_vibration", state.longPressVibrationLevel)
            putInt("scroll_multiplier", state.scrollMultiplier)
            putBoolean("auto_reconnect", state.autoReconnect)
        }
    }

    private fun loadSettings() {
        val loadedState = SettingsState(
            gyroSensitivity = prefs.getFloat("gyro_sensitivity", 4.0f),
            gyroAcceleration = prefs.getFloat("gyro_acceleration", 2.0f),
            touchpadSensitivity = prefs.getFloat("touchpad_sensitivity", 1.0f),
            touchpadAcceleration = prefs.getFloat("touchpad_acceleration", 0.05f),
            longPressVibrationLevel = prefs.getInt("long_press_vibration", 1),
            scrollMultiplier = prefs.getInt("scroll_multiplier", 1),
            autoReconnect = prefs.getBoolean("auto_reconnect", true)
        )
        _settingsState.value = loadedState
    }

    fun onConnectClicked() {
        val portInt = port.toIntOrNull() ?: return
        val clientVersionStr = "3.0"

        prefs.edit {
            putString("ip_address", ipAddress)
            putString("port", port)
        }

        viewModelScope.launch {
            tcpClientManager.connect(ipAddress, portInt)

            if (tcpClientManager.connectionState.value is TcpConnectionState.Connected) {
                val helloMsg = com.skymouse.skymouseclient.proto.messageToServer {
                    clientHello = com.skymouse.skymouseclient.proto.clientHello {
                        clientVersion = clientVersionStr
                    }
                }

                tcpClientManager.sendProto(helloMsg)

                val response = tcpClientManager.receiveProto()
                if (response != null && response.hasServerHello()) {
                    val serverVersion = response.serverHello.serverVersion


                    val versionVerificationResult = VersionVerifier.verify(clientVersionStr, serverVersion)

                    if (versionVerificationResult is VersionVerificationResult.Mismatch) {
                        Toast.makeText(
                            getApplication(),
                            versionVerificationResult.reason,
                            Toast.LENGTH_LONG
                        ).show()

                        tcpClientManager.disconnect()
                        return@launch
                    } else if (versionVerificationResult is VersionVerificationResult.Warning) {
                        Toast.makeText(
                            getApplication(),
                            versionVerificationResult.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    val udpPortFromServer = response.serverHello.udpPort
                    val udpToken = response.serverHello.udpToken
                    if (udpToken == 0) {
                        Toast.makeText(
                            getApplication(),
                            "Server sent an invalid UDP token",
                            Toast.LENGTH_LONG
                        ).show()

                        tcpClientManager.disconnect()
                        return@launch
                    }
                    udpClientManager.connect(ipAddress, udpPortFromServer, udpToken)
                    startReceivingServerEvents()
                } else {
                    tcpClientManager.disconnect()
                }
            }
        }
    }

    /**
     * Disconnect both TCP and UDP connections
     * Should be called when you need disconnect without reconnection
     */
    fun onDisconnectClicked() {
        shouldAutoReconnect = false
        disconnect()
    }

    /**
     * Disconnect both TCP and UDP connections
     * Should be called when you need disconnect without changing reconnect state
     */
    fun disconnect() {
        viewModelScope.launch {
            udpClientManager.disconnect()
            tcpClientManager.disconnect()
        }
    }

    private val tcpClientManager = TcpClientManager()

    val tcpConnectionState = tcpClientManager.connectionState

    fun onMouseButtonClicked(button: MouseButton, isPressed: Boolean) {
        if (isPressed) {
            val isFirstButton = pressedButtons.isEmpty()
            pressedButtons.add(button)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                vibrateLocal(VibrationEffect.EFFECT_CLICK, VibrationEffect.Composition.DELAY_TYPE_RELATIVE_START_OFFSET, 0.7f)
            } else {
                vibrateLocal(VibrationEffect.EFFECT_CLICK, 0, 0.8f)
            }

            if (isFirstButton) {
                holdVibrationJob?.cancel()
                holdVibrationJob = viewModelScope.launch {
                    delay(150.milliseconds)
                    startHoldVibration()
                }

            }
        } else {
            pressedButtons.remove(button)
            if (pressedButtons.isEmpty()) {
                holdVibrationJob?.cancel()
                vibrator.cancel()
            }
        }

        viewModelScope.launch {
            val message = com.skymouse.skymouseclient.proto.messageToServer {
                emulatorEvent = com.skymouse.skymouseclient.proto.emulatorEvent {
                    click = com.skymouse.skymouseclient.proto.clickEvent {
                        this.button = button
                        this.state = if (isPressed) {
                            com.skymouse.skymouseclient.proto.ButtonState.STATE_DOWN // STATE_PRESSED
                        } else {
                            com.skymouse.skymouseclient.proto.ButtonState.STATE_UP   // STATE_RELEASED
                        }
                        this.timestampMs = System.currentTimeMillis()
                    }
                }
            }
            tcpClientManager.sendProto(message)
        }
    }

    fun onScrollUpClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrateLocal(VibrationEffect.EFFECT_TICK, VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1f)
        } else {
            vibrateLocal(VibrationEffect.EFFECT_TICK, 0, 1f)
        }
        viewModelScope.launch {
            val message = com.skymouse.skymouseclient.proto.messageToServer {
                emulatorEvent = com.skymouse.skymouseclient.proto.emulatorEvent {
                    scroll = com.skymouse.skymouseclient.proto.scrollEvent {
                        deltaY = settingsState.value.scrollMultiplier
                        timestampMs = System.currentTimeMillis()
                    }

                }
            }
            tcpClientManager.sendProto(message)
        }
    }

    fun onScrollDownClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrateLocal(VibrationEffect.EFFECT_TICK, VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1f)
        } else {
            vibrateLocal(VibrationEffect.EFFECT_TICK, 0, 1f)
        }
        viewModelScope.launch {
            val message = com.skymouse.skymouseclient.proto.messageToServer {
                emulatorEvent = com.skymouse.skymouseclient.proto.emulatorEvent {
                    scroll = com.skymouse.skymouseclient.proto.scrollEvent {
                        deltaY = -1 * settingsState.value.scrollMultiplier
                        timestampMs = System.currentTimeMillis()
                    }
                }
            }
            tcpClientManager.sendProto(message)
        }
    }

    private var mouseSequenceId = 0

    fun onMouseMove(deltaX: Float, deltaY: Float) {
        viewModelScope.launch {
            val emulatorEvent = com.skymouse.skymouseclient.proto.emulatorEvent {
                mouse = com.skymouse.skymouseclient.proto.mouseEvent {
                    this.deltaX = deltaX
                    this.deltaY = deltaY
                    this.sequenceId = mouseSequenceId++
                    this.timestampMs = System.currentTimeMillis()
                }
            }

            udpClientManager.sendEmulatorEvent(emulatorEvent)
        }
    }

    private val gyroscopeProvider = GyroscopeProvider(application) { dx, dy ->
        onMouseMove(dx, dy)
    }

    init {
        loadSettings()

        viewModelScope.launch {
            settingsState.collect { state ->
                gyroscopeProvider.updateSettings(
                    state.gyroSensitivity,
                    state.gyroAcceleration
                )
            }
        }
    }

}
