package com.rehealth.genie.phm

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.google.gson.Gson

@Entity(
    tableName = "pias_attribution_cache",
    indices = [Index(value = ["updated_at"], name = "index_pias_attribution_cache_updated_at")],
)
data class PiasAttributionCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id") val userId: String,
    val status: String?,
    @ColumnInfo(name = "history_days") val historyDays: Int?,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "is_mock") val isMock: Boolean,
    @ColumnInfo(name = "model_version") val modelVersion: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface PiasAttributionCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PiasAttributionCacheEntity)

    @Query("SELECT * FROM pias_attribution_cache WHERE user_id = :userId LIMIT 1")
    suspend fun get(userId: String): PiasAttributionCacheEntity?
}

class PiasAttributionCacheRepository(
    private val dao: PiasAttributionCacheDao,
    private val userIdProvider: () -> String?,
    private val gson: Gson = Gson(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    suspend fun save(
        result: IndividualAttributionResult,
        isMock: Boolean,
        modelVersion: String,
    ): Boolean {
        val userId = userIdProvider()?.takeIf(String::isNotBlank) ?: return false
        dao.upsert(
            PiasAttributionCacheEntity(
                userId = userId,
                status = result.status,
                historyDays = result.historyDays,
                payloadJson = gson.toJson(result),
                isMock = isMock,
                modelVersion = modelVersion,
                updatedAt = nowProvider(),
            ),
        )
        return true
    }

    suspend fun load(allowMock: Boolean = false): IndividualAttributionResult? {
        val userId = userIdProvider()?.takeIf(String::isNotBlank) ?: return null
        val cached = dao.get(userId) ?: return null
        if (cached.isMock && !allowMock) return null
        return runCatching {
            gson.fromJson(cached.payloadJson, IndividualAttributionResult::class.java)
        }.getOrNull()
    }
}
