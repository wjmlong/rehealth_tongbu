package com.rehealth.genie.ring.hband

import com.rehealth.genie.ring.RingConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class HBandConnectionPhase {
    IDLE, SCANNING, CONNECTING, WAITING_NOTIFY, VERIFYING_PASSWORD,
    READING_CAPABILITIES, SYNCING_PROFILE, READY, SYNCING, ERROR,
}

internal class HBandConnectionStateMachine {
    private val mutablePhase = MutableStateFlow(HBandConnectionPhase.IDLE)
    val phase: StateFlow<HBandConnectionPhase> = mutablePhase.asStateFlow()

    val ringState: RingConnectionState
        get() = when (phase.value) {
            HBandConnectionPhase.IDLE -> RingConnectionState.DISCONNECTED
            HBandConnectionPhase.SCANNING -> RingConnectionState.SCANNING
            HBandConnectionPhase.CONNECTING,
            HBandConnectionPhase.WAITING_NOTIFY,
            HBandConnectionPhase.VERIFYING_PASSWORD,
            HBandConnectionPhase.READING_CAPABILITIES,
            HBandConnectionPhase.SYNCING_PROFILE,
            -> RingConnectionState.CONNECTING
            HBandConnectionPhase.READY -> RingConnectionState.CONNECTED
            HBandConnectionPhase.SYNCING -> RingConnectionState.SYNCING
            HBandConnectionPhase.ERROR -> RingConnectionState.ERROR
        }

    fun startScan() = transition(setOf(HBandConnectionPhase.IDLE, HBandConnectionPhase.READY, HBandConnectionPhase.ERROR), HBandConnectionPhase.SCANNING)
    fun finishScan(connected: Boolean) = transition(setOf(HBandConnectionPhase.SCANNING), if (connected) HBandConnectionPhase.READY else HBandConnectionPhase.IDLE)
    fun startConnect() = transition(setOf(HBandConnectionPhase.IDLE, HBandConnectionPhase.READY, HBandConnectionPhase.ERROR), HBandConnectionPhase.CONNECTING)
    fun waitForNotify() = transition(setOf(HBandConnectionPhase.CONNECTING), HBandConnectionPhase.WAITING_NOTIFY)
    fun verifyPassword() = transition(setOf(HBandConnectionPhase.WAITING_NOTIFY), HBandConnectionPhase.VERIFYING_PASSWORD)
    fun readCapabilities() = transition(setOf(HBandConnectionPhase.VERIFYING_PASSWORD), HBandConnectionPhase.READING_CAPABILITIES)
    fun syncProfile() = transition(setOf(HBandConnectionPhase.READING_CAPABILITIES), HBandConnectionPhase.SYNCING_PROFILE)
    fun ready() = transition(setOf(HBandConnectionPhase.SYNCING_PROFILE, HBandConnectionPhase.SYNCING), HBandConnectionPhase.READY)
    fun startSync() = transition(setOf(HBandConnectionPhase.READY), HBandConnectionPhase.SYNCING)
    fun recoverReady() { mutablePhase.value = HBandConnectionPhase.READY }
    fun disconnect() { mutablePhase.value = HBandConnectionPhase.IDLE }
    fun fail() { mutablePhase.value = HBandConnectionPhase.ERROR }

    private fun transition(from: Set<HBandConnectionPhase>, to: HBandConnectionPhase) {
        check(mutablePhase.value in from) { "Invalid HBand transition ${mutablePhase.value} -> $to" }
        mutablePhase.value = to
    }
}
