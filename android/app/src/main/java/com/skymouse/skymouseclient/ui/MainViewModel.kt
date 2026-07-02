package com.skymouse.skymouseclient.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skymouse.skymouseclient.data.TcpClientManager
import com.skymouse.skymouseclient.data.UdpClientManager
import com.skymouse.skymouseclient.proto.MouseButton
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val udpClientManager = UdpClientManager()

    val udpConnectionState = udpClientManager.connectionState

    var ipAddress by mutableStateOf("")
    var port by mutableStateOf("")
    var messageText by mutableStateOf("")

    fun onConnectClicked() {
        val portInt = port.toIntOrNull() ?: return

        viewModelScope.launch {
            udpClientManager.connect(ipAddress, portInt)
        }
    }

    fun onDisconnectClicked() {
        udpClientManager.disconnect()
    }

    fun onSendMessage() {
        if (messageText.isBlank()) return

        viewModelScope.launch {
            udpClientManager.sendText(messageText)
            messageText = ""
        }
    }
    private val tcpClientManager = TcpClientManager()

    val tcpConnectionState = tcpClientManager.connectionState

    var tcpPort by mutableStateOf("")
    var tcpMessageText by mutableStateOf("")

    fun onTcpConnectClicked() {
        val portInt = tcpPort.toIntOrNull() ?: return

        viewModelScope.launch {
            tcpClientManager.connect(ipAddress, portInt)
        }
    }

    fun onTcpDisconnectClicked() {
        viewModelScope.launch {
            tcpClientManager.disconnect()
        }
    }

    fun onTcpSendMessage() {
//        if (tcpMessageText.isBlank()) return
//
//        viewModelScope.launch {
//            tcpClientManager.sendText(tcpMessageText)
//            tcpMessageText = ""
//        }
    }

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
}