package com.skymouse.skymouseclient.ui.control

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.skymouse.skymouseclient.data.GyroscopeProvider
import com.skymouse.skymouseclient.data.PingType
import com.skymouse.skymouseclient.data.SkyMouseManager
import com.skymouse.skymouseclient.proto.ButtonState
import com.skymouse.skymouseclient.proto.CommandEvent
import com.skymouse.skymouseclient.proto.KeyboardKey
import com.skymouse.skymouseclient.proto.MouseButton
import com.skymouse.skymouseclient.proto.clipboardShareEvent
import com.skymouse.skymouseclient.proto.emulatorEvent
import com.skymouse.skymouseclient.proto.keyboardStringEvent
import com.skymouse.skymouseclient.proto.keyboardTapEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ControlViewModel(
    application: Application
) : AndroidViewModel(application), DefaultLifecycleObserver {

    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var isGyroEnabled by mutableStateOf(prefs.getBoolean("gyro_enabled", false))
        private set

    private val settingsState = SkyMouseManager.settingsState
    private val tcpClientManager = SkyMouseManager.tcpClient
    private val udpClientManager = SkyMouseManager.udpClient
    private val pingManager = SkyMouseManager.pingManager

    val udpPingState = pingManager.udpState
    val tcpPingState = pingManager.tcpState

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val gyroscopeProvider = GyroscopeProvider(application) { dx, dy ->
        onMouseMove(dx, dy)
    }

    val isGyroActive: StateFlow<Boolean> = gyroscopeProvider.isGyroActive

    private val pressedButtons = mutableSetOf<MouseButton>()
    private var holdVibrationJob: Job? = null
    private var isHoldingVibration = false

    init {
        viewModelScope.launch {
            settingsState.collect { state ->
                gyroscopeProvider.updateSettings(
                    state.gyroSensitivity,
                    state.gyroAcceleration
                )
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isGyroEnabled) {
            gyroscopeProvider.start()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        gyroscopeProvider.stop()
    }

    fun toggleControlMode() {
        isGyroEnabled = !isGyroEnabled
        prefs.edit { putBoolean("gyro_enabled", isGyroEnabled) }

        if (isGyroEnabled) {
            gyroscopeProvider.start()
        } else {
            gyroscopeProvider.stop()
        }
    }

    fun onMouseButtonClicked(button: MouseButton, isPressed: Boolean) {
        if (isPressed) {
            val isFirstButton = pressedButtons.isEmpty()
            pressedButtons.add(button)

            if (isFirstButton) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    vibrateLocal(VibrationEffect.EFFECT_CLICK, VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f)
                } else {
                    vibrateLocal(VibrationEffect.EFFECT_CLICK, 0, 0.8f)
                }

                holdVibrationJob?.cancel()
                holdVibrationJob = viewModelScope.launch {
                    delay(200.milliseconds)
                    isHoldingVibration = true
                    startHoldVibration()
                }
            }
        } else {
            pressedButtons.remove(button)
            if (pressedButtons.isEmpty()) {
                holdVibrationJob?.cancel()
                if (isHoldingVibration) {
                    vibrator.cancel()
                    isHoldingVibration = false

                    viewModelScope.launch {
                        delay(5.milliseconds)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            vibrateLocal(
                                VibrationEffect.EFFECT_TICK,
                                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                                0.5f
                            )
                        } else {
                            vibrateLocal(VibrationEffect.EFFECT_TICK, 0, 0.5f)
                        }
                    }

                }
            }
        }

        viewModelScope.launch {
            val message = com.skymouse.skymouseclient.proto.messageToServer {
                emulatorEvent = com.skymouse.skymouseclient.proto.emulatorEvent {
                    click = com.skymouse.skymouseclient.proto.clickEvent {
                        this.button = button
                        this.state = if (isPressed) {
                            ButtonState.STATE_DOWN
                        } else {
                            ButtonState.STATE_UP
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

    private fun vibrateLocal(effectId: Int, primitiveId: Int, intensity: Float = 0.6f) {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(primitiveId)) {
            VibrationEffect.startComposition().addPrimitive(primitiveId, intensity).compose()
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

    override fun onCleared() {
        gyroscopeProvider.stop()
    }


    var isCommandSheetShown by mutableStateOf(false)

    fun onSendCommand(command: CommandEvent) {
        viewModelScope.launch {
            val msg = com.skymouse.skymouseclient.proto.messageToServer {
                this.command = command
            }
            tcpClientManager.sendProto(msg)
        }
    }

    var isPingCheckSheetShown by mutableStateOf(false)

    fun startPingTests() {
        pingManager.startPingTest(PingType.UDP)
        pingManager.startPingTest(PingType.TCP)
    }

    fun stopPingTests() {
        pingManager.stopPingTest(PingType.UDP)
        pingManager.stopPingTest(PingType.TCP)
    }

    fun onClipboardShare() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrBlank()) {
                sendClipboardText(text)
            }
        }
    }

    fun sendClipboardText(text: String) {
        viewModelScope.launch {

            val msg = com.skymouse.skymouseclient.proto.messageToServer {
                clipboardShare = clipboardShareEvent {
                    this.text = text
                }
            }
            tcpClientManager.sendProto(msg)
        }
    }

    fun onKeyboardStringInput(text: String) {
        viewModelScope.launch {
            val msg = com.skymouse.skymouseclient.proto.messageToServer {
                emulatorEvent = emulatorEvent {
                    keyboardStringEvent = keyboardStringEvent {
                    this.text = text
                    this.timestampMs = System.currentTimeMillis()
                    }
                }

            }
            tcpClientManager.sendProto(msg)
        }
    }

    fun onKeyboardTapInput(key: KeyboardKey, isPressed: Boolean) {
        viewModelScope.launch {
            val msg = com.skymouse.skymouseclient.proto.messageToServer {
                emulatorEvent = emulatorEvent {
                    keyboardTapEvent = keyboardTapEvent {
                    this.key = key
                    this.state = if (isPressed) ButtonState.STATE_DOWN else ButtonState.STATE_UP
                    this.timestampMs = System.currentTimeMillis()
                    }
                }
            }
            tcpClientManager.sendProto(msg)
        }
    }

}
