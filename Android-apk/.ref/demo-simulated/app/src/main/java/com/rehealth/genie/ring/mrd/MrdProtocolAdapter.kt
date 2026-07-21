package com.rehealth.genie.ring.mrd

import android.content.Context
import com.manridy.sdk_mrd2019.Manridy
import com.manridy.sdk_mrd2019.bean.read.BoModel
import com.manridy.sdk_mrd2019.bean.read.BpModel
import com.manridy.sdk_mrd2019.bean.read.HRVModel
import com.manridy.sdk_mrd2019.bean.read.HeartModel
import com.manridy.sdk_mrd2019.bean.read.SleepModel
import com.manridy.sdk_mrd2019.bean.read.SportBean
import com.manridy.sdk_mrd2019.bean.read.TempModel
import com.manridy.sdk_mrd2019.bean.read.history.BoHistoryBean
import com.manridy.sdk_mrd2019.bean.read.history.BpHistoryBean
import com.manridy.sdk_mrd2019.bean.read.history.HrHistoryBean
import com.manridy.sdk_mrd2019.bean.read.history.HrvHistoryBean
import com.manridy.sdk_mrd2019.bean.read.history.PressureHistoryBean
import com.manridy.sdk_mrd2019.bean.read.history.SleepHistoryBean
import com.manridy.sdk_mrd2019.bean.read.history.StepHistoryBean
import com.manridy.sdk_mrd2019.bean.read.history.TempHistoryBean
import com.manridy.sdk_mrd2019.bean.send.StartTestEnum
import com.manridy.sdk_mrd2019.read.MrdReadRequest
import com.google.gson.Gson
import com.rehealth.genie.ring.RequiredRingMetrics
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Thin boundary around the vendor SDK.
 *
 * The SDK creates/parses protocol packets only. BLE transport, retries,
 * permissions and persistence remain owned by the app.
 */
class MrdProtocolAdapter(context: Context) {
    val supportedMetrics: Set<RingMetricType> = RequiredRingMetrics
    private val gson = Gson()

    init {
        Manridy.init(context.applicationContext)
    }

    fun latestCommand(type: RingMetricType): ByteArray = when (type) {
        RingMetricType.HEART_RATE -> Manridy.getMrdSend().getHrData(0).datas
        RingMetricType.HRV -> Manridy.getMrdSend().getHrvData(0).datas
        RingMetricType.BLOOD_OXYGEN -> Manridy.getMrdSend().getBoData(0).datas
        RingMetricType.BLOOD_PRESSURE -> Manridy.getMrdSend().getBpData(0).datas
        RingMetricType.SLEEP -> Manridy.getMrdSend().getSleep(0).datas
        RingMetricType.TEMPERATURE -> Manridy.getMrdSend().getTempData(0).datas
        RingMetricType.STEPS -> Manridy.getMrdSend().getStep(3).datas
        RingMetricType.ACTIVITY -> Manridy.getMrdSend().getStepHistoryData().datas
        RingMetricType.STRESS -> Manridy.getMrdSend().getHRVHistory(0).datas
        RingMetricType.RRI -> Manridy.getMrdSend().getRRI(2, 0).datas
        RingMetricType.PPG -> Manridy.getMrdSend().sportControl(1, true, true, true).datas
    }

    fun manualTestCommand(type: RingMetricType): ByteArray? = when (type) {
        RingMetricType.HEART_RATE -> Manridy.getMrdSend().setHrTest(StartTestEnum.Single_HR_Test.type).datas
        RingMetricType.HRV -> Manridy.getMrdSend().setHrTest(StartTestEnum.Single_HRV_Test.type).datas
        RingMetricType.BLOOD_OXYGEN -> Manridy.getMrdSend().setHrTest(StartTestEnum.Single_BO_Test.type).datas
        RingMetricType.BLOOD_PRESSURE -> Manridy.getMrdSend().setHrTest(StartTestEnum.Single_BP_Test.type).datas
        else -> null
    }

    fun stopManualTestCommand(): ByteArray = Manridy.getMrdSend().stopTest().datas

    fun enableTimingTemperatureCommand(): ByteArray =
        Manridy.getMrdSend().setTimingTempTest(true, 30).datas

