package com.rehealth.genie.network

import android.content.Context
import java.security.MessageDigest

/**
 * Stores the first-run health interview state per authenticated account.
 *
 * Existing accounts are complete by default. Registration explicitly marks the
 * new account pending, so logging in on this device never re-opens onboarding
 * for an established user just because another account has not completed it.
 */
class OnboardingStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun requiresOnboarding(userId: String?, username: String?): Boolean =
        prefs.getBoolean(key(userId, username), false)

    fun markRequired(userId: String?, username: String?) {
        prefs.edit().putBoolean(key(userId, username), true).apply()
    }

    fun markComplete(userId: String?, username: String?) {
        prefs.edit().putBoolean(key(userId, username), false).apply()
    }

    private fun key(userId: String?, username: String?): String {
        val identity = userId?.trim()?.takeIf(String::isNotEmpty)
            ?: username?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
            ?: ANONYMOUS_IDENTITY
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
        return KEY_PREFIX + digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val PREFERENCES_NAME = "rehealth_onboarding"
        const val KEY_PREFIX = "required_"
        const val ANONYMOUS_IDENTITY = "anonymous"
    }
}
