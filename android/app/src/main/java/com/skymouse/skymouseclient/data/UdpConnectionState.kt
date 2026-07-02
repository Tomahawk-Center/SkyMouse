package com.skymouse.skymouseclient.data

sealed interface UdpConnectionState {
    object Disconnected : UdpConnectionState
    object Connecting : UdpConnectionState
    object Connected : UdpConnectionState
    data class Error(val message: String) : UdpConnectionState
}