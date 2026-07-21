package com.rehealth.genie.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ReHealthBackendClient(
    private val baseUrl: String,
    private val apiToken: String? = null,
    private val httpClient: OkHttpClient = OkHttpClient.Builder().build(),
    private val gson: Gson = Gson(),
) {
    suspend fun uploadRingSnapshot(
        collectedAt: Long,
        trigger: String,
        device: RingDevice?,
        measurements: List<RingMeasurementEntity>,
        sleep: RingSleepSessionEntity?,
        activity: RingActivityEntity?,
        signals: List<RingSignalChunkEntity>,
    ): Result<CloudUploadResult> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = RingSnapshotPayload(
                collectedAt = collectedAt,
                trigger = trigger,
                device = device?.let {
                    DevicePayload(address = it.address, name = it.name, rssi = it.rssi)
                },
                measurements = measurements.map {
                    MeasurementPayload(
                        metricType = it.metricType,
                        measuredAt = it.measuredAt,
                        primaryValue = it.primaryValue,
                        secondaryValue = it.secondaryValue,
                        unit = it.unit,
                        quality = it.quality,
                        source = it.source,
                    )
                },
                sleep = sleep?.let {
                    SleepPayload(
                        startedAt = it.startedAt,
                        endedAt = it.endedAt,
                        deepMinutes = it.deepMinutes,
                        lightMinutes = it.lightMinutes,
                        awakeMinutes = it.awakeMinutes,
                        remMinutes = it.remMinutes,
                        interruptionMinutes = it.interruptionMinutes,
                        source = it.source,
                    )
                },
                activity = activity?.let {
                    ActivityPayload(
                        startedAt = it.startedAt,
                        endedAt = it.endedAt,
                        activityType = it.activityType,
                        steps = it.steps,
                        distanceMeters = it.distanceMeters,
                        caloriesKcal = it.caloriesKcal,
                        durationMinutes = it.durationMinutes,
                        averageHeartRate = it.averageHeartRate,
                        source = it.source,
                    )
                },
                signals = signals.map {
                    SignalPayload(
                        signalType = it.signalType,
                        startedAt = it.startedAt,
                        sampleRateHz = it.sampleRateHz,
                        sampleCount = it.sampleCount,
                        encoding = it.encoding,
                        source = it.source,
                    )
                },
            )
            postJson("/rehealth/mobile/ring/snapshots", payload)
        }
    }

    suspend fun fetchPatientMvp(): Result<PatientMvpPayload> = withContext(Dispatchers.IO) {
        runCatching {
            gson.fromJson(getJson("/rehealth/mobile/patient/mvp").resultJson(), PatientMvpPayload::class.java)
        }
    }

    suspend fun submitCheckIn(
        itemId: String,
        status: String = "done",
        mood: String = "stable",
        note: String? = null,
    ): Result<PatientCheckInPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = CheckInPayload(
                itemId = itemId,
                status = status,
                mood = mood,
                note = note,
                checkedAt = System.currentTimeMillis(),
            )
            gson.fromJson(postRawJson("/rehealth/mobile/patient/checkins", payload).resultJson(), PatientCheckInPayload::class.java)
        }
    }

    private fun postJson(path: String, payload: Any): CloudUploadResult {
        val responseBody = postRawJson(path, payload)
        return CloudUploadResult(
            ok = true,
            message = responseBody.ifBlank { "uploaded" },
            snapshotId = responseBody.findString("snapshotId"),
            riskScore = responseBody.findDouble("riskScore"),
            riskLevel = responseBody.findString("riskLevel"),
            riskMode = responseBody.findString("mode"),
            riskSummary = responseBody.findString("summary"),
            modelVersion = responseBody.findString("modelVersion"),
        )
    }

    private fun getJson(path: String): String {
        val url = baseUrl.trimEnd('/') + path
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
        apiToken?.let { requestBuilder.header("X-Access-Token", it) }
        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${responseBody.take(160)}")
            }
            return responseBody
        }
    }

    private fun postRawJson(path: String, payload: Any): String {
        val url = baseUrl.trimEnd('/') + path
        val body = gson.toJson(payload).toRequestBody(JSON)
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", "application/json")
        apiToken?.let { requestBuilder.header("X-Access-Token", it) }
        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${responseBody.take(160)}")
            }
            return responseBody
        }
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

