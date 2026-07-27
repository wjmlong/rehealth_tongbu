package com.rehealth.genie.data.sync

import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthState
import com.rehealth.genie.network.MeasurementUploadClient
import com.rehealth.genie.network.HealthInterviewUploadClient
import com.rehealth.genie.network.dto.HealthInterviewSubmitRequestDto
import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import com.rehealth.genie.network.dto.TelemetryBatchResponseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SyncRepositoryMeasurementTest {
    @Test
    fun `marks measurement done when backend confirms durable persistence`() = runTest {
        val dao = FakeUploadQueueDao()
        val client = FakeMeasurementUploadClient(
            ApiResult.Success(
                TelemetryBatchResponseDto(
                    batchId = "batch-1",
                    status = "ACCEPTED_PERSISTED",
                    accepted = true,
                    persisted = true,
                    recordCount = 1,
                ),
            ),
        )
        val repository = SyncRepository(dao, client, nowProvider = { NOW })
        val item = validQueueItem()

        val outcome = repository.uploadMeasurement(item)

        assertIs<MeasurementUploadOutcome.Uploaded>(outcome)
        assertEquals("done", dao.saved.single().status)
        assertEquals("batch-1", client.requests.single().batchId)
    }

    @Test
    fun `pauses queue without mutating item when backend returns unauthorized`() = runTest {
        val dao = FakeUploadQueueDao()
        val client = FakeMeasurementUploadClient(ApiResult.Unauthorized("expired"))
        val repository = SyncRepository(dao, client, nowProvider = { NOW })

        val outcome = repository.uploadMeasurement(validQueueItem())

        assertIs<MeasurementUploadOutcome.Paused>(outcome)
        assertIs<QueueState.Paused>(repository.queueState.value)
        assertTrue(dao.saved.isEmpty())
    }

    @Test
    fun `backs off same batch when hardware persistence returns service unavailable`() = runTest {
        val dao = FakeUploadQueueDao()
        val client = FakeMeasurementUploadClient(ApiResult.ServiceUnavailable("503"))
        val repository = SyncRepository(dao, client, nowProvider = { NOW })
        val item = validQueueItem(attempts = 1)

        val outcome = repository.uploadMeasurement(item)

        assertIs<MeasurementUploadOutcome.RetryScheduled>(outcome)
        val saved = dao.saved.single()
        assertEquals("failed", saved.status)
        assertEquals(2, saved.attempts)
        assertEquals(NOW + 60_000L, saved.nextRetryAt)
        assertEquals("measurement_upload_service_unavailable", saved.lastError)
        assertEquals(item.id, client.requests.single().batchId)
    }

    @Test
    fun `backs off same batch on network failure`() = runTest {
        val dao = FakeUploadQueueDao()
        val client = FakeMeasurementUploadClient(ApiResult.NetworkError("offline"))
        val repository = SyncRepository(dao, client, nowProvider = { NOW })

        val outcome = repository.uploadMeasurement(validQueueItem())

        assertIs<MeasurementUploadOutcome.RetryScheduled>(outcome)
        assertEquals(NOW + 30_000L, dao.saved.single().nextRetryAt)
        assertEquals("measurement_upload_network", dao.saved.single().lastError)
    }

    @Test
    fun `dead letters malformed payload without sending or storing health values in error`() = runTest {
        val dao = FakeUploadQueueDao()
        val client = FakeMeasurementUploadClient(ApiResult.NetworkError("unused"))
        val repository = SyncRepository(dao, client, nowProvider = { NOW })
        val item = validQueueItem(payloadJson = "{\"measurements\":[{\"primaryValue\":72}]")

        val outcome = repository.uploadMeasurement(item)

        assertIs<MeasurementUploadOutcome.DeadLettered>(outcome)
        val saved = dao.saved.single()
        assertEquals("dead_letter", saved.status)
        assertEquals("measurement_payload_invalid", saved.lastError)
        assertFalse(saved.lastError.orEmpty().contains("72"))
        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun `uploads queued health interview through authenticated client`() = runTest {
        val dao = FakeUploadQueueDao()
        val interview = HealthInterviewSubmitRequestDto(
            answers = listOf(
                com.rehealth.genie.network.dto.HealthInterviewAnswerDto("profile", "PROFILE", "32 岁"),
            ),
            generatedAt = NOW,
        )
        val client = FakeMeasurementUploadClient(
            result = ApiResult.NetworkError("unused"),
            interviewResult = ApiResult.Success(interview),
        )
        val repository = SyncRepository(dao, client, nowProvider = { NOW })
        val item = UploadQueueEntity(
            id = "interview-1",
            kind = "health_interview",
            payloadJson = com.google.gson.Gson().toJson(interview),
            status = "pending",
            createdAt = NOW,
            nextRetryAt = NOW,
        )

        val outcome = repository.uploadQueuedItem(item)

        assertIs<MeasurementUploadOutcome.Uploaded>(outcome)
        assertEquals("done", dao.saved.single().status)
        assertEquals("profile", client.interviewRequests.single().answers.single().questionId)
    }

    private fun validQueueItem(
        attempts: Int = 0,
        payloadJson: String = VALID_PAYLOAD,
    ) = UploadQueueEntity(
        id = "batch-1",
        kind = "telemetry_batch",
        payloadJson = payloadJson,
        status = "pending",
        attempts = attempts,
        createdAt = NOW,
        nextRetryAt = NOW,
    )

    private companion object {
        const val NOW = 1_720_000_000_000L
        const val VALID_PAYLOAD = """
            {"batchId":"batch-1","deviceId":"mrd-a1","collectedFrom":1719999999000,
             "collectedTo":1720000000000,"source":"ANDROID_ROOM",
             "measurements":[{"id":"m-1","metricType":"HEART_RATE","measuredAt":1720000000000,
             "primaryValue":72.0,"unit":"bpm","source":"mrd_ring"}],
             "sleepSessions":[],"activitySessions":[],"signalChunks":[],
             "quality":{"schemaVersion":"d2-v1","rawSignalExcluded":true}}
        """
    }
}

private class FakeMeasurementUploadClient(
    private val result: ApiResult<TelemetryBatchResponseDto>,
    private val interviewResult: ApiResult<HealthInterviewSubmitRequestDto> = ApiResult.NetworkError("unused"),
) : MeasurementUploadClient, HealthInterviewUploadClient {
    override var authState: AuthState = AuthState.Authorized
    val requests = mutableListOf<TelemetryBatchRequestDto>()
    val interviewRequests = mutableListOf<HealthInterviewSubmitRequestDto>()

    override suspend fun uploadMeasurements(
        request: TelemetryBatchRequestDto,
    ): ApiResult<TelemetryBatchResponseDto> {
        requests += request
        if (result is ApiResult.Unauthorized) authState = AuthState.Unauthorized
        return result
    }

    override suspend fun submitHealthInterview(
        request: HealthInterviewSubmitRequestDto,
    ): ApiResult<HealthInterviewSubmitRequestDto> {
        interviewRequests += request
        if (interviewResult is ApiResult.Unauthorized) authState = AuthState.Unauthorized
        return interviewResult
    }
}

private class FakeUploadQueueDao : UploadQueueDao {
    val saved = mutableListOf<UploadQueueEntity>()
    val rows = mutableListOf<UploadQueueEntity>()

    override suspend fun insert(item: UploadQueueEntity) {
        rows.removeAll { it.id == item.id }
        rows += item
    }

    override suspend fun update(item: UploadQueueEntity) {
        saved += item
        rows.removeAll { it.id == item.id }
        rows += item
    }

    override suspend fun pending(now: Long): List<UploadQueueEntity> =
        rows.filter { it.status in setOf("pending", "failed") && it.nextRetryAt <= now }

    override fun observeOutstanding(): Flow<List<UploadQueueEntity>> = flowOf(rows)
    override suspend fun pruneDone(before: Long) = Unit
    override suspend fun getById(id: String): UploadQueueEntity? = rows.firstOrNull { it.id == id }
    override suspend fun getPendingByKind(kind: String): List<UploadQueueEntity> = rows.filter { it.kind == kind }
}
