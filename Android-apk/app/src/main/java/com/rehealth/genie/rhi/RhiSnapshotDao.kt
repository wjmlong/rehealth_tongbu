package com.rehealth.genie.rhi

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * Persistence for the split RHI daily tables.
 *
 * A day is written as one atomic unit: replacing the index row and rebuilding
 * its domain, feature, and quality children. This keeps a recomputation from
 * leaving behind children that belong to a superseded calculation.
 */
@Dao
interface RhiSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIndex(index: RhiDailyIndexEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDomains(domains: List<RhiDailyDomainScoreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeatures(features: List<RhiDailyFeatureSnapshotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuality(quality: RhiDataQualitySnapshotEntity)

    @Query("DELETE FROM rhi_daily_domain_score WHERE user_id = :userId AND scored_on = :scoredOn")
    suspend fun deleteDomains(userId: String, scoredOn: String)

    @Query("DELETE FROM rhi_daily_feature_snapshot WHERE user_id = :userId AND scored_on = :scoredOn")
    suspend fun deleteFeatures(userId: String, scoredOn: String)

    @Query("DELETE FROM rhi_data_quality_snapshot WHERE user_id = :userId AND scored_on = :scoredOn")
    suspend fun deleteQuality(userId: String, scoredOn: String)

    /** Replaces one scored day and all of its explanatory children atomically. */
    @Transaction
    suspend fun replaceDay(
        index: RhiDailyIndexEntity,
        domains: List<RhiDailyDomainScoreEntity>,
        features: List<RhiDailyFeatureSnapshotEntity>,
        quality: RhiDataQualitySnapshotEntity,
    ) {
        deleteDomains(index.userId, index.scoredOn)
        deleteFeatures(index.userId, index.scoredOn)
        deleteQuality(index.userId, index.scoredOn)
        upsertIndex(index)
        upsertDomains(domains)
        upsertFeatures(features)
        upsertQuality(quality)
    }

    @Query(
        "SELECT * FROM rhi_daily_health_index " +
            "WHERE user_id = :userId AND scored_on = :scoredOn LIMIT 1",
    )
    suspend fun getIndex(userId: String, scoredOn: String): RhiDailyIndexEntity?

    @Query(
        "SELECT * FROM rhi_daily_health_index " +
            "WHERE user_id = :userId AND scored_on BETWEEN :fromDate AND :toDate " +
            "ORDER BY scored_on ASC",
    )
    suspend fun getIndexRange(
        userId: String,
        fromDate: String,
        toDate: String,
    ): List<RhiDailyIndexEntity>

    @Transaction
    @Query(
        "SELECT * FROM rhi_daily_health_index " +
            "WHERE user_id = :userId AND scored_on = :scoredOn LIMIT 1",
    )
    suspend fun getBundle(userId: String, scoredOn: String): RhiDailyBundle?

    @Query(
        "SELECT * FROM rhi_daily_domain_score " +
            "WHERE user_id = :userId AND scored_on = :scoredOn",
    )
    suspend fun getDomains(userId: String, scoredOn: String): List<RhiDailyDomainScoreEntity>

    @Query(
        "SELECT * FROM rhi_data_quality_snapshot " +
            "WHERE user_id = :userId AND scored_on = :scoredOn LIMIT 1",
    )
    suspend fun getQuality(userId: String, scoredOn: String): RhiDataQualitySnapshotEntity?

    /** Retention pruning; RHI history older than the cutoff is not needed on device. */
    @Query("DELETE FROM rhi_daily_health_index WHERE user_id = :userId AND scored_on < :cutoff")
    suspend fun pruneIndexBefore(userId: String, cutoff: String)

    @Query("DELETE FROM rhi_daily_domain_score WHERE user_id = :userId AND scored_on < :cutoff")
    suspend fun pruneDomainsBefore(userId: String, cutoff: String)

    @Query("DELETE FROM rhi_daily_feature_snapshot WHERE user_id = :userId AND scored_on < :cutoff")
    suspend fun pruneFeaturesBefore(userId: String, cutoff: String)

    @Query("DELETE FROM rhi_data_quality_snapshot WHERE user_id = :userId AND scored_on < :cutoff")
    suspend fun pruneQualityBefore(userId: String, cutoff: String)

    @Transaction
    suspend fun pruneBefore(userId: String, cutoff: String) {
        pruneDomainsBefore(userId, cutoff)
        pruneFeaturesBefore(userId, cutoff)
        pruneQualityBefore(userId, cutoff)
        pruneIndexBefore(userId, cutoff)
    }
}