data class CloudUploadResult(
    val ok: Boolean,
    val message: String,
    val snapshotId: String? = null,
    val riskScore: Double? = null,
    val riskLevel: String? = null,
    val riskMode: String? = null,
    val riskSummary: String? = null,
    val modelVersion: String? = null,
)

data class PatientMvpPayload(
    val profile: PatientProfilePayload?,
    val risk: PatientRiskPayload?,
    val interventionPlan: List<PatientInterventionPayload>?,
    val recentCheckins: List<PatientCheckInPayload>?,
    val updatedAt: Long?,
)

data class PatientProfilePayload(
    val patientId: String?,
    val name: String?,
    val gender: String?,
    val age: Int?,
    val heightCm: Double?,
    val weightKg: Double?,
    val bmi: Double?,
    val diagnoses: List<String>?,
    val medications: List<String>?,
    val allergies: List<String>?,
    val familyHistory: Boolean?,
    val smoking: Boolean?,
    val drinking: Boolean?,
    val diabetesHistory: Boolean?,
    val hypertensionHistory: Boolean?,
    val updatedAt: Long?,
)

data class PatientRiskPayload(
    val mode: String?,
    val modelVersion: String?,
    val riskScore: Double?,
    val riskLevel: String?,
    val summary: String?,
    val generatedAt: String?,
)

data class PatientInterventionPayload(
    val id: String?,
    val title: String?,
    val goal: String?,
    val action: String?,
    val duration: String?,
    val reason: String?,
    val status: String?,
)

data class PatientCheckInPayload(
    val checkInId: String?,
    val itemId: String?,
    val status: String?,
    val mood: String?,
    val note: String?,
    val checkedAt: Long?,
)

private data class CheckInPayload(
    val itemId: String,
    val status: String,
    val mood: String,
    val note: String?,
    val checkedAt: Long,
)

private fun String.findString(key: String): String? =
    parseJsonObject()
        ?.deepFind(key)
        ?.takeIf { !it.isJsonNull && it.isJsonPrimitive }
        ?.asString

private fun String.findDouble(key: String): Double? =
    parseJsonObject()
        ?.deepFind(key)
        ?.takeIf { !it.isJsonNull && it.isJsonPrimitive }
        ?.let { element -> runCatching { element.asDouble }.getOrNull() }

private fun String.parseJsonObject(): JsonObject? =
    runCatching { JsonParser.parseString(this).asJsonObject }.getOrNull()

private fun String.resultJson(): String {
    val json = parseJsonObject() ?: return this
    return json.get("result")?.toString() ?: this
}

private fun JsonObject.deepFind(key: String): com.google.gson.JsonElement? {
    get(key)?.let { return it }
    entrySet().forEach { (_, value) ->
        if (value.isJsonObject) {
            value.asJsonObject.deepFind(key)?.let { return it }
        }
    }
    return null
}

private data class RingSnapshotPayload(
    val collectedAt: Long,
    val trigger: String,
    val device: DevicePayload?,
    val measurements: List<MeasurementPayload>,
    val sleep: SleepPayload?,
    val activity: ActivityPayload?,
    val signals: List<SignalPayload>,
)

private data class DevicePayload(
    val address: String,
    val name: String?,
    val rssi: Int?,
)

private data class MeasurementPayload(
    val metricType: String,
    val measuredAt: Long,
    val primaryValue: Double,
    val secondaryValue: Double?,
    val unit: String,
    val quality: Int?,
    val source: String,
)

private data class SleepPayload(
    val startedAt: Long,
    val endedAt: Long,
    val deepMinutes: Int,
    val lightMinutes: Int,
    val awakeMinutes: Int,
    val remMinutes: Int,
    val interruptionMinutes: Int,
    val source: String,
)

private data class ActivityPayload(
    val startedAt: Long,
    val endedAt: Long?,
    val activityType: String,
    val steps: Int,
    val distanceMeters: Double,
    val caloriesKcal: Double,
    val durationMinutes: Int,
    val averageHeartRate: Double?,
    val source: String,
)

private data class SignalPayload(
    val signalType: String,
    val startedAt: Long,
    val sampleRateHz: Int?,
    val sampleCount: Int,
    val encoding: String,
    val source: String,
)
