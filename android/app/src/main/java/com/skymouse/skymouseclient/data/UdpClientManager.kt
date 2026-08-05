package com.skymouse.skymouseclient.data

import com.skymouse.skymouseclient.proto.MessageToClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpClientManager {

    private var socket: DatagramSocket? = null
    private var serverAddress: InetAddress? = null
    private var serverPort: Int = 8080
    private var token: Int = 0

    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow<UdpConnectionState>(UdpConnectionState.Disconnected)
    val connectionState: StateFlow<UdpConnectionState> = _connectionState

    private val _incomingMessages = MutableSharedFlow<MessageToClient>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<MessageToClient> = _incomingMessages.asSharedFlow()

    suspend fun connect(ip: String, port: Int, udpToken: Int) = withContext(Dispatchers.IO) {
        _connectionState.value = UdpConnectionState.Connecting
        try {
            serverAddress = InetAddress.getByName(ip)
            serverPort = port
            socket = DatagramSocket()
            token = udpToken

            _connectionState.value = UdpConnectionState.Connected

            startListening()
        } catch (e: Exception) {
            _connectionState.value = UdpConnectionState.Error(e.localizedMessage ?: "UDP Init Failed")
        }
    }

    private fun startListening() {
        clientScope.launch {
            while (socket?.isClosed == false) {
                val message = receiveNextProto()
                if (message != null) {
                    _incomingMessages.emit(message)
                } else {
                    break
                }
            }
            disconnect()
        }
    }

    private suspend fun receiveNextProto(): MessageToClient? = withContext(Dispatchers.IO) {
        val s = socket ?: return@withContext null
        try {
            val buffer = ByteArray(4096)
            val packet = DatagramPacket(buffer, buffer.size)

            s.receive(packet)

            val data = ByteArray(packet.length)
            System.arraycopy(packet.data, packet.offset, data, 0, packet.length)

            return@withContext MessageToClient.parseFrom(data)
        } catch (e: Exception) {
            if (!s.isClosed) {
                _connectionState.value = UdpConnectionState.Error(e.localizedMessage ?: "UDP Receive Failed")
            }
            null
        }
    }

    /**
     * Sends an EmulatorEvent to the server with token
     */
    suspend fun sendEmulatorEvent(event: com.skymouse.skymouseclient.proto.EmulatorEvent) = withContext(Dispatchers.IO) {
        val msg = com.skymouse.skymouseclient.proto.udpMessageToServer {
            this.udpToken = token
            this.emulatorEvent = event
        }

        sendProto(msg)
    }

    suspend fun sendPing(ping: com.skymouse.skymouseclient.proto.Ping) = withContext(Dispatchers.IO) {
        val msg = com.skymouse.skymouseclient.proto.udpMessageToServer {
            this.udpToken = token
            this.ping = ping
        }

        sendProto(msg)
    }

    private suspend fun sendProto(proto: com.google.protobuf.MessageLite) = withContext(Dispatchers.IO) {
        val s = socket
        val address = serverAddress
        if (s == null || address == null || _connectionState.value != UdpConnectionState.Connected) return@withContext

        try {
            val bytes = proto.toByteArray()
            val packet = DatagramPacket(bytes, bytes.size, address, serverPort)
            s.send(packet)
        } catch (e: Exception) {
            _connectionState.value = UdpConnectionState.Error(e.localizedMessage ?: "Send Failed")
        }
    }

    fun disconnect() {
        clientScope.coroutineContext.cancelChildren()
        socket?.close()
        socket = null
        serverAddress = null
        _connectionState.value = UdpConnectionState.Disconnected
    }
}
