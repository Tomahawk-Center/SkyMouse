package com.skymouse.skymouseclient.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.skymouse.skymouseclient.data.util.KeepScreenAwakeInGyroMode
import com.skymouse.skymouseclient.ui.connection.ConnectionScreen
import com.skymouse.skymouseclient.ui.connection.ConnectionViewModel
import com.skymouse.skymouseclient.ui.control.ControlScreen
import com.skymouse.skymouseclient.ui.control.ControlViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    KeepScreenAwakeInGyroMode(
        isGyroModeActive = controlViewModel.isGyroEnabled,
        isGyroActive = isGyroActive
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SkyMouse Client") },
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
                                contentDescription = "Scale change",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                                contentDescription = "Ping check",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                                contentDescription = "Commands",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }


                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onNavigateToSettings()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
