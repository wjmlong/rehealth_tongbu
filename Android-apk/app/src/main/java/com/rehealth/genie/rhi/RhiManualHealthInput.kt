package com.rehealth.genie.rhi

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.rehealth.genie.features.ClinicalBloodPressureValues
import com.rehealth.genie.features.ClinicalLabValues

@Entity(tableName = "rhi_manual_health_inputs")
data class RhiManualHealthInputEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "sedentary_hours_per_day")
    val sedentaryHoursPerDay: Double? = null,
    @ColumnInfo(name = "waist_circumference_cm")
    val waistCircumferenceCm: Double? = null,
    @ColumnInfo(name = "vo2_max_ml_kg_min")
    val vo2MaxMlKgMin: Double? = null,
    @ColumnInfo(name = "hba1c_percent")
    val hba1cPercent: Double? = null,
    @ColumnInfo(name = "egfr_ml_min_1_73m2")
    val egfrMlMin173m2: Double? = null,
    @ColumnInfo(name = "cuff_sbp_7d_mean")
    val cuffSbp7dMean: Double? = null,
    @ColumnInfo(name = "cuff_dbp_7d_mean")
    val cuffDbp7dMean: Double? = null,
    @ColumnInfo(name = "cuff_valid_days")
    val cuffValidDays: Int? = null,
    @ColumnInfo(name = "cuff_confirmed")
    val cuffConfirmed: Boolean = false,
    @ColumnInfo(name = "fasting_glucose_mmol_l")
    val fastingGlucoseMmolL: Double? = null,
    @ColumnInfo(name = "total_cholesterol_mmol_l")
    val totalCholesterolMmolL: Double? = null,
    @ColumnInfo(name = "ldl_mmol_l")
    val ldlMmolL: Double? = null,
    @ColumnInfo(name = "hdl_mmol_l")
    val hdlMmolL: Double? = null,
    @ColumnInfo(name = "triglycerides_mmol_l")
    val triglyceridesMmolL: Double? = null,
    @ColumnInfo(name = "lab_confirmed")
    val labConfirmed: Boolean = false,
    @ColumnInfo(name = "lab_recorded_at")
    val labRecordedAt: Long? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Dao
interface RhiManualHealthInputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(input: RhiManualHealthInputEntity)

    @Query("SELECT * FROM rhi_manual_health_inputs WHERE user_id = :userId LIMIT 1")
    suspend fun get(userId: String): RhiManualHealthInputEntity?

    @Query("SELECT * FROM rhi_manual_health_inputs WHERE user_id = :userId LIMIT 1")
    fun observe(userId: String): Flow<RhiManualHealthInputEntity?>
}

fun RhiManualHealthInputEntity.toClinicalLabValues(): ClinicalLabValues? =
    if (!labConfirmed) {
        null
    } else {
        ClinicalLabValues(
            fastingGlucose = fastingGlucoseMmolL,
            totalCholesterol = totalCholesterolMmolL,
            ldl = ldlMmolL,
            hdl = hdlMmolL,
            triglycerides = triglyceridesMmolL,
            recordedAt = labRecordedAt,
        )
    }

fun RhiManualHealthInputEntity.toClinicalBloodPressureValues(): ClinicalBloodPressureValues? =
    if (!cuffConfirmed) {
        null
    } else {
        ClinicalBloodPressureValues(
            sbp7dMean = cuffSbp7dMean,
            dbp7dMean = cuffDbp7dMean,
            validDays = cuffValidDays,
            confirmedUpperArmCuff = true,
            recordedAt = updatedAt,
        )
    }
