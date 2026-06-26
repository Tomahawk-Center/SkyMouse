package com.skymouse.skymouseclient.data

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    object Connecting : ConnectionState
    object Connected : ConnectionState
    data class Error(val message: String) : ConnectionState
}