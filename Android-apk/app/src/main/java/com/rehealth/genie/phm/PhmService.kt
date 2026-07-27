package com.rehealth.genie.phm

/**
 * App-facing health-intelligence facade. This is the single seam the UI depends on.
 *
 * - [modelInputs] is local/synchronous (describes ring inputs).
 * - [todayState] / [interventions] are derived from the latest backend results.
 * - The remaining methods are the encapsulated "algorithm" calls: they go through
 *   the WSL2 backend (which in turn calls the Python model-service) and never
 *   touch the model directly from the device.
 *
 * Production calls are implemented against the authenticated backend. A failed remote
 * evaluation must remain unavailable rather than synthesizing a local risk result.
 */
interface PhmService {
    fun modelInputs(): List<ModelInputStatus>

    suspend fun todayState(): LifeState
    suspend fun interventions(): List<Intervention>

    // Auth
    suspend fun login(username: String, password: String): LoginResult
    suspend fun logout()

    // Algorithm calls (encapsulated remote layer)
    suspend fun evaluateRisk(request: RiskEvaluateRequest): RiskResult
    suspend fun latestRisk(): RiskResult?
    suspend fun todayIntervention(): InterventionPlan?
    suspend fun generateIntervention(request: InterventionGenerateRequest): InterventionPlan
    suspend fun submitFeedback(planId: String, feedback: FeedbackRequest): FeedbackResult
    suspend fun bindDevice(request: DeviceBindRequest): DeviceBindResult
    suspend fun uploadMeasurements(request: TelemetryBatchRequest): TelemetryBatchResult
    suspend fun recordAttributionEvents(request: AttributionEventsRequest): AttributionResult

    // PIAS algorithm (real model-service call)
    suspend fun attributeIndividual(
        history: List<AttributionHistoryPoint>,
        forecastDays: Int = 30,
        language: String = "zh",
    ): IndividualAttributionResult
}
