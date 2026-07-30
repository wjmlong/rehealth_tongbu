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
class RdiMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "rdi-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migration7To8_preservesExistingDataAndCreatesRdiTables() {
        helper.createDatabase(databaseName, 7).apply {
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
            8,
            true,
            AppDatabase.Migration7To8,
        ).use { database ->
            database.query("SELECT value FROM health_records WHERE id = 'existing'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("6000", cursor.getString(0))
            }
            database.execSQL(
                """
                INSERT INTO rdi_daily_snapshots (
                    id, user_id, scored_on, raw_score, display_score, data_confidence,
                    status, algorithm_version, created_at, updated_at
                ) VALUES (
                    'user-1:2026-07-30', 'user-1', '2026-07-30', 48.0, 49.4, 0.8,
                    'confirmed', 'rdi-rule-1.0.0', 1, 1
                )
                """.trimIndent(),
            )
            database.query("SELECT COUNT(*) FROM rdi_daily_snapshots").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }

        context.deleteDatabase(databaseName)
    }
}
