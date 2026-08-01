package com.rehealth.genie.rdi

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** 个人基线锚定表的访问接口（设计 6.2）。 */
@Dao
interface RdiBaselineDao {
    @Query(
        """
        SELECT * FROM rdi_baselines
        WHERE user_id = :userId AND factor_code = :factorCode AND status = 'ACTIVE'
        ORDER BY version DESC
        LIMIT 1
        """,
    )
    suspend fun activeBaseline(userId: String, factorCode: String): RdiBaselineEntity?

    @Query(
        """
        SELECT * FROM rdi_baselines
        WHERE user_id = :userId AND status = 'ACTIVE'
        """,
    )
    suspend fun activeBaselines(userId: String): List<RdiBaselineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RdiBaselineEntity)

    @Query(
        """
        UPDATE rdi_baselines
        SET status = 'SUPERSEDED'
        WHERE user_id = :userId AND factor_code = :factorCode AND status = 'ACTIVE'
        """,
    )
    suspend fun supersede(userId: String, factorCode: String)

    @Transaction
    suspend fun establish(active: RdiBaselineEntity) {
        supersede(active.userId, active.factorCode)
        upsert(active)
    }
}
