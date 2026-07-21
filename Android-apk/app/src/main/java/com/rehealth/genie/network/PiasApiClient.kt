package com.rehealth.genie.network

import com.google.gson.Gson
import com.rehealth.genie.network.dto.IndividualAttributionRequestDto
import com.rehealth.genie.network.dto.IndividualAttributionResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Stateless client for the real PIAS service. It deliberately has no mobile auth
 * interceptor because the model-service endpoint is hosted separately from JeecgBoot.
 */
class PiasApiClient(
    private val baseUrl: String,
    private val httpClient: OkHttpClient,
    private val gson: Gson = Gson(),
) {
    suspend fun attributeIndividual(
        request: IndividualAttributionRequestDto,
    ): Result<IndividualAttributionResponseDto> = withContext(Dispatchers.IO) {
        runCatching {
            val httpRequest = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/attribute/individual")
                .header("Accept", "application/json")
                .post(gson.toJson(request).toRequestBody(JSON))
                .build()
            httpClient.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("PIAS returned HTTP ${response.code}")
                }
                val envelope = gson.fromJson(body, PiasEnvelope::class.java)
                    ?: throw IOException("PIAS returned an empty response")
                if (!envelope.success) {
                    throw IOException(envelope.message ?: "PIAS analysis failed")
                }
                envelope.result ?: throw IOException("PIAS returned no attribution result")
            }
        }
    }

    private data class PiasEnvelope(
        val success: Boolean = false,
        val message: String? = null,
        val result: IndividualAttributionResponseDto? = null,
    )

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
