package com.skymouse.skymouseclient.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SkyMouseManager {
    val tcpClient = TcpClientManager()
    val udpClient = UdpClientManager()
    val pingManager = PingManager(tcpClient, udpClient)

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState

    fun updateSettings(state: SettingsState) {
        _settingsState.value = state
    }

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val loadedState = SettingsState(
            gyroSensitivity = prefs.getFloat("gyro_sensitivity", 4.0f),
            gyroAcceleration = prefs.getFloat("gyro_acceleration", 2.0f),
            touchpadSensitivity = prefs.getFloat("touchpad_sensitivity", 1.0f),
            touchpadAcceleration = prefs.getFloat("touchpad_acceleration", 0.05f),
            longPressVibrationLevel = prefs.getInt("long_press_vibration", 1),
            scrollMultiplier = prefs.getInt("scroll_multiplier", 1),
            autoReconnect = prefs.getBoolean("auto_reconnect", true),
            autoConnectOnStartup = prefs.getBoolean("auto_connect_on_startup", false),
            volumeButtonsAction = try {
                VolumeButtonsAction.valueOf(
                    prefs.getString("volume_buttons_action", VolumeButtonsAction.SYSTEM_VOLUME.name)
                        ?: VolumeButtonsAction.SYSTEM_VOLUME.name
                )
            } catch (_: Exception) {
                VolumeButtonsAction.SYSTEM_VOLUME
            }
        )
        _settingsState.value = loadedState
    }
}
