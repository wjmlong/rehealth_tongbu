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
