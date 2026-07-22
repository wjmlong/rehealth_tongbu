package com.rehealth.genie.network

import com.rehealth.genie.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response

interface MeasurementUploadClient {
    val authState: AuthState

    suspend fun uploadMeasurements(
        request: TelemetryBatchRequestDto,
    ): ApiResult<TelemetryBatchResponseDto>
}

interface HealthInterviewUploadClient {
    val authState: AuthState

    suspend fun submitHealthInterview(
        request: HealthInterviewSubmitRequestDto,
    ): ApiResult<HealthInterviewSubmitRequestDto>
}

/**
 * D3 authenticated API client with 401 detection and queue pause.
 *
 * Wraps [ReHealthMobileApi] and detects HTTP 401 responses. When a 401 occurs:
 * - Sets [authState] to [AuthState.Unauthorized]
 * - Returns [ApiResult.Unauthorized] to the caller
 * - The caller (sync worker, PHM service) must pause queue and notify UI to re-login
 *
 * No refresh token exists (per E1.2 frozen contract), so 401 requires full re-login.
 */
class AuthenticatedApiClient(
    private val baseUrl: String,
    private val httpClient: OkHttpClient,
    private val sessionStore: SessionStore,
) : MeasurementUploadClient, HealthInterviewUploadClient {
    private var mobileApi = ReHealthMobileApi(
        baseUrl = baseUrl,
        httpClient = httpClient,
        apiToken = sessionStore.token,
    )

    override var authState: AuthState = if (sessionStore.isLoggedIn) AuthState.Authorized else AuthState.Unauthorized
        private set

    suspend fun evaluateFeatures(
        request: FeatureEvaluateRequest,
    ): ApiResult<RiskResultDto> = executeWithAuth {
        mobileApi.evaluateFeatures(request)
    }

    suspend fun submitInterventionFeedback(
        interventionId: String,
        request: InterventionFeedbackRequest,
    ): ApiResult<InterventionFeedbackResponse> = executeWithAuth {
        mobileApi.submitInterventionFeedback(interventionId, request)
    }

    override suspend fun uploadMeasurements(
        request: TelemetryBatchRequestDto,
    ): ApiResult<TelemetryBatchResponseDto> = executeWithAuth {
        mobileApi.uploadMeasurements(request)
    }

    override suspend fun submitHealthInterview(
        request: HealthInterviewSubmitRequestDto,
    ): ApiResult<HealthInterviewSubmitRequestDto> = executeWithAuth {
        mobileApi.submitHealthInterview(request)
    }

    suspend fun attributeIndividual(
        request: IndividualAttributionRequestDto,
    ): ApiResult<IndividualAttributionResponseDto> = executeWithAuth {
        mobileApi.attributeIndividual(request)
    }

    suspend fun getRiskLatest(): ApiResult<RiskResultDto?> = executeWithAuth {
        mobileApi.getRiskLatest()
    }

    suspend fun getInterventionsToday(): ApiResult<InterventionPlanDto?> = executeWithAuth {
        mobileApi.getInterventionsToday()
    }

    suspend fun getHealth(): ApiResult<HealthCheckResponse> = executeWithAuth {
        mobileApi.getHealth()
    }

    suspend fun getConfig(): ApiResult<MobileConfigResponse> = executeWithAuth {
        mobileApi.getConfig()
    }

    /**
     * Mobile login (no auth token required). Delegates to [ReHealthMobileApi.mobileLogin]
     * and maps the [RemotePhmOutcome] into the typed [ApiResult].
     */
    suspend fun mobileLogin(username: String, password: String): ApiResult<MobileLoginResponse> {
        return when (val outcome = mobileApi.mobileLogin(MobileLoginRequest(username, password))) {
            is RemotePhmOutcome.Success -> ApiResult.Success(outcome.data)
            // A login Failure is almost always a business/credential error (the backend returns
            // HTTP 200 with success=false, e.g. "用户名或密码错误"), so surface the backend message
            // as an InvalidRequest rather than a generic network error.
            is RemotePhmOutcome.Failure -> ApiResult.InvalidRequest(outcome.error.message)
        }
    }

    /**
     * Step 1 of registration: request a registration SMS. The shared OkHttp client carries
     * [SignInterceptor], which signs this `/sys/sms` call. Pre-auth, so no 401/403 handling.
     */
    suspend fun sendSms(mobile: String): ApiResult<Unit> {
        return when (val outcome = mobileApi.sendSms(mobile)) {
            is RemotePhmOutcome.Success -> ApiResult.Success(Unit)
            is RemotePhmOutcome.Failure -> mapPreAuthFailure(outcome.error)
        }
    }

    /**
     * Step 2 of registration: create the account with the SMS code. Public endpoint; if the
     * code is wrong/expired the backend returns a business error surfaced as [ApiResult.InvalidRequest].
     */
    suspend fun register(
        phone: String,
        smscode: String,
        username: String,
        password: String,
    ): ApiResult<Unit> {
        return when (val outcome = mobileApi.register(phone, smscode, username, password)) {
            is RemotePhmOutcome.Success -> ApiResult.Success(Unit)
            is RemotePhmOutcome.Failure -> mapPreAuthFailure(outcome.error)
        }
    }

    /**
     * Maps a [RemotePhmError] for the pre-auth registration endpoints. Business/validation
     * errors (e.g. "短信接口未配置", "手机验证码失效") are surfaced as [ApiResult.InvalidRequest]
     * so the UI can show the backend message; transport errors become [ApiResult.NetworkError].
     */
    private fun mapPreAuthFailure(error: RemotePhmError): ApiResult<Nothing> = when (error) {
        is RemotePhmError.HttpStatusError -> when (error.code) {
            401 -> ApiResult.Unauthorized(error.message)
            403 -> ApiResult.Forbidden(error.message)
            else -> ApiResult.NetworkError(error.message)
        }
        is RemotePhmError.ModelServiceUnavailable -> ApiResult.ServiceUnavailable(error.message)
        is RemotePhmError.InvalidDto -> ApiResult.InvalidRequest(error.message)
        is RemotePhmError.MissingFeatureFields ->
            ApiResult.InvalidRequest("Missing fields: ${error.fields.joinToString()}")
        else -> ApiResult.NetworkError(error.message)
    }

    /**
     * Call this after successful login to refresh the token in the API client
     * and reset auth state. Rebuilds [mobileApi] so the auth interceptor picks up the
     * new token for all subsequent authenticated calls.
     */
    fun onLoginSuccess(token: String) {
        sessionStore.token = token
        authState = AuthState.Authorized
        mobileApi = ReHealthMobileApi(baseUrl, httpClient, token)
    }

    /**
     * Call this when user logs out or when 401 is detected. Rebuilds [mobileApi]
     * without a token so no stale `X-Access-Token` is sent.
     */
    fun onLogout() {
        sessionStore.clear()
        authState = AuthState.Unauthorized
        mobileApi = ReHealthMobileApi(baseUrl, httpClient, null)
    }

    private suspend fun <T> executeWithAuth(
        block: suspend () -> RemotePhmOutcome<T>,
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        if (!sessionStore.isLoggedIn) {
            authState = AuthState.Unauthorized
            return@withContext ApiResult.Unauthorized("No token available, please login")
        }

        when (val outcome = block()) {
            is RemotePhmOutcome.Success -> ApiResult.Success(outcome.data)
            is RemotePhmOutcome.Failure -> {
                when (val error = outcome.error) {
                    is RemotePhmError.HttpStatusError -> {
                        if (error.code == 401) {
                            authState = AuthState.Unauthorized
                            ApiResult.Unauthorized("Token expired or invalid, please re-login")
                        } else if (error.code == 403) {
                            ApiResult.Forbidden(error.message)
                        } else {
                            ApiResult.NetworkError(error.message)
                        }
                    }
                    is RemotePhmError.ModelServiceUnavailable ->
                        ApiResult.ServiceUnavailable(error.message)
                    is RemotePhmError.InvalidDto ->
                        ApiResult.InvalidResponse(error.message)
                    is RemotePhmError.MissingFeatureFields ->
                        ApiResult.InvalidRequest("Missing fields: ${error.fields.joinToString()}")
                    is RemotePhmError.Timeout ->
                        ApiResult.NetworkError(error.message)
                    is RemotePhmError.Unknown ->
                        ApiResult.NetworkError(error.message)
                    else ->
                        ApiResult.NetworkError(error.message)
                }
            }
        }
    }
}

/**
 * Auth state tracked by [AuthenticatedApiClient].
 */
enum class AuthState {
    Authorized,
    Unauthorized,
}

/**
 * Typed API result that surfaces 401/403 separately from other errors.
 * Replaces the generic RemotePhmOutcome for D3 auth-aware code paths.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Unauthorized(val message: String) : ApiResult<Nothing>()
    data class Forbidden(val message: String) : ApiResult<Nothing>()
    data class InvalidRequest(val message: String) : ApiResult<Nothing>()
    data class InvalidResponse(val message: String) : ApiResult<Nothing>()
    data class ServiceUnavailable(val message: String) : ApiResult<Nothing>()
    data class NetworkError(val message: String) : ApiResult<Nothing>()
}