    fun getTimingTemperatureCommand(): ByteArray =
        Manridy.getMrdSend().getTimingTempTest().datas

    fun temperatureReadCommands(): List<ByteArray> = listOf(
        Manridy.getMrdSend().getTempData(0).datas,
        Manridy.getMrdSend().getTempData(2).datas,
        Manridy.getMrdSend().getTempData(1).datas,
    )

    fun historyCountCommand(type: RingMetricType): ByteArray? = when (type) {
        RingMetricType.HEART_RATE -> Manridy.getMrdSend().getHrData(2).datas
        RingMetricType.HRV -> Manridy.getMrdSend().getHrvData(2).datas
        RingMetricType.BLOOD_OXYGEN -> Manridy.getMrdSend().getBoData(2).datas
        RingMetricType.BLOOD_PRESSURE -> Manridy.getMrdSend().getBpData(2).datas
        RingMetricType.SLEEP -> Manridy.getMrdSend().getSleep(2).datas
        RingMetricType.TEMPERATURE -> Manridy.getMrdSend().getTempData(2).datas
        RingMetricType.STEPS, RingMetricType.ACTIVITY ->
            Manridy.getMrdSend().getStepHistoryNum().datas
        RingMetricType.STRESS -> Manridy.getMrdSend().getHRVHistory(2).datas
        RingMetricType.RRI -> Manridy.getMrdSend().getRRI(2, 0).datas
        RingMetricType.PPG -> null
    }

    fun parse(packet: ByteArray): MrdReadRequest = Manridy.getMrdRead().read(packet)

