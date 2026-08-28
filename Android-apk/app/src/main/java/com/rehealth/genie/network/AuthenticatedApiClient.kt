package com.rehealth.genie.network

import com.rehealth.genie.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response

interface MeasurementUploadClient {
    val authState: AuthState

    suspend fun uploadMeasurements(
        request: TelemetryBatchRequestDto,
    ): ApiResult<TelemetryBatchResponseDto>
}

interface RhiSnapshotUploadClient {
    val authState: AuthState

    suspend fun uploadRhiSnapshot(
        request: RhiDailySnapshotBatchDto,
    ): ApiResult<RhiDailySnapshotResponseDto>
}

interface RdiSnapshotUploadClient {
    val authState: AuthState

    suspend fun uploadRdiSnapshot(
        request: RdiDailySnapshotBatchDto,
    ): ApiResult<RdiDailySnapshotResponseDto>
}

interface HealthInterviewUploadClient {
    val authState: AuthState

    suspend fun submitHealthInterview(
        request: HealthInterviewSubmitRequestDto,
    ): ApiResult<HealthInterviewSubmitRequestDto>
}

interface RhiManualHealthInputSyncClient {
    val authState: AuthState

    suspend fun getRhiManualHealthInput(): ApiResult<RhiManualHealthInputDto?>

