package com.rehealth.genie.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted, local-first store for the JeecgBoot APP token.
 * Token is never logged and only sent via [AuthInterceptor] as `X-Access-Token`.
 * On a 401 the caller must call [clear] and force re-login (no refresh token exists).
 */
class SessionStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "rehealth_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()

    fun clear() = prefs.edit().clear().apply()

    /** First time the app was launched (real local anchor for "已陪伴 X 天"). Recorded once, never reset. */
    var firstUseAt: Long?
        get() = prefs.getLong(KEY_FIRST_USE, 0L).takeIf { it > 0L }
        set(value) = prefs.edit().putLong(KEY_FIRST_USE, value ?: 0L).apply()

    /** Days since first use, computed from [firstUseAt] (recorded on first call). Never a canned value. */
    fun firstUseDays(): Int {
        val now = System.currentTimeMillis()
        val first = firstUseAt ?: now.also { firstUseAt = it }
        return ((now - first) / DAY_MS).coerceAtLeast(0L).toInt()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FIRST_USE = "first_use_at"
        private const val DAY_MS = 86_400_000L
    }
}
