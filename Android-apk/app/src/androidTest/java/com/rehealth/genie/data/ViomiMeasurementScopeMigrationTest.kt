package com.rehealth.genie.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViomiMeasurementScopeMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "viomi-measurement-scope-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migration14To15_preservesMeasurementsAndAddsNullableScope() {
        helper.createDatabase(databaseName, 14).apply {
            execSQL(
                """
                INSERT INTO ring_measurements (
                    id, metric_type, measured_at, primary_value, secondary_value,
                    unit, quality, source, raw_payload
                ) VALUES ('legacy', 'HEART_RATE', 1, 70.0, NULL, 'bpm', NULL, 'viomi_cloud', NULL)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            15,
            true,
            AppDatabase.Migration14To15,
        ).use { database ->
            database.query(
                "SELECT id, owner_user_id, device_id FROM ring_measurements WHERE id = 'legacy'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("legacy", cursor.getString(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.isNull(2))
            }
        }

        context.deleteDatabase(databaseName)
    }

    @Test
    @Throws(IOException::class)
    fun migration15To16_preservesTelemetryAndAddsNullableOwnerScope() {
        helper.createDatabase(databaseName, 15).apply {
            execSQL(
                "INSERT INTO ring_sleep_sessions " +
                    "(id, started_at, ended_at, deep_minutes, light_minutes, awake_minutes, " +
                    "rem_minutes, interruption_minutes, source, raw_payload, total_sleep_minutes) " +
                    "VALUES ('sleep', 1, 2, 1, 0, 0, 0, 0, 'hband_wearable', NULL, 1)",
            )
            execSQL(
                "INSERT INTO ring_activities " +
                    "(id, started_at, ended_at, activity_type, steps, distance_meters, " +
                    "calories_kcal, duration_minutes, average_heart_rate, source, raw_payload) " +
                    "VALUES ('activity', 1, 2, 'walk', 100, 50.0, 10.0, 1, NULL, 'hband_wearable', NULL)",
            )
            execSQL(
                "INSERT INTO ring_signal_chunks " +
                    "(id, signal_type, started_at, sample_rate_hz, sample_count, encoding, payload, source, " +
                    "draw_frequency_hz, duration_seconds, lead_type, ecg_type, calibration_type, " +
                    "average_heart_rate, contact_quality) " +
                    "VALUES ('signal', 'ECG', 1, 100, 1, 'INT32_LE', X'01', 'hband_wearable', " +
                    "NULL, NULL, NULL, NULL, NULL, NULL, NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            16,
            true,
            AppDatabase.Migration15To16,
        ).use { database ->
            listOf("ring_sleep_sessions", "ring_activities", "ring_signal_chunks").forEach { table ->
                database.query("SELECT owner_user_id, device_id FROM $table").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertTrue(cursor.isNull(0))
                    assertTrue(cursor.isNull(1))
                }
            }
        }

        context.deleteDatabase(databaseName)
    }
}
