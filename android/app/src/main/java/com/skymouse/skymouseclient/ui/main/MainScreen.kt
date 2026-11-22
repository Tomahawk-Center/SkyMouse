package com.skymouse.skymouseclient.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skymouse.skymouseclient.data.util.KeepScreenAwakeInGyroMode
import com.skymouse.skymouseclient.ui.connection.ConnectionScreen
import com.skymouse.skymouseclient.ui.connection.ConnectionViewModel
import com.skymouse.skymouseclient.ui.control.ControlScreen
import com.skymouse.skymouseclient.ui.control.ControlViewModel

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    connectionViewModel: ConnectionViewModel,
    controlViewModel: ControlViewModel,
    onNavigateToSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val isConnected by connectionViewModel.isConnected.collectAsState()

    val isGyroActive by controlViewModel.isGyroActive.collectAsState()

    val isVolumeVisible by controlViewModel.isVolumeVisible.collectAsState()
    val isVolumeMuted by controlViewModel.isVolumeMuted.collectAsState()
    val currentVolume by controlViewModel.currentVolume.collectAsState()

    KeepScreenAwakeInGyroMode(
        isGyroModeActive = controlViewModel.isGyroEnabled,
        isGyroActive = isGyroActive
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .verticalFadingEdges(fadeHeight = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        AnimatedContent(
                            targetState = isVolumeVisible,
                            transitionSpec = {
                                if (targetState) {
                                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> height } + fadeOut())
                                } else {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> -height } + fadeOut())
                                }
                            },
                            label = "TitleVolumeTransition"
                        ) { showVolume ->
                            if (showVolume) {
                                PixelVolumeTitle(volumeLevel = currentVolume, muted = isVolumeMuted)
                            } else {
                                Text(
                                    text = "SkyMouse"
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (isConnected) {

                        // Scale change
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                controlViewModel.isScaleSheetShown = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Scale change"
                            )
                        }

                        // Ping check
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                controlViewModel.isPingCheckSheetShown = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Ping check"
                            )
                        }

                        // Commands
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                controlViewModel.isCommandSheetShown = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardCommandKey,
                                contentDescription = "Commands"
                            )
                        }
                    }

                    // Settings
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onNavigateToSettings()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isConnected) {
                ControlScreen(
                    viewModel = controlViewModel,
                    settingsStateFlow = mainViewModel.settingsState,
                    onDisconnectClicked = { connectionViewModel.onDisconnectClicked() }
                )
            } else {
                ConnectionScreen(viewModel = connectionViewModel)
            }
        }
    }
}

private fun Modifier.verticalFadingEdges(fadeHeight: Dp = 8.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fadePx = fadeHeight.toPx()
        val height = size.height

        if (fadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = fadePx
                ),
                blendMode = BlendMode.DstIn
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = height - fadePx,
                    endY = height
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

@Composable
fun PixelVolumeTitle(
    volumeLevel: Int,
    muted: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedVolume by animateFloatAsState(
        targetValue = (volumeLevel.coerceIn(0, 100)) / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PixelVolumeProgress"
    )

    val activeColor by animateColorAsState(
        targetValue = if (muted) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "PixelVolumeActiveColor"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = when {
                muted || volumeLevel == 0 -> Icons.AutoMirrored.Filled.VolumeOff
                volumeLevel < 40 -> Icons.AutoMirrored.Filled.VolumeDown
                else -> Icons.AutoMirrored.Filled.VolumeUp
            },
            contentDescription = null,
            tint = activeColor,
            modifier = Modifier.size(20.dp)
        )

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedVolume)
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }

        Text(
            text = "$volumeLevel%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = activeColor
        )
    }
}
