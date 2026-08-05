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
}
