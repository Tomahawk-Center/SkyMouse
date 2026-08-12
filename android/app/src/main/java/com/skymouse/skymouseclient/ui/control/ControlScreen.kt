package com.skymouse.skymouseclient.ui.control

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skymouse.skymouseclient.data.SettingsState
import com.skymouse.skymouseclient.proto.CommandEvent
import com.skymouse.skymouseclient.proto.MouseButton
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    viewModel: ControlViewModel,
    settingsStateFlow: StateFlow<SettingsState>,
    onDisconnectClicked: () -> Unit
) {
    val settingsState by settingsStateFlow.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current

    val bottomSheetState = rememberModalBottomSheetState()
    val pingCheckBottomSheetState = rememberModalBottomSheetState()

    val scope = rememberCoroutineScope()
    var pendingCommand by remember { mutableStateOf<CommandEvent?>(null) }

    if (pendingCommand != null) {
        val commandName = when (pendingCommand) {
            CommandEvent.COMMAND_SHUT_DOWN -> "Shut Down"
            CommandEvent.COMMAND_SLEEP -> "Sleep"
            CommandEvent.COMMAND_LOCK_SCREEN -> "Lock Screen"
            else -> "Unknown Command"
        }

        AlertDialog(
            onDismissRequest = { pendingCommand = null },
            title = { Text("Confirm Action") },
            text = { Text("Are you sure you want to $commandName the server?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val event = pendingCommand!!
                        pendingCommand = null

                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onSendCommand(event)

                        scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                            if (!bottomSheetState.isVisible) {
                                viewModel.isCommandSheetShown = false
                            }
                        }
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    pendingCommand = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (viewModel.isCommandSheetShown) {
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13)) {
            viewModel.isCommandSheetShown = false
            return
        }

        ModalBottomSheet(
            onDismissRequest = {viewModel.isCommandSheetShown = false},
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Server Commands",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                val onCommandSelected: (CommandEvent)->Unit = { event ->
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    pendingCommand = event
                }

                CommandItem(
                    text = "Shut Down",
                    icon = Icons.Default.PowerSettingsNew,
                    onClick = {
                        onCommandSelected(CommandEvent.COMMAND_SHUT_DOWN)

                    }
                )

                CommandItem(
                    text = "Sleep",
                    icon = Icons.Default.Bedtime,
                    onClick = {
                        onCommandSelected(CommandEvent.COMMAND_SLEEP)
                    }
                )
                CommandItem(
                    text = "Lock Screen",
                    icon = Icons.Default.Lock,
                    onClick = {
                        onCommandSelected(CommandEvent.COMMAND_LOCK_SCREEN)
                    }
                )

            }
        }
    }

    if (viewModel.isPingCheckSheetShown) {
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(
                Build.VERSION_CODES.S
            ) >= 13)
        ) {
            viewModel.isPingCheckSheetShown = false
            return
        }

        LaunchedEffect(Unit) {
            viewModel.startPingTests()
        }

        val udpState by viewModel.udpPingState.collectAsStateWithLifecycle()
        val tcpState by viewModel.tcpPingState.collectAsStateWithLifecycle()

        ModalBottomSheet(
            onDismissRequest = {
                viewModel.isPingCheckSheetShown = false
                viewModel.stopPingTests()
                               },
            sheetState = pingCheckBottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ping check",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PingResultColumn(
                        label = "UDP",
                        state = udpState,
                        modifier = Modifier.weight(1f)
                    )
                    PingResultColumn(
                        label = "TCP",
                        state = tcpState,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!udpState.isRunning && !tcpState.isRunning) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            viewModel.startPingTests()
                                  },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text("Restart Test")
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // touchpad
            if (!viewModel.isGyroEnabled){
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(ShapeDefaults.Large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectTapGestures (
                                onTap = {
                                    viewModel.onMouseButtonClicked(MouseButton.BUTTON_LEFT, true)
                                    viewModel.onMouseButtonClicked(MouseButton.BUTTON_LEFT, false)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val magnitude = sqrt(dragAmount.x*dragAmount.x + dragAmount.y*dragAmount.y)
                                val accMul = 1f + (magnitude * settingsState.touchpadAcceleration)
                                val sensitivity = settingsState.touchpadSensitivity

                                viewModel.onMouseMove(
                                    dragAmount.x * accMul * sensitivity,
                                    dragAmount.y * accMul * sensitivity
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Touchpad",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "Move finger to control mouse",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(ShapeDefaults.Large)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Gyroscope Active",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // scroll buttons
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.onScrollUpClicked() },
                    modifier = Modifier
                        .weight(1f)
                        .width(85.dp),
                    shape = ShapeDefaults.Medium
                ) {
                    Text("SCRL+", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = { viewModel.onScrollDownClicked() },
                    modifier = Modifier
                        .weight(1f)
                        .width(85.dp),
                    shape = ShapeDefaults.Medium
                ) {
                    Text("SCRL-", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // lmb, mmb, rmb
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MouseInteractionButton(
                text = "LMB",
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                onAction = {isPressed -> viewModel.onMouseButtonClicked(MouseButton.BUTTON_LEFT, isPressed)}
            )

            MouseInteractionButton(
                text = "MMB",
                modifier = Modifier.weight(0.7f),
                shape = RectangleShape,
                onAction = {isPressed -> viewModel.onMouseButtonClicked(MouseButton.BUTTON_MIDDLE, isPressed)}
            )

            MouseInteractionButton(
                text = "RMB",
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                onAction = {isPressed -> viewModel.onMouseButtonClicked(MouseButton.BUTTON_RIGHT, isPressed)}
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    viewModel.onClipboardShare()
                },
                modifier = Modifier
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPasteGo,
                    contentDescription = "clipboard share",
                    tint = LocalContentColor.current
                )
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.toggleControlMode()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text( // TODO
                    if (viewModel.isGyroEnabled) "Switch to Touchpad"
                    else "Switch to Gyroscope and Accelerometer"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // disconnect button
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                onDisconnectClicked()
                      },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Disconnect", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun PingResultColumn(
    label: String,
    state: com.skymouse.skymouseclient.data.PingState,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        if (state.stats?.isComplete == true) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            val stats = state.stats
            PingStatRow("Avg", "${stats.avg} ms")
            PingStatRow("Min", "${stats.min} ms")
            PingStatRow("Max", "${stats.max} ms")
            PingStatRow("Loss", "${stats.lossPercentage.toInt()}%")
        } else if (state.isRunning) {
            if (state.lastPingMs != null) haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            Text(
                text = state.lastPingMs?.let { "$it ms" } ?: "...",
                style = MaterialTheme.typography.headlineMedium
            )
        } else {
            Text("Ready", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PingStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MouseInteractionButton(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    onAction: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .padding(2.dp)
        .clip(shape)
        .background(
            if (isPressed) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.primary
        )
        .pointerInput(Unit){
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.any {it.changedToDown()}) {
                        isPressed = true
                        onAction(true)
                    }
                    if (event.changes.any {it.changedToUp() || it.isConsumed}) {
                        isPressed = false
                        onAction(false)
                    }
                }
            }
        }
        .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
    }
}


@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@Composable
fun CommandItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: ()->Unit) {
    ListItem(
        headlineContent = {Text(text)},
        leadingContent = {Icon(icon, contentDescription = null)},
        modifier = Modifier.clickable {onClick()},
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
