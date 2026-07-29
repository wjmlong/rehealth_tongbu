package com.rehealth.genie.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Configuration and shared OkHttp factory for the E1 mobile API client.
 *
 * Base URL is configurable; the committed debug default points at the internal (alpha)
 * test backend via the `rehealth.api.base.url` property in `gradle.properties`
 * (overridable per-machine via `local.properties` or the `REHEALTH_API_BASE_URL` env var):
 *
 *   https://rehealth.youngjimmy.store/jeecg-boot
 *
 * For local emulator dev without the tunnel, override with the emulator loopback address
 * for the local JeecgBoot instance, e.g. http://10.0.2.2:8080/jeecg-boot.
 *
 * D1 does not persist tokens or implement token refresh; [authToken] is taken from
 * BuildConfig and may be blank (JeecgBoot local dev tolerates missing tokens for the
 * mobile endpoints used here except where @IgnoreAuth applies).
 */
object BackendConfig {
    const val DEFAULT_BASE_URL: String = "https://rehealth.youngjimmy.store/jeecg-boot"

    fun buildHttpClient(
        connectTimeoutSeconds: Long = 15L,
        readTimeoutSeconds: Long = 20L,
        writeTimeoutSeconds: Long = 20L,
        enableVerboseLogging: Boolean = false,
        signSecret: String? = null,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
        // SignInterceptor is path-gated: it only attaches X-Sign/X-Timestamp to /sys/sms,
        // so it is safe to install on the shared client used by every endpoint.
        if (!signSecret.isNullOrBlank()) {
            builder.addInterceptor(SignInterceptor(signSecret))
        }
        if (enableVerboseLogging) {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logger)
        }
        return builder.build()
    }

    fun normalizeBaseUrl(baseUrl: String): String {
        require(baseUrl.isNotBlank()) { "ReHealth backend base URL must not be blank." }
        val normalized = baseUrl.trimEnd('/')
        normalized.toHttpUrl()
        return normalized
    }
}
