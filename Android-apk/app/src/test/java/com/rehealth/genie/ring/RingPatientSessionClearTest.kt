package com.rehealth.genie.ring

import com.rehealth.genie.network.PatientMvpPayload
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RingPatientSessionClearTest {
    @Test
    fun initialStateDoesNotSynthesizeRiskOrInterventionPlan() {
        val state = RingUiState()

        assertNull(state.patientMvp)
        assertNull(state.cloudRiskScore)
        assertNull(state.cloudRiskLevel)
        assertNull(state.cloudRiskMode)
        assertNull(state.cloudRiskSummary)
    }

    @Test
    fun logoutClearsAllPatientOwnedHealthState() {
        val measurement = RingMeasurementEntity("m", "HEART_RATE", 1L, 70.0, unit = "bpm", source = "test")
        val sleep = RingSleepSessionEntity("s", 1L, 2L, 1, 0, 0, 0, 0, "test")
        val activity = RingActivityEntity("a", 1L, 2L, "walk", 100, 20.0, 5.0, 1, null, "test")
        val signal = RingSignalChunkEntity("e", "ECG", 1L, 100, 1, payload = byteArrayOf(1), source = "test")
        val populated = RingUiState(
            isSyncing = true,
            lastSyncAt = 2L,
            cloudSnapshotId = "snapshot",
            cloudRiskLevel = "high",
            cloudRiskScore = 0.8,
            patientMvp = PatientMvpPayload(null, null, emptyList(), emptyList(), 2L),
            measurements = mapOf(RingMetricType.HEART_RATE to measurement),
            sleep = sleep,
            activity = activity,
            todayActivitySteps = 100,
            signals = mapOf(RingMetricType.ECG to signal),
            ecgHistory = listOf(signal),
            liveEcg = RingEcgLiveState(samplesMv = floatArrayOf(0.1f)),
            hasBoundBluetoothDevice = true,
            backgroundCollectionEnabled = true,
        )

        val cleared = populated.clearedForPatientSession()

        assertTrue(cleared.measurements.isEmpty())
        assertTrue(cleared.signals.isEmpty())
        assertTrue(cleared.ecgHistory.isEmpty())
        assertTrue(cleared.liveEcg.samplesMv.isEmpty())
        assertNull(cleared.sleep)
        assertNull(cleared.activity)
        assertNull(cleared.todayActivitySteps)
        assertNull(cleared.patientMvp)
        assertNull(cleared.cloudRiskScore)
        assertNull(cleared.lastSyncAt)
        assertEquals(false, cleared.isSyncing)
        assertEquals(false, cleared.hasBoundBluetoothDevice)
        assertEquals(false, cleared.backgroundCollectionEnabled)
    }
}
