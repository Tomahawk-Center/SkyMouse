package com.skymouse.skymouseclient.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skymouse.skymouseclient.data.SettingsState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
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
        onScrollMultiplierChange = viewModel::onScrollMultiplierChange,
        onAutoReconnectChange = viewModel::onAutoReconnectChange,
        onAutoConnectOnStartupChange = viewModel::onAutoConnectOnStartupChange
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
    onScrollMultiplierChange: (Int) -> Unit,
    onAutoReconnectChange: (Boolean) -> Unit,
    onAutoConnectOnStartupChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 30.dp, vertical = 16.dp)
        ) {
            // Gyro Settings
            Text(
                text = "Gyroscope Sensitivity: ${(settingsState.gyroSensitivity * 10).roundToInt() / 10.0}",
                style = MaterialTheme.typography.titleMedium
            )
            HapticSlider(
                value = settingsState.gyroSensitivity,
                onValueChange = onGyroSensitivityChange,
                valueRange = 0.1f..5.0f,
                getHapticBucket = { (it * 10).roundToInt() },
                isEdge = { it <= 0.1f || it >= 5.0f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Gyroscope Acceleration: ${(settingsState.gyroAcceleration * 100 + 0.0001f).toInt() / 100.0}",
                style = MaterialTheme.typography.titleMedium
            )
            HapticSlider(
                value = settingsState.gyroAcceleration,
                onValueChange = onGyroAccelerationChange,
                valueRange = 0.0f..3.0f,
                getHapticBucket = { (it * 10 + 0.0001f).toInt() },
                isEdge = { it <= 0.0f || it >= 3.0f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Touchpad Settings
            Text(
                text = "Touchpad Sensitivity: ${(settingsState.touchpadSensitivity * 100 + 0.0001f).toInt() / 100.0}",
                style = MaterialTheme.typography.titleMedium
            )
            HapticSlider(
                value = settingsState.touchpadSensitivity,
                onValueChange = onTouchpadSensitivityChange,
                valueRange = 0.1f..3.0f,
                getHapticBucket = { (it * 10 + 0.0001f).toInt() },
                isEdge = { it <= 0.1f || it >= 3.0f }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Touchpad Acceleration: ${(settingsState.touchpadAcceleration * 100).roundToInt() / 100.0}",
                style = MaterialTheme.typography.titleMedium
            )
            HapticSlider(
                value = settingsState.touchpadAcceleration,
                onValueChange = onTouchpadAccelerationChange,
                valueRange = 0.0f..0.2f,
                getHapticBucket = { (it * 100).roundToInt() },
                isEdge = { it <= 0.0f || it >= 0.2f }
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Haptics
            Text(
                text = "Long Press Vibration Intensity: ${settingsState.longPressVibrationLevel}",
                style = MaterialTheme.typography.titleMedium
            )
            HapticSlider(
                value = settingsState.longPressVibrationLevel.toFloat(),
                onValueChange = { onLongPressVibrationLevelChange(it.toInt()) },
                valueRange = 1.0f..255.0f,
                getHapticBucket = { it.toInt() / 5 },
                isEdge = { val intValue = it.toInt(); intValue == 1 || intValue == 255 }
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Scroll
            Text(
                text = "Scroll Multiplier: ${settingsState.scrollMultiplier}",
                style = MaterialTheme.typography.titleMedium
            )
            HapticSlider(
                value = settingsState.scrollMultiplier.toFloat(),
                onValueChange = { onScrollMultiplierChange(it.toInt()) },
                valueRange = 1.0f..10.0f,
                getHapticBucket = { it.toInt() },
                isEdge = { val intValue = it.toInt(); intValue == 1 || intValue == 10 }
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Auto-reconnect
            Text(
                text = "Auto Reconnect",
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = settingsState.autoReconnect,
                onCheckedChange = {
                    if (it) {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onAutoReconnectChange(it)
                },
                thumbContent = {
                    AnimatedContent(
                        targetState = settingsState.autoReconnect,
                        transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                        label = "switch_thumb_icon"
                    ) { isSelected ->
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedIconColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Auto-connect on startup
            Text(
                text = "Auto Connect on app startup",
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = settingsState.autoConnectOnStartup,
                onCheckedChange = {
                    if (it) {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onAutoConnectOnStartupChange(it)
                },
                thumbContent = {
                    AnimatedContent(
                        targetState = settingsState.autoConnectOnStartup,
                        transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                        label = "switch_thumb_icon"
                    ) { isSelected ->
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedIconColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun HapticSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    getHapticBucket: (Float) -> Int,
    isEdge: (Float) -> Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var lastHapticValue by remember { mutableIntStateOf(getHapticBucket(value)) }
    var hasVibratedOnEdge by remember { mutableStateOf(false) }

    Slider(
        value = value,
        onValueChange = { newValue ->
            val currentBucket = getHapticBucket(newValue)

            if (isEdge(newValue)) {
                if (!hasVibratedOnEdge) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    hasVibratedOnEdge = true
                }
            } else {
                hasVibratedOnEdge = false
                if (currentBucket != lastHapticValue) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    lastHapticValue = currentBucket
                }
            }
            onValueChange(newValue)
        },
        valueRange = valueRange,
        modifier = modifier
    )
}
