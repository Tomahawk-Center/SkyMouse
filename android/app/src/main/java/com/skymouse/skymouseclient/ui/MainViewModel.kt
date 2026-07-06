package com.skymouse.skymouseclient.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skymouse.skymouseclient.data.TcpClientManager
import com.skymouse.skymouseclient.data.TcpConnectionState
import com.skymouse.skymouseclient.data.UdpClientManager
import com.skymouse.skymouseclient.proto.MouseButton
import kotlinx.coroutines.launch
import android.widget.Toast

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val udpClientManager = UdpClientManager()

    val udpConnectionState = udpClientManager.connectionState

    var ipAddress by mutableStateOf(prefs.getString("ip_address", "") ?: "")
    var port by mutableStateOf(prefs.getString("port", "10000") ?: "10000")

    fun onConnectClicked() {
        val portInt = port.toIntOrNull() ?: return
        val clientVersionStr = "1"

        prefs.edit {
            putString("ip_address", ipAddress)
            putString("port", port)
        }

        viewModelScope.launch {
            tcpClientManager.connect(ipAddress, portInt)

            if (tcpClientManager.connectionState.value is TcpConnectionState.Connected) {
                val helloMsg = com.skymouse.skymouseclient.proto.messageToServer {
                    clientHello = com.skymouse.skymouseclient.proto.clientHello {
                        clientVersion = clientVersionStr
                    }
                }

                tcpClientManager.sendProto(helloMsg)

                val response = tcpClientManager.receiveProto()
                if (response != null && response.hasServerHello()) {
                    val serverVersion = response.serverHello.serverVersion
                    if (serverVersion != clientVersionStr) {
                        Toast.makeText(
                            getApplication(),
                            "Server version mismatch: $serverVersion, client version: $clientVersionStr",
                            Toast.LENGTH_LONG
                        ).show()

                        tcpClientManager.disconnect()
                        return@launch
                    }

                    val udpPortFromServer = response.serverHello.udpPort
                    udpClientManager.connect(ipAddress, udpPortFromServer)
                } else {
                    tcpClientManager.disconnect()
                }
            }
        }
    }

    fun onDisconnectClicked() {
        viewModelScope.launch {
            udpClientManager.disconnect()
            tcpClientManager.disconnect()
        }
    }

    private val tcpClientManager = TcpClientManager()

    val tcpConnectionState = tcpClientManager.connectionState

    fun onMouseButtonClicked(button: MouseButton, isPressed: Boolean) {
        viewModelScope.launch {
            val message = com.skymouse.skymouseclient.proto.messageToServer {
                click = com.skymouse.skymouseclient.proto.clickEvent {
                    this.button = button
                    this.state = if (isPressed) {
                        com.skymouse.skymouseclient.proto.ButtonState.STATE_DOWN // STATE_PRESSED
                    } else {
                        com.skymouse.skymouseclient.proto.ButtonState.STATE_UP   // STATE_RELEASED
                    }
                }
            }
            tcpClientManager.sendProto(message)
        }
    }

    fun onScrollUpClicked() {
        viewModelScope.launch {
            val message = com.skymouse.skymouseclient.proto.messageToServer {
                scroll = com.skymouse.skymouseclient.proto.scrollEvent {
                    deltaY = 1
                }
            }
            tcpClientManager.sendProto(message)
        }
    }

    fun onScrollDownClicked() {
        viewModelScope.launch {
            val message = com.skymouse.skymouseclient.proto.messageToServer {
                scroll = com.skymouse.skymouseclient.proto.scrollEvent {
                    deltaY = -1
                }
            }
            tcpClientManager.sendProto(message)
        }
    }

    private var mouseSequenceId = 0

    fun onMouseMove(deltaX: Float, deltaY: Float) {
        viewModelScope.launch {
            val msg = com.skymouse.skymouseclient.proto.messageToServer {
                mouse = com.skymouse.skymouseclient.proto.mouseEvent {
                    this.deltaX = deltaX
                    this.deltaY = deltaY
                    this.sequenceId = mouseSequenceId++
                    this.timestampMs = System.currentTimeMillis()
                }
            }
            udpClientManager.sendProto(msg)
        }
    }
}