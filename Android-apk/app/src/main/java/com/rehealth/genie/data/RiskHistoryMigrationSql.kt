package com.rehealth.genie.data

internal object RiskHistoryMigrationSql {
    fun forColumns(columns: Set<String>): List<String> = when {
        columns.isEmpty() -> listOf(CREATE_TABLE, CREATE_INDEX)
        columns == CANONICAL_COLUMNS -> listOf(CREATE_INDEX)
        columns == LEGACY_COLUMNS -> listOf(
            "ALTER TABLE cvd_risk_history RENAME TO cvd_risk_history_legacy",
            CREATE_TABLE.replace(" IF NOT EXISTS", ""),
            """
            INSERT INTO cvd_risk_history (
                user_id, evaluated_on, risk_score, risk_level, evaluated_at
            )
            SELECT '__legacy_unscoped__', evaluated_on, risk_score, NULL, evaluated_at
            FROM cvd_risk_history_legacy
            """.trimIndent(),
            "DROP TABLE cvd_risk_history_legacy",
            CREATE_INDEX,
        )
        else -> error("Unsupported cvd_risk_history schema; refusing destructive migration")
    }

    const val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS cvd_risk_history (
            user_id TEXT NOT NULL,
            evaluated_on TEXT NOT NULL,
            risk_score REAL NOT NULL,
            risk_level TEXT,
            evaluated_at INTEGER NOT NULL,
            PRIMARY KEY(user_id, evaluated_on)
        )
    """

    const val CREATE_INDEX =
        "CREATE INDEX IF NOT EXISTS index_cvd_risk_history_user_day " +
            "ON cvd_risk_history(user_id, evaluated_on)"

    private val LEGACY_COLUMNS = setOf("evaluated_on", "risk_score", "evaluated_at")
    private val CANONICAL_COLUMNS = LEGACY_COLUMNS + setOf("user_id", "risk_level")
}

internal object VersionThreeSchemaSql {
    fun forExistingTables(tables: Set<String>): List<String> = buildList {
        if ("sync_upload_queue" in tables) {
            add("ALTER TABLE sync_upload_queue RENAME TO sync_upload_queue_legacy")
            add(CREATE_UPLOAD_QUEUE)
            add(
                """
                INSERT INTO sync_upload_queue (
                    id, kind, payload_json, status, attempts, last_error, created_at, next_retry_at
                )
                SELECT id, kind, payload_json, status, attempts, last_error, created_at, next_retry_at
                FROM sync_upload_queue_legacy
                """.trimIndent(),
            )
            add("DROP TABLE sync_upload_queue_legacy")
        } else {
            add(CREATE_UPLOAD_QUEUE)
        }
        if ("intervention_feedback_queue" in tables) {
            add("ALTER TABLE intervention_feedback_queue RENAME TO intervention_feedback_queue_legacy")
            add(CREATE_FEEDBACK_QUEUE)
            add(
                """
                INSERT INTO intervention_feedback_queue (
                    id, intervention_id, status, note, checked_at, created_at,
                    upload_status, upload_attempts, last_error, next_retry_at
                )
                SELECT id, intervention_id, status, note, checked_at, created_at,
                    upload_status, upload_attempts, last_error, next_retry_at
                FROM intervention_feedback_queue_legacy
                """.trimIndent(),
            )
            add("DROP TABLE intervention_feedback_queue_legacy")
        } else {
            add(CREATE_FEEDBACK_QUEUE)
        }
    }

    private const val CREATE_UPLOAD_QUEUE = """
        CREATE TABLE sync_upload_queue (
            id TEXT NOT NULL PRIMARY KEY,
            kind TEXT NOT NULL,
            payload_json TEXT NOT NULL,
            status TEXT NOT NULL,
            attempts INTEGER NOT NULL,
            last_error TEXT,
            created_at INTEGER NOT NULL,
            next_retry_at INTEGER NOT NULL
        )
    """

    private const val CREATE_FEEDBACK_QUEUE = """
        CREATE TABLE intervention_feedback_queue (
            id TEXT NOT NULL PRIMARY KEY,
            intervention_id TEXT NOT NULL,
            status TEXT NOT NULL,
            note TEXT,
            checked_at INTEGER NOT NULL,
            created_at INTEGER NOT NULL,
            upload_status TEXT NOT NULL,
            upload_attempts INTEGER NOT NULL,
            last_error TEXT,
            next_retry_at INTEGER NOT NULL
        )
    """
}
