package com.rehealth.genie.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RiskCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RiskCacheEntity)

    @Query("SELECT * FROM cvd_risk_cache WHERE user_id = :userId")
    suspend fun get(userId: String): RiskCacheEntity?

    @Query("DELETE FROM cvd_risk_cache WHERE user_id = :userId")
    suspend fun clear(userId: String)
}
