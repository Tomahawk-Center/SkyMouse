package com.skymouse.skymouseclient.ui.settings

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.skymouse.skymouseclient.data.SettingsState
import com.skymouse.skymouseclient.data.SkyMouseManager
import com.skymouse.skymouseclient.data.VolumeButtonsAction
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val settingsState: StateFlow<SettingsState> = SkyMouseManager.settingsState

    fun onSensitivityChange(newValue: Float) {
        SkyMouseManager.updateSettings(settingsState.value.copy(gyroSensitivity = newValue))
    }

    fun onAccelerationChange(newValue: Float) {
        SkyMouseManager.updateSettings(settingsState.value.copy(gyroAcceleration = newValue))
    }

    fun onTouchpadSensitivityChange(newValue: Float) {
        SkyMouseManager.updateSettings(settingsState.value.copy(touchpadSensitivity = newValue))
    }

    fun onTouchpadAccelerationChange(newValue: Float) {
        SkyMouseManager.updateSettings(settingsState.value.copy(touchpadAcceleration = newValue))
    }

    fun onLongPressVibrationLevelChange(newValue: Int) {
        SkyMouseManager.updateSettings(settingsState.value.copy(longPressVibrationLevel = newValue))
    }

    fun onScrollMultiplierChange(newValue: Int) {
        SkyMouseManager.updateSettings(settingsState.value.copy(scrollMultiplier = newValue))
    }

    fun onAutoReconnectChange(newValue: Boolean) {
        SkyMouseManager.updateSettings(settingsState.value.copy(autoReconnect = newValue))
    }

    fun onAutoConnectOnStartupChange(newValue: Boolean) {
        SkyMouseManager.updateSettings(settingsState.value.copy(autoConnectOnStartup = newValue))
    }

    fun onVolumeButtonsActionChange(newValue: VolumeButtonsAction) {
        SkyMouseManager.updateSettings(settingsState.value.copy(volumeButtonsAction = newValue))
    }

    fun saveSettings() {
        val state = settingsState.value
        prefs.edit {
            putFloat("gyro_sensitivity", state.gyroSensitivity)
            putFloat("gyro_acceleration", state.gyroAcceleration)
            putFloat("touchpad_sensitivity", state.touchpadSensitivity)
            putFloat("touchpad_acceleration", state.touchpadAcceleration)
            putInt("long_press_vibration", state.longPressVibrationLevel)
            putInt("scroll_multiplier", state.scrollMultiplier)
            putBoolean("auto_reconnect", state.autoReconnect)
            putBoolean("auto_connect_on_startup", state.autoConnectOnStartup)
            putString("volume_buttons_action", state.volumeButtonsAction.name)
        }
    }
}
