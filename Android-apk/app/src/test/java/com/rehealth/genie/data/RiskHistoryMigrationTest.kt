package com.rehealth.genie.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RiskHistoryMigrationTest {
    @Test
    fun `legacy risk history is preserved before canonical index creation`() {
        val statements = RiskHistoryMigrationSql.forColumns(
            setOf("evaluated_on", "risk_score", "evaluated_at"),
        )

        assertEquals(5, statements.size)
        assertTrue(statements[0].contains("RENAME TO cvd_risk_history_legacy"))
        assertTrue(statements[1].contains("user_id TEXT NOT NULL"))
        assertTrue(statements[2].contains("__legacy_unscoped__"))
        assertTrue(statements[2].contains("SELECT"))
        assertTrue(statements[3].contains("DROP TABLE cvd_risk_history_legacy"))
        assertTrue(statements[4].contains("ON cvd_risk_history(user_id, evaluated_on)"))
    }

    @Test
    fun `canonical risk history only needs its index`() {
        val statements = RiskHistoryMigrationSql.forColumns(
            setOf("user_id", "evaluated_on", "risk_score", "risk_level", "evaluated_at"),
        )

        assertEquals(1, statements.size)
        assertTrue(statements.single().contains("CREATE INDEX IF NOT EXISTS"))
    }

    @Test
    fun `unknown risk history schema fails closed without destructive SQL`() {
        val error = assertFailsWith<IllegalStateException> {
            RiskHistoryMigrationSql.forColumns(setOf("user_id", "evaluated_on"))
        }

        assertTrue(error.message.orEmpty().contains("refusing destructive migration"))
    }

    @Test
    fun `version three reconciliation creates missing queue tables`() {
        val statements = VersionThreeSchemaSql.forExistingTables(emptySet())

        assertEquals(2, statements.size)
        assertTrue(statements[0].contains("CREATE TABLE sync_upload_queue"))
        assertTrue(statements[1].contains("CREATE TABLE intervention_feedback_queue"))
        assertTrue(statements.none { it.contains("CREATE INDEX") })
    }

    @Test
    fun `version three reconciliation rebuilds existing queues without legacy schema metadata`() {
        val statements = VersionThreeSchemaSql.forExistingTables(
            setOf("sync_upload_queue", "intervention_feedback_queue"),
        )

        assertEquals(8, statements.size)
        assertTrue(statements[0].contains("RENAME TO sync_upload_queue_legacy"))
        assertTrue(statements[1].contains("CREATE TABLE sync_upload_queue"))
        assertTrue(statements[2].contains("INSERT INTO sync_upload_queue"))
        assertTrue(statements[3].contains("DROP TABLE sync_upload_queue_legacy"))
        assertTrue(statements[4].contains("RENAME TO intervention_feedback_queue_legacy"))
        assertTrue(statements[5].contains("CREATE TABLE intervention_feedback_queue"))
        assertTrue(statements[6].contains("INSERT INTO intervention_feedback_queue"))
        assertTrue(statements[7].contains("DROP TABLE intervention_feedback_queue_legacy"))
        assertTrue(statements.none { it.contains("DEFAULT") || it.contains("CREATE INDEX") })
    }
}
