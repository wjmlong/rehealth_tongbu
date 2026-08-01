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

/**
 * Guards the RHI table split. The migration is additive, so the contract is
 * twofold: existing rows survive untouched, and the four new tables are created
 * exactly as Room expects them (validated by `runMigrationsAndValidate`).
 */
@RunWith(AndroidJUnit4::class)
class RhiSnapshotMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "rhi-snapshot-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migration13To14_preservesExistingDataAndCreatesRhiTables() {
        helper.createDatabase(databaseName, 13).apply {
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
            14,
            true,
            AppDatabase.Migration13To14,
        ).use { database ->
            database.query("SELECT value FROM health_records WHERE id = 'existing'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("6000", cursor.getString(0))
            }

            database.execSQL(
                """
                INSERT INTO rhi_daily_health_index (
                    id, user_id, scored_on, raw_score, display_score, data_confidence,
                    status, product_tier, available_days, available_feature_count,
                    smoothing_alpha, algorithm_version, calculation_source,
                    created_at, updated_at
                ) VALUES (
                    'user-1:2026-07-31', 'user-1', '2026-07-31', 62.0, 58.5, 0.86,
                    'confirmed', 'clinical', 7, 32,
                    0.25, 'rhi-deterministic-preview-2.2.0-android-lite', 'local',
                    1, 1
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO rhi_daily_domain_score (
                    id, index_id, user_id, scored_on, domain, score, weight, created_at
                ) VALUES (
                    'user-1:2026-07-31:hemodynamic', 'user-1:2026-07-31', 'user-1',
                    '2026-07-31', 'hemodynamic', 61.2, 0.25, 1
                )
                """.trimIndent(),
            )
            // A domain with no eligible indicator is stored as NULL, not as a
            // neutral 50, so an excluded domain is never mistaken for a measured one.
            database.execSQL(
                """
                INSERT INTO rhi_daily_domain_score (
                    id, index_id, user_id, scored_on, domain, score, weight, created_at
                ) VALUES (
                    'user-1:2026-07-31:metabolic_control', 'user-1:2026-07-31', 'user-1',
                    '2026-07-31', 'metabolic_control', NULL, 0.20, 1
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO rhi_daily_feature_snapshot (
                    id, index_id, user_id, scored_on, feature, value, confidence,
                    baseline_median, baseline_mad, baseline_sample_count, created_at
                ) VALUES (
                    'user-1:2026-07-31:steps_7d_mean', 'user-1:2026-07-31', 'user-1',
                    '2026-07-31', 'steps_7d_mean', 7000.0, 0.9, NULL, NULL, 0, 1
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO rhi_data_quality_snapshot (
                    id, index_id, user_id, scored_on, confidence_score, confidence_grade,
                    missing_fields, low_confidence_fields, warning_codes, warning_messages,
                    device_change_detected, created_at
                ) VALUES (
                    'user-1:2026-07-31', 'user-1:2026-07-31', 'user-1', '2026-07-31',
                    0.86, 'A', '', '', 'activity_duration_missing', 'msg', 0, 1
                )
                """.trimIndent(),
            )

            database.query("SELECT COUNT(*) FROM rhi_daily_health_index").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM rhi_daily_domain_score").use { cursor ->
                cursor.moveToFirst()
                assertEquals(2, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM rhi_daily_feature_snapshot").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM rhi_data_quality_snapshot").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }

        context.deleteDatabase(databaseName)
    }
}
