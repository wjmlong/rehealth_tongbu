package com.rehealth.genie.rdi

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RdiDao {
    @Transaction
    @Query(
        """
        SELECT * FROM rdi_daily_snapshots
        WHERE user_id = :userId
        ORDER BY scored_on DESC
        LIMIT 1
        """,
    )
    fun observeLatest(userId: String): Flow<RdiSnapshotBundle?>

    @Query(
        """
        SELECT * FROM rdi_daily_snapshots
        WHERE user_id = :userId AND scored_on < :scoredOn
        ORDER BY scored_on DESC
        LIMIT 1
        """,
    )
    suspend fun latestBefore(userId: String, scoredOn: String): RdiDailySnapshotEntity?

    @Query(
        """
        SELECT * FROM rdi_daily_snapshots
        WHERE user_id = :userId AND scored_on = :scoredOn
        LIMIT 1
        """,
    )
    suspend fun snapshotForDay(userId: String, scoredOn: String): RdiDailySnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: RdiDailySnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContributions(contributions: List<RdiContributionEntity>)

    @Query("DELETE FROM rdi_contribution_records WHERE snapshot_id = :snapshotId")
    suspend fun deleteContributions(snapshotId: String)

    @Transaction
    suspend fun replaceCalculation(
        snapshot: RdiDailySnapshotEntity,
        contributions: List<RdiContributionEntity>,
    ) {
        upsertSnapshot(snapshot)
        deleteContributions(snapshot.id)
        upsertContributions(contributions)
    }
}
