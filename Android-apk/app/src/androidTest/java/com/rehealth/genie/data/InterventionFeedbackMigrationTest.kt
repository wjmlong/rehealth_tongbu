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
class InterventionFeedbackMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "intervention-feedback-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        databaseClass = AppDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migration17To18_preservesLegacyRowsAndAddsInstitutionScope() {
        helper.createDatabase(databaseName, 17).apply {
            execSQL(
                """
                INSERT INTO intervention_feedback_queue (
                    id, intervention_id, status, checked_at, created_at,
                    upload_status, upload_attempts, next_retry_at
                ) VALUES ('legacy-1', 'plan-1', 'completed', 1, 1, 'done', 0, 0)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            18,
            true,
            AppDatabase.Migration17To18,
        ).use { database ->
            database.query(
                "SELECT owner_user_id, verification_type FROM intervention_feedback_queue WHERE id='legacy-1'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertTrue(cursor.isNull(0))
                assertEquals("self_report", cursor.getString(1))
            }
            database.execSQL(
                """
                INSERT INTO intervention_feedback_queue (
                    id, owner_user_id, intervention_id, binding_id, tenant_id, plan_item_id,
                    status, expected_count, completed_count, verification_type,
                    checked_at, created_at, upload_status, upload_attempts, next_retry_at
                ) VALUES (
                    'institution-1', 'user-1', 'plan-1', 'binding-1', 9102, 'item-1',
                    'partially_completed', 1, 0.5, 'self_report',
                    2, 2, 'pending', 0, 2
                )
                """.trimIndent(),
            )
            database.query(
                "SELECT tenant_id, completed_count FROM intervention_feedback_queue WHERE id='institution-1'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(9102, cursor.getInt(0))
                assertEquals(0.5, cursor.getDouble(1), 0.0)
            }
        }

        context.deleteDatabase(databaseName)
    }

    @Test
    @Throws(IOException::class)
    fun migration18To19_addsVersionedOccurrenceIdentity() {
        helper.createDatabase(databaseName, 18).close()

        helper.runMigrationsAndValidate(
            databaseName,
            19,
            true,
            AppDatabase.Migration18To19,
        ).use { database ->
            database.execSQL(
                """
                INSERT INTO intervention_feedback_queue (
                    id, owner_user_id, intervention_id, occurrence_id, status,
                    checked_at, created_at, upload_status, upload_attempts, next_retry_at
                ) VALUES (
                    'occurrence-feedback-1', 'user-1', 'plan-1', 'occurrence-1', 'completed',
                    1, 1, 'pending', 0, 1
                )
                """.trimIndent(),
            )
            database.query(
                "SELECT occurrence_id FROM intervention_feedback_queue WHERE id='occurrence-feedback-1'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("occurrence-1", cursor.getString(0))
            }
        }

        context.deleteDatabase(databaseName)
    }
}