    fun toDataBatch(parsedPackets: List<Pair<MrdReadRequest, ByteArray>>, collectedAt: Long): RingDataBatch {
        val measurements = mutableListOf<RingMeasurementEntity>()
        val sleepSessions = mutableListOf<RingSleepSessionEntity>()
        val activities = mutableListOf<RingActivityEntity>()

        parsedPackets.forEach { (request, raw) ->
            val json = request.json.orEmpty()
            if (json.isBlank()) return@forEach
            when (request.mrdReadEnum?.name) {
                "HrLast", "HrTest" -> readAs<HeartModel>(json)?.let { model ->
                    measurements += measurement(
                        type = RingMetricType.HEART_RATE,
                        value = model.heartRate.toDouble(),
                        unit = "bpm",
                        measuredAt = model.updateDate.takeIf { it > 0 } ?: collectedAt,
                        raw = raw,
                    )
                }
                "HrHistory", "Hr_History_Data" -> readAs<HrHistoryBean>(json)?.hrList
                    ?.lastOrNull()
                    ?.takeIf { it > 0 }
                    ?.let { value ->
                        measurements += measurement(RingMetricType.HEART_RATE, value.toDouble(), "bpm", collectedAt, raw)
                    }
                "HrvLast", "HrvTest" -> readAs<HRVModel>(json)?.let { model ->
                    measurements += measurement(
                        type = RingMetricType.HRV,
                        value = model.hrv.toDouble(),
                        unit = "ms",
                        measuredAt = model.updateDate.takeIf { it > 0 } ?: collectedAt,
                        raw = raw,
                    )
                }
                "HrvHistory", "Hrv_History_Data" -> readAs<HrvHistoryBean>(json)?.let { model ->
                    val measuredAt = dateMillis(
                        year = model.year,
                        month = model.month,
                        day = model.day,
                        hour = model.hour,
                        minute = model.minute,
                        second = model.second,
                    ) ?: collectedAt
                    if (model.hrv > 0) {
                        measurements += measurement(RingMetricType.HRV, model.hrv.toDouble(), "ms", measuredAt, raw)
                    }
                }
                "BoLast", "BoTest" -> readAs<BoModel>(json)?.let { model ->
                    model.getboRate().toDoubleOrNull()?.takeIf { it > 0 }?.let { value ->
                        measurements += measurement(
                            type = RingMetricType.BLOOD_OXYGEN,
                            value = value,
                            unit = "%",
                            measuredAt = model.updateDate.takeIf { it > 0 } ?: collectedAt,
                            raw = raw,
                        )
                    }
                }
                "BoHistory", "Bo_History_Data" -> readAs<BoHistoryBean>(json)?.boList
                    ?.lastOrNull()
                    ?.takeIf { it > 0f }
                    ?.let { value ->
                        measurements += measurement(RingMetricType.BLOOD_OXYGEN, value.toDouble(), "%", collectedAt, raw)
                    }
                "BpLast", "BpTest" -> readAs<BpModel>(json)?.let { model ->
                    if (model.bpHp > 0 && model.bpLp > 0) {
                        measurements += measurement(
                            type = RingMetricType.BLOOD_PRESSURE,
                            value = model.bpHp.toDouble(),
                            unit = "mmHg",
                            measuredAt = model.updateDate.takeIf { it > 0 } ?: collectedAt,
                            raw = raw,
                            secondaryValue = model.bpLp.toDouble(),
                        )
                    }
                }
                "BpHistory", "Bp_History_Data" -> readAs<BpHistoryBean>(json)?.bpList
                    ?.lastOrNull()
                    ?.takeIf { it.hp > 0 && it.lp > 0 }
                    ?.let { item ->
                        measurements += measurement(
                            type = RingMetricType.BLOOD_PRESSURE,
                            value = item.hp.toDouble(),
                            unit = "mmHg",
                            measuredAt = collectedAt,
                            raw = raw,
                            secondaryValue = item.lp.toDouble(),
                        )
                    }
                "TempLast", "TempTest", "TempHistory" -> readAs<TempModel>(json)?.let { model ->
                    if (model.userTemp > 0f) {
                        measurements += measurement(
                            type = RingMetricType.TEMPERATURE,
                            value = model.userTemp.toDouble(),
                            unit = "°C",
                            measuredAt = collectedAt,
                            raw = raw,
                        )
                    }
                }
                "Temp_History_Data" -> readAs<TempHistoryBean>(json)?.tempList
                    ?.lastOrNull()
                    ?.takeIf { it > 0f }
                    ?.let { value ->
                        measurements += measurement(RingMetricType.TEMPERATURE, value.toDouble(), "°C", collectedAt, raw)
                    }
                "Step_realTime" -> readAs<SportBean>(json)?.let { model ->
                    val measuredAt = model.stepDateLong.takeIf { it > 0 } ?: collectedAt
                    if (model.stepNum > 0) {
                        measurements += measurement(RingMetricType.STEPS, model.stepNum.toDouble(), "steps", measuredAt, raw)
                        activities += activity(model, measuredAt, raw)
                    }
                }
                "Step_history", "Step_History_Data" -> readAs<StepHistoryBean>(json)?.stepList
                    ?.lastOrNull()
                    ?.takeIf { it.stepCount > 0 }
                    ?.let { item ->
                        measurements += measurement(RingMetricType.STEPS, item.stepCount.toDouble(), "steps", collectedAt, raw)
                        activities += RingActivityEntity(
                            id = id("activity"),
                            startedAt = collectedAt,
                            endedAt = collectedAt,
                            activityType = "daily",
                            steps = item.stepCount,
                            distanceMeters = item.mileage.toDouble(),
                            caloriesKcal = item.calories.toDouble(),
                            durationMinutes = 0,
                            averageHeartRate = null,
                            source = SOURCE,
                            rawPayload = raw.toHex(),
                        )
                    }
                "SleepLast", "SleepDay" -> readAs<SleepModel>(json)?.let { model ->
                    sleepSessions += sleepSession(model, collectedAt, raw)
                }
                "SleepHistory", "Sleep_History_Data" -> readAs<SleepHistoryBean>(json)?.let { history ->
                    val deep = history.sleepList.orEmpty().filter { it.type == 0 }.sumOf { it.duration }
                    val light = history.sleepList.orEmpty().filter { it.type == 1 }.sumOf { it.duration }
                    val awake = history.sleepList.orEmpty().filter { it.type != 0 && it.type != 1 }.sumOf { it.duration }
                    if (deep + light + awake > 0) {
                        sleepSessions += RingSleepSessionEntity(
                            id = id("sleep"),
                            startedAt = collectedAt - (deep + light + awake) * 60_000L,
                            endedAt = collectedAt,
                            deepMinutes = deep,
                            lightMinutes = light,
                            awakeMinutes = awake,
                            remMinutes = 0,
                            interruptionMinutes = 0,
                            source = SOURCE,
                            rawPayload = raw.toHex(),
                        )
                    }
                }
                "Pressure_History_Data" -> readAs<PressureHistoryBean>(json)?.pressureList
                    ?.lastOrNull()
                    ?.takeIf { it > 0 }
                    ?.let { value ->
                        measurements += measurement(RingMetricType.STRESS, value.toDouble(), "score", collectedAt, raw)
                    }
            }
        }

        return RingDataBatch(
            measurements = measurements.distinctBy { it.metricType to it.measuredAt },
            sleepSessions = sleepSessions.distinctBy { it.startedAt to it.endedAt },
            activities = activities.distinctBy { it.startedAt to it.steps },
        )
    }

