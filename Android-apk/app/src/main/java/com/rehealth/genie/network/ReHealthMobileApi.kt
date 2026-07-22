package com.rehealth.genie.network

import com.rehealth.genie.network.dto.FeatureEvaluateRequest
import com.rehealth.genie.network.dto.HealthCheckResponse
import com.rehealth.genie.network.dto.HealthInterviewSubmitRequestDto
import com.rehealth.genie.network.dto.InterventionFeedbackRequest
import com.rehealth.genie.network.dto.InterventionFeedbackResponse
import com.rehealth.genie.network.dto.InterventionPlanDto
import com.rehealth.genie.network.dto.IndividualAttributionRequestDto
import com.rehealth.genie.network.dto.IndividualAttributionResponseDto
import com.rehealth.genie.network.dto.MobileConfigResponse
import com.rehealth.genie.network.dto.MobileLoginRequest
import com.rehealth.genie.network.dto.MobileLoginResponse
import com.rehealth.genie.network.dto.RegisterRequest
import com.rehealth.genie.network.dto.RiskResultDto
import com.rehealth.genie.network.dto.SendSmsRequest
import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import com.rehealth.genie.network.dto.TelemetryBatchResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

/**
 * Typed E1 mobile API client built on Retrofit/Moshi.
 *
 * Responsibilities:
 *  - Build the Retrofit instance with the configured [baseUrl], [httpClient], and auth token.
 *  - Unwrap the JeecgBoot `Result` envelope and surface typed payloads.
 *  - Translate HTTP/timeout/JSON errors into [RemotePhmError] for the PHM layer.
 *
 * Raw signal streaming remains outside this typed E2.1 measurement batch path.
 */
class ReHealthMobileApi(
    baseUrl: String,
    private val httpClient: OkHttpClient,
    private val apiToken: String? = null,
) {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = apiToken?.takeIf { it.isNotBlank() }
        if (token != null && request.header("X-Access-Token") == null) {
            chain.proceed(request.newBuilder().header("X-Access-Token", token).build())
        } else {
            chain.proceed(request)
        }
    }

    private val authenticatedClient = httpClient.newBuilder().addInterceptor(authInterceptor).build()

    private val api: ReHealthApi = Retrofit.Builder()
        .baseUrl(BackendConfig.normalizeBaseUrl(baseUrl) + "/")
        .client(authenticatedClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .build()
        .create(ReHealthApi::class.java)

    suspend fun getHealth(): RemotePhmOutcome<HealthCheckResponse> =
        unwrap { api.getHealth() }

    suspend fun getConfig(): RemotePhmOutcome<MobileConfigResponse> =
        unwrap { api.getConfig() }

    suspend fun evaluateFeatures(
        request: FeatureEvaluateRequest,
    ): RemotePhmOutcome<RiskResultDto> =
        unwrap { api.evaluateFeatures(request) }

    suspend fun getRiskLatest(): RemotePhmOutcome<RiskResultDto?> =
        unwrapNullable { api.getRiskLatest() }

    suspend fun getInterventionsToday(): RemotePhmOutcome<InterventionPlanDto?> =
        unwrapNullable { api.getInterventionsToday() }

    suspend fun submitInterventionFeedback(
        interventionId: String,
        request: InterventionFeedbackRequest,
    ): RemotePhmOutcome<InterventionFeedbackResponse> =
        unwrap { api.submitInterventionFeedback(interventionId, request) }

    suspend fun uploadMeasurements(
        request: TelemetryBatchRequestDto,
    ): RemotePhmOutcome<TelemetryBatchResponseDto> =
        unwrap { api.uploadMeasurements(request) }

    suspend fun submitHealthInterview(
        request: HealthInterviewSubmitRequestDto,
    ): RemotePhmOutcome<HealthInterviewSubmitRequestDto> =
        unwrap { api.submitHealthInterview(request) }

    suspend fun getLatestHealthInterview(): RemotePhmOutcome<HealthInterviewSubmitRequestDto?> =
        unwrapNullable { api.getLatestHealthInterview() }

    suspend fun attributeIndividual(
        request: IndividualAttributionRequestDto,
    ): RemotePhmOutcome<IndividualAttributionResponseDto> =
        unwrap { api.attributeIndividual(request) }

    /**
     * JeecgBoot system login. No auth token is attached (the auth interceptor only adds
     * `X-Access-Token` when a non-blank token is present). Maps the `JeecgResult`
     * envelope into [RemotePhmOutcome].
     */
    suspend fun mobileLogin(request: MobileLoginRequest): RemotePhmOutcome<MobileLoginResponse> =
        unwrap { api.mobileLogin(request) }

    /**
     * Send a registration SMS. The shared client carries [SignInterceptor], which signs
     * this `/sys/sms` request with `X-Sign`/`X-Timestamp`.
     */
    suspend fun sendSms(mobile: String): RemotePhmOutcome<Unit> =
        unwrapUnit { api.sendSms(SendSmsRequest(mobile = mobile, smsmode = "1")) }

    /**
     * Register a new account. Public endpoint (no sign, no token). After this succeeds the
     * caller typically auto-logs-in via [mobileLogin].
     */
    suspend fun register(
        phone: String,
        smscode: String,
        username: String,
        password: String,
    ): RemotePhmOutcome<Unit> =
        unwrapUnit { api.register(RegisterRequest(phone, smscode, username, password)) }
}

