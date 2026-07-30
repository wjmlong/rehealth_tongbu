package com.rehealth.genie.rdi

import com.rehealth.genie.ring.data.RingDataDao
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    private val ringDataDao: RingDataDao,
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
        val calculation = RdiEngine.calculate(
            RdiCalculationInput(
                scoredOn = scoredOn,
                zoneId = zoneId,
                activities = ringDataDao.getActivitiesSince(since),
                sleepSessions = ringDataDao.getSleepSessionsSince(since),
                measurements = ringDataDao.getMeasurementsSince(since),
                previousDisplayScore = previous?.displayScore ?: existing?.displayScore,
            ),
        )
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

    private fun userKey(): String = userIdProvider()?.takeIf { it.isNotBlank() } ?: LOCAL_DEVICE_USER

    companion object {
        private const val LOCAL_DEVICE_USER = "__local_device__"
    }
}
