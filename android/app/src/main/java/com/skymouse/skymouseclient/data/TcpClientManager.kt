package com.skymouse.skymouseclient.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class TcpClientManager {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow<TcpConnectionState>(TcpConnectionState.Disconnected)
    val connectionState: StateFlow<TcpConnectionState> = _connectionState

    suspend fun connect(ip: String, port: Int) = withContext(Dispatchers.IO) {
        _connectionState.value = TcpConnectionState.Connecting

        try {
            val s = Socket()
            s.connect(InetSocketAddress(ip, port), 5000)
            socket = s

            outputStream = s.getOutputStream()
            _connectionState.value = TcpConnectionState.Connected
        } catch (error: Exception) {
            _connectionState.value = TcpConnectionState.Error(error.localizedMessage ?: "Tcp connection failed")
        }
    }

    suspend fun sendText(text: String) = withContext(Dispatchers.IO) {
        val os = outputStream
        if (os==null || _connectionState.value != TcpConnectionState.Connected) {
            return@withContext
        }

        try {
            val bytes = text.toByteArray(Charsets.UTF_8)
            os.write(bytes)
            os.flush()
        } catch (error: Exception) {
            _connectionState.value = TcpConnectionState.Error(error.localizedMessage ?: "Tcp send failed")
            disconnect()
        }
    }

    suspend fun sendProto(message: com.google.protobuf.MessageLite) = withContext(Dispatchers.IO){
        val os = outputStream
        if (os == null || _connectionState.value !is TcpConnectionState.Connected) {
            return@withContext
        }

        try {
            val size = message.serializedSize
            os.write(size)
            message.writeTo(os)
            os.flush()
        } catch (error: Exception){
            _connectionState.value = TcpConnectionState.Error(error.localizedMessage ?: "Proto send failed")
            disconnect()
        }
    }

     suspend fun disconnect() =withContext(Dispatchers.IO) {
        try {
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {}

         outputStream = null
         socket = null
         _connectionState.value = TcpConnectionState.Disconnected
    }
}