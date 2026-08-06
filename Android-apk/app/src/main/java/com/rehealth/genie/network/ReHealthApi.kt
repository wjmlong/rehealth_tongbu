package com.rehealth.genie.network

import com.rehealth.genie.network.dto.FeatureEvaluateRequest
import com.rehealth.genie.network.dto.HealthCheckResponse
import com.rehealth.genie.network.dto.HealthAgentMessageRequest
import com.rehealth.genie.network.dto.HealthAgentResponse
import com.rehealth.genie.network.dto.HealthAgentConversation
import com.rehealth.genie.network.dto.HealthInterviewSubmitRequestDto
import com.rehealth.genie.network.dto.InterventionFeedbackRequest
import com.rehealth.genie.network.dto.InterventionFeedbackResponse
import com.rehealth.genie.network.dto.InterventionPlanDto
import com.rehealth.genie.network.dto.IndividualAttributionRequestDto
import com.rehealth.genie.network.dto.IndividualAttributionResponseDto
import com.rehealth.genie.network.dto.MobileConfigResponse
import com.rehealth.genie.network.dto.MobileLoginRequest
import com.rehealth.genie.network.dto.MobileLoginResponse
import com.rehealth.genie.network.dto.DeviceBindRequestDto
import com.rehealth.genie.network.dto.DeviceBindResponseDto
import com.rehealth.genie.network.dto.InterventionGenerateRequestDto
import com.rehealth.genie.network.dto.PatientProfileDto
import com.rehealth.genie.network.dto.RegisterRequest
import com.rehealth.genie.network.dto.RiskResultDto
import com.rehealth.genie.network.dto.RhiV2SeriesEvaluateRequestDto
import com.rehealth.genie.network.dto.RhiV2SeriesEvaluateResponseDto
import com.rehealth.genie.network.dto.SendSmsRequest
import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import com.rehealth.genie.network.dto.TelemetryBatchResponseDto
import com.rehealth.genie.network.dto.RhiDailySnapshotBatchDto
import com.rehealth.genie.network.dto.RhiDailySnapshotResponseDto
import com.rehealth.genie.network.dto.RhiManualHealthInputDto
import com.rehealth.genie.network.dto.RecentTelemetryResponseDto
import com.rehealth.genie.network.dto.ViomiBindRequestDto
import com.rehealth.genie.network.dto.ViomiBindResponseDto
import com.rehealth.genie.network.dto.ViomiSyncRequestDto
import com.rehealth.genie.network.dto.ViomiSyncResponseDto
import com.rehealth.genie.network.dto.BehaviorRecordDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Multipart
import retrofit2.http.Part

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

    @GET("rehealth/mobile/profile")
    suspend fun getProfile(): Response<JeecgResult<PatientProfileDto?>>

    @PUT("rehealth/mobile/profile")
    suspend fun updateProfile(
        @Body request: PatientProfileDto,
    ): Response<JeecgResult<PatientProfileDto>>

    @GET("rehealth/mobile/rhi/manual-inputs")
    suspend fun getRhiManualHealthInput(): Response<JeecgResult<RhiManualHealthInputDto?>>

    @PUT("rehealth/mobile/rhi/manual-inputs")
    suspend fun updateRhiManualHealthInput(
        @Body request: RhiManualHealthInputDto,
    ): Response<JeecgResult<RhiManualHealthInputDto>>

    @POST("rehealth/mobile/devices/bind")
    suspend fun bindDevice(
        @Body request: DeviceBindRequestDto,
    ): Response<JeecgResult<DeviceBindResponseDto>>

    @POST("rehealth/mobile/viomi/bind")
    suspend fun bindViomi(
        @Body request: ViomiBindRequestDto,
    ): Response<JeecgResult<ViomiBindResponseDto>>

    @POST("rehealth/mobile/viomi/sync")
    suspend fun syncViomi(
        @Body request: ViomiSyncRequestDto,
    ): Response<JeecgResult<ViomiSyncResponseDto>>

    @POST("rehealth/mobile/features/evaluate")
    suspend fun evaluateFeatures(@Body request: FeatureEvaluateRequest): Response<JeecgResult<RiskResultDto>>

@POST("rehealth/mobile/rhi/daily-snapshot")
    suspend fun uploadRhiSnapshot(
        @Body request: RhiDailySnapshotBatchDto,
    ): Response<JeecgResult<RhiDailySnapshotResponseDto>>

    @POST("rehealth/mobile/rhi/evaluate-series")
    suspend fun evaluateRhiSeries(
        @Body request: RhiV2SeriesEvaluateRequestDto,
    ): Response<JeecgResult<RhiV2SeriesEvaluateResponseDto>>

    @GET("rehealth/mobile/risk/latest")
    suspend fun getRiskLatest(): Response<JeecgResult<RiskResultDto?>>

    @GET("rehealth/mobile/interventions/today")
    suspend fun getInterventionsToday(): Response<JeecgResult<InterventionPlanDto?>>

    @POST("rehealth/mobile/interventions/generate")
    suspend fun generateIntervention(
        @Body request: InterventionGenerateRequestDto,
    ): Response<JeecgResult<InterventionPlanDto>>

    @POST("rehealth/mobile/interventions/{id}/feedback")
    suspend fun submitInterventionFeedback(
        @Path("id") interventionId: String,
        @Body request: InterventionFeedbackRequest,
    ): Response<JeecgResult<InterventionFeedbackResponse>>

    @POST("rehealth/mobile/measurements/batch")
    suspend fun uploadMeasurements(
        @Body request: TelemetryBatchRequestDto,
    ): Response<JeecgResult<TelemetryBatchResponseDto>>

    @GET("rehealth/mobile/measurements/recent")
    suspend fun getRecentTelemetry(
        @Query("limit") limit: Int,
    ): Response<JeecgResult<RecentTelemetryResponseDto>>

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

    @POST("rehealth/mobile/agent/messages")
    suspend fun sendHealthAgentMessage(
        @Body request: HealthAgentMessageRequest,
    ): Response<JeecgResult<HealthAgentResponse>>

    @GET("rehealth/mobile/agent/conversations/latest")
    suspend fun getLatestHealthAgentConversation(
        @Query("limit") limit: Int = 100,
    ): Response<JeecgResult<HealthAgentConversation?>>

    @Multipart
    @POST("rehealth/mobile/behavior-records/analyze-photo")
    suspend fun analyzeBehaviorPhoto(
        @Part image: MultipartBody.Part,
        @Part("requestId") requestId: RequestBody,
        @Part("occurredAt") occurredAt: RequestBody,
    ): Response<JeecgResult<BehaviorRecordDto>>

    @GET("rehealth/mobile/behavior-records/today")
    suspend fun getTodayBehaviorRecords(
        @Query("date") date: String,
        @Query("zoneOffsetMinutes") zoneOffsetMinutes: Int,
    ): Response<JeecgResult<List<BehaviorRecordDto>>>

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
