package com.skymouse.skymouseclient.data

enum class VolumeButtonsAction {
    SYSTEM_VOLUME,
    APP_CONTROL
}

data class SettingsState(
    val gyroSensitivity: Float = 4.0f,
    val gyroAcceleration: Float = 2.0f,

    val touchpadSensitivity: Float = 1.0f,
    val touchpadAcceleration: Float = 0.05f,

    val longPressVibrationLevel: Int = 1,

    val scrollMultiplier: Int = 1,

    val autoReconnect: Boolean = true,

    val autoConnectOnStartup: Boolean = false,

    val volumeButtonsAction: VolumeButtonsAction = VolumeButtonsAction.SYSTEM_VOLUME
)
