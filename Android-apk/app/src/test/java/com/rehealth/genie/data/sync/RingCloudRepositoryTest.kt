package com.rehealth.genie.data.sync

import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.provider.WearableVendor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RingCloudRepositoryTest {
    @Test
    fun `preserves hband advanced metric types in telemetry payload`() {
        val metricTypes = listOf("BLOOD_GLUCOSE", "TEMPERATURE", "STRESS", "MET")
        val measurements = metricTypes.mapIndexed { index, metricType ->
            RingMeasurementEntity(
                id = "hband-advanced-$index",
                metricType = metricType,
                measuredAt = 1_720_000_000_000L + index,
                primaryValue = index + 1.0,
                unit = "vendor_unit",
                source = "hband_wearable",
            )
        }

        val payload = RingCloudRepository.telemetryBatchPayload(
            device = RingDevice("11:22:33:44:55:77", "HBand", -40),
            collectedAt = 1_720_000_000_100L,
            trigger = "manual_sync",
            measurements = measurements,
            sleep = null,
            activity = null,
            vendor = WearableVendor.HBAND,
        )

        assertTrue(payload.deviceId.orEmpty().startsWith("hband-"))
        assertEquals("hband_room", payload.source)
        assertEquals(metricTypes, payload.measurements.orEmpty().map { it["metricType"] })
    }

    @Test
    fun `uses rwfit identity and provenance for rwfit telemetry`() {
        val measurement = RingMeasurementEntity(
            id = "rwfit-heart-1",
            metricType = "HEART_RATE",
            measuredAt = 1_720_000_000_000L,
            primaryValue = 70.0,
            unit = "bpm",
            source = "rwfit",
        )

        val payload = RingCloudRepository.telemetryBatchPayload(
            device = RingDevice("11:22:33:44:55:66", "RW Ring", -40),
            collectedAt = 1_720_000_000_100L,
            trigger = "manual_sync",
            measurements = listOf(measurement),
            sleep = null,
            activity = null,
            vendor = WearableVendor.RWFIT,
        )

        assertTrue(payload.deviceId.orEmpty().startsWith("rwfit-"))
        assertEquals("rwfit_room", payload.source)
    }

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
