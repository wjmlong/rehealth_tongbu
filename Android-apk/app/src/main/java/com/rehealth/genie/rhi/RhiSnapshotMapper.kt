package com.rehealth.genie.rhi

import com.rehealth.genie.network.dto.RhiDailyDomainScoreDto
import com.rehealth.genie.network.dto.RhiDailyFeatureSnapshotDto
import com.rehealth.genie.network.dto.RhiDailyIndexDto
import com.rehealth.genie.network.dto.RhiDailyQualitySnapshotDto
import com.rehealth.genie.network.dto.RhiDailySnapshotBatchDto
import java.time.LocalDate

/**
 * Translates one day's calculation into the split RHI tables.
 *
 * The row id is derived from `(user_id, scored_on)` rather than generated, so a
 * recomputation of the same day overwrites the previous record instead of
 * accumulating duplicates of a score that no longer exists.
 */
object RhiSnapshotMapper {
    fun toEntities(
        userId: String,
        scoredOn: LocalDate,
        calculation: RhiLiteCalculation,
        calculationSource: RhiCalculationSource,
        algorithmVersion: String = RHI_LITE_ALGORITHM_VERSION,
        now: Long = System.currentTimeMillis(),
    ): RhiSnapshotEntities {
        val day = scoredOn.toString()
        val indexId = indexId(userId, day)
        val index = RhiDailyIndexEntity(
            id = indexId,
            userId = userId,
            scoredOn = day,
            rawScore = calculation.rawScore,
            displayScore = calculation.displayScore,
            dataConfidence = calculation.confidence,
            status = rhiColdStartStatus(calculation.availableDays),
            productTier = calculation.productTier.name.lowercase(),
            availableDays = calculation.availableDays,
            availableFeatureCount = calculation.availableFeatureCount,
            smoothingAlpha = RHI_DISPLAY_SMOOTHING_ALPHA,
            algorithmVersion = algorithmVersion,
            calculationSource = calculationSource.name.lowercase(),
            createdAt = now,
            updatedAt = now,
        )
        val domains = calculation.domains.map { (domain, score) ->
            RhiDailyDomainScoreEntity(
                id = "$indexId:$domain",
                indexId = indexId,
                userId = userId,
                scoredOn = day,
                domain = domain,
                score = score,
                weight = RHI_DOMAIN_WEIGHTS[domain] ?: 0.0,
                createdAt = now,
            )
        }
        val features = calculation.features.map { (name, feature) ->
            RhiDailyFeatureSnapshotEntity(
                id = "$indexId:$name",
                indexId = indexId,
                userId = userId,
                scoredOn = day,
                feature = name,
                value = feature.value,
                confidence = feature.confidence,
                baselineMedian = feature.baselineMedian,
                baselineMad = feature.baselineMad,
                baselineSampleCount = feature.baselineSampleCount,
                createdAt = now,
            )
        }
        val quality = RhiDataQualitySnapshotEntity(
            id = indexId,
            indexId = indexId,
            userId = userId,
            scoredOn = day,
            confidenceScore = calculation.confidence,
            confidenceGrade = rhiConfidenceGrade(calculation.confidence),
            missingFields = calculation.missingFields.joinToString(","),
            lowConfidenceFields = calculation.lowConfidenceFields.joinToString(","),
            warningCodes = calculation.qualityWarnings.joinToString(",") { it.code },
            warningMessages = calculation.qualityWarnings.joinToString("\n") { it.message },
            deviceChangeDetected = false,
            createdAt = now,
        )
        return RhiSnapshotEntities(index, domains, features, quality)
    }

    private fun indexId(userId: String, scoredOn: String): String = "$userId:$scoredOn"

    /**
     * Projects the split entities into the upload contract sent to the backend
     * management platform. Only aggregated outputs travel off-device; raw
     * wearable series are never embedded in the upload payload.
     */
    fun toUploadDto(entities: RhiSnapshotEntities): RhiDailyIndexDto {
        val index = entities.index
        val quality = entities.quality
        return RhiDailyIndexDto(
            scoredOn = index.scoredOn,
            rawScore = index.rawScore,
            displayScore = index.displayScore,
            dataConfidence = index.dataConfidence,
            status = index.status,
            productTier = index.productTier,
            availableDays = index.availableDays,
            availableFeatureCount = index.availableFeatureCount,
            smoothingAlpha = index.smoothingAlpha,
            algorithmVersion = index.algorithmVersion,
            calculationSource = index.calculationSource,
            domains = entities.domains.map {
                RhiDailyDomainScoreDto(domain = it.domain, score = it.score, weight = it.weight)
            },
            features = entities.features.map {
                RhiDailyFeatureSnapshotDto(
                    feature = it.feature,
                    value = it.value,
                    confidence = it.confidence,
                    baselineMedian = it.baselineMedian,
                    baselineMad = it.baselineMad,
                    baselineSampleCount = it.baselineSampleCount,
                )
            },
            quality = RhiDailyQualitySnapshotDto(
                confidenceScore = quality.confidenceScore,
                confidenceGrade = quality.confidenceGrade,
                missingFields = quality.missingFields.split(',').filter { it.isNotBlank() },
                lowConfidenceFields = quality.lowConfidenceFields.split(',').filter { it.isNotBlank() },
                warningCodes = quality.warningCodes.split(',').filter { it.isNotBlank() },
                warningMessages = quality.warningMessages.split('\n').filter { it.isNotBlank() },
                deviceChangeDetected = quality.deviceChangeDetected,
            ),
        )
    }
}

data class RhiSnapshotEntities(
    val index: RhiDailyIndexEntity,
    val domains: List<RhiDailyDomainScoreEntity>,
    val features: List<RhiDailyFeatureSnapshotEntity>,
    val quality: RhiDataQualitySnapshotEntity,
)
