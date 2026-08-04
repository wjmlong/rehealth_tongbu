package com.rehealth.genie.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Configuration and shared OkHttp factory for the E1 mobile API client.
 *
 * The base URL is injected per build type. Release configuration is validated by
 * Gradle before an APK or app bundle can be packaged.
 */
object BackendConfig {
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
