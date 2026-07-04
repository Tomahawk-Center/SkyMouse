package com.skymouse.skymouseclient.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.skymouse.skymouseclient.data.TcpConnectionState
import com.skymouse.skymouseclient.data.UdpConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val tcpState by viewModel.tcpConnectionState.collectAsState()
    val udpState by viewModel.udpConnectionState.collectAsState()

    val isConnected = tcpState is TcpConnectionState.Connected || udpState is UdpConnectionState.Connected

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SkyMouse Client") },
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
                ControlScreen(viewModel)
            } else {
                ConnectionScreen(viewModel)
            }
        }
    }
}