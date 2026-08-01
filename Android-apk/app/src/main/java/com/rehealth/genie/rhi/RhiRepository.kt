package com.rehealth.genie.rhi

import com.rehealth.genie.data.sync.InterventionFeedbackDao
import com.rehealth.genie.data.sync.InterventionFeedbackEntity
import com.rehealth.genie.data.sync.SyncRepository
import com.rehealth.genie.data.sync.UploadQueueEntity
import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.network.dto.FeatureQualityDto
import com.rehealth.genie.network.dto.RhiDailySnapshotBatchDto
import com.rehealth.genie.network.dto.RhiV2DeviceContextDto
import com.rehealth.genie.network.dto.RhiV2EvaluateRequestDto
import com.rehealth.genie.network.dto.RhiV2FeatureFields
import com.rehealth.genie.network.dto.RhiV2FeatureVectorDto
import com.rehealth.genie.network.dto.RhiV2HistoryContextDto
import com.rehealth.genie.network.dto.RhiV2PersonalBaselineDto
import com.rehealth.genie.network.dto.RhiV2SeriesEvaluateRequestDto
import com.rehealth.genie.network.dto.RhiV2SeriesEvaluateResponseDto
import com.rehealth.genie.ring.data.RingDataDao
import com.google.gson.Gson
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RhiRepository(
    private val ringDataDao: RingDataDao,
    private val manualInputDao: RhiManualHealthInputDao? = null,
    private val interventionFeedbackDao: InterventionFeedbackDao? = null,
    private val snapshotDao: RhiSnapshotDao? = null,
    private val syncRepository: SyncRepository? = null,
    private val gson: Gson = Gson(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val userIdProvider: () -> String? = { null },
    private val remoteSeriesEvaluator:
        (suspend (RhiV2SeriesEvaluateRequestDto) -> RhiV2SeriesEvaluateResponseDto)? = null,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun refreshPeriod(
        periodDays: Int,
        scoredOn: LocalDate = LocalDate.now(zoneId),
        profile: PatientProfilePayload? = null,
        calculationSource: RhiCalculationSource = RhiCalculationSource.LOCAL,
    ): RhiPeriodSummary {
        require(periodDays in setOf(7, 30, 90)) { "RHI period must be 7, 30, or 90 days" }
        val calculationDays = maxOf(periodDays, PERSONAL_BASELINE_DAYS)
        val historyWarmupDays = 42
        val since = scoredOn.minusDays((calculationDays + historyWarmupDays).toLong())
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val activities = ringDataDao.getActivitiesSince(since)
        val sleepSessions = ringDataDao.getSleepSessionsSince(since)
        val measurements = ringDataDao.getMeasurementsSince(since)
        val authenticatedUserId = userIdProvider()?.takeIf { it.isNotBlank() }
        val persistenceUserId = authenticatedUserId ?: LOCAL_DEVICE_USER
        val manual = authenticatedUserId?.let { manualInputDao?.get(it) }
        val feedback = interventionFeedbackDao?.completedFeedbackSince(since).orEmpty()
        val daily = withContext(Dispatchers.Default) {
            val firstDate = scoredOn.minusDays((calculationDays + 27).toLong())
            var previousDisplay: Double? = null
            var previousConfidence: Double? = null
            generateSequence(firstDate) { date ->
                date.plusDays(1).takeIf { it <= scoredOn }
            }.map { date ->
                val adherence = feedback.adherenceFor(date, zoneId)
                val context = RhiContextInput(
                    manual = manual,
                    profileBmi = profile.bmiValue(),
                    profileObservedAt = profile?.updatedAt,
                    age = profile?.age,
                    biologicalSex = profile?.gender,
                    nicotineExposure = profile?.smoking?.let { if (it) 1 else 0 },
                    diabetesStatus = profile?.diabetesHistory?.let { if (it) 1 else 0 },
                    antihypertensiveMedication = profile.medicationFlag(
                        "降压", "氨氯地平", "硝苯地平", "缬沙坦", "厄贝沙坦",
                        "替米沙坦", "贝那普利", "依那普利", "美托洛尔",
                    ),
                    lipidLoweringMedication = profile.medicationFlag(
                        "降脂", "他汀", "依折麦布", "非诺贝特",
                    ),
                    prematureCvdFamilyHistory = profile?.familyHistory?.let { if (it) 1 else 0 },
                    adherencePercent = adherence?.first,
                    adherenceConfidence = adherence?.second ?: 0.0,
                )
                val input = RhiLiteCalculationInput(
                    scoredOn = date,
                    zoneId = zoneId,
                    activities = activities,
                    sleepSessions = sleepSessions,
                    measurements = measurements,
                    previousDisplayScore = previousDisplay,
                    previousConfidence = previousConfidence,
                    context = context,
                )
                val result = RhiLiteEngine.calculate(input)
                val isValid = result.confidence >= RhiPeriodAggregator.MIN_VALID_CONFIDENCE &&
                    result.availableFeatureCount > 0 &&
                    result.availableDays > 0
                if (isValid) {
                    previousDisplay = result.displayScore
                    previousConfidence = result.confidence
                }
                DailyRhiCalculation(
                    date = date,
                    result = result,
                    remoteRequest = input.toRemoteRequest(result),
                )
            }.toList()
        }
        val summary = if (calculationSource == RhiCalculationSource.LOCAL) {
            summarizeLocal(periodDays, scoredOn, daily)
        } else {
            summarizeRemote(periodDays, scoredOn, daily)
        }
        persist(
            userId = persistenceUserId,
            daily = daily,
            calculationSource = calculationSource,
            algorithmVersion = summary.algorithmVersion,
            enqueueForUpload = authenticatedUserId != null,
        )
        return summary
    }

    /**
     * Writes each recomputed day into the split RHI tables. Persistence is a
     * side effect of scoring, never a precondition for it: a storage failure
     * must not deny the user a score that was already computed correctly.
     */
    private suspend fun persist(
        userId: String,
        daily: List<DailyRhiCalculation>,
        calculationSource: RhiCalculationSource,
        algorithmVersion: String,
        enqueueForUpload: Boolean,
    ) {
        val dao = snapshotDao ?: return
        val uploadSnapshots = mutableListOf<com.rehealth.genie.network.dto.RhiDailyIndexDto>()
        daily.forEach { item ->
            val entities = RhiSnapshotMapper.toEntities(
                userId = userId,
                scoredOn = item.date,
                calculation = item.result,
                calculationSource = calculationSource,
                algorithmVersion = algorithmVersion,
            )
            dao.replaceDay(
                index = entities.index,
                domains = entities.domains,
                features = entities.features,
                quality = entities.quality,
            )
            uploadSnapshots += RhiSnapshotMapper.toUploadDto(entities)
        }
        val cutoff = daily.last().date.minusDays(RETENTION_DAYS).toString()
        dao.pruneBefore(userId, cutoff)
        if (enqueueForUpload) {
            enqueueUpload(userId, uploadSnapshots)
        }
    }

    /**
     * Enqueues the day's locally-computed RHI snapshots for upload to the
     * backend management platform. Upload is best-effort and never blocks
     * scoring or local persistence: if the queue or client is unavailable the
     * day is simply not uploaded this pass and will be retried on a later
     * refresh.
     */
    private suspend fun enqueueUpload(
        userId: String,
        snapshots: List<com.rehealth.genie.network.dto.RhiDailyIndexDto>,
    ) {
        val repo = syncRepository ?: return
        if (snapshots.isEmpty()) return
        val request = RhiDailySnapshotBatchDto(userId = userId, snapshots = snapshots)
        val now = nowProvider()
        repo.enqueue(
            UploadQueueEntity(
                id = "rhi:$userId:${snapshots.last().scoredOn}",
                kind = "rhi_daily_snapshot",
                payloadJson = gson.toJson(request),
                status = "pending",
                createdAt = now,
                nextRetryAt = now,
            ),
        )
    }

    private fun summarizeLocal(
        periodDays: Int,
        scoredOn: LocalDate,
        daily: List<DailyRhiCalculation>,
    ): RhiPeriodSummary {
        val allValid = daily.mapNotNull { item ->
            item.result.takeIf {
                it.confidence >= RhiPeriodAggregator.MIN_VALID_CONFIDENCE &&
                    it.availableFeatureCount > 0 &&
                    it.availableDays > 0
            }?.let {
                RhiDailyScore(
                    date = item.date,
                    score = it.displayScore,
                    confidence = it.confidence,
                )
            }
        }
        val periodStart = scoredOn.minusDays((periodDays - 1).toLong())
        val valid = allValid.filter { it.date >= periodStart }
        // Momentum is measured against the full warm-up series so that a fixed
        // 7- or 28-day lookback stays available even in the 7-day view.
        val byDate = allValid.associate { it.date to it.score }
        val baseline90d = allValid.firstOrNull {
            it.date >= scoredOn.minusDays((PERSONAL_BASELINE_DAYS - 1).toLong()) && it.date <= scoredOn
        }
        return RhiPeriodAggregator.summarize(
            periodDays = periodDays,
            current = valid.lastOrNull { it.date == scoredOn },
            dailyScores = valid,
            delta7d = RhiPeriodAggregator.delta(byDate, scoredOn, 7),
            delta28d = RhiPeriodAggregator.delta(byDate, scoredOn, 28),
            baseline90d = baseline90d,
        )
    }

    private suspend fun summarizeRemote(
        periodDays: Int,
        scoredOn: LocalDate,
        daily: List<DailyRhiCalculation>,
    ): RhiPeriodSummary {
        val evaluator = remoteSeriesEvaluator
            ?: throw IllegalStateException("远程 RHI 复算未配置")
        val response = evaluator(RhiV2SeriesEvaluateRequestDto(daily.map { it.remoteRequest }))
        if (response.evaluations.size != daily.size) {
            throw IllegalStateException("远程 RHI 返回数量与请求不一致")
        }
        val allValid = daily.zip(response.evaluations).mapNotNull { (local, remote) ->
            val confidence = remote.dataConfidence.score
            RhiDailyScore(
                date = local.date,
                score = remote.dynamicHealthIndex.score,
                confidence = confidence,
            ).takeIf { confidence >= RhiPeriodAggregator.MIN_VALID_CONFIDENCE }
        }
        val periodStart = scoredOn.minusDays((periodDays - 1).toLong())
        val valid = allValid.filter { it.date >= periodStart }
        val byDate = allValid.associate { it.date to it.score }
        val baseline90d = allValid.firstOrNull {
            it.date >= scoredOn.minusDays((PERSONAL_BASELINE_DAYS - 1).toLong()) && it.date <= scoredOn
        }
        return RhiPeriodAggregator.summarize(
            periodDays = periodDays,
            current = valid.lastOrNull { it.date == scoredOn },
            dailyScores = valid,
            algorithmVersion = response.evaluations.last().algorithmVersion,
            calculationSource = RhiCalculationSource.REMOTE,
            delta7d = RhiPeriodAggregator.delta(byDate, scoredOn, 7),
            delta28d = RhiPeriodAggregator.delta(byDate, scoredOn, 28),
            baseline90d = baseline90d,
        )
    }

    private companion object {
        const val LOCAL_DEVICE_USER = "__local_device__"
        /** On-device RHI history horizon; older days are pruned after each refresh. */
        const val RETENTION_DAYS = 400L
        const val PERSONAL_BASELINE_DAYS = 90
    }
}

private data class DailyRhiCalculation(
    val date: LocalDate,
    val result: RhiLiteCalculation,
    val remoteRequest: RhiV2EvaluateRequestDto,
)

internal fun RhiLiteCalculationInput.toRemoteRequest(
    calculation: RhiLiteCalculation,
): RhiV2EvaluateRequestDto {
    fun value(name: String): Double? = calculation.features[name]?.value
    val sex = context.biologicalSex.toRemoteBiologicalSex()
    val snapshotObservedAt = scoredOn.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val qualities = RhiV2FeatureFields.ALL.associateWith { field ->
        val feature = calculation.features[field]
        val available = if (field == "biological_sex") sex != null else feature != null
        val confidence = if (field == "biological_sex") {
            if (sex == null) 0.0 else 0.60
        } else {
            feature?.confidence ?: 0.0
        }
        FeatureQualityDto(
            status = when {
                !available -> "MISSING"
                confidence >= 0.60 -> "VALID"
                else -> "LOW_CONFIDENCE"
            },
            source = field.remoteFeatureSource(context).takeIf { available } ?: "UNKNOWN",
            observedAt = field.remoteObservedAt(context, snapshotObservedAt).takeIf { available },
            reason = if (available) {
                "由 Android Room、健康档案或已确认临床报告生成的 RHI 特征快照"
            } else {
                "当前日期没有可验证的 RHI 输入"
            },
        )
    }
    val baselines = calculation.features.mapNotNull { (name, feature) ->
        val median = feature.baselineMedian
        val mad = feature.baselineMad
        if (
            name !in RhiV2FeatureFields.ALL ||
            median == null ||
            mad == null ||
            feature.baselineSampleCount < 7
        ) {
            null
        } else {
            name to RhiV2PersonalBaselineDto(
                median = median,
                mad = mad,
                sampleCount = feature.baselineSampleCount,
            )
        }
    }.toMap()
    return RhiV2EvaluateRequestDto(
        featureVector = RhiV2FeatureVectorDto(
            age = value("age")?.toInt(),
            biologicalSex = sex,
            waistCircumferenceCm = value("waist_circumference_cm"),
            bmi = value("bmi"),
            sbp7dMean = value("sbp_7d_mean"),
            totalCholesterol = value("total_cholesterol"),
            hdlC = value("hdl_c"),
            ldlC = value("ldl_c"),
            triglycerides = value("triglycerides"),
            glycemiaValue = value("glycemia_value"),
            egfr = value("egfr"),
            nicotineExposure = value("nicotine_exposure")?.toInt(),
            diabetesStatus = value("diabetes_status")?.toInt(),
            antihypertensiveMedication = value("antihypertensive_medication")?.toInt(),
            lipidLoweringMedication = value("lipid_lowering_medication")?.toInt(),
            prematureCvdFamilyHistory = value("premature_cvd_family_history")?.toInt(),
            dbp7dMean = value("dbp_7d_mean"),
            restingHr14dMedian = value("resting_hr_14d_median"),
            restingHrChange28dPct = value("resting_hr_change_28d_pct"),
            nocturnalHrv14dMedian = value("nocturnal_hrv_14d_median"),
            hrvChange28dPct = value("hrv_change_28d_pct"),
            cardiorespiratoryFitnessScore = value("cardiorespiratory_fitness_score"),
            sleepDuration7dMeanHours = value("sleep_duration_7d_mean_hours"),
            sleepRegularity14dPct = value("sleep_regularity_14d_pct"),
            sleepEfficiency14dPct = value("sleep_efficiency_14d_pct"),
            nocturnalSpo2DropBurden14dPct = value("nocturnal_spo2_drop_burden_14d_pct"),
            steps7dMean = value("steps_7d_mean"),
            mvpaMinutes7d = value("mvpa_minutes_7d"),
            sedentaryHours7dMean = value("sedentary_hours_7d_mean"),
            activeDayRegularity14dPct = value("active_day_regularity_14d_pct"),
            weightChange28dPct = value("weight_change_28d_pct"),
            adherenceComposite28dPct = value("adherence_composite_28d_pct"),
        ),
        featureQuality = qualities,
        productTier = "clinical",
        glycemiaMetric = when {
            value("glycemia_value") == null -> null
            context.manual?.hba1cPercent != null -> "hba1c_percent"
            else -> "fasting_glucose_mmol_l"
        },
        personalBaselines = baselines,
        history = RhiV2HistoryContextDto(
            availableDays = calculation.availableDays,
            previousDisplayScore = previousDisplayScore,
        ),
        deviceContext = RhiV2DeviceContextDto(
            brand = "ReHealth",
            model = "Room aggregate",
            algorithmVersion = RHI_LITE_ALGORITHM_VERSION,
            measurementMethod = "room_daily_aggregate",
        ),
        requestId = "rhi-$scoredOn",
    )
}

private fun String.remoteFeatureSource(context: RhiContextInput): String = when (this) {
    "total_cholesterol", "hdl_c", "ldl_c", "triglycerides",
    "glycemia_value", "egfr" -> when {
        !hasManualClinicalValue(context.manual) -> "DERIVED"
        context.manual?.labConfirmed == true -> "CLINICAL_REPORT"
        else -> "USER_REPORTED"
    }
    "sbp_7d_mean", "dbp_7d_mean",
    "waist_circumference_cm", "sedentary_hours_7d_mean",
    "cardiorespiratory_fitness_score", "age", "biological_sex",
    "nicotine_exposure", "diabetes_status",
    "antihypertensive_medication", "lipid_lowering_medication",
    "premature_cvd_family_history" -> "USER_REPORTED"
    else -> "DERIVED"
}

private fun String.remoteObservedAt(
    context: RhiContextInput,
    snapshotObservedAt: Long,
): Long = when (this) {
    "total_cholesterol", "hdl_c", "ldl_c", "triglycerides",
    "glycemia_value", "egfr" ->
        if (hasManualClinicalValue(context.manual) && context.manual?.labConfirmed == true) {
            context.manual.labRecordedAt ?: context.manual.updatedAt
        } else if (hasManualClinicalValue(context.manual)) {
            context.manual!!.updatedAt
        } else {
            snapshotObservedAt
        }
    "sbp_7d_mean", "dbp_7d_mean",
    "waist_circumference_cm", "sedentary_hours_7d_mean",
    "cardiorespiratory_fitness_score" ->
        context.manual?.updatedAt ?: snapshotObservedAt
    "age", "biological_sex", "nicotine_exposure", "diabetes_status",
    "antihypertensive_medication", "lipid_lowering_medication",
    "premature_cvd_family_history" ->
        context.profileObservedAt ?: snapshotObservedAt
    else -> snapshotObservedAt
}

private fun String.hasManualClinicalValue(manual: RhiManualHealthInputEntity?): Boolean = when (this) {
    "total_cholesterol" -> manual?.totalCholesterolMmolL != null
    "hdl_c" -> manual?.hdlMmolL != null
    "ldl_c" -> manual?.ldlMmolL != null
    "triglycerides" -> manual?.triglyceridesMmolL != null
    "glycemia_value" -> manual?.hba1cPercent != null || manual?.fastingGlucoseMmolL != null
    "egfr" -> manual?.egfrMlMin173m2 != null
    else -> false
}

private fun String?.toRemoteBiologicalSex(): String? {
    val normalized = this?.trim()?.lowercase() ?: return null
    return when {
        normalized in setOf("male", "m", "1", "男", "男性") -> "male"
        normalized in setOf("female", "f", "0", "女", "女性") -> "female"
        normalized in setOf("unspecified", "unknown", "未知") -> "unspecified"
        else -> null
    }
}

private fun PatientProfilePayload?.medicationFlag(vararg keywords: String): Int? {
    val medications = this?.medications ?: return null
    return if (medications.any { medication -> keywords.any(medication::contains) }) 1 else 0
}

private fun PatientProfilePayload?.bmiValue(): Double? {
    this ?: return null
    bmi?.takeIf { it.isFinite() && it in 10.0..80.0 }?.let { return it }
    val heightM = heightCm?.takeIf { it.isFinite() && it in 80.0..250.0 }?.div(100.0)
    val weight = weightKg?.takeIf { it.isFinite() && it in 20.0..350.0 }
    return if (heightM != null && weight != null) {
        (weight / (heightM * heightM)).takeIf { it in 10.0..80.0 }
    } else {
        null
    }
}

private fun List<InterventionFeedbackEntity>.adherenceFor(
    scoredOn: LocalDate,
    zoneId: ZoneId,
): Pair<Double, Double>? {
    val end = scoredOn.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val start = scoredOn.minusDays(27).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val applicable = filter { it.checkedAt in start until end && it.status != "not_applicable" }
    if (applicable.isEmpty()) return null
    val percent = applicable.map {
        when (it.status) {
            "completed" -> 100.0
            "partially_completed" -> 50.0
            "skipped" -> 0.0
            else -> 0.0
        }
    }.average()
    val confidence = (applicable.size / 7.0 * 0.80).coerceIn(0.0, 0.80)
    return percent to confidence
}
