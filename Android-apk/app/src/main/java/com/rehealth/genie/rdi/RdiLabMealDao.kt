package com.rehealth.genie.rdi

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** 已确认血检与饮食记录的访问接口（设计 4.2 / 5.6 / 5.7）。 */
@Dao
interface RdiLabMealDao {
    @Query("SELECT * FROM rdi_confirmed_labs WHERE user_id = :userId")
    suspend fun confirmedLabs(userId: String): List<RdiConfirmedLabEntity>

    @Query("SELECT * FROM rdi_confirmed_meals WHERE user_id = :userId")
    suspend fun confirmedMeals(userId: String): List<RdiConfirmedMealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLab(entity: RdiConfirmedLabEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeal(entity: RdiConfirmedMealEntity)

    @Query("DELETE FROM rdi_confirmed_labs WHERE id = :id")
    suspend fun deleteLab(id: String)

    @Query("DELETE FROM rdi_confirmed_meals WHERE id = :id")
    suspend fun deleteMeal(id: String)

    @Transaction
    suspend fun replaceLabs(userId: String, labs: List<RdiConfirmedLabEntity>) {
        labs.forEach { upsertLab(it) }
    }

    @Transaction
    suspend fun replaceMeals(userId: String, meals: List<RdiConfirmedMealEntity>) {
        meals.forEach { upsertMeal(it) }
    }
}
