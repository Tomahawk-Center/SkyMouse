package com.skymouse.skymouseclient.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.skymouse.skymouseclient.data.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()

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
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    when (connectionState) {
                        is ConnectionState.Disconnected, is ConnectionState.Error -> {

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

                                // port
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

                            if (connectionState is ConnectionState.Error) {
                                Text((connectionState as ConnectionState.Error).message, color = MaterialTheme.colorScheme.error)
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

                        ConnectionState.Connecting -> {
                            CircularProgressIndicator()
                            Text("Connecting...")
                        }

                        ConnectionState.Connected -> {
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