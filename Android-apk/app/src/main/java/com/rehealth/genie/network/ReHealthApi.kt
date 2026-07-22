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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * E1 mobile API Retrofit interface. Only the D1-safe endpoints are declared.
 * Mobile endpoints are relative to the configured backend base URL (for example
 * `/jeecg-boot/`) so the deployment context is preserved.
 *
 * Telemetry upload targets the authenticated E2.1 durable hardware-ingest endpoint.
 */
interface ReHealthApi {
    @GET("rehealth/mobile/health")
    suspend fun getHealth(): Response<JeecgResult<HealthCheckResponse>>

    @GET("rehealth/mobile/config")
    suspend fun getConfig(): Response<JeecgResult<MobileConfigResponse>>

    @POST("rehealth/mobile/features/evaluate")
    suspend fun evaluateFeatures(@Body request: FeatureEvaluateRequest): Response<JeecgResult<RiskResultDto>>

    @GET("rehealth/mobile/risk/latest")
    suspend fun getRiskLatest(): Response<JeecgResult<RiskResultDto?>>

    @GET("rehealth/mobile/interventions/today")
    suspend fun getInterventionsToday(): Response<JeecgResult<InterventionPlanDto?>>

    @POST("rehealth/mobile/interventions/{id}/feedback")
    suspend fun submitInterventionFeedback(
        @Path("id") interventionId: String,
        @Body request: InterventionFeedbackRequest,
    ): Response<JeecgResult<InterventionFeedbackResponse>>

    @POST("rehealth/mobile/measurements/batch")
    suspend fun uploadMeasurements(
        @Body request: TelemetryBatchRequestDto,
    ): Response<JeecgResult<TelemetryBatchResponseDto>>

    @POST("rehealth/mobile/interviews")
    suspend fun submitHealthInterview(
        @Body request: HealthInterviewSubmitRequestDto,
    ): Response<JeecgResult<HealthInterviewSubmitRequestDto>>

    @GET("rehealth/mobile/interviews/latest")
    suspend fun getLatestHealthInterview(): Response<JeecgResult<HealthInterviewSubmitRequestDto?>>

    @POST("rehealth/mobile/attribution/events")
    suspend fun attributeIndividual(
        @Body request: IndividualAttributionRequestDto,
    ): Response<JeecgResult<IndividualAttributionResponseDto>>

    /**
     * JeecgBoot system login. Lives under `/jeecg-boot` (not the `/rehealth/mobile`
     * prefix). The leading slash makes Retrofit resolve against the host root, so with
     * base `…/jeecg-boot/` this becomes `…/jeecg-boot/sys/mLogin`.
     */
    @POST("/jeecg-boot/sys/mLogin")
    suspend fun mobileLogin(
        @Body request: MobileLoginRequest,
    ): Response<JeecgResult<MobileLoginResponse>>

    /**
     * Send a registration SMS. Requires the `X-Sign`/`X-Timestamp` headers (added by
     * [SignInterceptor], which is installed on the shared OkHttp client and only acts on
     * paths ending in `/sys/sms`). The verification code is stored server-side in Redis.
     */
    @POST("/jeecg-boot/sys/sms")
    suspend fun sendSms(
        @Body request: SendSmsRequest,
    ): Response<JeecgResult<*>>

    /**
     * Public registration (no signature, no auth token). Validates the SMS code from
     * Redis and creates the user. On success the app auto-logs-in via `/sys/mLogin`.
     */
    @POST("/jeecg-boot/sys/user/register")
    suspend fun register(
        @Body request: RegisterRequest,
    ): Response<JeecgResult<*>>
}

/**
 * JeecgBoot envelope. `result` carries the typed payload. E1 dev returns `result: null`
 * for read endpoints that are still persistence-pending (risk/latest, interventions/today).
 */
data class JeecgResult<T>(
    val success: Boolean? = null,
    val code: Int? = null,
    val message: String? = null,
    val result: T? = null,
)
