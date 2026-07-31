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
class RhiManualInputMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "rhi-manual-input-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migration8To9_preservesHealthDataAndCreatesManualInputTable() {
        helper.createDatabase(databaseName, 8).apply {
            execSQL(
                """
                INSERT INTO health_records (id, type, value, unit, recordedAt, source)
                VALUES ('existing', 'steps', '6000', 'steps', 1, 'ring-a')
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            9,
            true,
            AppDatabase.Migration8To9,
        ).use { database ->
            database.query("SELECT value FROM health_records WHERE id = 'existing'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("6000", cursor.getString(0))
            }
            database.execSQL(
                """
                INSERT INTO rhi_manual_health_inputs (
                    user_id, sedentary_hours_per_day, waist_circumference_cm,
                    vo2_max_ml_kg_min, hba1c_percent, egfr_ml_min_1_73m2, updated_at
                ) VALUES ('user-1', 8.0, 82.0, 36.0, 5.6, 96.0, 1)
                """.trimIndent(),
            )
            database.query(
                "SELECT waist_circumference_cm FROM rhi_manual_health_inputs WHERE user_id = 'user-1'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(82.0, cursor.getDouble(0), 0.0)
            }
        }

        context.deleteDatabase(databaseName)
    }

    @Test
    @Throws(IOException::class)
    fun migration9To10_preservesManualRhiValuesAndAddsVerifiedClinicalColumns() {
        val versionedDatabaseName = "$databaseName-9-10"
        helper.createDatabase(versionedDatabaseName, 9).apply {
            execSQL(
                """
                INSERT INTO rhi_manual_health_inputs (
                    user_id, sedentary_hours_per_day, waist_circumference_cm,
                    vo2_max_ml_kg_min, hba1c_percent, egfr_ml_min_1_73m2, updated_at
                ) VALUES ('user-1', 8.0, 82.0, 36.0, 5.6, 96.0, 1)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            versionedDatabaseName,
            10,
            true,
            AppDatabase.Migration9To10,
        ).use { database ->
            database.query(
                """
                SELECT waist_circumference_cm, hba1c_percent, cuff_confirmed,
                       lab_confirmed, ldl_mmol_l
                FROM rhi_manual_health_inputs
                WHERE user_id = 'user-1'
                """.trimIndent(),
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(82.0, cursor.getDouble(0), 0.0)
                assertEquals(5.6, cursor.getDouble(1), 0.0)
                assertEquals(0, cursor.getInt(2))
                assertEquals(0, cursor.getInt(3))
                assertEquals(true, cursor.isNull(4))
            }
        }

        context.deleteDatabase(versionedDatabaseName)
    }
}
