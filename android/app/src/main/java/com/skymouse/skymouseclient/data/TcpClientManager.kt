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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class TcpClientManager {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private val sendMutex = Mutex()
    private val connectionMutex = Mutex()
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow<TcpConnectionState>(TcpConnectionState.Disconnected)
    val connectionState: StateFlow<TcpConnectionState> = _connectionState

    private val _incomingMessages = MutableSharedFlow<MessageToClient>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<MessageToClient> = _incomingMessages

    suspend fun connect(ip: String, port: Int) = withContext(Dispatchers.IO) {
        connectionMutex.withLock {
            if (_connectionState.value is TcpConnectionState.Connected || _connectionState.value is TcpConnectionState.Connecting) {
                return@withContext
            }

            closeResources()
            _connectionState.value = TcpConnectionState.Connecting

            try {
                val s = Socket()
                s.connect(InetSocketAddress(ip, port), 5000)
                socket = s

                outputStream = s.getOutputStream()
                inputStream = s.getInputStream()

                _connectionState.value = TcpConnectionState.Connected

                startListening()
            } catch (error: Exception) {
                closeResources()
                _connectionState.value =
                    TcpConnectionState.Error(error.localizedMessage ?: "Tcp connection failed")
            }
        }
    }

    private fun startListening() {
        clientScope.launch {
            while (socket?.isConnected == true && socket?.isClosed == false) {
                val message = readNextProto()
                if (message != null) {
                    _incomingMessages.emit(message)
                } else {
                    break
                }
            }
        }
    }

    private suspend fun readNextProto(): MessageToClient? = withContext(Dispatchers.IO) {
        val stream = inputStream ?: return@withContext null

        try {
            val header = ByteArray(4)
            var totalReadHeader = 0
            while (totalReadHeader < 4) {
                val read = stream.read(header, totalReadHeader, 4 - totalReadHeader)
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
                val read = stream.read(body, totalReadBody, size - totalReadBody)
                if (read == -1) {
                    return@withContext null
                }
                totalReadBody += read
            }

            return@withContext MessageToClient.parseFrom(body)
        } catch (e: Exception) {
            if (socket?.isClosed == false) {
                _connectionState.value = TcpConnectionState.Error(e.localizedMessage ?: "Tcp receive failed")
            }
            null
        }
    }

    suspend fun sendProto(message: com.google.protobuf.MessageLite) = withContext(Dispatchers.IO) {
        sendMutex.withLock {
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
                _connectionState.value =
                    TcpConnectionState.Error(e.localizedMessage ?: "Tcp send failed")
                disconnect()
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        connectionMutex.withLock {
            closeResources()
            _connectionState.value = TcpConnectionState.Disconnected
        }
    }

    private fun closeResources() {
        clientScope.coroutineContext.cancelChildren()
        try {
            socket?.shutdownOutput()
        } catch (_: Exception) { }

        try {
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {
        }

        outputStream = null
        inputStream = null
        socket = null
    }
}