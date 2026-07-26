package com.rehealth.genie.data.sync

import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.data.RingMeasurementEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RingCloudRepositoryTest {
    @Test
    fun `maps persisted room measurement into stable privacy safe telemetry batch`() {
        val device = RingDevice(
            address = "AA:BB:CC:DD:EE:FF",
            name = "MRD Ring",
            rssi = -55,
        )
        val measurement = RingMeasurementEntity(
            id = "measurement-1",
            metricType = "HEART_RATE",
            measuredAt = 1_720_000_000_000L,
            primaryValue = 72.0,
            unit = "bpm",
            quality = 95,
            source = "mrd_ring",
            rawPayload = "must-not-upload",
        )

        val first = RingCloudRepository.telemetryBatchPayload(
            device = device,
            collectedAt = 1_720_000_000_100L,
            trigger = "manual_sync",
            measurements = listOf(measurement),
            sleep = null,
            activity = null,
        )
        val second = RingCloudRepository.telemetryBatchPayload(
            device = device,
            collectedAt = 1_720_000_000_100L,
            trigger = "manual_sync",
            measurements = listOf(measurement),
            sleep = null,
            activity = null,
        )

        assertEquals(first.batchId, second.batchId)
        assertTrue(first.deviceId.orEmpty().startsWith("mrd-"))
        assertFalse(first.deviceId.orEmpty().contains(device.address))
        assertEquals("mrd_room", first.source)
        assertEquals(72.0, first.measurements?.single()?.get("primaryValue"))
        assertFalse(first.measurements?.single()?.containsKey("rawPayload") == true)
        assertTrue(first.signalChunks.isNullOrEmpty())
        assertEquals(true, first.quality?.get("rawSignalExcluded"))
    }

    @Test
    fun `labels synthetic source and changes batch identity for a new trigger`() {
        val device = RingDevice("AA:BB", "Synthetic Ring", -40)
        val measurement = RingMeasurementEntity(
            id = "measurement-qa",
            metricType = "BLOOD_OXYGEN",
            measuredAt = 1L,
            primaryValue = 98.0,
            unit = "%",
            source = "synthetic_qa",
        )

        val automatic = RingCloudRepository.telemetryBatchPayload(
            device,
            collectedAt = 2L,
            trigger = "auto_collection",
            measurements = listOf(measurement),
            sleep = null,
            activity = null,
        )
        val manual = RingCloudRepository.telemetryBatchPayload(
            device,
            collectedAt = 2L,
            trigger = "manual_sync",
            measurements = listOf(measurement),
            sleep = null,
            activity = null,
        )

        assertEquals("synthetic_qa", automatic.source)
        assertNotEquals(automatic.batchId, manual.batchId)
    }
}
