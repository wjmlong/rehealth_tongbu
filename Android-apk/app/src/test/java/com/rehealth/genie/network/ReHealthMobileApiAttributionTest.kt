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
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReHealthMobileApiAttributionTest {
    private val server = MockWebServer()

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `posts history to exact authenticated Jeecg attribution route`() = runTest {
        server.start()
        server.enqueue(MockResponse().setResponseCode(200).setBody(READY_ENVELOPE))
        val api = ReHealthMobileApi(
            baseUrl = server.url("/jeecg-boot/").toString(),
            httpClient = OkHttpClient(),
            apiToken = "synthetic-test-token",
        )
        val request = IndividualAttributionRequestDto(
            risk_history = listOf(AttributionHistoryPointDto("2026-07-22", 0.219, 1)),
            forecast_days = 30,
            language = "zh",
        )

        val outcome = api.attributeIndividual(request)

        val recorded = server.takeRequest()
        assertEquals("/jeecg-boot/rehealth/mobile/attribution/events", recorded.path)
        assertEquals("synthetic-test-token", recorded.getHeader("X-Access-Token"))
        assertEquals("POST", recorded.method)
        val requestJson = recorded.body.readUtf8()
        assertTrue(requestJson.contains("\"risk_history\""))
        assertTrue(requestJson.contains("\"Y\":0.219"))
        assertTrue(requestJson.contains("\"Z\":1"))
        val success = assertIs<RemotePhmOutcome.Success<*>>(outcome)
        assertEquals("ready", success.data.let { it as com.rehealth.genie.network.dto.IndividualAttributionResponseDto }.status)
    }

    private companion object {
        const val READY_ENVELOPE = """
            {"success":true,"code":200,"result":{
              "status":"ready","history_days":14,"min_history_days":14,
              "current_state":{"risk_score":0.219,"risk_level":"low","trend":"improving"},
              "forecast":{"raw":{"dates":["Day 1"],"no_action":[0.22],"with_plan":[0.21],"ci_upper":[0.23],"ci_lower":[0.20]},"summary":{"30d_no_action":0.22,"30d_with_plan":0.21,"risk_reduction":0.01}},
              "intervention_effect":{"individual_att":-0.01,"att_ci_lower":-0.02,"att_ci_upper":0.0,"att_p_value":0.04,"att_significant":true},
              "reports":{"user":{"headline":"计划有效","body":"合成测试报告","advice":"继续保持"}}
            }}
        """
    }
}
