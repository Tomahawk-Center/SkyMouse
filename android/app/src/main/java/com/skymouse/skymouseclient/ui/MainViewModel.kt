package com.skymouse.skymouseclient.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skymouse.skymouseclient.data.UdpClientManager
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val udpClientManager = UdpClientManager()

    val connectionState = udpClientManager.connectionState

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
}