package com.rehealth.genie.data.sync

import com.rehealth.genie.network.dto.RecentActivityDto
import com.rehealth.genie.network.dto.RecentMeasurementDto
import com.rehealth.genie.network.dto.RecentSleepSessionDto
import com.rehealth.genie.network.dto.RecentTelemetryResponseDto
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
    fun `maps recent cloud telemetry into stable scoped room records`() {
        val response = RecentTelemetryResponseDto(
            userId = "admin-user",
            limit = 200,
            measurements = listOf(
                RecentMeasurementDto(
                    id = "measurement-1",
                    deviceId = "ring-1",
                    metricType = "HEART_RATE",
                    measuredAt = 1_720_000_000_000L,
                    primaryValue = 72.0,
                    unit = "bpm",
                    qualityCode = "VALID",
                    source = "MRD",
                ),
            ),
            sleepSessions = listOf(
                RecentSleepSessionDto(
                    id = "sleep-1",
                    deviceId = "ring-1",
                    startedAt = 1_719_970_000_000L,
                    endedAt = 1_720_000_000_000L,
                    deepMinutes = 90,
                    lightMinutes = 280,
                    awakeMinutes = 20,
                    remMinutes = 110,
                    interruptionMinutes = 5,
                    source = "MRD",
                ),
            ),
            activities = listOf(
                RecentActivityDto(
                    id = "activity-1",
                    deviceId = "ring-1",
                    startedAt = 1_720_000_000_000L,
                    endedAt = 1_720_003_600_000L,
                    activityType = "DAILY",
                    steps = 8_500,
                    distanceMeters = 6_100.0,
                    caloriesKcal = 430.0,
                    durationMinutes = 60,
                    averageHeartRate = 105.0,
                    source = "MRD",
                ),
            ),
        )

        val batch = RingCloudRepository.telemetryRestoreBatch(response, "admin-user")

        assertEquals(3, batch.size)
        assertTrue(batch.measurements.single().id.startsWith("cloud-measurement-"))
        assertEquals("admin-user", batch.measurements.single().ownerUserId)
        assertEquals("ring-1", batch.measurements.single().deviceId)
        assertEquals(100, batch.measurements.single().quality)
        assertTrue(batch.sleepSessions.single().id.startsWith("cloud-sleep-"))
        assertEquals("admin-user", batch.sleepSessions.single().ownerUserId)
        assertEquals("ring-1", batch.sleepSessions.single().deviceId)
        assertTrue(batch.activities.single().id.startsWith("cloud-activity-"))
        assertEquals("admin-user", batch.activities.single().ownerUserId)
        assertEquals("ring-1", batch.activities.single().deviceId)

        val otherOwnerBatch = RingCloudRepository.telemetryRestoreBatch(response, "other-user")
        assertNotEquals(batch.measurements.single().id, otherOwnerBatch.measurements.single().id)
        assertNotEquals(batch.sleepSessions.single().id, otherOwnerBatch.sleepSessions.single().id)
        assertNotEquals(batch.activities.single().id, otherOwnerBatch.activities.single().id)
    }

    @Test
    fun `drops malformed recent telemetry and derives deterministic fallback ids`() {
        val valid = RecentMeasurementDto(
            deviceId = "ring-1",
            metricType = "BLOOD_OXYGEN",
            measuredAt = 1_720_000_000_000L,
            primaryValue = 98.0,
            unit = "%",
        )
        val response = RecentTelemetryResponseDto(
            measurements = listOf(
                valid,
                valid.copy(measuredAt = -1L),
                valid.copy(primaryValue = Double.NaN),
            ),
        )

        val first = RingCloudRepository.telemetryRestoreBatch(response, "admin-user")
        val second = RingCloudRepository.telemetryRestoreBatch(response, "admin-user")

        assertEquals(1, first.measurements.size)
        assertEquals(first.measurements.single().id, second.measurements.single().id)
        assertTrue(first.measurements.single().id.startsWith("cloud-measurement-"))
    }

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

    @Test
    fun `labels debug ring simulation as synthetic qa`() {
        val payload = RingCloudRepository.telemetryBatchPayload(
            device = RingDevice("QA:50:M:NORMAL", "QA ring", -42),
            collectedAt = 2L,
            trigger = "full_chain_qa_50m",
            measurements = listOf(
                RingMeasurementEntity(
                    id = "ring-sim-heart",
                    metricType = "HEART_RATE",
                    measuredAt = 1L,
                    primaryValue = 68.0,
                    unit = "bpm",
                    source = "ring_sim",
                ),
            ),
            sleep = null,
            activity = null,
            vendor = WearableVendor.MOCK,
        )

        assertEquals("synthetic_qa", payload.source)
        assertTrue(payload.deviceId.orEmpty().startsWith("mock-"))
    }
}
