package com.rehealth.genie.ring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RingBackgroundCollectionPolicyTest {
    private val interval = 5 * 60 * 1000L

    @Test
    fun firstCollectionCanRunImmediately() {
        assertEquals(0L, RingBackgroundCollectionPolicy.nextDelayMillis(1_000L, null, interval))
        assertTrue(RingBackgroundCollectionPolicy.shouldCollect(1_000L, null, interval))
    }

    @Test
    fun recentAttemptWaitsForConservativeInterval() {
        val lastAttempt = 10_000L
        val now = lastAttempt + 60_000L

        assertEquals(
            interval - 60_000L,
            RingBackgroundCollectionPolicy.nextDelayMillis(now, lastAttempt, interval),
        )
        assertFalse(RingBackgroundCollectionPolicy.shouldCollect(now, lastAttempt, interval))
    }

    @Test
    fun staleAttemptCanCollectAgain() {
        val lastAttempt = 10_000L
        val now = lastAttempt + interval

        assertEquals(0L, RingBackgroundCollectionPolicy.nextDelayMillis(now, lastAttempt, interval))
        assertTrue(RingBackgroundCollectionPolicy.shouldCollect(now, lastAttempt, interval))
    }

    @Test
    fun bloodPressureUsesThirtyMinuteCooldown() {
        assertTrue(RingBackgroundCollectionPolicy.shouldMeasureBloodPressure(1_000L, null))
        assertFalse(RingBackgroundCollectionPolicy.shouldMeasureBloodPressure(31 * 60_000L, 2 * 60_000L))
        assertTrue(RingBackgroundCollectionPolicy.shouldMeasureBloodPressure(32 * 60_000L, 2 * 60_000L))
    }


    @Test
    fun bloodOxygenUsesFiveMinuteCooldown() {
        assertTrue(RingBackgroundCollectionPolicy.shouldMeasureBloodOxygen(1_000L, null))
        assertFalse(RingBackgroundCollectionPolicy.shouldMeasureBloodOxygen(5 * 60_000L, 60_000L))
        assertTrue(RingBackgroundCollectionPolicy.shouldMeasureBloodOxygen(6 * 60_000L, 60_000L))
    }
}
