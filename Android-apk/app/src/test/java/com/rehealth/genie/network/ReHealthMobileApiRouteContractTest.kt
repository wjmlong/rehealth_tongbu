package com.rehealth.genie.network

import com.rehealth.genie.network.dto.CvdFeatureVectorDto
import com.rehealth.genie.network.dto.FeatureEvaluateRequest
import com.rehealth.genie.network.dto.InterventionFeedbackRequest
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReHealthMobileApiRouteContractTest {
    private val server = MockWebServer()

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `preserves Jeecg context for evaluate risk intervention and feedback`() = runTest {
        server.start()
        repeat(4) {
            server.enqueue(MockResponse().setResponseCode(200).setBody(SUCCESS_ENVELOPE))
        }
        val api = ReHealthMobileApi(
            baseUrl = server.url("/jeecg-boot/").toString(),
            httpClient = OkHttpClient(),
            apiToken = "synthetic-test-token",
        )

        assertIs<RemotePhmOutcome.Success<*>>(
            api.evaluateFeatures(
                FeatureEvaluateRequest(
                    featureVector = CvdFeatureVectorDto(featureQuality = emptyMap()),
                ),
            ),
        )
        assertRequest("/jeecg-boot/rehealth/mobile/features/evaluate", "POST")

        assertIs<RemotePhmOutcome.Success<*>>(api.getRiskLatest())
        assertRequest("/jeecg-boot/rehealth/mobile/risk/latest", "GET")

        assertIs<RemotePhmOutcome.Success<*>>(api.getInterventionsToday())
        assertRequest("/jeecg-boot/rehealth/mobile/interventions/today", "GET")

        assertIs<RemotePhmOutcome.Success<*>>(
            api.submitInterventionFeedback(
                interventionId = "plan-7",
                request = InterventionFeedbackRequest(status = "completed"),
            ),
        )
        assertRequest("/jeecg-boot/rehealth/mobile/interventions/plan-7/feedback", "POST")
    }

    private fun assertRequest(expectedPath: String, expectedMethod: String) {
        val request = server.takeRequest()
        assertEquals(expectedPath, request.path)
        assertEquals(expectedMethod, request.method)
        assertEquals("synthetic-test-token", request.getHeader("X-Access-Token"))
    }

    private companion object {
        const val SUCCESS_ENVELOPE = """
            {"success":true,"code":200,"result":{}}
        """
    }
}
