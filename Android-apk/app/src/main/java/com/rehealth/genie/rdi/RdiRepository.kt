package com.rehealth.genie.rdi

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.rhi.RhiManualHealthInputDao
import com.rehealth.genie.rhi.toClinicalBloodPressureValues
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import com.rehealth.genie.diet.DietRecordDao
import com.rehealth.genie.diet.DietRecordEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class RdiDisplayData(
    val score: Double,
    val delta7d: Double?,
    val confidence: Double,
    val status: String,
    val scoredOn: String,
    val contributions: List<RdiContributionEntity>,
) {
    val topContributions: List<RdiContributionEntity>
        get() = contributions.filter { it.confidence >= 0.60 && kotlin.math.abs(it.finalPoints) >= 0.01 }
            .sortedByDescending { kotlin.math.abs(it.finalPoints) }
            .take(3)
}

class RdiRepository(
    private val rdiDao: RdiDao,
    private val rdiBaselineDao: RdiBaselineDao,
    private val ringDataDao: RingDataDao,
    private val rhiManualHealthInputDao: RhiManualHealthInputDao,
    private val rdiLabMealDao: RdiLabMealDao,
    private val dietRecordDao: DietRecordDao,
    private val userIdProvider: () -> String?,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observeLatest(): Flow<RdiSnapshotBundle?> = rdiDao.observeLatest(userKey())

    fun observeLatestDisplay(): Flow<RdiDisplayData?> = observeLatest().map { bundle ->
        bundle ?: return@map null
        val sevenDaysAgo = rdiDao.snapshotForDay(
            bundle.snapshot.userId,
            LocalDate.parse(bundle.snapshot.scoredOn).minusDays(7).toString(),
        )
        RdiDisplayData(
            score = bundle.snapshot.displayScore,
            delta7d = sevenDaysAgo?.let { bundle.snapshot.displayScore - it.displayScore },
            confidence = bundle.snapshot.dataConfidence,
            status = bundle.snapshot.status,
            scoredOn = bundle.snapshot.scoredOn,
            contributions = bundle.contributions,
        )
    }

    suspend fun refresh(scoredOn: LocalDate = LocalDate.now(zoneId)): RdiDisplayData {
        val userId = userKey()
        val since = scoredOn.minusDays(28).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val existing = rdiDao.snapshotForDay(userId, scoredOn.toString())
        val previous = rdiDao.latestBefore(userId, scoredOn.toString())
        val activities = ringDataDao.getActivitiesSince(since)
        val sleepSessions = ringDataDao.getSleepSessionsSince(since)
        val measurements = ringDataDao.getMeasurementsSince(since)
        val (validDays, staleDays) = computeValidDays(activities, sleepSessions, measurements, scoredOn)
        val anchoredBaselines = loadAnchoredBaselines(userId, scoredOn)
        val bloodPressure = rhiManualHealthInputDao.get(userId)?.toClinicalBloodPressureValues()
        val confirmedLabs = rdiLabMealDao.confirmedLabs(userId)
        val confirmedMeals = rdiLabMealDao.confirmedMeals(userId)
        val dietRecords = dietRecordsForDay(userId, scoredOn)
        val isMock = containsMockSource(activities, sleepSessions, measurements)
        val calculation = RdiEngine.calculate(
            RdiCalculationInput(
                scoredOn = scoredOn,
                zoneId = zoneId,
                activities = activities,
                sleepSessions = sleepSessions,
                measurements = measurements,
                previousDisplayScore = previous?.displayScore ?: existing?.displayScore,
                validDays = validDays,
                staleDays = staleDays,
                anchoredBaselines = anchoredBaselines,
                bloodPressure = bloodPressure,
                confirmedLabs = confirmedLabs,
                confirmedMeals = confirmedMeals,
                dietRecords = dietRecords,
                isMock = isMock,
            ),
        )
        maybeEstablishBaselines(userId, activities, sleepSessions, measurements, scoredOn)
        val now = clock()
        val snapshotId = "$userId:${scoredOn}"
        val snapshot = RdiDailySnapshotEntity(
            id = snapshotId,
            userId = userId,
            scoredOn = scoredOn.toString(),
            rawScore = calculation.rawScore,
            displayScore = calculation.displayScore,
            dataConfidence = calculation.confidence,
            status = calculation.status,
            isMock = isMock,
            algorithmVersion = RDI_ALGORITHM_VERSION,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val records = calculation.contributions.map { item ->
            RdiContributionEntity(
                id = "$snapshotId:${item.factorCode}",
                snapshotId = snapshotId,
                userId = userId,
                scoredOn = scoredOn.toString(),
                factorCode = item.factorCode,
                domain = item.domain,
                source = item.source,
                currentValue = item.currentValue,
                baselineValue = item.baselineValue,
                unit = item.unit,
                rawPoints = item.rawPoints,
                confidence = item.confidence,
                finalPoints = item.finalPoints,
                evidenceText = item.evidenceText,
                algorithmVersion = RDI_ALGORITHM_VERSION,
                sourceFactorId = item.sourceFactorId,
                createdAt = now,
            )
        }
        rdiDao.replaceCalculation(snapshot, records)
        val sevenDaysAgo = rdiDao.snapshotForDay(userId, scoredOn.minusDays(7).toString())
        return RdiDisplayData(
            score = snapshot.displayScore,
            delta7d = sevenDaysAgo?.let { snapshot.displayScore - it.displayScore },
            confidence = snapshot.dataConfidence,
            status = snapshot.status,
            scoredOn = snapshot.scoredOn,
            contributions = records,
        )
    }

    suspend fun refreshPeriod(
        periodDays: Int,
        scoredOn: LocalDate = LocalDate.now(zoneId),
        interventions: List<RdiScenarioIntervention> = emptyList(),
    ): RdiPeriodSummary {
        require(periodDays in setOf(7, 30, 90)) { "RDI period must be 7, 30, or 90 days" }
        val current = refresh(scoredOn)
        val since = scoredOn.minusDays((RDI_HISTORY_DAYS + RDI_WARMUP_DAYS).toLong())
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val activities = ringDataDao.getActivitiesSince(since)
        val sleepSessions = ringDataDao.getSleepSessionsSince(since)
        val measurements = ringDataDao.getMeasurementsSince(since)
        val userId = userKey()
        val anchoredBaselines = loadAnchoredBaselines(userId, scoredOn)
        val bloodPressure = rhiManualHealthInputDao.get(userId)?.toClinicalBloodPressureValues()
        val confirmedLabs = rdiLabMealDao.confirmedLabs(userId)
        val confirmedMeals = rdiLabMealDao.confirmedMeals(userId)
        val dietRecords = dietRecordsBetween(userId, since)
        val isMock = containsMockSource(activities, sleepSessions, measurements)
        val calculatedDays = withContext(Dispatchers.Default) {
            (RDI_HISTORY_DAYS - 1 downTo 0).map { daysAgo ->
                val date = scoredOn.minusDays(daysAgo.toLong())
                val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val dayDietRecords = dietRecords.filter {
                    it.consumedAt in dayStart until dayEnd
                }
                val calculation = RdiEngine.calculate(
                    RdiCalculationInput(
                        scoredOn = date,
                        zoneId = zoneId,
                        activities = activities,
                        sleepSessions = sleepSessions,
                        measurements = measurements,
                        previousDisplayScore = null,
                        anchoredBaselines = anchoredBaselines,
                        bloodPressure = bloodPressure,
                        confirmedLabs = confirmedLabs,
                        confirmedMeals = confirmedMeals,
                        dietRecords = dayDietRecords,
                        isMock = isMock,
                    ),
                )
                CalculatedRdiDay(date, calculation)
            }
        }
        if (isMock) {
            calculatedDays.filter { it.date != scoredOn }.forEach { day ->
                persistCalculatedDay(userId, day.date, day.calculation, isMock = true)
            }
        }
        val calculatedDailyScores = calculatedDays.mapNotNull { day ->
            day.calculation.takeIf {
                it.confidence >= RdiPeriodAggregator.MIN_VALID_CONFIDENCE &&
                    it.contributions.isNotEmpty()
            }?.let {
                RdiDailyScore(
                    date = day.date,
                    score = it.displayScore,
                    confidence = it.confidence,
                )
            }
        }
        val currentIsValid = current.confidence >= RdiPeriodAggregator.MIN_VALID_CONFIDENCE &&
            current.contributions.isNotEmpty()
        val dailyScores = calculatedDailyScores.map { point ->
            if (point.date == scoredOn && currentIsValid) {
                point.copy(score = current.score, confidence = current.confidence)
            } else {
                point
            }
        }
        val summary = RdiPeriodAggregator.summarize(
            periodDays = periodDays,
            currentScore = current.score.takeIf { currentIsValid },
            currentConfidence = current.confidence,
            dailyScores = dailyScores,
        )
        val impact = RdiPeriodImpactAggregator.summarize(
            periodDays = periodDays,
            scoredOn = scoredOn,
            dailyCalculations = calculatedDays.map { day ->
                RdiPeriodCalculation(day.date, day.calculation)
            },
        )
        val scenario = summary.score?.let { referenceScore ->
            RdiScenarioForecaster.forecast(
                scoredOn = scoredOn,
                zoneId = zoneId,
                activities = activities,
                sleepSessions = sleepSessions,
                measurements = measurements,
                currentScore = referenceScore,
                referenceDays = periodDays,
                anchoredBaselines = anchoredBaselines,
                bloodPressure = bloodPressure,
                confirmedLabs = confirmedLabs,
                confirmedMeals = confirmedMeals,
                interventions = interventions,
                isMock = isMock,
            )
        }
        return summary.copy(scenario = scenario, impact = impact)
    }

    private suspend fun persistCalculatedDay(
        userId: String,
        scoredOn: LocalDate,
        calculation: RdiCalculation,
        isMock: Boolean,
    ) {
        val now = clock()
        val snapshotId = "$userId:$scoredOn"
        val existing = rdiDao.snapshotForDay(userId, scoredOn.toString())
        val snapshot = RdiDailySnapshotEntity(
            id = snapshotId,
            userId = userId,
            scoredOn = scoredOn.toString(),
            rawScore = calculation.rawScore,
            displayScore = calculation.displayScore,
            dataConfidence = calculation.confidence,
            status = calculation.status,
            isMock = isMock,
            algorithmVersion = RDI_ALGORITHM_VERSION,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val records = calculation.contributions.map { item ->
            RdiContributionEntity(
                id = "$snapshotId:${item.factorCode}",
                snapshotId = snapshotId,
                userId = userId,
                scoredOn = scoredOn.toString(),
                factorCode = item.factorCode,
                domain = item.domain,
                source = item.source,
                currentValue = item.currentValue,
                baselineValue = item.baselineValue,
                unit = item.unit,
                rawPoints = item.rawPoints,
                confidence = item.confidence,
                finalPoints = item.finalPoints,
                evidenceText = item.evidenceText,
                algorithmVersion = RDI_ALGORITHM_VERSION,
                sourceFactorId = item.sourceFactorId,
                createdAt = now,
            )
        }
        rdiDao.replaceCalculation(snapshot, records)
    }

    private fun containsMockSource(
        activities: List<RingActivityEntity>,
        sleepSessions: List<RingSleepSessionEntity>,
        measurements: List<RingMeasurementEntity>,
    ): Boolean = sequenceOf(
        activities.asSequence().map(RingActivityEntity::source),
        sleepSessions.asSequence().map(RingSleepSessionEntity::source),
        measurements.asSequence().map(RingMeasurementEntity::source),
    ).flatten().any { source ->
        source.contains("synthetic", ignoreCase = true) ||
            source.equals("ring_sim", ignoreCase = true) ||
            source.contains("mock", ignoreCase = true)
    }

    private fun Long.toDate(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    /** 与引擎一致的睡眠按日聚合（设计 6.2 基线口径）。 */
    private fun aggregateSleepDaysLocal(
        sleepSessions: List<RingSleepSessionEntity>,
        zoneId: ZoneId,
    ): Map<LocalDate, SleepDay> =
        sleepSessions.mapNotNull { session ->
            val durationMinutes = ((session.endedAt - session.startedAt) / 60_000L).toInt()
            if (durationMinutes !in 120..900) return@mapNotNull null
            val asleep = (session.deepMinutes + session.lightMinutes + session.remMinutes).takeIf { it > 0 }
                ?: (durationMinutes - session.awakeMinutes).coerceAtLeast(0)
            SleepDay(
                date = session.endedAt.toDate(zoneId),
                durationMinutes = durationMinutes,
                bedtimeMinute = run {
                    val t = Instant.ofEpochMilli(session.startedAt).atZone(zoneId).toLocalTime()
                    ((t.hour - 12 + 24) % 24) * 60 + t.minute
                },
                efficiency = (asleep.toDouble() / durationMinutes * 100.0).coerceIn(0.0, 100.0),
                source = session.source,
            )
        }.groupBy { it.date }.mapValues { (_, values) -> values.maxBy { it.durationMinutes } }

    private fun List<Double>.median(): Double? {
        if (isEmpty()) return null
        val sorted = sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }

    /** 读取未过冻结期的活跃基线，组装为 factorCode -> 锚定值。 */
    private suspend fun loadAnchoredBaselines(userId: String, scoredOn: LocalDate): Map<String, Double> {
        val today = scoredOn.toString()
        return rdiBaselineDao.activeBaselines(userId)
            .filter {
                it.frozenUntil >= today &&
                    it.algorithmVersion == RDI_ALGORITHM_VERSION &&
                    it.factorCode != "sleep_regularity"
            }
            .associate { it.factorCode to it.baselineValue }
    }

    /**
     * 个人基线建立/重建（设计 6.2）：当某 factor 近 28 天有效日 >= 14 时，
     * 计算稳健中位数 + MAD，冻结 90 天；重建时旧版本保留为 SUPERSEDED。
     */
    private suspend fun maybeEstablishBaselines(
        userId: String,
        activities: List<RingActivityEntity>,
        sleepSessions: List<RingSleepSessionEntity>,
        measurements: List<RingMeasurementEntity>,
        scoredOn: LocalDate,
    ) {
        val establishedOn = scoredOn.toString()
        val frozenUntil = scoredOn.plusDays(90).toString()
        val now = clock()

        val stepsByDay = activities.groupBy { it.startedAt.toDate(zoneId) }
            .mapValues { it.value.maxOf { a -> a.steps }.toDouble() }.values.toList()
        val minutesByDay = activities.groupBy { it.startedAt.toDate(zoneId) }
            .mapValues { it.value.filter { a -> a.activityType.isExerciseSession() }.sumOf { a -> a.durationMinutes.coerceAtLeast(0) }.toDouble() }
            .values.toList()
        val sleepByDay = aggregateSleepDaysLocal(sleepSessions, zoneId).values
        val hrvByDay = measurements.filter { it.metricType.equals(RingMetricType.HRV.name, true) }
            .groupBy { it.measuredAt.toDate(zoneId) }
            .mapValues { it.value.map { m -> m.primaryValue } }.values.toList()
        val hrByDay = activities.filter { it.averageHeartRate != null }
            .groupBy { it.startedAt.toDate(zoneId) }
            .mapValues { it.value.mapNotNull { a -> a.averageHeartRate!! } }.values.toList()

        suspend fun tryEstablish(factorCode: String, values: List<Double>) {
            if (values.size < 14) return
            val active = rdiBaselineDao.activeBaseline(userId, factorCode)
            if (active != null && active.frozenUntil >= establishedOn) return
            val median = values.median() ?: return
            val mad = values.map { kotlin.math.abs(it - median) }.median() ?: 0.0
            val version = (active?.version ?: 0) + 1
            rdiBaselineDao.establish(
                RdiBaselineEntity(
                    userId = userId,
                    factorCode = factorCode,
                    baselineValue = median,
                    mad = mad,
                    establishedOn = establishedOn,
                    frozenUntil = frozenUntil,
                    version = version,
                    status = "ACTIVE",
                    algorithmVersion = RDI_ALGORITHM_VERSION,
                    updatedAt = now,
                ),
            )
        }

        tryEstablish("steps", stepsByDay)
        // The engine compares a rolling 7-day total with this baseline. Persist a
        // weekly-equivalent value rather than the old daily-minute median.
        tryEstablish("verified_activity_minutes", minutesByDay.map { it * 7.0 })
        tryEstablish("sleep_duration", sleepByDay.map { it.durationMinutes.toDouble() })
        // Sleep regularity is a standard deviation, not a clock minute. Keep it
        // window-relative in the engine until a dedicated SD baseline is stored.
        tryEstablish("sleep_efficiency", sleepByDay.map { it.efficiency })
        tryEstablish("hrv_personal_trend", hrvByDay.map { it.median() ?: 0.0 })
        tryEstablish("resting_hr", hrByDay.map { it.median() ?: 0.0 })

        // 血压基线（设计 6.7）：仅已确认袖带且有效日足够。
        val bp = rhiManualHealthInputDao.get(userId)?.toClinicalBloodPressureValues()
        if (bp != null && bp.confirmedUpperArmCuff && (bp.validDays ?: 0) >= 14) {
            bp.sbp7dMean?.let { tryEstablish("bp_sbp", listOf(it)) }
            bp.dbp7dMean?.let { tryEstablish("bp_dbp", listOf(it)) }
        }
    }

    private fun userKey(): String = userIdProvider()?.takeIf { it.isNotBlank() } ?: LOCAL_DEVICE_USER

    private suspend fun dietRecordsForDay(userId: String, date: LocalDate): List<DietRecordEntity> {
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return dietRecordDao.recordsBetween(userId, start, end)
    }

    private suspend fun dietRecordsBetween(userId: String, sinceInclusiveMs: Long): List<DietRecordEntity> {
        val now = clock()
        return dietRecordDao.recordsBetween(userId, sinceInclusiveMs, now)
    }

    /**
     * 统计近 28 天中有任意有效数据的自然日数（validDays），
     * 以及截止到 scoredOn 的连续无有效数据自然日数（staleDays）。
     * 某自然日只要存在步数聚合记录、睡眠会话或测量记录之一即视为有效。
     */
    private fun computeValidDays(
        activities: List<RingActivityEntity>,
        sleepSessions: List<RingSleepSessionEntity>,
        measurements: List<RingMeasurementEntity>,
        scoredOn: LocalDate,
    ): Pair<Int, Int> {
        val activeDays = mutableSetOf<LocalDate>()
        activities.forEach { activeDays.add(it.startedAt.toDate(zoneId)) }
        sleepSessions.forEach { activeDays.add(it.startedAt.toDate(zoneId)) }
        measurements.forEach { activeDays.add(it.measuredAt.toDate(zoneId)) }

        var validDays = 0
        for (daysAgo in 0..27) {
            if (activeDays.contains(scoredOn.minusDays(daysAgo.toLong()))) validDays++
        }
        var staleDays = 0
        var cursor = scoredOn
        while (!activeDays.contains(cursor)) {
            staleDays++
            cursor = cursor.minusDays(1)
            if (staleDays > 30) break
        }
        return validDays to staleDays
    }

    companion object {
        private const val LOCAL_DEVICE_USER = "__local_device__"
        private const val RDI_HISTORY_DAYS = 90
        private const val RDI_WARMUP_DAYS = 28
    }
}

private data class CalculatedRdiDay(
    val date: LocalDate,
    val calculation: RdiCalculation,
)
