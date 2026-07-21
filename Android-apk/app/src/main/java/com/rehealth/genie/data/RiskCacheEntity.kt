package com.rehealth.genie.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Last persisted CVD risk result per user, cached locally so the home screen can
 * render a risk state instantly (and as an offline fallback when the backend
 * is unreachable). The JSON is the serialized [com.rehealth.genie.phm.RiskResult].
 */
@Entity(tableName = "cvd_risk_cache")
data class RiskCacheEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "risk_json") val riskJson: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long,
)
