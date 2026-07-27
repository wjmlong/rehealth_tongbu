package com.rehealth.genie.network

import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReHealthMobileApiMeasurementTest {
    private val server = MockWebServer()

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `posts measurement batch to authenticated Jeecg endpoint`() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":200,"result":{"batchId":"batch-1","status":"ACCEPTED_PERSISTED","accepted":true,"persisted":true,"recordCount":1}}""",
            ),
        )
        val api = ReHealthMobileApi(
            baseUrl = server.url("/jeecg-boot/").toString(),
            httpClient = OkHttpClient(),
            apiToken = "synthetic-test-token",
        )
        val request = TelemetryBatchRequestDto(
            batchId = "batch-1",
            deviceId = "mrd-a1",
            measurements = listOf(mapOf("metricType" to "HEART_RATE", "primaryValue" to 72.0)),
        )

        val outcome = api.uploadMeasurements(request)

        val recorded = server.takeRequest()
        assertEquals("/jeecg-boot/rehealth/mobile/measurements/batch", recorded.path)
        assertEquals("synthetic-test-token", recorded.getHeader("X-Access-Token"))
        assertEquals("POST", recorded.method)
        assertTrue(recorded.body.readUtf8().contains("\"batchId\":\"batch-1\""))
        assertIs<RemotePhmOutcome.Success<*>>(outcome)
    }
}