    private inline fun <reified T> readAs(json: String): T? =
        runCatching { gson.fromJson(json, T::class.java) }.getOrNull()

    private fun measurement(
        type: RingMetricType,
        value: Double,
        unit: String,
        measuredAt: Long,
        raw: ByteArray,
        secondaryValue: Double? = null,
    ) = RingMeasurementEntity(
        id = id(type.name.lowercase()),
        metricType = type.name,
        measuredAt = measuredAt,
        primaryValue = value,
        secondaryValue = secondaryValue,
        unit = unit,
        source = SOURCE,
        rawPayload = raw.toHex(),
    )

    private fun activity(model: SportBean, measuredAt: Long, raw: ByteArray) = RingActivityEntity(
        id = id("activity"),
        startedAt = measuredAt,
        endedAt = measuredAt,
        activityType = "daily",
        steps = model.stepNum,
        distanceMeters = model.stepMileage.toDouble(),
        caloriesKcal = model.stepCalorie.toDouble(),
        durationMinutes = model.stepTime,
        averageHeartRate = null,
        source = SOURCE,
        rawPayload = raw.toHex(),
    )

    private fun sleepSession(model: SleepModel, collectedAt: Long, raw: ByteArray): RingSleepSessionEntity {
        val total = model.sleepDeep + model.sleepLight + model.sleepAwake + model.sleepEyeMove
        val endAt = parseSleepTime(model.sleepDay, model.sleepEndTime) ?: collectedAt
        val startAt = parseSleepTime(model.sleepDay, model.sleepStartTime)
            ?: endAt - total.coerceAtLeast(1) * 60_000L
        return RingSleepSessionEntity(
            id = id("sleep"),
            startedAt = startAt,
            endedAt = endAt,
            deepMinutes = model.sleepDeep,
            lightMinutes = model.sleepLight,
            awakeMinutes = model.sleepAwake,
            remMinutes = model.sleepEyeMove,
            interruptionMinutes = model.sleepInterrupt,
            source = SOURCE,
            rawPayload = raw.toHex(),
        )
    }

    private fun parseSleepTime(day: String?, time: String?): Long? {
        if (day.isNullOrBlank() || time.isNullOrBlank()) return null
        val candidates = listOf(
            "$day $time" to "yyyy-MM-dd HH:mm",
            "$day $time" to "yyyy/MM/dd HH:mm",
            "$day $time" to "yyyy.MM.dd HH:mm",
            "$day $time" to "yyyyMMdd HH:mm",
        )
        return candidates.firstNotNullOfOrNull { (value, pattern) ->
            runCatching {
                SimpleDateFormat(pattern, Locale.getDefault()).parse(value)?.time
            }.getOrNull()
        }
    }

    private fun dateMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long? {
        if (year <= 0 || month <= 0 || day <= 0) return null
        return runCatching {
            Calendar.getInstance().apply {
                set(Calendar.YEAR, if (year < 100) 2000 + year else year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, second)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.getOrNull()
    }

    private fun id(prefix: String): String = "${prefix}_${UUID.randomUUID()}"

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

    private companion object {
        const val SOURCE = "mrd_ring"
    }
}

