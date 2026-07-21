package com.rehealth.genie.ring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RingBackgroundCollectionPolicyTest {
    @Test
    fun firstCollectionCanRunImmediately() {
        assertEquals(0L, RingBackgroundCollectionPolicy.nextDelayMillis(1_000L, null))
        assertTrue(RingBackgroundCollectionPolicy.shouldCollect(1_000L, null))
    }

    @Test
    fun recentAttemptWaitsForConservativeInterval() {
        val lastAttempt = 10_000L
        val now = lastAttempt + 60_000L

        assertEquals(
            RingBackgroundCollectionPolicy.COLLECTION_INTERVAL_MS - 60_000L,
            RingBackgroundCollectionPolicy.nextDelayMillis(now, lastAttempt),
        )
        assertFalse(RingBackgroundCollectionPolicy.shouldCollect(now, lastAttempt))
    }

    @Test
    fun staleAttemptCanCollectAgain() {
        val lastAttempt = 10_000L
        val now = lastAttempt + RingBackgroundCollectionPolicy.COLLECTION_INTERVAL_MS

        assertEquals(0L, RingBackgroundCollectionPolicy.nextDelayMillis(now, lastAttempt))
        assertTrue(RingBackgroundCollectionPolicy.shouldCollect(now, lastAttempt))
    }
}
