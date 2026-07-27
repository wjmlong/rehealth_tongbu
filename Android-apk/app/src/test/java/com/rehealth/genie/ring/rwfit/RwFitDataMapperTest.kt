package com.rehealth.genie.ring.rwfit

import com.rehealth.genie.ring.RingMetricType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RwFitDataMapperTest {
    @Test
    fun createsDeterministicVendorNeutralEntitiesWithoutRawPayload() {
        val payload = RwFitPayload(
            measurements = listOf(
                RwFitMetricSample(RingMetricType.HEART_RATE, 1_700_000_000_000L, 71.0, "bpm"),
            ),
        )

        val first = RwFitDataMapper.toEntities(payload, "AA:BB:CC:DD:EE:FF")
        val second = RwFitDataMapper.toEntities(payload, "aa:bb:cc:dd:ee:ff")

        assertEquals(first.measurements.single().id, second.measurements.single().id)
        assertEquals("rwfit", first.measurements.single().source)
        assertNull(first.measurements.single().rawPayload)
        assertEquals(setOf(RingMetricType.HEART_RATE), RwFitDataMapper.collectedTypes(first))
    }

    @Test
    fun activityAndSleepUseExistingRoomTables() {
        val batch = RwFitDataMapper.toEntities(
            RwFitPayload(
                sleep = listOf(RwFitSleepRecord(1000, 2000, 10, 20, 5)),
                activities = listOf(RwFitActivityRecord(3000, 4000, 500, 600.0, 7.0, 60)),
            ),
            "device",
        )

        assertEquals(1, batch.sleepSessions.size)
        assertEquals(1, batch.activities.size)
        assertEquals(0, batch.sleepSessions.single().remMinutes)
        assertTrue(RingMetricType.SLEEP in RwFitDataMapper.collectedTypes(batch))
        assertTrue(RingMetricType.STEPS in RwFitDataMapper.collectedTypes(batch))
        assertTrue(RingMetricType.ACTIVITY in RwFitDataMapper.collectedTypes(batch))
    }
}
