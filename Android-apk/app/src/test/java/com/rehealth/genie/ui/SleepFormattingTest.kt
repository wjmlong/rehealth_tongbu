package com.rehealth.genie.ui

import com.rehealth.genie.ring.data.RingSleepSessionEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class SleepFormattingTest {
    @Test
    fun usesSessionSpanWhenDeviceOmitsStageBreakdown() {
        val startedAt = 1_700_000_000_000L
        val sleep = RingSleepSessionEntity(
            id = "sleep",
            startedAt = startedAt,
            endedAt = startedAt + 420 * 60_000L,
            deepMinutes = 0,
            lightMinutes = 0,
            awakeMinutes = 0,
            remMinutes = 0,
            interruptionMinutes = 0,
            source = "hband_wearable",
        )

        assertEquals(420, sleepDurationMinutes(sleep))
        assertEquals("7h0m", formatSleepMinutes(sleep))
    }
}
