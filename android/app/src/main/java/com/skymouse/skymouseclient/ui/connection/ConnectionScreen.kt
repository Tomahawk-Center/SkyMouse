package com.skymouse.skymouseclient.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.skymouse.skymouseclient.data.SkyMouseManager
import com.skymouse.skymouseclient.data.TcpConnectionState
import com.skymouse.skymouseclient.data.UdpConnectionState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel) {
    val tcpState by SkyMouseManager.tcpClient.connectionState.collectAsState()
    val udpState by SkyMouseManager.udpClient.connectionState.collectAsState()

    val isConnecting = tcpState is TcpConnectionState.Connecting || udpState is UdpConnectionState.Connecting
    val error = (tcpState as? TcpConnectionState.Error)?.message ?: (udpState as? UdpConnectionState.Error)?.message

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(error) {
        if (error != null) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connect to server",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.ipAddress,
                onValueChange = { viewModel.ipAddress = it },
                label = { Text("IP Address") },
                placeholder = { Text("192.168.1.1") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(2f),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = viewModel.port,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 5) {
                        viewModel.port = input
                    }
                },
                label = { Text("Port") },
                placeholder = { Text("10000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isConnecting) {
            LoadingIndicator(modifier = Modifier.size(64.dp))
        } else {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    viewModel.onConnectClicked()
                          },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(text = "Connect", style = MaterialTheme.typography.titleMedium)
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
