package com.skymouse.skymouseclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.skymouse.skymouseclient.data.UdpConnectionState
import com.skymouse.skymouseclient.data.TcpConnectionState
import com.skymouse.skymouseclient.proto.MouseButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val connectionState by viewModel.udpConnectionState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("SkyMouse Client") }) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
                contentAlignment = Alignment.Center){

                Column(modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("TCP control", style = MaterialTheme.typography.titleLarge)
                    TcpControlBlock(viewModel = viewModel)

                    when (connectionState) {
                        is UdpConnectionState.Disconnected, is UdpConnectionState.Error -> {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ip
                                OutlinedTextField(
                                    value = viewModel.ipAddress,
                                    onValueChange = { viewModel.ipAddress = it },
                                    label = { Text("IP Address") },
                                    placeholder = { Text("192.168.1.1") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = ShapeDefaults.Large,
                                    modifier = Modifier.weight(2f)
                                )

                                // port UDP
                                OutlinedTextField(
                                    value = viewModel.port,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 5) {
                                            viewModel.port = input
                                        }
                                    },
                                    label = { Text("Port") },
                                    placeholder = { Text("8080") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = ShapeDefaults.Large,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (connectionState is UdpConnectionState.Error) {
                                Text((connectionState as UdpConnectionState.Error).message, color = MaterialTheme.colorScheme.error)
                            }

                            Button (onClick = { viewModel.onConnectClicked() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = ShapeDefaults.ExtraLarge,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                Text(text = "Connect", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        UdpConnectionState.Connecting -> {
                            CircularProgressIndicator()
                            Text("Connecting...")
                        }

                        UdpConnectionState.Connected -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // text
                                OutlinedTextField(
                                    value = viewModel.messageText,
                                    onValueChange = { viewModel.messageText = it },
                                    label = { Text("Message") },
                                    placeholder = { Text("Some text...") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                    shape = ShapeDefaults.Large,
                                    modifier = Modifier.weight(2f)
                                )

                                // send text
                                Button(
                                    onClick = { viewModel.onSendMessage() },
                                    shape = ShapeDefaults.Large,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = "Send",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            TextButton(onClick = { viewModel.onDisconnectClicked() }) {
                                Text("Disconnect", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TcpControlBlock(viewModel: MainViewModel) {
    val tcpState by viewModel.tcpConnectionState.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)

    ) {
        when (tcpState) {
            is TcpConnectionState.Disconnected, is TcpConnectionState.Error -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.tcpPort,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 5) {
                                viewModel.tcpPort = input
                            }
                        },
                        label = {Text("TCP port")},
                        placeholder = {Text("10000")},
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = ShapeDefaults.Large,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {viewModel.onTcpConnectClicked()},
                        shape = ShapeDefaults.Large
                    ) {
                        Text("Connect")
                    }
                }

                if (tcpState is TcpConnectionState.Error) {
                    Text(
                        text = (tcpState as TcpConnectionState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

            }

            TcpConnectionState.Connecting -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    Text("Connecting TCP")
                }
            }

            TcpConnectionState.Connected -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.tcpMessageText,
                        onValueChange = { viewModel.tcpMessageText = it},
                        label = { Text("TCP message") },
                        modifier = Modifier.weight(2f)
                    )

                    Button(
                        onClick = {viewModel.onTcpSendMessage()}
                    ) {
                        Text("Send")
                    }
                }

                TextButton(
                    onClick = {viewModel.onTcpDisconnectClicked()}
                ) {
                    Text("Disconnect TCP", color = MaterialTheme.colorScheme.error)
                }

                MouseComponents(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MouseInteractionButton(
    text: String,
    onAction: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .padding(8.dp)
        .clip(ShapeDefaults.Large)
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
        contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun MouseComponents(viewModel: MainViewModel){
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // left mouse button
        MouseInteractionButton(
            text = "LMB",
            onAction = {isPressed -> viewModel.onMouseButtonClicked(MouseButton.BUTTON_LEFT, isPressed)}
        )

        // vertical block with scrolls and middle button
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.onScrollUpClicked() },
                modifier = Modifier.size(width = 110.dp, height = 48.dp),
                shape = ShapeDefaults.Medium) { Text("SCRL+") }

            MouseInteractionButton(
                text = "MMB",
                onAction = { isPressed -> viewModel.onMouseButtonClicked(MouseButton.BUTTON_MIDDLE, isPressed) }
            )

            Button(onClick = { viewModel.onScrollDownClicked() },
                modifier = Modifier.size(width = 110.dp, height = 48.dp),
                shape = ShapeDefaults.Medium) { Text("SCRL-") }
        }

        MouseInteractionButton(
            text = "RMB",
            onAction = {isPressed -> viewModel.onMouseButtonClicked(MouseButton.BUTTON_RIGHT, isPressed)}
        )
    }
}