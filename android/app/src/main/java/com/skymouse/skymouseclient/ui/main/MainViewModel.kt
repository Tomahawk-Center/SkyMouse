package com.skymouse.skymouseclient.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.skymouse.skymouseclient.data.SkyMouseManager

class MainViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver {

    val settingsState = SkyMouseManager.settingsState

    var isConnectingProvider: (()-> Boolean)? = null

    private var shouldAutoReconnect = false
    private var isFirstStart = true

    var onAutoConnect: (() -> Unit)? = null
    var onDisconnect: (() -> Unit)? = null

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        
        val autoConnectOnStartup = settingsState.value.autoConnectOnStartup
        val shouldConnect = (isFirstStart && autoConnectOnStartup) || shouldAutoReconnect
        isFirstStart = false

        val currentlyConnected = isConnectingProvider?.invoke() ?: false

        if (shouldConnect && !currentlyConnected) {
            onAutoConnect?.invoke()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)

        val currentlyConnected = isConnectingProvider?.invoke() ?: false
        
        if (currentlyConnected) {
            if (settingsState.value.autoReconnect) {
                shouldAutoReconnect = true
            }
            onDisconnect?.invoke()
        }
    }
}
