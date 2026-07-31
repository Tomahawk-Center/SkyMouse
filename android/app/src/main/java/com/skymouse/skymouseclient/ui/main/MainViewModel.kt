package com.skymouse.skymouseclient.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.skymouse.skymouseclient.data.SkyMouseManager
import com.skymouse.skymouseclient.data.TcpConnectionState

class MainViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver {

    val settingsState = SkyMouseManager.settingsState
    val tcpConnectionState = SkyMouseManager.tcpClient.connectionState
    val udpConnectionState = SkyMouseManager.udpClient.connectionState

    private var shouldAutoReconnect = false
    private var isFirstStart = true

    var onAutoConnect: (() -> Unit)? = null
    var onDisconnect: (() -> Unit)? = null

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        
        val autoConnectOnStartup = settingsState.value.autoConnectOnStartup
        val shouldConnect = (isFirstStart && autoConnectOnStartup) || shouldAutoReconnect
        isFirstStart = false

        if (shouldConnect && tcpConnectionState.value !is TcpConnectionState.Connected) {
            onAutoConnect?.invoke()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        
        if (tcpConnectionState.value is TcpConnectionState.Connected) {
            if (settingsState.value.autoReconnect) {
                shouldAutoReconnect = true
            }
            onDisconnect?.invoke()
        }
    }
}