    suspend fun updateRhiManualHealthInput(
        request: RhiManualHealthInputDto,
    ): ApiResult<RhiManualHealthInputDto>
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
) : MeasurementUploadClient,
    HealthInterviewUploadClient,
    RhiSnapshotUploadClient,
    RdiSnapshotUploadClient,
    RhiManualHealthInputSyncClient {
    private var mobileApi = ReHealthMobileApi(
        baseUrl = baseUrl,
        httpClient = httpClient,
        apiToken = sessionStore.token,
    )

    override var authState: AuthState = if (sessionStore.isLoggedIn) AuthState.Authorized else AuthState.Unauthorized
        private set

    /**
     * True once a previously-authorized session received a real HTTP 401. The
     * root navigation uses this (and not [authState] alone) to return to Login,
     * so an anonymous guest browsing Main is not bounced by the missing token.
     */
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    suspend fun evaluateFeatures(
        request: FeatureEvaluateRequest,
    ): ApiResult<RiskResultDto> = executeWithAuth {
        mobileApi.evaluateFeatures(request)
    }

    override suspend fun uploadRhiSnapshot(
        request: RhiDailySnapshotBatchDto,
    ): ApiResult<RhiDailySnapshotResponseDto> = executeWithAuth {
        mobileApi.uploadRhiSnapshot(request)
    }

    override suspend fun uploadRdiSnapshot(
        request: RdiDailySnapshotBatchDto,
    ): ApiResult<RdiDailySnapshotResponseDto> = executeWithAuth {
        mobileApi.uploadRdiSnapshot(request)
    }

    suspend fun evaluateRhiSeries(
        request: RhiV2SeriesEvaluateRequestDto,
    ): ApiResult<RhiV2SeriesEvaluateResponseDto> = executeWithAuth {
        mobileApi.evaluateRhiSeries(request)
    }

    suspend fun getProfile(): ApiResult<PatientProfileDto?> = executeWithAuth {
        mobileApi.getProfile()
    }

    suspend fun updateProfile(request: PatientProfileDto): ApiResult<PatientProfileDto> = executeWithAuth {
        mobileApi.updateProfile(request)
    }

    override suspend fun getRhiManualHealthInput(): ApiResult<RhiManualHealthInputDto?> = executeWithAuth {
        mobileApi.getRhiManualHealthInput()
    }

    override suspend fun updateRhiManualHealthInput(
        request: RhiManualHealthInputDto,
    ): ApiResult<RhiManualHealthInputDto> = executeWithAuth {
        mobileApi.updateRhiManualHealthInput(request)
    }

    suspend fun bindDevice(request: DeviceBindRequestDto): ApiResult<DeviceBindResponseDto> = executeWithAuth {
        mobileApi.bindDevice(request)
    }

    suspend fun bindViomi(request: ViomiBindRequestDto): ApiResult<ViomiBindResponseDto> = executeWithAuth {
        mobileApi.bindViomi(request)
    }

    suspend fun syncViomi(request: ViomiSyncRequestDto): ApiResult<ViomiSyncResponseDto> = executeWithAuth {
        mobileApi.syncViomi(request)
    }

    suspend fun saveViomiMeasurementPlan(
        request: com.rehealth.genie.network.dto.ViomiMeasurementPlanRequestDto,
    ): ApiResult<com.rehealth.genie.network.dto.ViomiMeasurementPlanResponseDto> = executeWithAuth {
        mobileApi.saveViomiMeasurementPlan(request)
    }

    suspend fun submitInterventionFeedback(
        interventionId: String,
        request: InterventionFeedbackRequest,
    ): ApiResult<InterventionFeedbackResponse> = executeWithAuth {
        mobileApi.submitInterventionFeedback(interventionId, request)
    }

    suspend fun bindInsurancePlan(
        request: InsurancePlanBindRequestDto,
    ): ApiResult<InsurancePlanBindingDto> = executeWithAuth {
        mobileApi.bindInsurancePlan(request)
    }

    suspend fun getBindableInsurancePolicies(): ApiResult<List<InsuranceMobileBindablePolicyDto>> = executeWithAuth {
        mobileApi.getBindableInsurancePolicies()
    }

    suspend fun getCurrentInsurancePlan(): ApiResult<InsurancePlanBindingDto?> = executeWithAuth {
        mobileApi.getCurrentInsurancePlan()
    }

    suspend fun getServiceContact(): ApiResult<InsuranceServiceContactDto?> = executeWithAuth {
        mobileApi.getServiceContact()
    }

    suspend fun scanInsuranceAssignment(
        request: InsuranceScanRequestDto,
    ): ApiResult<InsuranceScanPreviewDto> = executeWithAuth {
        mobileApi.scanInsuranceAssignment(request)
    }

    suspend fun confirmInsuranceScan(
        sessionId: String,
        request: InsuranceScanConfirmRequestDto,
    ): ApiResult<InsuranceScanConfirmResultDto> = executeWithAuth {
        mobileApi.confirmInsuranceScan(sessionId, request)
    }

    suspend fun cancelInsuranceScan(sessionId: String): ApiResult<Boolean> = executeWithAuth {
        mobileApi.cancelInsuranceScan(sessionId)
    }

    suspend fun getActiveInsurancePlans(): ApiResult<List<InsurancePlanBindingDto>> = executeWithAuth {
        mobileApi.getActiveInsurancePlans()
    }

    suspend fun submitInsurancePlanFeedback(
        bindingId: String,
        request: InsurancePlanFeedbackRequestDto,
    ): ApiResult<Map<String, Any>> = executeWithAuth {
        mobileApi.submitInsurancePlanFeedback(bindingId, request)
    }

    suspend fun getCurrentInstitutionCarePlans(): ApiResult<List<InstitutionCarePlanDto>> = executeWithAuth {
        mobileApi.getCurrentInstitutionCarePlans()
    }

    suspend fun submitInstitutionCarePlanFeedback(
        occurrenceId: String,
        request: InstitutionCarePlanFeedbackRequestDto,
    ): ApiResult<Map<String, Any>> = executeWithAuth {
        mobileApi.submitInstitutionCarePlanFeedback(occurrenceId, request)
    }

    override suspend fun uploadMeasurements(
        request: TelemetryBatchRequestDto,
    ): ApiResult<TelemetryBatchResponseDto> = executeWithAuth {
        mobileApi.uploadMeasurements(request)
    }

    suspend fun getRecentTelemetry(limit: Int): ApiResult<RecentTelemetryResponseDto> = executeWithAuth {
        mobileApi.getRecentTelemetry(limit)
    }

    override suspend fun submitHealthInterview(
        request: HealthInterviewSubmitRequestDto,
    ): ApiResult<HealthInterviewSubmitRequestDto> = executeWithAuth {
        mobileApi.submitHealthInterview(request)
    }

    suspend fun getLatestHealthInterview(): ApiResult<HealthInterviewSubmitRequestDto?> = executeWithAuth {
        mobileApi.getLatestHealthInterview()
    }

    suspend fun attributeIndividual(
        request: IndividualAttributionRequestDto,
    ): ApiResult<IndividualAttributionResponseDto> = executeWithAuth {
        mobileApi.attributeIndividual(request)
    }

    suspend fun sendHealthAgentMessage(
        request: HealthAgentMessageRequest,
    ): ApiResult<HealthAgentResponse> = executeWithAuth {
        mobileApi.sendHealthAgentMessage(request)
    }

    suspend fun getLatestHealthAgentConversation(
        limit: Int = 100,
    ): ApiResult<com.rehealth.genie.network.dto.HealthAgentConversation?> = executeWithAuth {
        mobileApi.getLatestHealthAgentConversation(limit)
    }

    suspend fun analyzeBehaviorPhoto(
        image: ByteArray,
        contentType: String,
        fileName: String,
        requestId: String,
        occurredAt: Long,
    ): ApiResult<BehaviorRecordDto> = executeWithAuth {
        mobileApi.analyzeBehaviorPhoto(image, contentType, fileName, requestId, occurredAt)
    }

    suspend fun getTodayBehaviorRecords(
        date: String,
        zoneOffsetMinutes: Int,
    ): ApiResult<List<BehaviorRecordDto>> = executeWithAuth {
        mobileApi.getTodayBehaviorRecords(date, zoneOffsetMinutes)
    }

    suspend fun getRiskLatest(): ApiResult<RiskResultDto?> = executeWithAuth {
        mobileApi.getRiskLatest()
    }

    suspend fun getInterventionsToday(): ApiResult<InterventionPlanDto?> = executeWithAuth {
        mobileApi.getInterventionsToday()
    }

    suspend fun generateIntervention(
        request: InterventionGenerateRequestDto,
    ): ApiResult<InterventionPlanDto> = executeWithAuth {
        mobileApi.generateIntervention(request)
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
     * WeChat Open Platform mobile-app login (pre-auth). The one-time SDK `code` is exchanged
     * server-side; the response carries the same `{token, userInfo}` shape as [mobileLogin].
     */
    suspend fun wechatLogin(code: String): ApiResult<MobileLoginResponse> {
        return when (val outcome = mobileApi.wechatAppLogin(WechatAppLoginRequest(code))) {
            is RemotePhmOutcome.Success -> ApiResult.Success(outcome.data)
            is RemotePhmOutcome.Failure -> ApiResult.InvalidRequest(outcome.error.message)
        }
    }

    /**
     * Send a bind-phone SMS for the authenticated account (registration-SMS infra,
     * cooldown/quota and Dypnsapi). Maps backend business errors to [ApiResult.InvalidRequest].
     */
    suspend fun bindPhoneSms(phone: String): ApiResult<Unit> {
        return when (val outcome = mobileApi.bindPhoneSms(phone)) {
            is RemotePhmOutcome.Success -> ApiResult.Success(Unit)
            is RemotePhmOutcome.Failure -> ApiResult.InvalidRequest(outcome.error.message)
        }
    }

    /**
     * Verify the SMS code and bind the phone to the authenticated account. Backend business
     * errors (wrong/expired code, phone taken) surface as [ApiResult.InvalidRequest] with the
     * backend message.
     */
    suspend fun bindPhone(phone: String, smsCode: String): ApiResult<BindPhoneResponse> {
        return when (val outcome = mobileApi.bindPhone(phone, smsCode)) {
            is RemotePhmOutcome.Success -> ApiResult.Success(outcome.data)
            is RemotePhmOutcome.Failure -> ApiResult.InvalidRequest(outcome.error.message)
        }
    }

    /**
     * Step 1 of registration: request a registration SMS through the public, rate-limited
     * `/sys/registerSms` route. Pre-auth, so no 401/403 handling.
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
     *
     * Note: the backend wraps business failures in HTTP 200 with JeecgResult.code = 0/500/412,
     * which [toRemotePhmError] represents as [RemotePhmError.HttpStatusError]. For pre-auth
     * endpoints only 401/403 are true auth errors, so everything else is shown to the user.
     */
    private fun mapPreAuthFailure(error: RemotePhmError): ApiResult<Nothing> = when (error) {
        is RemotePhmError.HttpStatusError -> when (error.code) {
            401 -> ApiResult.Unauthorized(error.message)
            403 -> ApiResult.Forbidden(error.message)
            else -> ApiResult.InvalidRequest(error.message)
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
        _sessionExpired.value = false
        mobileApi = ReHealthMobileApi(baseUrl, httpClient, token)
    }

    /**
     * Call this when user logs out or when 401 is detected. Rebuilds [mobileApi]
     * without a token so no stale `X-Access-Token` is sent.
     */
    fun onLogout() {
        sessionStore.clear()
        authState = AuthState.Unauthorized
        _sessionExpired.value = false
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
                            _sessionExpired.value = true
                            // Drop the rejected token immediately so no later call
                            // re-sends it before the root navigation clears the session.
                            sessionStore.clear()
                            ApiResult.Unauthorized("Token expired or invalid, please re-login")
                        } else if (error.code == 403) {
                            ApiResult.Forbidden(error.message)
                        } else if (error.code == 503) {
                            ApiResult.ServiceUnavailable(error.message)
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
                        ApiResult.NetworkError(error.message, isTimeout = true)
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
    data class NetworkError(
        val message: String,
        val isTimeout: Boolean = false,
    ) : ApiResult<Nothing>()
}
