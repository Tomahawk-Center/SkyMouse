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

    suspend fun sendProto(message: com.google.protobuf.MessageLite) = withContext(Dispatchers.IO){
        val os = outputStream ?: return@withContext

        try {
            val bytes = message.toByteArray()
            val size = bytes.size

            val header = byteArrayOf(
                (size shr 24).toByte(),
                (size shr 16).toByte(),
                (size shr 8).toByte(),
                size.toByte()
            )

            os.write(header)
            os.write(bytes)
            os.flush()
        } catch (e: Exception) {
            _connectionState.value = TcpConnectionState.Error(e.localizedMessage ?: "Tcp send failed")
            disconnect()
        }
    }

    suspend fun receiveProto(): com.skymouse.skymouseclient.proto.MessageToClient? = withContext(
        Dispatchers.IO) {
        val s = socket ?: return@withContext null
        try {
            val inputStream = s.getInputStream()

            val header = ByteArray(4)
            var totalReadHeader = 0
            while (totalReadHeader<4) {
                val read = inputStream.read(header, totalReadHeader, 4-totalReadHeader)
                if (read == -1) {
                    return@withContext null
                }
                totalReadHeader += read
            }

            val size = ((header[0].toInt() and 0xFF) shl 24) or
                    ((header[1].toInt() and 0xFF) shl 16) or
                    ((header[2].toInt() and 0xFF) shl 8) or
                    (header[3].toInt() and 0xFF)

            if (size <= 0 || size > 1024 * 1024) return@withContext null // Too big packets protection

            val body = ByteArray(size)
            var totalReadBody = 0
            while (totalReadBody < size) {
                val read = inputStream.read(body, totalReadBody, size - totalReadBody)
                if (read == -1) {
                    return@withContext null
                }
                totalReadBody += read
            }

            return@withContext com.skymouse.skymouseclient.proto.MessageToClient.parseFrom(body)
        } catch (e: Exception) {
            if (!s.isClosed) {
                _connectionState.value = TcpConnectionState.Error(e.localizedMessage ?: "Tcp receive failed")
            }
            null
        }
    }

     suspend fun disconnect() =withContext(Dispatchers.IO) {
        try {
            socket?.shutdownOutput()
        } catch (_: Exception) { }

         try {
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {}

         outputStream = null
         socket = null
         _connectionState.value = TcpConnectionState.Disconnected
    }
}