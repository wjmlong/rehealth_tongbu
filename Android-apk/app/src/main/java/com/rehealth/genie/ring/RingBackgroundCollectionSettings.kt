package com.rehealth.genie.ring

import android.content.Context

object RingBackgroundCollectionSettings {
    private const val PREFERENCES_NAME = "ring_background_collection"
    private const val KEY_ACTIVE = "active"
    private const val KEY_LAST_ATTEMPT_AT = "last_attempt_at"
    private const val KEY_LAST_SUCCESS_AT = "last_success_at"

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

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
