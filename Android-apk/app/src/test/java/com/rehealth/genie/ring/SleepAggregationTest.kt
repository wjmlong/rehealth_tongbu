package com.rehealth.genie.ring

import com.rehealth.genie.ring.data.RingSleepSessionEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class SleepAggregationTest {
    @Test
    fun `actual sleep stages exclude awake minutes`() {
        val session = RingSleepSessionEntity(
            id = "sleep-1",
            startedAt = 1_000L,
            endedAt = 10 * 60 * 60 * 1_000L,
            deepMinutes = 90,
            lightMinutes = 230,
            awakeMinutes = 20,
            remMinutes = 70,
            interruptionMinutes = 15,
            source = "test",
        )

        assertEquals(390, canonicalSleepMinutes(session))
    }

    @Test
    fun `vendor total wins over stages and wall clock span`() {
        val session = RingSleepSessionEntity(
            id = "sleep-vendor-total",
            startedAt = 1_000L,
            endedAt = 10 * 60 * 60 * 1_000L,
            deepMinutes = 90,
            lightMinutes = 230,
            awakeMinutes = 40,
            remMinutes = 70,
            interruptionMinutes = 40,
            source = "hband_wearable",
            totalSleepMinutes = 375,
        )

        assertEquals(375, canonicalSleepMinutes(session))
    }

    @Test
    fun `wall clock span is used only when device omitted stages`() {
        val session = RingSleepSessionEntity(
            id = "sleep-2",
            startedAt = 1_000L,
            endedAt = 1_000L + 420 * 60_000L,
            deepMinutes = 0,
            lightMinutes = 0,
            awakeMinutes = 0,
            remMinutes = 0,
            interruptionMinutes = 0,
            source = "test",
        )

        assertEquals(420, canonicalSleepMinutes(session))
    }
}
