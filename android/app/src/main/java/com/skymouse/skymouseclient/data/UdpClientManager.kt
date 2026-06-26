package com.skymouse.skymouseclient.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpClientManager {

    private var socket: DatagramSocket? = null
    private var serverAddress: InetAddress? = null
    private var serverPort: Int = 8080

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    suspend fun connect(ip: String, port: Int) = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.Connecting
        try {
            serverAddress = InetAddress.getByName(ip)
            serverPort = port
            socket = DatagramSocket()

            _connectionState.value = ConnectionState.Connected
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.localizedMessage ?: "UDP Init Failed")
        }
    }

    suspend fun sendText(text: String) = withContext(Dispatchers.IO) {
        val s = socket
        val address = serverAddress
        if (s == null || address == null || _connectionState.value != ConnectionState.Connected) return@withContext

        try {
            val bytes = text.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(bytes, bytes.size, address, serverPort)
            s.send(packet)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.localizedMessage ?: "Send Failed")
        }
    }

    fun disconnect() {
        socket?.close()
        socket = null
        serverAddress = null
        _connectionState.value = ConnectionState.Disconnected
    }
}