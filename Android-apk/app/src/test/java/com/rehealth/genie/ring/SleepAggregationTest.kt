package com.rehealth.genie.ring

import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.time.LocalDateTime
import java.time.ZoneId
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

    @Test
    fun `cumulative snapshots from one night contribute only the final total`() {
        val firstWakeDay = LocalDateTime.of(2026, 7, 31, 8, 15)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val sessions = listOf(
            sleepSnapshot("first", firstWakeDay - 170 * 60_000L, 299),
            sleepSnapshot("second", firstWakeDay - 50 * 60_000L, 419),
            sleepSnapshot("final", firstWakeDay, 469),
        )

        assertEquals(469.0, averageDailySleepMinutes(sessions))
    }

    @Test
    fun `period average is calculated from one final result per wake day`() {
        val firstWakeDay = LocalDateTime.of(2026, 7, 31, 8, 15)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val previousWakeDay = firstWakeDay - 24 * 60 * 60_000L
        val sessions = listOf(
            sleepSnapshot("today-partial", firstWakeDay - 50 * 60_000L, 419),
            sleepSnapshot("today-final", firstWakeDay, 469),
            sleepSnapshot("previous-final", previousWakeDay, 420),
        )

        assertEquals(444.5, averageDailySleepMinutes(sessions))
    }

    @Test
    fun `vendor authoritative sleep wins duplicate cloud stages at same wake time`() {
        val wakeAt = LocalDateTime.of(2026, 8, 7, 7, 30)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val vendor = sleepSnapshot("vendor", wakeAt, 410)
        val cloud = vendor.copy(
            id = "cloud",
            source = "hband_cloud_restore",
            totalSleepMinutes = null,
            deepMinutes = 120,
            lightMinutes = 250,
            remMinutes = 80,
        )

        assertEquals("vendor", preferredSleepSession(listOf(cloud, vendor))?.id)
        assertEquals(410.0, averageDailySleepMinutes(listOf(cloud, vendor)))
    }

    private fun sleepSnapshot(id: String, endedAt: Long, totalMinutes: Int) =
        RingSleepSessionEntity(
            id = id,
            startedAt = endedAt - 60 * 60_000L,
            endedAt = endedAt,
            deepMinutes = 0,
            lightMinutes = 0,
            awakeMinutes = 0,
            remMinutes = 0,
            interruptionMinutes = 0,
            source = "hband_wearable",
            totalSleepMinutes = totalMinutes,
        )
}
