package com.rehealth.genie.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the nullable `total_sleep_minutes` column that the entity declares on
 * `ring_sleep_sessions`. The column is carried forward by the 9→10 migration
 * (the first schema snapshot that contains it is v10), so this test walks the
 * whole 7→8→9→10 chain and validates the resulting schema against Room's
 * exported v10 expectation while preserving an existing sleep row.
 */
@RunWith(AndroidJUnit4::class)
class SleepTotalMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Before
    fun deleteExistingDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun cleanUpDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrationChain7To10PreservesSleepAndAddsNullableVendorTotal() {
        helper.createDatabase(TEST_DATABASE, 7).apply {
            execSQL(
                """
                INSERT INTO ring_sleep_sessions (
                    id, started_at, ended_at, deep_minutes, light_minutes, awake_minutes,
                    rem_minutes, interruption_minutes, source, raw_payload
                ) VALUES (
                    'sleep-existing', 1000, 2000, 10, 20, 5, 0, 5, 'hband_wearable', NULL
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            10,
            true,
            AppDatabase.Migration7To8,
            AppDatabase.Migration8To9,
            AppDatabase.Migration9To10,
        ).apply {
            query(
                "SELECT id, deep_minutes, total_sleep_minutes FROM ring_sleep_sessions",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("sleep-existing", cursor.getString(0))
                assertEquals(10, cursor.getInt(1))
                assertTrue(cursor.isNull(2))
            }
            close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "sleep-total-migration-test"
    }
}
