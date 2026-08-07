package com.rehealth.genie.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rehealth.genie.diet.DietRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DietRecordDaoTest {
    @Test
    fun insertedMealIsReturnedOnlyForItsOwnerAndDay() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = database.dietRecordDao()
            dao.insert(meal("meal-today", "user-a", 1_500L))
            dao.insert(meal("meal-other-user", "user-b", 1_500L))
            dao.insert(meal("meal-yesterday", "user-a", 500L))

            val rows = dao.observeBetween("user-a", 1_000L, 2_000L).first()

            assertEquals(listOf("meal-today"), rows.map { it.record.id })
        } finally {
            database.close()
        }
    }

    private fun meal(id: String, userId: String, consumedAt: Long) = DietRecordEntity(
        id = id,
        userId = userId,
        consumedAt = consumedAt,
        mealType = "lunch",
        description = "test meal",
        caloriesKcal = 420.0,
        proteinGrams = null,
        carbohydrateGrams = null,
        fatGrams = null,
        fiberGrams = null,
        sodiumMilligrams = null,
        source = "test",
        createdAt = consumedAt,
        uploadBatchId = null,
    )
}
