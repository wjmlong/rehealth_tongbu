package com.rehealth.genie.rhi

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * RHI daily persistence, split per the RHI-100 v2 data model.
 *
 * The clinical-risk assessment is an immutable, versioned record and lives in a
 * separate table owned by the RDI-16 pipeline. RHI is a daily time series and is
 * never merged into that table. Within RHI itself the concerns are split so that
 * a score, its domain breakdown, the features that produced it, and the data
 * quality that qualified it can each be queried and retained independently:
 *
 * - [RhiDailyIndexEntity]     `daily_health_index`
 * - [RhiDailyDomainScoreEntity] `daily_domain_score`
 * - [RhiDailyFeatureSnapshotEntity] `daily_feature_snapshot`
 * - [RhiDataQualitySnapshotEntity]  `data_quality_snapshot`
 *
 * Rows are keyed by `(user_id, scored_on)` so a recomputation replaces a day
 * rather than appending a duplicate.
 */
@Entity(
    tableName = "rhi_daily_health_index",
    indices = [
        Index(value = ["user_id", "scored_on"], unique = true),
        Index(value = ["user_id", "updated_at"]),
    ],
)
data class RhiDailyIndexEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    /** Local date in ISO-8601 (`yyyy-MM-dd`). */
    @ColumnInfo(name = "scored_on") val scoredOn: String,
    /** Unsmoothed score for the day. */
    @ColumnInfo(name = "raw_score") val rawScore: Double,
    /** Smoothed value shown to the user. */
    @ColumnInfo(name = "display_score") val displayScore: Double,
    @ColumnInfo(name = "data_confidence") val dataConfidence: Double,
    /** Cold-start state: provisional / initial / baseline_confirmed / confirmed. */
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "product_tier") val productTier: String,
    /** Number of days with usable evidence inside the trailing 7-day window. */
    @ColumnInfo(name = "available_days") val availableDays: Int,
    @ColumnInfo(name = "available_feature_count") val availableFeatureCount: Int,
    @ColumnInfo(name = "smoothing_alpha") val smoothingAlpha: Double,
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: String,
    @ColumnInfo(name = "calculation_source") val calculationSource: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "rhi_daily_domain_score",
    indices = [
        Index(value = ["index_id"]),
        Index(value = ["user_id", "scored_on", "domain"], unique = true),
    ],
)
data class RhiDailyDomainScoreEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "index_id") val indexId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "scored_on") val scoredOn: String,
    /** hemodynamic / activity_fitness / sleep_recovery / metabolic_control / behavior_adherence. */
    @ColumnInfo(name = "domain") val domain: String,
    /** Null when the domain had no eligible indicator and was excluded from the total. */
    @ColumnInfo(name = "score") val score: Double?,
    @ColumnInfo(name = "weight") val weight: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "rhi_daily_feature_snapshot",
    indices = [
        Index(value = ["index_id"]),
        Index(value = ["user_id", "scored_on", "feature"], unique = true),
    ],
)
data class RhiDailyFeatureSnapshotEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "index_id") val indexId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "scored_on") val scoredOn: String,
    /** One of the 32 core field names. */
    @ColumnInfo(name = "feature") val feature: String,
    @ColumnInfo(name = "value") val value: Double,
    @ColumnInfo(name = "confidence") val confidence: Double,
    @ColumnInfo(name = "baseline_median") val baselineMedian: Double?,
    @ColumnInfo(name = "baseline_mad") val baselineMad: Double?,
    @ColumnInfo(name = "baseline_sample_count") val baselineSampleCount: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "rhi_data_quality_snapshot",
    indices = [
        Index(value = ["index_id"]),
        Index(value = ["user_id", "scored_on"], unique = true),
    ],
)
data class RhiDataQualitySnapshotEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "index_id") val indexId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "scored_on") val scoredOn: String,
    @ColumnInfo(name = "confidence_score") val confidenceScore: Double,
    /** A / B / C / D. */
    @ColumnInfo(name = "confidence_grade") val confidenceGrade: String,
    /** Comma-separated field names; empty string when none. */
    @ColumnInfo(name = "missing_fields") val missingFields: String,
    @ColumnInfo(name = "low_confidence_fields") val lowConfidenceFields: String,
    /** Comma-separated warning codes; empty string when none. */
    @ColumnInfo(name = "warning_codes") val warningCodes: String,
    /** Human-readable warnings, one per line. */
    @ColumnInfo(name = "warning_messages") val warningMessages: String,
    @ColumnInfo(name = "device_change_detected") val deviceChangeDetected: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** A day's index together with everything that explains it. */
data class RhiDailyBundle(
    @Embedded val index: RhiDailyIndexEntity,
    @Relation(parentColumn = "id", entityColumn = "index_id")
    val domains: List<RhiDailyDomainScoreEntity>,
    @Relation(parentColumn = "id", entityColumn = "index_id")
    val features: List<RhiDailyFeatureSnapshotEntity>,
    @Relation(parentColumn = "id", entityColumn = "index_id")
    val quality: List<RhiDataQualitySnapshotEntity>,
)

/** Maps a confidence score to the published A–D grade. */
fun rhiConfidenceGrade(score: Double): String = when {
    score >= 0.85 -> "A"
    score >= 0.70 -> "B"
    score >= 0.50 -> "C"
    else -> "D"
}

/** Cold-start status derived from the number of accumulated days. */
fun rhiColdStartStatus(availableDays: Int): String = when {
    availableDays < 7 -> "provisional"
    availableDays < 14 -> "initial"
    availableDays < 28 -> "baseline_confirmed"
    else -> "confirmed"
}
