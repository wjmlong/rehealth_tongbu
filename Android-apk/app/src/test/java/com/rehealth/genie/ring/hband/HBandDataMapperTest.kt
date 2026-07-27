package com.rehealth.genie.ring.hband

import com.rehealth.genie.ring.RingMetricType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HBandDataMapperTest {
    @Test
    fun createsStableVendorNeutralRecordsInExistingTables() {
        val payload = HBandPayload(
            measurements = listOf(HBandMetricSample(RingMetricType.HEART_RATE, 1_700_000_000_000L, 72.0, "bpm")),
            sleep = listOf(HBandSleepRecord(1_700_000_000_000L, 1_700_020_000_000L, 100, 180, 20)),
            activities = listOf(HBandActivityRecord(1_700_000_000_000L, 1_700_010_000_000L, 1234, 800.0, 40.0)),
        )
        val first = HBandDataMapper.toEntities(payload, "AA:BB")
        val second = HBandDataMapper.toEntities(payload, "aa:bb")

        assertEquals(first.measurements.single().id, second.measurements.single().id)
        assertEquals("hband_wearable", first.measurements.single().source)
        assertEquals("hband_wearable", first.sleepSessions.single().source)
        assertEquals("hband_wearable", first.activities.single().source)
        assertNull(first.measurements.single().rawPayload)
        assertTrue(setOf(RingMetricType.HEART_RATE, RingMetricType.SLEEP, RingMetricType.STEPS, RingMetricType.ACTIVITY)
            .all { it in HBandDataMapper.collectedTypes(first) })
    }

    @Test
    fun dropsInvalidOrZeroTelemetryInsteadOfCreatingSyntheticMeasurements() {
        val batch = HBandDataMapper.toEntities(
            HBandPayload(
                measurements = listOf(HBandMetricSample(RingMetricType.HEART_RATE, 1000, 0.0, "bpm")),
                activities = listOf(HBandActivityRecord(1000, 2000, 0, 0.0, 0.0)),
            ),
            "device",
        )
        assertEquals(0, batch.size)
    }
}
