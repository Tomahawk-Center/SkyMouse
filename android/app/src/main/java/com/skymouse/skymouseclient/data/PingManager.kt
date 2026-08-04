package com.skymouse.skymouseclient.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

enum class PingType { UDP, TCP }

data class PingStats(
    val min: Long = 0,
    val max: Long = 0,
    val avg: Long = 0,
    val lossPercentage: Float = 0f,
    val isComplete: Boolean = false
)

data class PingState(
    val lastPingMs: Long? = null,
    val stats: PingStats? = null,
    val isRunning: Boolean = false
)

class PingManager(
    private val tcpClient: TcpClientManager,
    private val udpClient: UdpClientManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _udpState = MutableStateFlow(PingState())
    val udpState: StateFlow<PingState> = _udpState.asStateFlow()

    private val _tcpState = MutableStateFlow(PingState())
    val tcpState: StateFlow<PingState> = _tcpState.asStateFlow()

    private var udpJob: Job? = null
    private var tcpJob: Job? = null

    fun startPingTest(type: PingType) {
        when (type) {
            PingType.UDP -> {
                udpJob?.cancel()
                udpJob = scope.launch { runPingSequence(PingType.UDP) }
            }
            PingType.TCP -> {
                tcpJob?.cancel()
                tcpJob = scope.launch { runPingSequence(PingType.TCP) }
            }
        }
    }

    fun stopPingTest(type: PingType) {
        when (type) {
            PingType.UDP -> {
                udpJob?.cancel()
                _udpState.value = _udpState.value.copy(isRunning = false)
            }
            PingType.TCP -> {
                tcpJob?.cancel()
                _tcpState.value = _tcpState.value.copy(isRunning = false)
            }
        }
    }

    private suspend fun runPingSequence(type: PingType) {
        val stateFlow = if (type == PingType.UDP) _udpState else _tcpState
        val incomingFlow = if (type == PingType.UDP) udpClient.incomingMessages else tcpClient.incomingMessages

        stateFlow.value = PingState(isRunning = true)

        val results = mutableListOf<Long?>()
        val numPings = 10

        for (i in 0 until numPings) {
            val sequenceId = System.currentTimeMillis() + i
            val startTime = System.currentTimeMillis()

            val ping = com.skymouse.skymouseclient.proto.ping {
                this.sequenceId = sequenceId
                this.timestampMs = startTime
            }

            val pong = withTimeoutOrNull(500.milliseconds) {
                coroutineScope {
                    val pongDeferred = async {
                        incomingFlow.first { msg ->
                            msg.hasPong() && msg.pong.sequenceId == sequenceId
                        }
                    }

                    if (type == PingType.UDP) {
                        udpClient.sendPing(ping)
                    } else {
                        val message = com.skymouse.skymouseclient.proto.messageToServer {
                            this.ping = ping
                        }
                        tcpClient.sendProto(message)
                    }

                    pongDeferred.await()
                }
            }

            if (pong != null) {
                val rtt = System.currentTimeMillis() - startTime
                results.add(rtt)
                stateFlow.value = stateFlow.value.copy(lastPingMs = rtt)
            } else {
                results.add(null) // Packet loss or timeout
            }

            if (i < numPings - 1) delay(100.milliseconds)
        }

        val successful = results.filterNotNull()
        val stats = if (successful.isNotEmpty()) {
            PingStats(
                min = successful.minOrNull() ?: 0,
                max = successful.maxOrNull() ?: 0,
                avg = successful.average().toLong(),
                lossPercentage = ((numPings - successful.size).toFloat() / numPings) * 100f,
                isComplete = true
            )
        } else {
            PingStats(lossPercentage = 100f, isComplete = true)
        }

        stateFlow.value = stateFlow.value.copy(isRunning = false, stats = stats)
    }
}
