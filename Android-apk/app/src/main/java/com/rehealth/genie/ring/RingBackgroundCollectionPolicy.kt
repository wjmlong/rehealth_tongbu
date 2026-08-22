package com.rehealth.genie.ring

object RingBackgroundCollectionPolicy {
    const val RECOVERY_INTERVAL_MINUTES: Long = 15L
    const val BLOOD_PRESSURE_COOLDOWN_MS: Long = 30 * 60 * 1000L
    const val BLOOD_OXYGEN_COOLDOWN_MS: Long = 5 * 60 * 1000L

    fun nextDelayMillis(
        nowMillis: Long,
        lastAttemptAtMillis: Long?,
        intervalMillis: Long,
    ): Long {
        require(intervalMillis > 0L)
        val lastAttempt = lastAttemptAtMillis ?: return 0L
        val elapsed = (nowMillis - lastAttempt).coerceAtLeast(0L)
        return (intervalMillis - elapsed).coerceAtLeast(0L)
    }

    fun shouldCollect(nowMillis: Long, lastAttemptAtMillis: Long?, intervalMillis: Long): Boolean =
        nextDelayMillis(nowMillis, lastAttemptAtMillis, intervalMillis) == 0L

    fun shouldMeasureBloodPressure(nowMillis: Long, lastSuccessAtMillis: Long?): Boolean =
        lastSuccessAtMillis == null || nowMillis - lastSuccessAtMillis >= BLOOD_PRESSURE_COOLDOWN_MS

    fun shouldMeasureBloodOxygen(nowMillis: Long, lastSuccessAtMillis: Long?): Boolean =
        lastSuccessAtMillis == null || nowMillis - lastSuccessAtMillis >= BLOOD_OXYGEN_COOLDOWN_MS
}
