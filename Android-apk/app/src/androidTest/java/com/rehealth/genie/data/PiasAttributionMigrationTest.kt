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
class PiasAttributionMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "pias-attribution-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migration16To17_preservesExistingDataAndCreatesPiasCache() {
        helper.createDatabase(databaseName, 16).apply {
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
            17,
            true,
            AppDatabase.Migration16To17,
        ).use { database ->
            database.query("SELECT value FROM health_records WHERE id = 'existing'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("6000", cursor.getString(0))
            }
            database.execSQL(
                """
                INSERT INTO pias_attribution_cache (
                    user_id, status, history_days, payload_json,
                    is_mock, model_version, updated_at
                ) VALUES (
                    'user-1', 'ready', 90, '{"status":"ready"}',
                    1, 'debug-pias-preview-1.0.0', 1
                )
                """.trimIndent(),
            )
            database.query(
                "SELECT is_mock, model_version FROM pias_attribution_cache WHERE user_id = 'user-1'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
                assertEquals("debug-pias-preview-1.0.0", cursor.getString(1))
            }
        }

        context.deleteDatabase(databaseName)
    }
}
