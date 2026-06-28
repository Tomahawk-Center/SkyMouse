package com.skymouse.skymouseclient.data

sealed interface TcpConnectionState {
    object Disconnected : TcpConnectionState
    object Connecting : TcpConnectionState
    object Connected : TcpConnectionState
    data class Error(val message: String) : TcpConnectionState
}
