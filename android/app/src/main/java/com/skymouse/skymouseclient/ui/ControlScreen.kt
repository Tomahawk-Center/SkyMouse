package com.skymouse.skymouseclient.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.skymouse.skymouseclient.proto.MouseButton
import kotlin.math.sqrt

@Composable
fun ControlScreen(viewModel: MainViewModel) {
    val acceleration = 0.05f

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
                                val accMul = 1f + (magnitude * acceleration)

                                viewModel.onMouseMove(dragAmount.x * accMul, dragAmount.y * accMul)
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

        Button(
            onClick = { viewModel.toggleControlMode() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                if (viewModel.isGyroEnabled) "Switch to Touchpad"
                else "Switch to Gyroscope and Accelerometer"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // disconnect button
        Button(
            onClick = { viewModel.onDisconnectClicked() },
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