/**
 * Either a typed payload or a typed error. The PHM layer pattern-matches on this to
 * decide retry/fallback behavior without leaking Retrofit/OkHttp exception types upward.
 */
sealed class RemotePhmOutcome<out T> {
    data class Success<T>(val data: T) : RemotePhmOutcome<T>()
    data class Failure(val error: RemotePhmError) : RemotePhmOutcome<Nothing>()
}

private suspend fun <T> unwrap(call: suspend () -> retrofit2.Response<JeecgResult<T>>): RemotePhmOutcome<T> =
    withContext(Dispatchers.IO) {
        val response = try {
            call()
        } catch (t: Throwable) {
            return@withContext RemotePhmOutcome.Failure(t.toRemotePhmError())
        }

        if (!response.isSuccessful) {
            return@withContext RemotePhmOutcome.Failure(
                RemotePhmError.HttpStatusError(
                    response.code(),
                    "HTTP ${response.code()} from backend.",
                ),
            )
        }

        val rawBody = response.raw().body
        if (rawBody == null || rawBody.contentLength() == 0L) {
            return@withContext RemotePhmOutcome.Failure(
                RemotePhmError.InvalidDto("Empty response body from backend."),
            )
        }

        val envelope = response.body() ?: return@withContext RemotePhmOutcome.Failure(
            RemotePhmError.InvalidDto("Empty response body from backend."),
        )

        if (envelope.success != true) {
            return@withContext RemotePhmOutcome.Failure(envelope.toRemotePhmError())
        }

        val payload = envelope.result
            ?: return@withContext RemotePhmOutcome.Failure(
                RemotePhmError.InvalidDto("Backend returned success but no result payload."),
            )
        RemotePhmOutcome.Success(payload)
    }

private suspend fun <T> unwrapNullable(call: suspend () -> retrofit2.Response<JeecgResult<T?>>): RemotePhmOutcome<T?> =
    withContext(Dispatchers.IO) {
        val response = try {
            call()
        } catch (t: Throwable) {
            return@withContext RemotePhmOutcome.Failure(t.toRemotePhmError())
        }

        if (!response.isSuccessful) {
            return@withContext RemotePhmOutcome.Failure(
                RemotePhmError.HttpStatusError(
                    response.code(),
                    "HTTP ${response.code()} from backend.",
                ),
            )
        }

        val rawBody = response.raw().body
        if (rawBody == null || rawBody.contentLength() == 0L) {
            return@withContext RemotePhmOutcome.Failure(
                RemotePhmError.InvalidDto("Empty response body from backend."),
            )
        }

        val envelope = response.body() ?: return@withContext RemotePhmOutcome.Failure(
            RemotePhmError.InvalidDto("Empty response body from backend."),
        )

        if (envelope.success != true) {
            return@withContext RemotePhmOutcome.Failure(envelope.toRemotePhmError())
        }

        // E1 returns null for risk/latest and interventions/today while persistence is pending.
        RemotePhmOutcome.Success(envelope.result)
    }

private suspend fun unwrapUnit(call: suspend () -> retrofit2.Response<JeecgResult<*>>): RemotePhmOutcome<Unit> =
    withContext(Dispatchers.IO) {
        val response = try {
            call()
        } catch (t: Throwable) {
            return@withContext RemotePhmOutcome.Failure(t.toRemotePhmError())
        }

        if (!response.isSuccessful) {
            return@withContext RemotePhmOutcome.Failure(
                RemotePhmError.HttpStatusError(response.code(), "HTTP ${response.code()} from backend."),
            )
        }

        val envelope = response.body() ?: return@withContext RemotePhmOutcome.Failure(
            RemotePhmError.InvalidDto("Empty response body from backend."),
        )

        if (envelope.success != true) {
            return@withContext RemotePhmOutcome.Failure(envelope.toRemotePhmError())
        }
        RemotePhmOutcome.Success(Unit)
    }

private fun JeecgResult<*>.toRemotePhmError(): RemotePhmError {
    val code = code
    val msg = message ?: "Backend returned an unsuccessful response."
    return when (code) {
        ReHealthErrorCodes.MODEL_SERVICE_UNAVAILABLE,
        ReHealthErrorCodes.MODEL_SERVICE_CONFIG_INVALID ->
            RemotePhmError.ModelServiceUnavailable(msg)
        ReHealthErrorCodes.MODEL_SERVICE_INVALID_RESPONSE ->
            RemotePhmError.InvalidDto("Backend received an invalid model-service response: $msg")
        ReHealthErrorCodes.MODEL_CONTRACT_VIOLATION ->
            RemotePhmError.MissingFeatureFields(emptyList(), "Model contract violation: $msg")
        else -> RemotePhmError.HttpStatusError(code ?: 0, msg)
    }
}
