package com.rehealth.genie.ring

object RingBackgroundCollectionPolicy {
    const val COLLECTION_INTERVAL_MS: Long = 15 * 60 * 1000L

    fun nextDelayMillis(nowMillis: Long, lastAttemptAtMillis: Long?): Long {
        val lastAttempt = lastAttemptAtMillis ?: return 0L
        val elapsed = (nowMillis - lastAttempt).coerceAtLeast(0L)
        return (COLLECTION_INTERVAL_MS - elapsed).coerceAtLeast(0L)
    }

    fun shouldCollect(nowMillis: Long, lastAttemptAtMillis: Long?): Boolean =
        nextDelayMillis(nowMillis, lastAttemptAtMillis) == 0L
}
