package com.rehealth.genie.network

import com.rehealth.genie.network.dto.CvdFeatureVectorDto
import com.rehealth.genie.network.dto.DeviceBindRequestDto
import com.rehealth.genie.network.dto.FeatureEvaluateRequest
import com.rehealth.genie.network.dto.HealthAgentMessageRequest
import com.rehealth.genie.network.dto.InterventionFeedbackRequest
import com.rehealth.genie.network.dto.InterventionGenerateRequestDto
import com.rehealth.genie.network.dto.HealthInterviewAnswerDto
import com.rehealth.genie.network.dto.HealthInterviewSubmitRequestDto
import com.rehealth.genie.network.dto.RhiManualHealthInputDto
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

    @Test
    fun `submits health interview to authenticated Jeecg route`() = runTest {
        server.start()
        server.enqueue(MockResponse().setResponseCode(200).setBody(INTERVIEW_ENVELOPE))
        val api = ReHealthMobileApi(
            baseUrl = server.url("/jeecg-boot/").toString(),
            httpClient = OkHttpClient(),
            apiToken = "synthetic-test-token",
        )
        val payload = HealthInterviewSubmitRequestDto(
            answers = listOf(HealthInterviewAnswerDto("profile", "PROFILE", "32 岁")),
            generatedAt = 1_726_000_000_000L,
        )

        assertIs<RemotePhmOutcome.Success<*>>(api.submitHealthInterview(payload))

        val request = server.takeRequest()
        assertEquals("/jeecg-boot/rehealth/mobile/interviews", request.path)
        assertEquals("POST", request.method)
        assertEquals("synthetic-test-token", request.getHeader("X-Access-Token"))
        assertTrue(request.body.readUtf8().contains("\"questionId\":\"profile\""))
    }

    @Test
    fun `reads and writes manual RHI health archive through authenticated routes`() = runTest {
        server.start()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true,"code":200,"result":null}"""))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":200,"result":{"sedentaryHoursPerDay":8.5,"waistCircumferenceCm":92.0,"updatedAt":1720000000000}}""",
            ),
        )
        val api = ReHealthMobileApi(
            baseUrl = server.url("/jeecg-boot/").toString(),
            httpClient = OkHttpClient(),
            apiToken = "synthetic-test-token",
        )

        assertIs<RemotePhmOutcome.Success<*>>(api.getRhiManualHealthInput())
        assertRequest("/jeecg-boot/rehealth/mobile/rhi/manual-inputs", "GET")

        assertIs<RemotePhmOutcome.Success<*>>(
            api.updateRhiManualHealthInput(
                RhiManualHealthInputDto(
                    sedentaryHoursPerDay = 8.5,
                    waistCircumferenceCm = 92.0,
                    updatedAt = 1_720_000_000_000L,
                ),
            ),
        )
        assertRequest("/jeecg-boot/rehealth/mobile/rhi/manual-inputs", "PUT")
    }

    @Test
    fun `reads latest health interview from authenticated Jeecg route`() = runTest {
        server.start()
        server.enqueue(MockResponse().setResponseCode(200).setBody(INTERVIEW_ENVELOPE))
        val api = ReHealthMobileApi(
            baseUrl = server.url("/jeecg-boot/").toString(),
            httpClient = OkHttpClient(),
            apiToken = "synthetic-test-token",
        )

        assertIs<RemotePhmOutcome.Success<*>>(api.getLatestHealthInterview())

        assertRequest("/jeecg-boot/rehealth/mobile/interviews/latest", "GET")
    }

    @Test
    fun `restores latest authenticated health chat conversation`() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":200,"result":{"conversationId":"conversation-1","messages":[]}}""",
            ),
        )
        val api = ReHealthMobileApi(
            baseUrl = server.url("/jeecg-boot/").toString(),
            httpClient = OkHttpClient(),
            apiToken = "synthetic-test-token",
        )

        val result = assertIs<RemotePhmOutcome.Success<*>>(api.getLatestHealthAgentConversation(50))
        val conversation = result.data as com.rehealth.genie.network.dto.HealthAgentConversation
        assertEquals("conversation-1", conversation.conversationId)
        assertRequest("/jeecg-boot/rehealth/mobile/agent/conversations/latest?limit=50", "GET")
    }

    @Test
    fun `uploads camera photo and reads today behavior records`() = runTest {
        server.start()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":200,"result":{"id":"behavior-1","category":"FOOD","title":"午餐","caloriesKcal":520}}""",
            ),
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"code":200,"result":[{"id":"behavior-1","category":"FOOD","title":"午餐"}]}""",
            ),
        )
        val api = ReHealthMobileApi(
            baseUrl = server.url("/jeecg-boot/").toString(),
            httpClient = OkHttpClient(),
            apiToken = "synthetic-test-token",
        )

        val analyzed = assertIs<RemotePhmOutcome.Success<*>>(
            api.analyzeBehaviorPhoto(
                image = byteArrayOf(1, 2, 3),
                contentType = "image/jpeg",
                fileName = "meal.jpg",
                requestId = "behavior-request-1",
                occurredAt = 1_785_470_400_000L,
            ),
        )
        assertEquals("午餐", (analyzed.data as com.rehealth.genie.network.dto.BehaviorRecordDto).title)
        val upload = server.takeRequest()
        assertEquals("/jeecg-boot/rehealth/mobile/behavior-records/analyze-photo", upload.path)
        assertEquals("POST", upload.method)
        assertEquals("synthetic-test-token", upload.getHeader("X-Access-Token"))
        val multipart = upload.body.readUtf8()
        assertTrue(multipart.contains("behavior-request-1"))
        assertTrue(multipart.contains("meal.jpg"))

        val today = assertIs<RemotePhmOutcome.Success<*>>(
            api.getTodayBehaviorRecords("2026-07-31", 480),
        )
        assertEquals(1, (today.data as List<*>).size)
        assertRequest(
            "/jeecg-boot/rehealth/mobile/behavior-records/today?date=2026-07-31&zoneOffsetMinutes=480",
            "GET",
        )
    }

    @Test
    fun `photo analysis uses its dedicated long read timeout`() = runTest {
        server.start()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"success":true,"code":200,"result":{"id":"behavior-2","category":"FOOD","title":"晚餐"}}""",
                )
                .setBodyDelay(300, TimeUnit.MILLISECONDS),
        )
        val api = ReHealthMobileApi(
            baseUrl = server.url("/jeecg-boot/").toString(),
            httpClient = OkHttpClient.Builder()
                .readTimeout(100, TimeUnit.MILLISECONDS)
                .build(),
            apiToken = "synthetic-test-token",
        )

        assertIs<RemotePhmOutcome.Success<*>>(
            api.analyzeBehaviorPhoto(
                image = byteArrayOf(1, 2, 3),
                contentType = "image/jpeg",
                fileName = "meal.jpg",
                requestId = "behavior-request-2",
                occurredAt = 1_785_470_400_000L,
            ),
        )
        assertRequest("/jeecg-boot/rehealth/mobile/behavior-records/analyze-photo", "POST")
    }

    @Test
    fun `uses authenticated software loop routes and parses persistence acknowledgements`() = runTest {
        server.start()
        listOf(
            """{"success":true,"code":200,"result":{"modelContract":"cvd-feature-vector-v1"}}""",
            """{"success":true,"code":200,"result":{"patientId":"patient-1","age":42}}""",
            """{"success":true,"code":200,"result":{"deviceId":"mrd-a1","status":"BOUND","persisted":true,"persistenceStage":"software_db"}}""",
            """{"success":true,"code":200,"result":{"plan_id":"plan-9","priority_intervention":"步行"}}""",
            """{"success":true,"code":200,"result":{"interventionId":"plan-9","status":"completed","persisted":true,"persistenceStage":"software_db"}}""",
            """{"success":true,"code":200,"result":{"request_id":"agent-1","status":"ok","answer":"请保持规律作息","provider":"model-service"}}""",
        ).forEach { server.enqueue(MockResponse().setResponseCode(200).setBody(it)) }
        val api = ReHealthMobileApi(
            baseUrl = server.url("/jeecg-boot/").toString(),
            httpClient = OkHttpClient(),
            apiToken = "synthetic-test-token",
        )

        val config = assertIs<RemotePhmOutcome.Success<*>>(api.getConfig()).data
        assertEquals("cvd-feature-vector-v1", (config as com.rehealth.genie.network.dto.MobileConfigResponse).modelContract)
        assertRequest("/jeecg-boot/rehealth/mobile/config", "GET")

        assertIs<RemotePhmOutcome.Success<*>>(api.getProfile())
        assertRequest("/jeecg-boot/rehealth/mobile/profile", "GET")

        val bind = assertIs<RemotePhmOutcome.Success<*>>(
            api.bindDevice(DeviceBindRequestDto(deviceId = "mrd-a1", hardwareAddressHash = "hash")),
        ).data as com.rehealth.genie.network.dto.DeviceBindResponseDto
        assertTrue(bind.persisted)
        assertRequest("/jeecg-boot/rehealth/mobile/devices/bind", "POST")

        assertIs<RemotePhmOutcome.Success<*>>(
            api.generateIntervention(InterventionGenerateRequestDto()),
        )
        assertRequest("/jeecg-boot/rehealth/mobile/interventions/generate", "POST")

        val feedback = assertIs<RemotePhmOutcome.Success<*>>(
            api.submitInterventionFeedback("plan-9", InterventionFeedbackRequest("completed")),
        ).data as com.rehealth.genie.network.dto.InterventionFeedbackResponse
        assertTrue(feedback.persisted)
        assertRequest("/jeecg-boot/rehealth/mobile/interventions/plan-9/feedback", "POST")

        val agent = assertIs<RemotePhmOutcome.Success<*>>(
            api.sendHealthAgentMessage(
                HealthAgentMessageRequest(
                    requestId = "agent-1",
                    conversationId = "conversation-1",
                    clientMessageId = "message-1",
                    message = "如何改善睡眠？",
                ),
            ),
        ).data as com.rehealth.genie.network.dto.HealthAgentResponse
        assertEquals("请保持规律作息", agent.answer)
        assertRequest("/jeecg-boot/rehealth/mobile/agent/messages", "POST")
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
        const val INTERVIEW_ENVELOPE = """
            {"success":true,"code":200,"result":{"answers":[],"generatedAt":1726000000000}}
        """
    }
}
