package com.rehealth.genie.qa

import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.data.sync.MeasurementUploadOutcome
import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.features.HealthFeatureExtractor
import com.rehealth.genie.features.HealthMemorySnapshot
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.network.dto.PatientProfileDto
import com.rehealth.genie.rhi.RhiCalculationSource
import com.rehealth.genie.rhi.RhiManualHealthInputEntity
import com.rehealth.genie.rhi.toClinicalBloodPressureValues
import com.rehealth.genie.rhi.toClinicalLabValues
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import com.rehealth.genie.ring.provider.DEBUG_MOCK_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableVendor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun createRuntimeFullChainSimulationRunner(
    application: ReHealthApplication,
): FullChainSimulationRunner = DebugFullChainSimulationRunner(application)

private class DebugFullChainSimulationRunner(
    private val application: ReHealthApplication,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : FullChainSimulationRunner {
    override val available: Boolean = true

    override suspend fun run(): FullChainSimulationReport = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val stages = mutableListOf<SimulationStageResult>()
        val userId = application.sessionStore.userId
        if (!application.sessionStore.isLoggedIn || userId.isNullOrBlank()) {
            stages += failed("authentication", "登录状态", "请先登录真实测试账号，再运行全链路演练。")
            return@withContext FullChainSimulationReport(startedAt, System.currentTimeMillis(), stages)
        }

        val today = LocalDate.now(zoneId)
        val firstDay = today.minusDays(SIMULATION_DAYS - 1L)
        val observedAt = firstDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val profile = normalProfile(observedAt)
        val manual = normalManualInput(userId, observedAt)

        stages += updateRemoteProfile(profile).second

        runCatching {
            application.activeWearableManager.switchProduct(DEBUG_MOCK_PRODUCT_CODE)
            application.activeWearableStore.recordConnectedDevice(
                vendor = WearableVendor.MOCK,
                device = SIMULATED_DEVICE,
                modelCode = "RH-QA-50M",
                firmwareVersion = "qa-1.0",
                capabilityJson = """{"source":"synthetic_qa","days":$SIMULATION_DAYS}""",
            )
            application.activeWearableStore.saveUserProfile(
                userId,
                BaselineHealthProfile(
                    age = profile.age,
                    gender = profile.gender,
                    heightCm = profile.heightCm,
                    weightKg = profile.weightKg,
                    bmi = profile.bmi,
                    smoking = profile.smoking,
                    drinking = profile.drinking,
                    diabetesHistory = profile.diabetesHistory,
                    hypertensionHistory = profile.hypertensionHistory,
                    familyHistory = profile.familyHistory,
                    updatedAt = observedAt,
                ),
            )
            application.database.rhiManualHealthInputDao().upsert(manual)
            application.database.ringDataDao().deleteSourceData(SOURCE)
            application.database.ringDataDao().insertBatch(
                normalWearableBatch(firstDay, today).ownedBy(userId, SIMULATED_DEVICE.address),
            )
        }.fold(
            onSuccess = {
                stages += success(
                    "room",
                    "Room 数据",
                    "已写入 $VISIBLE_HISTORY_DAYS 天展示数据及 $RDI_WARMUP_DAYS 天计算预热数据，包含 synthetic_qa 戒指、睡眠、活动、体成分及完整临床手工字段。",
                )
            },
            onFailure = { error ->
                stages += failed("room", "Room 数据", safeError(error))
                return@withContext FullChainSimulationReport(startedAt, System.currentTimeMillis(), stages)
            },
        )

        stages += bindAndUpload()

        val rhiStage = runCatching {
            val localSummaries = listOf(7, 30, 90).associateWith { days ->
                application.rhiRepository.refreshPeriod(days, today, profile)
            }
            val localDetails = localSummaries.entries.joinToString("；") { (days, summary) ->
                val value = summary.score?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
                "${days}日 $value（有效${summary.validDays}日）"
            }
            val remote = application.rhiRepository.refreshPeriod(
                periodDays = 7,
                scoredOn = today,
                profile = profile,
                calculationSource = RhiCalculationSource.REMOTE,
            )
            val remoteValue = remote.score?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            success(
                "rhi",
                "RHI-100",
                "本地 $localDetails；JeecgBoot→model-service 远程7日 $remoteValue。",
            )
        }.getOrElse { failed("rhi", "RHI-100", safeError(it)) }
        stages += rhiStage

        val rdiStage = runCatching {
            val summary = application.rdiRepository.refreshPeriod(VISIBLE_HISTORY_DAYS, today)
            val current = summary.history.lastOrNull()?.score
            val detail = buildString {
                append("本地真实规则逐日计算并落库 ${summary.validDays}/$VISIBLE_HISTORY_DAYS 日")
                append("，当前 ${current?.let { String.format(Locale.US, "%.1f", it) } ?: "--"}/100")
                append("，90日代表值 ${summary.score?.let { String.format(Locale.US, "%.1f", it) } ?: "--"}/100。")
            }
            if (summary.validDays == VISIBLE_HISTORY_DAYS && current != null) {
                success("rdi16", "RDI-16", detail)
            } else {
                warning("rdi16", "RDI-16", detail)
            }
        }.getOrElse { failed("rdi16", "RDI-16", safeError(it)) }
        stages += rdiStage

        val riskResults = mutableListOf<Pair<Long, com.rehealth.genie.network.dto.RiskResultDto>>()
        val measurements = application.database.ringDataDao().getMeasurementsSince(observedAt)
        val activities = application.database.ringDataDao().getActivitiesSince(observedAt)
        val sleeps = application.database.ringDataDao().getSleepSessionsSince(
            firstDay.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
        )
        for (daysAgo in RISK_HISTORY_DAYS - 1 downTo 0) {
            val scoreDate = today.minusDays(daysAgo.toLong())
            val endExclusive = scoreDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val lookback = scoreDate.minusDays(6).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val vector = HealthFeatureExtractor(nowProvider = { endExclusive - 1 }).extract(
                HealthMemorySnapshot(
                    profile = BaselineHealthProfile(
                        age = profile.age,
                        gender = profile.gender,
                        heightCm = profile.heightCm,
                        weightKg = profile.weightKg,
                        bmi = profile.bmi,
                        smoking = profile.smoking,
                        drinking = profile.drinking,
                        diabetesHistory = profile.diabetesHistory,
                        hypertensionHistory = profile.hypertensionHistory,
                        familyHistory = profile.familyHistory,
                        updatedAt = observedAt,
                    ),
                    labs = manual.toClinicalLabValues(),
                    clinicalBloodPressure = manual.toClinicalBloodPressureValues(),
                    ringMeasurements = measurements.filter { it.measuredAt in lookback until endExclusive },
                    ringActivities = activities.filter { it.startedAt in lookback until endExclusive },
                    ringSleepSessions = sleeps.filter { it.endedAt in lookback until endExclusive },
                ),
            )
            val requestId = "qa-50m-${startedAt}-${scoreDate}"
            val outcome = application.remotePhmService.evaluateFeatures(vector, requestId)
            val result = outcome.result
            if (result?.normalizedIsMock == false && result.normalizedRiskScore != null) {
                application.riskHistoryRepository.recordConfirmedRemoteRisk(
                    result = result,
                    evaluatedAt = endExclusive - 1,
                )
                riskResults += (endExclusive - 1) to result
            }
        }
        stages += if (riskResults.isEmpty()) {
            failed("cvd16", "CVD-16 远程风险", "远程模型未返回可确认的非 Mock 评分；未写入 PIAS 风险历史。")
        } else {
            val latest = riskResults.last().second
            val score = String.format(Locale.US, "%.1f", latest.normalizedRiskScore!! * 100.0)
            val complete = riskResults.size == RISK_HISTORY_DAYS
            val factorVersion = latest.normalizedFactorContributionVersion
            val detail =
                "真实远程评估 ${riskResults.size}/$RISK_HISTORY_DAYS 日，当前 $score；贡献规则 ${factorVersion ?: "缺失"}。"
            if (complete && factorVersion == FACTOR16_VERSION) {
                success("cvd16", "CVD-16 远程风险", detail)
            } else {
                warning("cvd16", "CVD-16 远程风险", detail)
            }
        }

        val piasStage = runCatching {
            val history = application.riskHistoryRepository.attributionHistory(limit = 90)
            check(history.isNotEmpty()) { "没有可用于归因的已确认远程风险历史。" }
            val result = application.remotePhmService.attributeIndividual(history, forecastDays = 30)
            val detail = buildString {
                append("服务状态 ${result.status ?: "未知"}，历史 ${result.historyDays ?: history.size} 日")
                append("，预测点 ${result.forecastNoAction.size}/${result.forecastWithPlan.size}")
                if (result.attAvailable != true) {
                    append("；个体 ATT 尚不可用：${result.attUnavailableReason ?: "缺少干预/对照日"}")
                }
            }
            if (result.status == "ready" && result.forecastNoAction.isNotEmpty()) {
                success("pias", "PIAS", detail)
            } else {
                warning("pias", "PIAS", detail)
            }
        }.getOrElse { failed("pias", "PIAS", safeError(it)) }
        stages += piasStage

        FullChainSimulationReport(startedAt, System.currentTimeMillis(), stages)
    }

    private suspend fun updateRemoteProfile(
        profile: PatientProfilePayload,
    ): Pair<PatientProfileDto?, SimulationStageResult> {
        val existing = (application.authenticatedApiClient.getProfile() as? ApiResult.Success)?.data
        val request = (existing ?: PatientProfileDto()).copy(
            name = "RHI-RDI-PIAS QA 50M",
            gender = profile.gender,
            age = profile.age,
            heightCm = profile.heightCm,
            weightKg = profile.weightKg,
            bmi = profile.bmi,
            diagnoses = emptyList(),
            medications = emptyList(),
            familyHistory = false,
            smoking = false,
            drinking = false,
            diabetesHistory = false,
            hypertensionHistory = false,
        )
        return when (val result = application.authenticatedApiClient.updateProfile(request)) {
            is ApiResult.Success -> result.data to success(
                "profile",
                "远程健康档案",
                "已由真实 PUT 接口保存 50 岁男性正常档案。",
            )
            is ApiResult.Unauthorized -> null to failed("profile", "远程健康档案", "登录已失效。")
            is ApiResult.Forbidden -> null to failed("profile", "远程健康档案", "当前账号无修改权限。")
            is ApiResult.InvalidRequest -> null to failed("profile", "远程健康档案", result.message)
            is ApiResult.InvalidResponse -> null to failed("profile", "远程健康档案", result.message)
            is ApiResult.ServiceUnavailable -> null to failed("profile", "远程健康档案", result.message)
            is ApiResult.NetworkError -> null to failed("profile", "远程健康档案", result.message)
        }
    }

    private suspend fun bindAndUpload(): SimulationStageResult = runCatching {
        application.ringCloudRepository.bindDevice(SIMULATED_DEVICE).getOrThrow()
        val batchId = application.ringCloudRepository.enqueueLatestTelemetry(
            device = SIMULATED_DEVICE,
            collectedAt = System.currentTimeMillis(),
            trigger = "full_chain_qa_50m",
        ).getOrThrow()
        val queued = application.database.uploadQueueDao().getById(batchId)
            ?: error("上传队列中找不到刚创建的批次。")
        val outcome = application.syncRepository.uploadQueuedItem(queued)
        val persisted = application.database.uploadQueueDao().getById(batchId)
        when (outcome) {
            MeasurementUploadOutcome.Uploaded ->
                success("telemetry", "设备绑定与遥测上传", "批次已通过 Room 队列真实上传并获得持久化确认。")
            MeasurementUploadOutcome.RetryScheduled ->
                warning("telemetry", "设备绑定与遥测上传", "真实上传失败，批次已按退避策略保留重试。")
            MeasurementUploadOutcome.Paused ->
                failed("telemetry", "设备绑定与遥测上传", "认证失效，上传队列已暂停。")
            MeasurementUploadOutcome.DeadLettered ->
                failed("telemetry", "设备绑定与遥测上传", "服务端未确认持久化，批次进入 dead-letter。")
            MeasurementUploadOutcome.Skipped ->
                failed(
                    "telemetry",
                    "设备绑定与遥测上传",
                    "队列未处理该批次（状态 ${persisted?.status ?: "未知"}）。",
                )
        }
    }.getOrElse { failed("telemetry", "设备绑定与遥测上传", safeError(it)) }

    private fun normalProfile(observedAt: Long): PatientProfilePayload = PatientProfilePayload(
        patientId = null,
        name = "RHI-RDI-PIAS QA 50M",
        gender = "male",
        age = 50,
        heightCm = 175.0,
        weightKg = 68.6,
        bmi = 22.4,
        diagnoses = emptyList(),
        medications = emptyList(),
        allergies = emptyList(),
        familyHistory = false,
        smoking = false,
        drinking = false,
        diabetesHistory = false,
        hypertensionHistory = false,
        updatedAt = observedAt,
    )

    private fun normalManualInput(userId: String, observedAt: Long) = RhiManualHealthInputEntity(
        userId = userId,
        sedentaryHoursPerDay = 6.5,
        waistCircumferenceCm = 84.0,
        vo2MaxMlKgMin = 38.0,
        hba1cPercent = 5.3,
        egfrMlMin173m2 = 96.0,
        cuffSbp7dMean = 118.0,
        cuffDbp7dMean = 76.0,
        cuffValidDays = 7,
        cuffConfirmed = true,
        fastingGlucoseMmolL = 5.0,
        totalCholesterolMmolL = 4.5,
        ldlMmolL = 2.5,
        hdlMmolL = 1.35,
        triglyceridesMmolL = 1.1,
        labConfirmed = true,
        labRecordedAt = observedAt,
        updatedAt = observedAt,
    )

    private fun normalWearableBatch(firstDay: LocalDate, lastDay: LocalDate): RingDataBatch {
        val generatedAt = System.currentTimeMillis()
        val measurements = mutableListOf<RingMeasurementEntity>()
        val activities = mutableListOf<RingActivityEntity>()
        val sleeps = mutableListOf<RingSleepSessionEntity>()
        var date = firstDay
        while (!date.isAfter(lastDay)) {
            val index = java.time.temporal.ChronoUnit.DAYS.between(firstDay, date).toInt()
            val wave = sin(index * 2.0 * PI / 14.0)
            val trend = index / (SIMULATION_DAYS - 1.0)
            val bedtimeJitterMinutes = (wave * 28.0 * (1.0 - trend)).roundToInt()
            val sleepStartOffsetMinutes =
                (24 * 60 + 30 - trend * 90.0).roundToInt() + bedtimeJitterMinutes
            val sleepDurationMinutes = (360 + trend * 130.0).roundToInt()
            val sleepStart = date.minusDays(1)
                .atStartOfDay(zoneId)
                .plusMinutes(sleepStartOffsetMinutes.toLong())
                .toInstant()
                .toEpochMilli()
            val sleepEnd = sleepStart + sleepDurationMinutes * 60_000L
            val nightAt = date.atTime(2, 30).atZone(zoneId).toInstant().toEpochMilli()
            val morningAt = date.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
            val plannedEveningAt = date.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli()
            val eveningAt = minOf(plannedEveningAt, generatedAt - 60_000L)
            val steps = (3_500 + trend * 4_500.0 + wave * 400.0).roundToInt()
            val restingHr = 77.5 - trend * 10.0 + wave * 1.2
            val hrv = 34.0 + trend * 22.0 - wave * 1.5
            val weight = 72.5 - trend * 3.9
            val bmi = weight / (1.75 * 1.75)
            val spo2 = 95.2 + trend * 2.0
            val exerciseMinutes = (20 + trend * 28.0).roundToInt()

            measurements += measurement("hr_night", RingMetricType.HEART_RATE, nightAt, restingHr, "bpm")
            measurements += measurement("hr_morning", RingMetricType.HEART_RATE, morningAt, restingHr + 1.5, "bpm")
            measurements += measurement("hrv", RingMetricType.HRV, nightAt + 60_000L, hrv, "ms")
            measurements += measurement("spo2a", RingMetricType.BLOOD_OXYGEN, nightAt + 120_000L, spo2, "%")
            measurements += measurement(
                "spo2b",
                RingMetricType.BLOOD_OXYGEN,
                nightAt + 180_000L,
                spo2 - 0.5,
                "%",
            )
            measurements += measurement(
                "bp",
                RingMetricType.BLOOD_PRESSURE,
                morningAt + 240_000L,
                118.0 + wave,
                "mmHg",
                76.0 + wave * 0.5,
            )
            measurements += measurement("steps", RingMetricType.STEPS, eveningAt, steps.toDouble(), "steps")
            measurements += measurement("bmi", RingMetricType.BMI, morningAt + 300_000L, bmi, "kg/m2")
            measurements += measurement("fat", RingMetricType.FAT_MASS, morningAt + 360_000L, weight * 0.19, "kg")
            measurements += measurement("lean", RingMetricType.FAT_FREE_MASS, morningAt + 360_000L, weight * 0.81, "kg")

            val awake = (sleepDurationMinutes * (0.22 - trend * 0.14)).roundToInt()
            val asleep = sleepDurationMinutes - awake
            val deep = (asleep * 0.22).roundToInt()
            val rem = (asleep * 0.20).roundToInt()
            val light = asleep - deep - rem
            sleeps += RingSleepSessionEntity(
                id = "qa50m_sleep_$date",
                startedAt = sleepStart,
                endedAt = sleepEnd,
                deepMinutes = deep,
                lightMinutes = light,
                awakeMinutes = awake,
                remMinutes = rem,
                interruptionMinutes = (14 - trend * 8.0).roundToInt(),
                source = SOURCE,
                rawPayload = """{"simulated":true,"profile":"improving_to_normal_50m"}""",
            )
            val plannedActivityStart =
                date.atTime(LocalTime.of(18, 0)).atZone(zoneId).toInstant().toEpochMilli()
            val activityStart = minOf(
                plannedActivityStart,
                generatedAt - (exerciseMinutes + 1L) * 60_000L,
            )
            activities += RingActivityEntity(
                id = "qa50m_activity_$date",
                startedAt = activityStart,
                endedAt = activityStart + exerciseMinutes * 60_000L,
                activityType = "walking",
                steps = steps,
                distanceMeters = steps * 0.68,
                caloriesKcal = steps * 0.036,
                durationMinutes = exerciseMinutes,
                averageHeartRate = 112.0 - trend * 5.0 + wave * 2.0,
                source = SOURCE,
                rawPayload = """{"simulated":true,"profile":"improving_to_normal_50m"}""",
            )
            date = date.plusDays(1)
        }
        return RingDataBatch(
            measurements = measurements,
            sleepSessions = sleeps,
            activities = activities,
        )
    }

    private fun measurement(
        prefix: String,
        type: RingMetricType,
        measuredAt: Long,
        value: Double,
        unit: String,
        secondaryValue: Double? = null,
    ) = RingMeasurementEntity(
        id = "qa50m_${prefix}_$measuredAt",
        metricType = type.name,
        measuredAt = measuredAt,
        primaryValue = (value * 10.0).roundToInt() / 10.0,
        secondaryValue = secondaryValue?.let { (it * 10.0).roundToInt() / 10.0 },
        unit = unit,
        quality = 96,
        source = SOURCE,
        rawPayload = """{"simulated":true,"profile":"normal_50m"}""",
    )

    private fun success(code: String, label: String, detail: String) =
        SimulationStageResult(code, label, SimulationStageStatus.SUCCESS, detail)

    private fun warning(code: String, label: String, detail: String) =
        SimulationStageResult(code, label, SimulationStageStatus.WARNING, detail)

    private fun failed(code: String, label: String, detail: String) =
        SimulationStageResult(code, label, SimulationStageStatus.FAILED, detail)

    private fun safeError(error: Throwable): String =
        error.message?.take(180)?.takeIf { it.isNotBlank() } ?: error::class.java.simpleName

    private companion object {
        const val VISIBLE_HISTORY_DAYS = 90
        const val RDI_WARMUP_DAYS = 28
        const val SIMULATION_DAYS = VISIBLE_HISTORY_DAYS + RDI_WARMUP_DAYS
        const val RISK_HISTORY_DAYS = 30
        const val SOURCE = "synthetic_qa"
        const val FACTOR16_VERSION = "factor16-rule-v1.0.0"
        val SIMULATED_DEVICE = RingDevice(
            address = "QA:50:M:NORMAL",
            name = "睿禾全链路模拟戒指（50岁男性）",
            rssi = -42,
        )
    }
}
