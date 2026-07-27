package com.rehealth.genie.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * One confirmed, non-mock backend risk evaluation per user and calendar day.
 *
 * The table is deliberately small: it stores the score needed by the PIAS input,
 * not the source feature vector or raw health measurements.
 */
@Entity(
    tableName = "cvd_risk_history",
    primaryKeys = ["user_id", "evaluated_on"],
    indices = [Index(value = ["user_id", "evaluated_on"], name = "index_cvd_risk_history_user_day")],
)
data class RiskHistoryEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "evaluated_on") val evaluatedOn: String,
    @ColumnInfo(name = "risk_score") val riskScore: Double,
    @ColumnInfo(name = "risk_level") val riskLevel: String?,
    @ColumnInfo(name = "evaluated_at") val evaluatedAt: Long,
)
