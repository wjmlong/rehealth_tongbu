package com.rehealth.genie.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RiskHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RiskHistoryEntity)

    @Query(
        "SELECT * FROM cvd_risk_history " +
            "WHERE user_id = :userId ORDER BY evaluated_on DESC LIMIT :limit",
    )
    suspend fun latestForUser(userId: String, limit: Int): List<RiskHistoryEntity>
}
