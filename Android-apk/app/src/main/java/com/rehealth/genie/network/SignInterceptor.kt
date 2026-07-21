package com.rehealth.genie.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.security.MessageDigest
import java.util.TreeMap

/**
 * Signs JeecgBoot requests that require the `X-Sign` header (currently `/sys/sms`).
 *
 * Algorithm (mirrors the backend `SignAuthInterceptor` + `SignUtil` and the JeecgBoot
 * Vue frontend `signMd5Utils`):
 *   - Collect all request params (here: the JSON body), sort keys ascending.
 *   - Serialize as compact JSON: `{"k1":v1,"k2":v2}` (no spaces; null values skipped,
 *     matching fastjson's default which omits nulls).
 *   - `X-Sign = md5(json + signatureSecret).uppercase()`
 *   - `X-Timestamp = System.currentTimeMillis()` (unix millis; the backend accepts both
 *     14-digit yyyyMMddHHmmss and unix-millis formats).
 *
 * Only requests whose path ends with `/sys/sms` are signed; all others pass through
 * untouched (e.g. `/sys/user/register` and `/sys/mLogin` do not require signing).
 */
class SignInterceptor(private val secret: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.encodedPath.endsWith("/sys/sms")) {
            return chain.proceed(request)
        }
        val body = request.body ?: return chain.proceed(request)

        val buffer = Buffer()
        body.writeTo(buffer)
        val jsonBytes = buffer.readByteArray()
        val jsonStr = buildSortedJson(jsonBytes)
        val sign = md5Hex(jsonStr + secret).uppercase()
        val timestamp = System.currentTimeMillis().toString()

        val newBody = jsonBytes.toRequestBody(body.contentType() ?: "application/json".toMediaTypeOrNull())
        val signed = request.newBuilder()
            .method(request.method, newBody)
            .header("X-Sign", sign)
            .header("X-Timestamp", timestamp)
            .build()
        return chain.proceed(signed)
    }

    private fun buildSortedJson(bytes: ByteArray): String {
        val obj = JSONObject(String(bytes, Charsets.UTF_8))
        val map = TreeMap<String, Any?>()
        obj.keys().forEach { map[it] = obj.opt(it) }
        val sb = StringBuilder("{")
        var first = true
        for ((k, v) in map) {
            if (v == null || v === JSONObject.NULL) continue
            if (!first) sb.append(",")
            first = false
            sb.append("\"").append(k).append("\":")
            sb.append(jsonValue(v))
        }
        sb.append("}")
        return sb.toString()
    }

    /**
     * Serialize a body value the way fastjson serializes a `Map<String,String>` on the
     * backend: strings are quoted/escaped, booleans/numbers are emitted raw. Registration
     * SMS bodies are flat string maps, so this covers every value we sign.
     */
    private fun jsonValue(v: Any?): String = when (v) {
        null -> "null"
        is String -> "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Boolean, is Number -> v.toString()
        else -> "\"" + v.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    private fun md5Hex(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
