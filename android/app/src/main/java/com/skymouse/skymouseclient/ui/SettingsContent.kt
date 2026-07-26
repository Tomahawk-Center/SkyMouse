package com.skymouse.skymouseclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skymouse.skymouseclient.data.SettingsState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.saveSettings()
        }
    }


    SettingsContent(
        settingsState = settingsState,
        onGyroSensitivityChange = viewModel::onSensitivityChange,
        onGyroAccelerationChange = viewModel::onAccelerationChange,
        onTouchpadSensitivityChange = viewModel::onTouchpadSensitivityChange,
        onTouchpadAccelerationChange = viewModel::onTouchpadAccelerationChange,
        onLongPressVibrationLevelChange = viewModel::onLongPressVibrationLevelChange,
        onScrollMultiplierChange = viewModel::onScrollMultiplierChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    settingsState: SettingsState,
    onGyroSensitivityChange: (Float) -> Unit,
    onGyroAccelerationChange: (Float) -> Unit,
    onTouchpadSensitivityChange: (Float) -> Unit,
    onTouchpadAccelerationChange: (Float) -> Unit,
    onLongPressVibrationLevelChange: (Int) -> Unit,
    onScrollMultiplierChange: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Gyro Settings
            Text(
                text = "Gyroscope Sensitivity: ${(settingsState.gyroSensitivity * 10).roundToInt() / 10.0}",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settingsState.gyroSensitivity,
                onValueChange = onGyroSensitivityChange,
                valueRange = 0.1f..5.0f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Gyroscope Acceleration: ${(settingsState.gyroAcceleration * 100).roundToInt() / 100.0}",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settingsState.gyroAcceleration,
                onValueChange = onGyroAccelerationChange,
                valueRange = 0.0f..3.0f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Touchpad Settings
            Text(
                text = "Touchpad Sensitivity: ${(settingsState.touchpadSensitivity * 100).roundToInt() / 100.0}",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settingsState.touchpadSensitivity,
                onValueChange = onTouchpadSensitivityChange,
                valueRange = 0.1f..3.0f
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Touchpad Acceleration: ${(settingsState.touchpadAcceleration * 100).roundToInt() / 100.0}",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settingsState.touchpadAcceleration,
                onValueChange = onTouchpadAccelerationChange,
                valueRange = 0.0f..0.2f
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Haptics
            Text(
                text = "Long Press Vibration Intensity: ${settingsState.longPressVibrationLevel}",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settingsState.longPressVibrationLevel.toFloat(),
                onValueChange = { onLongPressVibrationLevelChange(it.toInt()) },
                valueRange = 1.0f..255.0f
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Scroll
            Text(
                text = "Scroll Multiplier: ${settingsState.scrollMultiplier}",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settingsState.scrollMultiplier.toFloat(),
                onValueChange = { onScrollMultiplierChange(it.toInt()) },
                valueRange = 1.0f..10.0f
            )
        }
    }
}