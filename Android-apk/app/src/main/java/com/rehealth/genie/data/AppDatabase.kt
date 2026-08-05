package com.rehealth.genie.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rehealth.genie.data.sync.InterventionFeedbackDao
import com.rehealth.genie.data.sync.InterventionFeedbackEntity
import com.rehealth.genie.data.sync.UploadQueueDao
import com.rehealth.genie.data.sync.UploadQueueEntity
import com.rehealth.genie.diet.DietRecordDao
import com.rehealth.genie.diet.DietRecordEntity
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import com.rehealth.genie.rdi.RdiBaselineDao
import com.rehealth.genie.rdi.RdiBaselineEntity
import com.rehealth.genie.rdi.RdiConfirmedLabEntity
import com.rehealth.genie.rdi.RdiConfirmedMealEntity
import com.rehealth.genie.rdi.RdiContributionEntity
import com.rehealth.genie.rdi.RdiDailySnapshotEntity
import com.rehealth.genie.rdi.RdiDao
import com.rehealth.genie.rdi.RdiLabMealDao
import com.rehealth.genie.rhi.RhiDailyDomainScoreEntity
import com.rehealth.genie.rhi.RhiDailyFeatureSnapshotEntity
import com.rehealth.genie.rhi.RhiDailyIndexEntity
import com.rehealth.genie.rhi.RhiDataQualitySnapshotEntity
import com.rehealth.genie.rhi.RhiManualHealthInputDao
import com.rehealth.genie.rhi.RhiManualHealthInputEntity
import com.rehealth.genie.rhi.RhiSnapshotDao

@Entity(tableName = "health_records")
data class HealthRecordEntity(
    @PrimaryKey val id: String,
    val type: String,
    val value: String,
    val unit: String,
    val recordedAt: Long,
    val source: String = "mock",
)

@Entity(tableName = "attribution_logs")
data class AttributionLogEntity(
    @PrimaryKey val id: String,
    val date: String,
    val completeness: Double,
    val evidenceGrade: String,
    val auditHash: String,
)

