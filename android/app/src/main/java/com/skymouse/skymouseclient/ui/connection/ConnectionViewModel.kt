package com.skymouse.skymouseclient.ui.connection

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skymouse.skymouseclient.data.SkyMouseManager
import com.skymouse.skymouseclient.data.TcpConnectionState
import com.skymouse.skymouseclient.data.util.VersionVerificationResult
import com.skymouse.skymouseclient.data.util.VersionVerifier
import com.skymouse.skymouseclient.proto.HapticEventType
import com.skymouse.skymouseclient.proto.ServerEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var ipAddress by mutableStateOf(prefs.getString("ip_address", "") ?: "")
    var port by mutableStateOf(prefs.getString("port", "10000") ?: "10000")

    private val tcpClientManager = SkyMouseManager.tcpClient
    private val udpClientManager = SkyMouseManager.udpClient

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun onConnectClicked() {
        val portInt = port.toIntOrNull() ?: return
        val clientVersionStr = "3.2"

        prefs.edit {
            putString("ip_address", ipAddress)
            putString("port", port)
        }

        viewModelScope.launch {
            tcpClientManager.connect(ipAddress, portInt)

            if (tcpClientManager.connectionState.value is TcpConnectionState.Connected) {
                val helloMsg = com.skymouse.skymouseclient.proto.messageToServer {
                    clientHello = com.skymouse.skymouseclient.proto.clientHello {
                        clientVersion = clientVersionStr
                    }
                }

                val response = withTimeoutOrNull(5.seconds) {
                    coroutineScope {
                        val responseDeferred = async {
                            tcpClientManager.incomingMessages.first { it.hasServerHello() }
                        }
                        tcpClientManager.sendProto(helloMsg)
                        responseDeferred.await()
                    }
                }

                if (response != null && response.hasServerHello()) {
                    val serverVersion = response.serverHello.serverVersion
                    val versionVerificationResult = VersionVerifier.verify(clientVersionStr, serverVersion)

                    if (versionVerificationResult is VersionVerificationResult.Mismatch) {
                        Toast.makeText(getApplication(), versionVerificationResult.reason, Toast.LENGTH_LONG).show()
                        tcpClientManager.disconnect()
                        return@launch
                    } else if (versionVerificationResult is VersionVerificationResult.Warning) {
                        Toast.makeText(getApplication(), versionVerificationResult.message, Toast.LENGTH_LONG).show()
                    }

                    val udpPortFromServer = response.serverHello.udpPort
                    val udpToken = response.serverHello.udpToken
                    if (udpToken == 0) {
                        Toast.makeText(getApplication(), "Server sent an invalid UDP token", Toast.LENGTH_LONG).show()
                        tcpClientManager.disconnect()
                        return@launch
                    }
                    udpClientManager.connect(ipAddress, udpPortFromServer, udpToken)
                    startReceivingServerEvents()
                } else {
                    Toast.makeText(getApplication(), "Handshake response timed out", Toast.LENGTH_LONG).show()
                    tcpClientManager.disconnect()
                }
            }
        }
    }

    fun onDisconnectClicked() {
        disconnect()
    }

    fun disconnect() {
        viewModelScope.launch {
            udpClientManager.disconnect()
            tcpClientManager.disconnect()
        }
    }

    private fun startReceivingServerEvents() {
        viewModelScope.launch {
            udpClientManager.incomingMessages
                .filter { it.hasServerEvent() }
                .collect { msg ->
                    triggerHaptic(msg.serverEvent)
                }
        }
    }

    private fun triggerHaptic(event: ServerEvent) {
        val effect = when (event.type) {
            HapticEventType.EVENT_BORDER_CROSSING -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                        .compose()
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                }
            }
            HapticEventType.EVENT_EDGE_HIT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                        .compose()
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                }
            }
            else -> null
        }
        effect?.let { vibrator.vibrate(it) }
    }
}
