package com.rehealth.genie.diet

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diet_records",
    indices = [
        Index(value = ["user_id", "consumed_at"]),
        Index(value = ["upload_batch_id"]),
    ],
)
data class DietRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "consumed_at") val consumedAt: Long,
    @ColumnInfo(name = "meal_type") val mealType: String,
    val description: String,
    @ColumnInfo(name = "calories_kcal") val caloriesKcal: Double,
    @ColumnInfo(name = "protein_grams") val proteinGrams: Double?,
    @ColumnInfo(name = "carbohydrate_grams") val carbohydrateGrams: Double?,
    @ColumnInfo(name = "fat_grams") val fatGrams: Double?,
    @ColumnInfo(name = "fiber_grams") val fiberGrams: Double?,
    @ColumnInfo(name = "sodium_milligrams") val sodiumMilligrams: Double?,
    val source: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "upload_batch_id") val uploadBatchId: String?,
)

data class DietRecordWithUploadState(
    @Embedded val record: DietRecordEntity,
    @ColumnInfo(name = "upload_status") val uploadStatus: String?,
)

data class DietRecordDraft(
    val mealType: String,
    val description: String,
    val caloriesKcal: Double,
    val proteinGrams: Double? = null,
    val carbohydrateGrams: Double? = null,
    val fatGrams: Double? = null,
    val fiberGrams: Double? = null,
    val sodiumMilligrams: Double? = null,
    val consumedAt: Long = System.currentTimeMillis(),
)

enum class DietMealType(val wireValue: String) {
    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner"),
    SNACK("snack"),
    ;

    companion object {
        fun isSupported(value: String): Boolean = entries.any { it.wireValue == value }
    }
}