@Database(
    entities = [
        HealthRecordEntity::class,
        AttributionLogEntity::class,
        RingMeasurementEntity::class,
        RingSleepSessionEntity::class,
        RingActivityEntity::class,
        RingSignalChunkEntity::class,
        UploadQueueEntity::class,
        InterventionFeedbackEntity::class,
        RiskHistoryEntity::class,
        HealthChatConversationEntity::class,
        HealthChatMessageEntity::class,
        RdiDailySnapshotEntity::class,
        RdiContributionEntity::class,
        RdiBaselineEntity::class,
        RdiConfirmedLabEntity::class,
        RdiConfirmedMealEntity::class,
        RhiManualHealthInputEntity::class,
        RhiDailyIndexEntity::class,
        RhiDailyDomainScoreEntity::class,
        RhiDailyFeatureSnapshotEntity::class,
        RhiDataQualitySnapshotEntity::class,
        DietRecordEntity::class,
    ],
    version = 15,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ringDataDao(): RingDataDao
    abstract fun uploadQueueDao(): UploadQueueDao
    abstract fun interventionFeedbackDao(): InterventionFeedbackDao
    abstract fun riskHistoryDao(): RiskHistoryDao
    abstract fun healthChatDao(): HealthChatDao
    abstract fun rdiDao(): RdiDao
    abstract fun rdiBaselineDao(): RdiBaselineDao
    abstract fun rdiLabMealDao(): RdiLabMealDao
    abstract fun rhiManualHealthInputDao(): RhiManualHealthInputDao
    abstract fun rhiSnapshotDao(): RhiSnapshotDao
    abstract fun dietRecordDao(): DietRecordDao

    companion object {
        /** Adds nullable owner/device scope to wearable measurements without deleting legacy rows. */
        val Migration14To15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ring_measurements ADD COLUMN owner_user_id TEXT")
                db.execSQL("ALTER TABLE ring_measurements ADD COLUMN device_id TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "index_ring_measurements_owner_user_id_device_id_source_metric_type_measured_at " +
                        "ON ring_measurements(owner_user_id, device_id, source, metric_type, measured_at)",
                )
            }
        }

        /**
         * Splits RHI daily persistence into index / domain / feature / quality
         * tables. Purely additive: no existing table is altered or dropped, so
         * no user data is lost.
         */
        val Migration13To14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rhi_daily_health_index (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        scored_on TEXT NOT NULL,
                        raw_score REAL NOT NULL,
                        display_score REAL NOT NULL,
                        data_confidence REAL NOT NULL,
                        status TEXT NOT NULL,
                        product_tier TEXT NOT NULL,
                        available_days INTEGER NOT NULL,
                        available_feature_count INTEGER NOT NULL,
                        smoothing_alpha REAL NOT NULL,
                        algorithm_version TEXT NOT NULL,
                        calculation_source TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_rhi_daily_health_index_user_id_scored_on " +
                        "ON rhi_daily_health_index(user_id, scored_on)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "index_rhi_daily_health_index_user_id_updated_at " +
                        "ON rhi_daily_health_index(user_id, updated_at)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rhi_daily_domain_score (
                        id TEXT NOT NULL PRIMARY KEY,
                        index_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        scored_on TEXT NOT NULL,
                        domain TEXT NOT NULL,
                        score REAL,
                        weight REAL NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_rhi_daily_domain_score_index_id " +
                        "ON rhi_daily_domain_score(index_id)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_rhi_daily_domain_score_user_id_scored_on_domain " +
                        "ON rhi_daily_domain_score(user_id, scored_on, domain)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rhi_daily_feature_snapshot (
                        id TEXT NOT NULL PRIMARY KEY,
                        index_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        scored_on TEXT NOT NULL,
                        feature TEXT NOT NULL,
                        value REAL NOT NULL,
                        confidence REAL NOT NULL,
                        baseline_median REAL,
                        baseline_mad REAL,
                        baseline_sample_count INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_rhi_daily_feature_snapshot_index_id " +
                        "ON rhi_daily_feature_snapshot(index_id)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_rhi_daily_feature_snapshot_user_id_scored_on_feature " +
                        "ON rhi_daily_feature_snapshot(user_id, scored_on, feature)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rhi_data_quality_snapshot (
                        id TEXT NOT NULL PRIMARY KEY,
                        index_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        scored_on TEXT NOT NULL,
                        confidence_score REAL NOT NULL,
                        confidence_grade TEXT NOT NULL,
                        missing_fields TEXT NOT NULL,
                        low_confidence_fields TEXT NOT NULL,
                        warning_codes TEXT NOT NULL,
                        warning_messages TEXT NOT NULL,
                        device_change_detected INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_rhi_data_quality_snapshot_index_id " +
                        "ON rhi_data_quality_snapshot(index_id)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_rhi_data_quality_snapshot_user_id_scored_on " +
                        "ON rhi_data_quality_snapshot(user_id, scored_on)",
                )
            }
        }

        val Migration12To13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rdi_confirmed_labs (
                        id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        marker_code TEXT NOT NULL,
                        measured_value REAL NOT NULL,
                        unit TEXT NOT NULL,
                        measured_at TEXT NOT NULL,
                        control_trend REAL NOT NULL,
                        source TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        algorithm_version TEXT NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rdi_confirmed_meals (
                        id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        meal_type TEXT NOT NULL,
                        kcal_low REAL NOT NULL,
                        kcal_high REAL NOT NULL,
                        protein_low REAL NOT NULL,
                        protein_high REAL NOT NULL,
                        fat_low REAL NOT NULL,
                        fat_high REAL NOT NULL,
                        sodium_low REAL NOT NULL,
                        sodium_high REAL NOT NULL,
                        meal_impact REAL NOT NULL,
                        reason_text TEXT NOT NULL,
                        recorded_at TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        algorithm_version TEXT NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (id)
                    )
                    """.trimIndent(),
                )
            }
        }

        val Migration11To12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE rdi_daily_snapshots ADD COLUMN is_mock INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val Migration10To11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS diet_records (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        consumed_at INTEGER NOT NULL,
                        meal_type TEXT NOT NULL,
                        description TEXT NOT NULL,
                        calories_kcal REAL NOT NULL,
                        protein_grams REAL,
                        carbohydrate_grams REAL,
                        fat_grams REAL,
                        fiber_grams REAL,
                        sodium_milligrams REAL,
                        source TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        upload_batch_id TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_diet_records_user_id_consumed_at " +
                        "ON diet_records(user_id, consumed_at)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_diet_records_upload_batch_id " +
                        "ON diet_records(upload_batch_id)",
                )
            }
        }

        val Migration9To10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN cuff_sbp_7d_mean REAL")
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN cuff_dbp_7d_mean REAL")
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN cuff_valid_days INTEGER")
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN cuff_confirmed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN fasting_glucose_mmol_l REAL")
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN total_cholesterol_mmol_l REAL")
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN ldl_mmol_l REAL")
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN hdl_mmol_l REAL")
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN triglycerides_mmol_l REAL")
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN lab_confirmed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rhi_manual_health_inputs ADD COLUMN lab_recorded_at INTEGER")
            }
        }

        val Migration8To9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rhi_manual_health_inputs (
                        user_id TEXT NOT NULL PRIMARY KEY,
                        sedentary_hours_per_day REAL,
                        waist_circumference_cm REAL,
                        vo2_max_ml_kg_min REAL,
                        hba1c_percent REAL,
                        egfr_ml_min_1_73m2 REAL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val Migration7To8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rdi_daily_snapshots (
                        id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        scored_on TEXT NOT NULL,
                        raw_score REAL NOT NULL,
                        display_score REAL NOT NULL,
                        data_confidence REAL NOT NULL,
                        status TEXT NOT NULL,
                        algorithm_version TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_rdi_daily_snapshots_user_id_scored_on " +
                        "ON rdi_daily_snapshots(user_id, scored_on)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_rdi_daily_snapshots_user_id_updated_at " +
                        "ON rdi_daily_snapshots(user_id, updated_at)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rdi_contribution_records (
                        id TEXT NOT NULL PRIMARY KEY,
                        snapshot_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        scored_on TEXT NOT NULL,
                        factor_code TEXT NOT NULL,
                        domain TEXT NOT NULL,
                        source TEXT NOT NULL,
                        current_value REAL NOT NULL,
                        baseline_value REAL,
                        unit TEXT NOT NULL,
                        raw_points REAL NOT NULL,
                        confidence REAL NOT NULL,
                        final_points REAL NOT NULL,
                        evidence_text TEXT NOT NULL,
                        algorithm_version TEXT NOT NULL,
                        source_factor_id TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_rdi_contribution_records_snapshot_id " +
                        "ON rdi_contribution_records(snapshot_id)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_rdi_contribution_records_user_id_scored_on " +
                        "ON rdi_contribution_records(user_id, scored_on)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_rdi_contribution_records_source_factor_id " +
                        "ON rdi_contribution_records(source_factor_id)",
                )
            }
        }

        val Migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS health_chat_conversations (
                        user_id TEXT NOT NULL,
                        conversation_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        is_active INTEGER NOT NULL,
                        is_deleted INTEGER NOT NULL,
                        PRIMARY KEY(user_id, conversation_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_health_chat_conversations_user_id_updated_at " +
                        "ON health_chat_conversations(user_id, updated_at)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_health_chat_conversations_user_id_is_active " +
                        "ON health_chat_conversations(user_id, is_active)",
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO health_chat_conversations (
                        user_id,
                        conversation_id,
                        title,
                        created_at,
                        updated_at,
                        is_active,
                        is_deleted
                    )
                    SELECT
                        message.user_id,
                        message.conversation_id,
                        COALESCE(
                            (
                                SELECT SUBSTR(first_user.content, 1, 48)
                                FROM health_chat_messages AS first_user
                                WHERE first_user.user_id = message.user_id
                                  AND first_user.conversation_id = message.conversation_id
                                  AND first_user.role = 'USER'
                                ORDER BY first_user.created_at ASC, first_user.message_id ASC
                                LIMIT 1
                            ),
                            '健康对话'
                        ),
                        MIN(message.created_at),
                        MAX(message.created_at),
                        CASE WHEN message.conversation_id = (
                            SELECT latest.conversation_id
                            FROM health_chat_messages AS latest
                            WHERE latest.user_id = message.user_id
                            GROUP BY latest.conversation_id
                            ORDER BY MAX(latest.created_at) DESC, latest.conversation_id DESC
                            LIMIT 1
                        ) THEN 1 ELSE 0 END,
                        0
                    FROM health_chat_messages AS message
                    GROUP BY message.user_id, message.conversation_id
                    """.trimIndent(),
                )
            }
        }

        private val Migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS health_chat_messages (
                        message_id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        conversation_id TEXT NOT NULL,
                        request_id TEXT,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        delivery_status TEXT NOT NULL,
                        provider TEXT,
                        model_version TEXT,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_health_chat_messages_user_id_created_at " +
                        "ON health_chat_messages(user_id, created_at)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_health_chat_messages_user_id_conversation_id_created_at " +
                        "ON health_chat_messages(user_id, conversation_id, created_at)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_health_chat_messages_user_id_request_id " +
                        "ON health_chat_messages(user_id, request_id)",
                )
            }
        }

        private val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ring_signal_chunks ADD COLUMN draw_frequency_hz INTEGER")
                db.execSQL("ALTER TABLE ring_signal_chunks ADD COLUMN duration_seconds INTEGER")
                db.execSQL("ALTER TABLE ring_signal_chunks ADD COLUMN lead_type TEXT")
                db.execSQL("ALTER TABLE ring_signal_chunks ADD COLUMN ecg_type INTEGER")
                db.execSQL("ALTER TABLE ring_signal_chunks ADD COLUMN calibration_type TEXT")
                db.execSQL("ALTER TABLE ring_signal_chunks ADD COLUMN average_heart_rate INTEGER")
                db.execSQL("ALTER TABLE ring_signal_chunks ADD COLUMN contact_quality TEXT")
            }
        }

        private val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val tables = buildSet {
                    db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                        val nameIndex = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            add(cursor.getString(nameIndex))
                        }
                    }
                }
                VersionThreeSchemaSql.forExistingTables(tables).forEach(db::execSQL)
                val columns = buildSet {
                    db.query("PRAGMA table_info(cvd_risk_history)").use { cursor ->
                        val nameIndex = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            add(cursor.getString(nameIndex))
                        }
                    }
                }
                RiskHistoryMigrationSql.forColumns(columns).forEach(db::execSQL)
            }
        }

        private val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // D3 sync queue for offline uploads
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_upload_queue (
                        id TEXT NOT NULL PRIMARY KEY,
                        kind TEXT NOT NULL,
                        payload_json TEXT NOT NULL,
                        status TEXT NOT NULL,
                        attempts INTEGER NOT NULL DEFAULT 0,
                        last_error TEXT,
                        created_at INTEGER NOT NULL,
                        next_retry_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sync_upload_queue_status_retry ON sync_upload_queue(status, next_retry_at)",
                )

                // D3 intervention feedback queue
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS intervention_feedback_queue (
                        id TEXT NOT NULL PRIMARY KEY,
                        intervention_id TEXT NOT NULL,
                        status TEXT NOT NULL,
                        note TEXT,
                        checked_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        upload_status TEXT NOT NULL DEFAULT 'pending',
                        upload_attempts INTEGER NOT NULL DEFAULT 0,
                        last_error TEXT,
                        next_retry_at INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_intervention_feedback_intervention_id ON intervention_feedback_queue(intervention_id)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_intervention_feedback_upload_status ON intervention_feedback_queue(upload_status, next_retry_at)",
                )
            }
        }

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ring_measurements (
                        id TEXT NOT NULL PRIMARY KEY,
                        metric_type TEXT NOT NULL,
                        measured_at INTEGER NOT NULL,
                        primary_value REAL NOT NULL,
                        secondary_value REAL,
                        unit TEXT NOT NULL,
                        quality INTEGER,
                        source TEXT NOT NULL,
                        raw_payload TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ring_sleep_sessions (
                        id TEXT NOT NULL PRIMARY KEY,
                        started_at INTEGER NOT NULL,
                        ended_at INTEGER NOT NULL,
                        deep_minutes INTEGER NOT NULL,
                        light_minutes INTEGER NOT NULL,
                        awake_minutes INTEGER NOT NULL,
                        rem_minutes INTEGER NOT NULL,
                        interruption_minutes INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        raw_payload TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ring_activities (
                        id TEXT NOT NULL PRIMARY KEY,
                        started_at INTEGER NOT NULL,
                        ended_at INTEGER,
                        activity_type TEXT NOT NULL,
                        steps INTEGER NOT NULL,
                        distance_meters REAL NOT NULL,
                        calories_kcal REAL NOT NULL,
                        duration_minutes INTEGER NOT NULL,
                        average_heart_rate REAL,
                        source TEXT NOT NULL,
                        raw_payload TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ring_signal_chunks (
                        id TEXT NOT NULL PRIMARY KEY,
                        signal_type TEXT NOT NULL,
                        started_at INTEGER NOT NULL,
                        sample_rate_hz INTEGER,
                        sample_count INTEGER NOT NULL,
                        encoding TEXT NOT NULL,
                        payload BLOB NOT NULL,
                        source TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_ring_measurements_metric_type_measured_at ON ring_measurements(metric_type, measured_at)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_ring_signal_chunks_signal_type_started_at ON ring_signal_chunks(signal_type, started_at)",
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "rehealth-local.db")
                .addMigrations(
                    Migration1To2,
                    Migration2To3,
                    Migration3To4,
                    Migration4To5,
                    Migration5To6,
                    Migration6To7,
                    Migration7To8,
                    Migration8To9,
                    Migration9To10,
                    Migration10To11,
                    Migration11To12,
                    Migration12To13,
                    Migration13To14,
                    Migration14To15,
                )
                .build()
    }
}
