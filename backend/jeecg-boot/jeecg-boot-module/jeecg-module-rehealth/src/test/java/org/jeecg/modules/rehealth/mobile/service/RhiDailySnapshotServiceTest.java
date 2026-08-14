package org.jeecg.modules.rehealth.mobile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.jeecg.modules.rehealth.mobile.dto.RhiDailySnapshotBatchDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RhiDailySnapshotServiceTest {
    private JdbcTemplate jdbc;
    private RhiDailySnapshotService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:rhi-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE rehealth_rhi_daily_snapshot (
                  id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64) NOT NULL, scored_on DATE NOT NULL,
                  raw_score DECIMAL(8,4) NOT NULL, display_score DECIMAL(8,4) NOT NULL,
                  data_confidence DECIMAL(8,6) NOT NULL, status VARCHAR(32) NOT NULL,
                  product_tier VARCHAR(32) NOT NULL, available_days INT NOT NULL,
                  available_feature_count INT NOT NULL, smoothing_alpha DECIMAL(8,6) NOT NULL,
                  algorithm_version VARCHAR(128) NOT NULL, calculation_source VARCHAR(64) NOT NULL,
                  domains_json LONGTEXT NOT NULL, features_json LONGTEXT NOT NULL, quality_json LONGTEXT,
                  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
                  UNIQUE(user_id, scored_on)
                )
                """);
        service = new RhiDailySnapshotService(jdbc, new ObjectMapper());
    }

    @Test
    void authenticatedOwnerUpsertsDailyAggregate() {
        RhiDailySnapshotBatchDto batch = batch("user-a", 81.0);
        assertTrue(service.persist("user-a", batch).persisted());
        batch.snapshots.get(0).displayScore = 83.0;
        service.persist("user-a", batch);

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM rehealth_rhi_daily_snapshot", Integer.class));
        assertEquals(83.0, jdbc.queryForObject("SELECT display_score FROM rehealth_rhi_daily_snapshot", Double.class));
    }

    @Test
    void rejectsClientSelectedOwner() {
        assertThrows(SecurityException.class, () -> service.persist("user-a", batch("user-b", 80.0)));
    }

    private RhiDailySnapshotBatchDto batch(String userId, double score) {
        RhiDailySnapshotBatchDto.Snapshot snapshot = new RhiDailySnapshotBatchDto.Snapshot();
        snapshot.scoredOn = "2026-08-14";
        snapshot.rawScore = score;
        snapshot.displayScore = score;
        snapshot.dataConfidence = 0.8;
        snapshot.status = "available";
        snapshot.productTier = "LITE";
        snapshot.availableDays = 7;
        snapshot.availableFeatureCount = 10;
        snapshot.smoothingAlpha = 0.3;
        snapshot.algorithmVersion = "rhi-2.2.0";
        snapshot.calculationSource = "android_local";
        RhiDailySnapshotBatchDto batch = new RhiDailySnapshotBatchDto();
        batch.userId = userId;
        batch.snapshots = List.of(snapshot);
        return batch;
    }
}
