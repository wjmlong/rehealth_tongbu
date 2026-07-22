package com.rehealth.genie.network

import com.rehealth.genie.network.dto.AttributionHistoryPointDto
import com.rehealth.genie.network.dto.IndividualAttributionRequestDto
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PiasApiClientTest {
    private val server = MockWebServer()

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `parses accumulating status and ATT availability fields`() = runTest {
        server.start()
        server.enqueue(MockResponse().setResponseCode(200).setBody(ATTRIBUTION_ENVELOPE))
        val client = PiasApiClient(
            baseUrl = server.url("/api/pias/v2/").toString(),
            httpClient = OkHttpClient(),
        )

        val response = client.attributeIndividual(
            IndividualAttributionRequestDto(
                riskHistory = listOf(AttributionHistoryPointDto("2026-07-22", 0.219, 0)),
                forecastDays = 30,
                language = "zh",
            ),
        ).getOrThrow()

        assertEquals("accumulating", response.status)
        assertEquals(5, response.historyDays)
        assertEquals(14, response.minHistoryDays)
        assertFalse(response.interventionDataSufficient!!)
        assertFalse(response.interventionEffect?.attAvailable!!)
        assertEquals("intervention_days_lt_7", response.interventionEffect?.attUnavailableReason)
    }

    private companion object {
        const val ATTRIBUTION_ENVELOPE = """
            {
              "success": true,
              "message": "ok",
              "result": {
                "status": "accumulating",
                "history_days": 5,
                "min_history_days": 14,
                "intervention_days": 1,
                "intervention_data_sufficient": false,
                "intervention_effect": {
                  "att_available": false,
                  "att_unavailable_reason": "intervention_days_lt_7",
                  "intervention_days": 1,
                  "intervention_data_sufficient": false
                }
              }
            }
        """
    }
}
