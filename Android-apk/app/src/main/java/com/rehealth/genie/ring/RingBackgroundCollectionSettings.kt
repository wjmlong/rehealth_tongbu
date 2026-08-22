package com.rehealth.genie.ring

import android.content.Context
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object RingBackgroundCollectionSettings {
    private const val PREFERENCES_NAME = "ring_background_collection"
    private const val KEY_ACTIVE = "active"
    private const val KEY_LAST_ATTEMPT_AT = "last_attempt_at"
    private const val KEY_LAST_SUCCESS_AT = "last_success_at"
    private const val KEY_MEASUREMENT_INTERVAL_MINUTES = "measurement_interval_minutes"
    private const val KEY_UPLOAD_INTERVAL_MINUTES = "upload_interval_minutes"
    private const val KEY_LAST_BLOOD_PRESSURE_AT = "last_blood_pressure_at"
    private const val KEY_LAST_BLOOD_OXYGEN_AT = "last_blood_oxygen_at"

    const val DEFAULT_MEASUREMENT_INTERVAL_MINUTES = 5
    const val MIN_MEASUREMENT_INTERVAL_MINUTES = 3
    const val MAX_MEASUREMENT_INTERVAL_MINUTES = 60
    const val DEFAULT_UPLOAD_INTERVAL_MINUTES = 120
    const val MIN_UPLOAD_INTERVAL_MINUTES = 30
    const val MAX_UPLOAD_INTERVAL_MINUTES = 24 * 60

    val measurementIntervalOptions = listOf(3, 5, 10, 15)
    val uploadIntervalOptions = listOf(30, 60, 120, 240)

    fun isActive(context: Context): Boolean =
        preferences(context).getBoolean(KEY_ACTIVE, false)

    fun setActive(context: Context, active: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_ACTIVE, active)
            .apply()
    }

    fun lastAttemptAt(context: Context): Long? =
        preferences(context).getLong(KEY_LAST_ATTEMPT_AT, 0L).takeIf { it > 0L }

    fun markAttempt(context: Context, timestamp: Long) {
        preferences(context).edit()
            .putLong(KEY_LAST_ATTEMPT_AT, timestamp)
            .apply()
    }

    fun markSuccess(context: Context, timestamp: Long) {
        preferences(context).edit()
            .putLong(KEY_LAST_SUCCESS_AT, timestamp)
            .apply()
    }

    fun measurementIntervalMinutes(context: Context): Int =
        preferences(context).getInt(
            KEY_MEASUREMENT_INTERVAL_MINUTES,
            DEFAULT_MEASUREMENT_INTERVAL_MINUTES,
        ).coerceIn(MIN_MEASUREMENT_INTERVAL_MINUTES, MAX_MEASUREMENT_INTERVAL_MINUTES)

    fun setMeasurementIntervalMinutes(context: Context, minutes: Int) {
        require(minutes in MIN_MEASUREMENT_INTERVAL_MINUTES..MAX_MEASUREMENT_INTERVAL_MINUTES)
        preferences(context).edit().putInt(KEY_MEASUREMENT_INTERVAL_MINUTES, minutes).apply()
    }

    fun uploadIntervalMinutes(context: Context): Int =
        preferences(context).getInt(
            KEY_UPLOAD_INTERVAL_MINUTES,
            DEFAULT_UPLOAD_INTERVAL_MINUTES,
        ).coerceIn(MIN_UPLOAD_INTERVAL_MINUTES, MAX_UPLOAD_INTERVAL_MINUTES)

    fun setUploadIntervalMinutes(context: Context, minutes: Int) {
        require(minutes in MIN_UPLOAD_INTERVAL_MINUTES..MAX_UPLOAD_INTERVAL_MINUTES)
        preferences(context).edit().putInt(KEY_UPLOAD_INTERVAL_MINUTES, minutes).apply()
    }

    fun lastBloodPressureAt(context: Context): Long? =
        preferences(context).getLong(KEY_LAST_BLOOD_PRESSURE_AT, 0L).takeIf { it > 0L }

    fun markBloodPressureSuccess(context: Context, timestamp: Long) {
        preferences(context).edit().putLong(KEY_LAST_BLOOD_PRESSURE_AT, timestamp).apply()
    }

    fun lastBloodOxygenAt(context: Context): Long? =
        preferences(context).getLong(KEY_LAST_BLOOD_OXYGEN_AT, 0L).takeIf { it > 0L }

    fun markBloodOxygenSuccess(context: Context, timestamp: Long) {
        preferences(context).edit().putLong(KEY_LAST_BLOOD_OXYGEN_AT, timestamp).apply()
    }

    fun isCloudPlanActive(context: Context, ownerUserId: String?): Boolean =
        ownerUserId?.takeIf(String::isNotBlank)?.let { owner ->
            preferences(context).getBoolean(cloudPlanKey(owner), false)
        } ?: false

    fun setCloudPlanActive(context: Context, ownerUserId: String?, active: Boolean) {
        val owner = ownerUserId?.takeIf(String::isNotBlank) ?: return
        preferences(context).edit().putBoolean(cloudPlanKey(owner), active).apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun cloudPlanKey(ownerUserId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(ownerUserId.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "cloud_plan_active_${digest.take(24)}"
    }
}
