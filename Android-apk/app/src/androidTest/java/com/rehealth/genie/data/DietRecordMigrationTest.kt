package com.rehealth.genie.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DietRecordMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "diet-record-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migration10To11_preservesExistingDataAndCreatesDietTable() {
        helper.createDatabase(databaseName, 10).apply {
            execSQL(
                """
                INSERT INTO health_records (id, type, value, unit, recordedAt, source)
                VALUES ('existing', 'steps', '7000', 'steps', 1, 'ring-a')
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            11,
            true,
            AppDatabase.Migration10To11,
        ).use { database ->
            database.query("SELECT value FROM health_records WHERE id = 'existing'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("7000", cursor.getString(0))
            }
            database.execSQL(
                """
                INSERT INTO diet_records (
                    id, user_id, consumed_at, meal_type, description, calories_kcal,
                    protein_grams, carbohydrate_grams, fat_grams, fiber_grams,
                    sodium_milligrams, source, created_at, upload_batch_id
                ) VALUES (
                    'diet-1', 'user-1', 2, 'lunch', '牛肉面', 780.0,
                    29.0, 86.0, 25.0, NULL, NULL, 'manual_diet_room', 3, NULL
                )
                """.trimIndent(),
            )
            database.query(
                "SELECT meal_type, calories_kcal FROM diet_records WHERE id = 'diet-1'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("lunch", cursor.getString(0))
                assertEquals(780.0, cursor.getDouble(1), 0.0)
            }
        }

        context.deleteDatabase(databaseName)
    }
}
