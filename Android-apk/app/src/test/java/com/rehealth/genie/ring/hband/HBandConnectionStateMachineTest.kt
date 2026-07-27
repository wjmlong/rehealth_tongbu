package com.rehealth.genie.ring.hband

import com.rehealth.genie.ring.RingConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HBandConnectionStateMachineTest {
    @Test
    fun requiresNotifyPasswordCapabilitiesAndProfileBeforeReady() {
        val state = HBandConnectionStateMachine()
        state.startConnect()
        state.waitForNotify()
        state.verifyPassword()
        state.readCapabilities()
        state.syncProfile()
        state.ready()

        assertEquals(HBandConnectionPhase.READY, state.phase.value)
        assertEquals(RingConnectionState.CONNECTED, state.ringState)
    }

    @Test
    fun rejectsSkippingPasswordAndCapabilityHandshake() {
        val state = HBandConnectionStateMachine()
        state.startConnect()
        assertFailsWith<IllegalStateException> { state.ready() }
    }

    @Test
    fun failureIsNotReportedAsConnected() {
        val state = HBandConnectionStateMachine()
        state.startConnect()
        state.fail()
        assertEquals(RingConnectionState.ERROR, state.ringState)
        state.startConnect()
        assertEquals(RingConnectionState.CONNECTING, state.ringState)
    }
}
