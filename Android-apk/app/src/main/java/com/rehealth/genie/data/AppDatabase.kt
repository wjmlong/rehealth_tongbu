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
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity

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
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ringDataDao(): RingDataDao
    abstract fun uploadQueueDao(): UploadQueueDao
    abstract fun interventionFeedbackDao(): InterventionFeedbackDao

    companion object {
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
                .addMigrations(Migration1To2, Migration2To3)
                .fallbackToDestructiveMigration()
                .build()
    }
}
