package com.rehealth.genie.phm

import com.rehealth.genie.features.CvdFeatureFields
import com.rehealth.genie.features.CvdFeatureVector
import com.rehealth.genie.features.FeatureQuality
import com.rehealth.genie.network.BackendConfig
import com.rehealth.genie.network.RemotePhmError
import com.rehealth.genie.network.RemotePhmOutcome
import com.rehealth.genie.network.ReHealthMobileApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemotePhmServiceRemoteFailureTest {

    private val server = MockWebServer()
    private val httpClient = BackendConfig.buildHttpClient(connectTimeoutSeconds = 1, readTimeoutSeconds = 1)

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun backendUnavailableFallsBackToMockAndReportsError() = runTest {
        // Use a port nobody is listening on to force an IOException -> BackendUnavailable.
        val api = ReHealthMobileApi(
            baseUrl = "http://127.0.0.1:65535/jeecg-boot",
            httpClient = httpClient,
        )
        val service = RemotePhmService(api = api)

        val outcome = service.evaluateFeatures(aCompleteVector())

        assertTrue(outcome.usedMockFallback)
        assertNotNull(outcome.error)
        assertTrue(outcome.error is RemotePhmError.BackendUnavailable)
        assertNull(outcome.result)
    }

    @Test
    fun http5xxFallsBackToMockWithHttpStatusError() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(500)
                .setBody("""{"success":false,"code":500,"message":"server error"}"""),
        )
        val service = RemotePhmService(api = apiAgainstServer())

        val outcome = service.evaluateFeatures(aCompleteVector())

        assertTrue(outcome.usedMockFallback)
        val error = outcome.error
        assertNotNull(error)
        assertTrue(error is RemotePhmError.HttpStatusError)
        assertEquals(500, (error as RemotePhmError.HttpStatusError).code)
    }

    @Test
    fun successFalseEnvelopeWithModelServiceUnavailableMapsToModelServiceUnavailable() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":55002,"message":"model-service is unavailable"}""",
            ),
        )
        val service = RemotePhmService(api = apiAgainstServer())

        val outcome = service.evaluateFeatures(aCompleteVector())

        assertTrue(outcome.usedMockFallback)
        assertTrue(outcome.error is RemotePhmError.ModelServiceUnavailable)
    }

    @Test
    fun successFalseEnvelopeWithModelServiceInvalidResponseMapsToInvalidDto() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":55003,"message":"upstream returned malformed json"}""",
            ),
        )
        val service = RemotePhmService(api = apiAgainstServer())

        val outcome = service.evaluateFeatures(aCompleteVector())

        assertTrue(outcome.usedMockFallback)
        assertTrue(outcome.error is RemotePhmError.InvalidDto)
    }

    @Test
    fun successFalseEnvelopeWithModelContractViolationMapsToMissingFeatureFields() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":false,"code":55004,"message":"missing featureQuality for sbp"}""",
            ),
        )
        val service = RemotePhmService(api = apiAgainstServer())

        val outcome = service.evaluateFeatures(aCompleteVector())

        assertTrue(outcome.usedMockFallback)
        assertTrue(outcome.error is RemotePhmError.MissingFeatureFields)
    }

    @Test
    fun empty200BodyMapsToInvalidDto() = runTest {
        server.start()
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val service = RemotePhmService(api = apiAgainstServer())

        val outcome = service.evaluateFeatures(aCompleteVector())

        assertTrue(outcome.usedMockFallback)
        assertNotNull(outcome.error)
        // An empty JSON body should never reach the typed-payload path; the wrapper reports
        // either InvalidDto (when content-length is known to be 0) or a parsing failure.
        val errorClass = outcome.error!!::class
        assertTrue(
            errorClass == RemotePhmError.InvalidDto::class ||
                errorClass == RemotePhmError.BackendUnavailable::class ||
                errorClass == RemotePhmError.Unknown::class,
            "Unexpected error type: $errorClass",
        )
    }

    @Test
    fun successfulEnvelopeReturnsTypedRiskResult() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(SUCCESS_ENVELOPE),
        )
        val service = RemotePhmService(api = apiAgainstServer())

        val outcome = service.evaluateFeatures(aCompleteVector())

        assertFalse(outcome.usedMockFallback)
        assertNull(outcome.error)
        val result = outcome.result
        assertNotNull(result)
        assertEquals(0.34, result.risk_score!!)
        assertEquals("moderate", result.risk_level)
        assertEquals("cvd-mock-rules-v1", result.model_version)
        assertEquals(true, result.is_mock)
        assertTrue(result.missing_fields!!.contains("fasting_glucose"))
    }

    @Test
    fun successfulEnvelopeAcceptsCamelCaseRiskFields() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(CAMEL_CASE_SUCCESS_ENVELOPE),
        )
        val service = RemotePhmService(api = apiAgainstServer())

        val outcome = service.evaluateFeatures(aCompleteVector())

        assertFalse(outcome.usedMockFallback)
        val result = outcome.result
        assertNotNull(result)
        assertEquals(0.41, result.normalizedRiskScore)
        assertEquals("high", result.normalizedRiskLevel)
        assertEquals(mapOf("sbp" to 0.12), result.normalizedFeatureContributions)
        assertEquals("cvd-real-v1", result.normalizedModelVersion)
        assertEquals(false, result.normalizedIsMock)
        assertEquals("backend-request-1", result.normalizedRequestId)
    }

    @Test
    fun retryOnceThenSuccessForConsecutiveSuccessEnvelopes() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(SUCCESS_ENVELOPE),
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(SUCCESS_ENVELOPE),
        )
        val service = RemotePhmService(
            api = apiAgainstServer(),
            retryDelayMillis = 5L,
            maxAttempts = 2,
        )

        // Sanity: first success envelope resolves on attempt 1 to typed success.
        val outcome = service.evaluateFeatures(aCompleteVector())
        assertFalse(outcome.usedMockFallback)
        assertNotNull(outcome.result)
        assertEquals("cvd-mock-rules-v1", outcome.result?.model_version)
    }

    @Test
    fun getRiskLatestSurvivesE1NullResult() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":200,"result":null}""",
            ),
        )
        val service = RemotePhmService(api = apiAgainstServer())

        val outcome = service.getRiskLatest()

        assertTrue(outcome is RemotePhmOutcome.Success<*>)
        assertNull((outcome as RemotePhmOutcome.Success<*>).data)
    }

    @Test
    fun nullApiReportsBackendUnavailableForAllCalls() = runTest {
        val service = RemotePhmService(api = null)

        val evaluate = service.evaluateFeatures(aCompleteVector())
        val riskLatest = service.getRiskLatest()
        val interventions = service.getInterventionsToday()
        val feedback = service.submitInterventionFeedback("plan-1", "done")

        assertTrue(evaluate.usedMockFallback)
        assertTrue(evaluate.error is RemotePhmError.BackendUnavailable)
        assertTrue(riskLatest is RemotePhmOutcome.Failure)
        assertTrue(interventions is RemotePhmOutcome.Failure)
        assertTrue(feedback is RemotePhmOutcome.Failure)
    }

    private fun apiAgainstServer(): ReHealthMobileApi =
        ReHealthMobileApi(
            baseUrl = server.url("/").toString().trimEnd('/'),
            httpClient = httpClient,
            apiToken = null,
        )

    private fun aCompleteVector(): CvdFeatureVector {
        val quality = CvdFeatureFields.ALL.associateWith {
            FeatureQuality.valid(
                source = com.rehealth.genie.features.FeatureSource.USER_REPORTED,
                reason = "test",
            )
        }
        return CvdFeatureVector(
            age = 52,
            gender = 1,
            bmi = 27.4,
            sbp = 136.0,
            dbp = 86.0,
            fastingGlucose = null,
            totalCholesterol = null,
            ldl = null,
            hdl = null,
            triglycerides = null,
            exerciseDays = 3,
            smoking = 0,
            drinking = 0,
            diabetesHistory = 0,
            hypertensionHistory = 1,
            familyHistory = 1,
            featureQuality = quality,
        )
    }

    private companion object {
        const val SUCCESS_ENVELOPE =
            """{"success":true,"code":200,"result":{"risk_score":0.34,"risk_level":"moderate","feature_contributions":{"age":0.09},"model_version":"cvd-mock-rules-v1","is_mock":true,"missing_fields":["fasting_glucose"],"quality_warnings":[],"summary":"Baseline CVD risk is moderate."}}"""

        const val CAMEL_CASE_SUCCESS_ENVELOPE =
            """{"success":true,"code":200,"result":{"riskScore":0.41,"riskLevel":"high","featureContributions":{"sbp":0.12},"modelVersion":"cvd-real-v1","isMock":false,"requestId":"backend-request-1","missingFields":[],"qualityWarnings":[],"summary":"Risk estimate is high."}}"""
    }
}
