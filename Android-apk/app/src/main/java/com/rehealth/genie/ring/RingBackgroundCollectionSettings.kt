package com.rehealth.genie.ring

import android.content.Context
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Background-collection settings, scoped per signed-in user.
 *
 * Every state key is derived from a SHA-256 hash of the user id so an account
 * switch can never inherit another account's enabled flag, intervals, or
 * per-metric cooldowns. Legacy unscoped values written by older builds are
 * claimed once by the first signed-in user and then removed, so existing
 * single-user devices keep their configuration without re-binding.
 */
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

    /** Presets must cover the contracted 3–60 minute measurement range. */
    val measurementIntervalOptions = listOf(3, 5, 10, 15, 30, 60)

    /** Presets must cover the contracted 30–1440 minute upload range. */
    val uploadIntervalOptions = listOf(30, 60, 120, 240, 720, 1440)

    fun isActive(context: Context, ownerUserId: String?): Boolean =
        claimLegacyBoolean(context, ownerUserId, KEY_ACTIVE)

    fun setActive(context: Context, ownerUserId: String?, active: Boolean) {
        val prefix = ownerPrefix(ownerUserId) ?: return
        preferences(context).edit().putBoolean(scopedKey(prefix, KEY_ACTIVE), active).apply()
    }

    fun lastAttemptAt(context: Context, ownerUserId: String?): Long? =
        claimLegacyLong(context, ownerUserId, KEY_LAST_ATTEMPT_AT).takeIf { it > 0L }

    fun markAttempt(context: Context, ownerUserId: String?, timestamp: Long) {
        val prefix = ownerPrefix(ownerUserId) ?: return
        preferences(context).edit().putLong(scopedKey(prefix, KEY_LAST_ATTEMPT_AT), timestamp).apply()
    }

    fun markSuccess(context: Context, ownerUserId: String?, timestamp: Long) {
        val prefix = ownerPrefix(ownerUserId) ?: return
        preferences(context).edit().putLong(scopedKey(prefix, KEY_LAST_SUCCESS_AT), timestamp).apply()
    }

    fun measurementIntervalMinutes(context: Context, ownerUserId: String?): Int =
        claimLegacyInt(context, ownerUserId, KEY_MEASUREMENT_INTERVAL_MINUTES)
            .coerceIn(MIN_MEASUREMENT_INTERVAL_MINUTES, MAX_MEASUREMENT_INTERVAL_MINUTES)

    fun setMeasurementIntervalMinutes(context: Context, ownerUserId: String?, minutes: Int) {
        require(minutes in MIN_MEASUREMENT_INTERVAL_MINUTES..MAX_MEASUREMENT_INTERVAL_MINUTES)
        val prefix = ownerPrefix(ownerUserId) ?: return
        preferences(context).edit()
            .putInt(scopedKey(prefix, KEY_MEASUREMENT_INTERVAL_MINUTES), minutes)
            .apply()
    }

    fun uploadIntervalMinutes(context: Context, ownerUserId: String?): Int =
        claimLegacyInt(context, ownerUserId, KEY_UPLOAD_INTERVAL_MINUTES)
            .coerceIn(MIN_UPLOAD_INTERVAL_MINUTES, MAX_UPLOAD_INTERVAL_MINUTES)

    fun setUploadIntervalMinutes(context: Context, ownerUserId: String?, minutes: Int) {
        require(minutes in MIN_UPLOAD_INTERVAL_MINUTES..MAX_UPLOAD_INTERVAL_MINUTES)
        val prefix = ownerPrefix(ownerUserId) ?: return
        preferences(context).edit()
            .putInt(scopedKey(prefix, KEY_UPLOAD_INTERVAL_MINUTES), minutes)
            .apply()
    }

    fun lastBloodPressureAt(context: Context, ownerUserId: String?): Long? =
        claimLegacyLong(context, ownerUserId, KEY_LAST_BLOOD_PRESSURE_AT).takeIf { it > 0L }

    fun markBloodPressureSuccess(context: Context, ownerUserId: String?, timestamp: Long) {
        val prefix = ownerPrefix(ownerUserId) ?: return
        preferences(context).edit().putLong(scopedKey(prefix, KEY_LAST_BLOOD_PRESSURE_AT), timestamp).apply()
    }

    fun lastBloodOxygenAt(context: Context, ownerUserId: String?): Long? =
        claimLegacyLong(context, ownerUserId, KEY_LAST_BLOOD_OXYGEN_AT).takeIf { it > 0L }

    fun markBloodOxygenSuccess(context: Context, ownerUserId: String?, timestamp: Long) {
        val prefix = ownerPrefix(ownerUserId) ?: return
        preferences(context).edit().putLong(scopedKey(prefix, KEY_LAST_BLOOD_OXYGEN_AT), timestamp).apply()
    }

    fun isCloudPlanActive(context: Context, ownerUserId: String?): Boolean {
        val prefix = ownerPrefix(ownerUserId) ?: return false
        val prefs = preferences(context)
        val scoped = scopedKey(prefix, "cloud_plan_active")
        if (prefs.contains(scoped)) return prefs.getBoolean(scoped, false)
        // Migrate the older per-user mirror key format once.
        val legacy = ownerUserId?.takeIf(String::isNotBlank)?.let { owner ->
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(owner.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            "cloud_plan_active_${digest.take(24)}"
        }
        if (legacy != null && prefs.contains(legacy)) {
            val value = prefs.getBoolean(legacy, false)
            prefs.edit().putBoolean(scoped, value).remove(legacy).apply()
            return value
        }
        return false
    }

    fun setCloudPlanActive(context: Context, ownerUserId: String?, active: Boolean) {
        val prefix = ownerPrefix(ownerUserId) ?: return
        preferences(context).edit().putBoolean(scopedKey(prefix, "cloud_plan_active"), active).apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun ownerPrefix(ownerUserId: String?): String? =
        ownerUserId?.takeIf(String::isNotBlank)?.let { owner ->
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(owner.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            "user_${digest.take(24)}"
        }

    private fun scopedKey(prefix: String, name: String) = "${prefix}_$name"

    /**
     * Reads a boolean setting for [ownerUserId]. When the per-user key is absent
     * and a legacy unscoped value exists, that value is claimed by this user
     * (written to the scoped key and removed from the legacy key) exactly once.
     */
    private fun claimLegacyBoolean(
        context: Context,
        ownerUserId: String?,
        legacyKey: String,
    ): Boolean {
        val prefix = ownerPrefix(ownerUserId) ?: return false
        val prefs = preferences(context)
        val scoped = scopedKey(prefix, legacyKey)
        if (prefs.contains(scoped)) return prefs.getBoolean(scoped, false)
        if (prefs.contains(legacyKey)) {
            val value = prefs.getBoolean(legacyKey, false)
            prefs.edit().putBoolean(scoped, value).remove(legacyKey).apply()
            return value
        }
        return false
    }

    private fun claimLegacyInt(
        context: Context,
        ownerUserId: String?,
        legacyKey: String,
    ): Int {
        val prefix = ownerPrefix(ownerUserId) ?: return when (legacyKey) {
            KEY_MEASUREMENT_INTERVAL_MINUTES -> DEFAULT_MEASUREMENT_INTERVAL_MINUTES
            KEY_UPLOAD_INTERVAL_MINUTES -> DEFAULT_UPLOAD_INTERVAL_MINUTES
            else -> 0
        }
        val prefs = preferences(context)
        val scoped = scopedKey(prefix, legacyKey)
        if (prefs.contains(scoped)) return prefs.getInt(scoped, 0)
        if (prefs.contains(legacyKey)) {
            val value = prefs.getInt(legacyKey, 0)
            prefs.edit().putInt(scoped, value).remove(legacyKey).apply()
            return value
        }
        return when (legacyKey) {
            KEY_MEASUREMENT_INTERVAL_MINUTES -> DEFAULT_MEASUREMENT_INTERVAL_MINUTES
            KEY_UPLOAD_INTERVAL_MINUTES -> DEFAULT_UPLOAD_INTERVAL_MINUTES
            else -> 0
        }
    }

    private fun claimLegacyLong(
        context: Context,
        ownerUserId: String?,
        legacyKey: String,
    ): Long {
        val prefix = ownerPrefix(ownerUserId) ?: return 0L
        val prefs = preferences(context)
        val scoped = scopedKey(prefix, legacyKey)
        if (prefs.contains(scoped)) return prefs.getLong(scoped, 0L)
        if (prefs.contains(legacyKey)) {
            val value = prefs.getLong(legacyKey, 0L)
            prefs.edit().putLong(scoped, value).remove(legacyKey).apply()
            return value
        }
        return 0L
    }
}
