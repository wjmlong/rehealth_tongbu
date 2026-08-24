package com.rehealth.genie.rdi

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "rdi_daily_snapshots",
    indices = [
        Index(value = ["user_id", "scored_on"], unique = true),
        Index(value = ["user_id", "updated_at"]),
    ],
)
data class RdiDailySnapshotEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "scored_on") val scoredOn: String,
    @ColumnInfo(name = "raw_score") val rawScore: Double,
    @ColumnInfo(name = "display_score") val displayScore: Double,
    @ColumnInfo(name = "data_confidence") val dataConfidence: Double,
    val status: String,
    @ColumnInfo(name = "is_mock", defaultValue = "0") val isMock: Boolean = false,
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "rdi_contribution_records",
    indices = [
        Index(value = ["snapshot_id"]),
        Index(value = ["user_id", "scored_on"]),
        Index(value = ["source_factor_id"]),
    ],
)
data class RdiContributionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "snapshot_id") val snapshotId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "scored_on") val scoredOn: String,
    @ColumnInfo(name = "factor_code") val factorCode: String,
    val domain: String,
    val source: String,
    @ColumnInfo(name = "current_value") val currentValue: Double,
    @ColumnInfo(name = "baseline_value") val baselineValue: Double?,
    val unit: String,
    @ColumnInfo(name = "raw_points") val rawPoints: Double,
    val confidence: Double,
    @ColumnInfo(name = "final_points") val finalPoints: Double,
    @ColumnInfo(name = "evidence_text") val evidenceText: String,
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: String,
    @ColumnInfo(name = "source_factor_id") val sourceFactorId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

data class RdiSnapshotBundle(
    @Embedded val snapshot: RdiDailySnapshotEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "snapshot_id",
    )
    val contributions: List<RdiContributionEntity>,
)

/**
 * 个人基线锚定（设计 6.2）：首个连续积累到 14 个有效日后冻结为稳健中位数 + MAD，
 * 冻结 90 天；重建时保留旧版本（status = SUPERSEDED），不覆盖历史锚定。
 */
@Entity(
    tableName = "rdi_baselines",
    primaryKeys = ["user_id", "factor_code", "version"],
    indices = [Index(value = ["user_id", "factor_code"])],
)
data class RdiBaselineEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "factor_code") val factorCode: String,
    /** 锚定的稳健中位数。 */
    @ColumnInfo(name = "baseline_value") val baselineValue: Double,
    /** 中位数绝对偏差（MAD），用于质量与一致性校验。 */
    @ColumnInfo(name = "mad") val mad: Double,
    /** 基线建立日（本地日期）。 */
    @ColumnInfo(name = "established_on") val establishedOn: String,
    /** 冻结到期日（established_on + 90 天）。 */
    @ColumnInfo(name = "frozen_until") val frozenUntil: String,
    /** 基线版本，每次重建递增，旧版本保留为 SUPERSEDED。 */
    @ColumnInfo(name = "version") val version: Int,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * 已确认血检锚点（设计 4.2 / 5.6 / 6.1 C_lab）。
 * 来自血检照片 → model-service OCR 标准化 → 用户逐项确认后写入 Room。
 * OCR 未确认（confidence=0）不计分。
 */
@Entity(tableName = "rdi_confirmed_labs")
data class RdiConfirmedLabEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    /** 指标代码，如 LDL_C、HDL_C、FASTING_GLUCOSE。 */
    @ColumnInfo(name = "marker_code") val markerCode: String,
    @ColumnInfo(name = "measured_value") val measuredValue: Double,
    @ColumnInfo(name = "unit") val unit: String,
    /** 测量/报告日期（本地日期）。 */
    @ColumnInfo(name = "measured_at") val measuredAt: String,
    /**
     * model-service 标准化服务输出的贡献点（-10~+10）。原始实测值量纲不同，
     * 不能直接当作贡献分；该字段缺失时本指标不参与计分。
     */
    @ColumnInfo(name = "normalized_point") val normalizedPoint: Double? = null,
    /** 近期控制支持趋势分（设计 6.1：实测 80% + 控制支持 20%），范围约 -1~+1。 */
    @ColumnInfo(name = "control_trend") val controlTrend: Double,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "confidence") val confidence: Double,
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * 已确认饮食记录（设计 4.2 / 5.7 / 6.1 C_diet）。
 * 来自食物照片 → model-service 食物候选/份量/营养 → 用户确认或修正后写入 Room。
 * 单餐影响范围 -2~+2。
 */
@Entity(tableName = "rdi_confirmed_meals")
data class RdiConfirmedMealEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "meal_type") val mealType: String,
    @ColumnInfo(name = "kcal_low") val kcalLow: Double,
    @ColumnInfo(name = "kcal_high") val kcalHigh: Double,
    @ColumnInfo(name = "protein_low") val proteinLow: Double,
    @ColumnInfo(name = "protein_high") val proteinHigh: Double,
    @ColumnInfo(name = "fat_low") val fatLow: Double,
    @ColumnInfo(name = "fat_high") val fatHigh: Double,
    @ColumnInfo(name = "sodium_low") val sodiumLow: Double,
    @ColumnInfo(name = "sodium_high") val sodiumHigh: Double,
    /** 单餐影响分，范围 -2~+2（设计 6.1）。 */
    @ColumnInfo(name = "meal_impact") val mealImpact: Double,
    @ColumnInfo(name = "reason_text") val reasonText: String,
    @ColumnInfo(name = "recorded_at") val recordedAt: String,
    @ColumnInfo(name = "confidence") val confidence: Double,
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
