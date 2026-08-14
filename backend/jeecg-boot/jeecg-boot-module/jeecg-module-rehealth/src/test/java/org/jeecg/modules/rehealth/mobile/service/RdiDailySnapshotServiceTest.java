package org.jeecg.modules.rehealth.mobile.service;

import org.h2.jdbcx.JdbcDataSource;
import org.jeecg.modules.rehealth.mobile.dto.RdiDailySnapshotBatchDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RdiDailySnapshotServiceTest {
    private JdbcTemplate jdbc;
    private RdiDailySnapshotService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:rdi-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE rehealth_rdi_daily_snapshot (
                  id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64) NOT NULL, scored_on DATE NOT NULL,
                  raw_score DECIMAL(8,4) NOT NULL, display_score DECIMAL(8,4) NOT NULL,
                  data_confidence DECIMAL(8,6) NOT NULL, status VARCHAR(32) NOT NULL,
                  is_mock TINYINT NOT NULL, algorithm_version VARCHAR(128) NOT NULL,
                  calculation_source VARCHAR(64) NOT NULL, created_at DATETIME(3) NOT NULL,
                  updated_at DATETIME(3) NOT NULL, UNIQUE(user_id, scored_on)
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_rdi_contribution (
                  id VARCHAR(64) PRIMARY KEY, snapshot_id VARCHAR(64) NOT NULL,
                  factor_code VARCHAR(64) NOT NULL, domain_code VARCHAR(64) NOT NULL,
                  source_code VARCHAR(64) NOT NULL, current_value DECIMAL(16,6) NOT NULL,
                  baseline_value DECIMAL(16,6), unit VARCHAR(32) NOT NULL,
                  raw_points DECIMAL(10,6) NOT NULL, confidence DECIMAL(8,6) NOT NULL,
                  final_points DECIMAL(10,6) NOT NULL, source_factor_id VARCHAR(255) NOT NULL,
                  algorithm_version VARCHAR(128) NOT NULL, created_at DATETIME(3) NOT NULL,
                  UNIQUE(snapshot_id, factor_code)
                )
                """);
        service = new RdiDailySnapshotService(jdbc);
    }

    @Test
    void authenticatedOwnerAtomicallyReplacesDailyAggregateAndContributions() {
        RdiDailySnapshotBatchDto batch = batch("user-a", 57.0, "steps");
        assertTrue(service.persist("user-a", batch).persisted());
        batch.snapshots.get(0).displayScore = 54.0;
        batch.snapshots.get(0).contributions = List.of(contribution("sleep_duration"));
        service.persist("user-a", batch);

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM rehealth_rdi_daily_snapshot", Integer.class));
        assertEquals(54.0, jdbc.queryForObject("SELECT display_score FROM rehealth_rdi_daily_snapshot", Double.class));
        assertEquals("sleep_duration", jdbc.queryForObject(
                "SELECT factor_code FROM rehealth_rdi_contribution", String.class));
    }

    @Test
    void rejectsClientSelectedOwnerAndUnsupportedStatus() {
        assertThrows(SecurityException.class, () -> service.persist("user-a", batch("user-b", 57.0, "steps")));
        RdiDailySnapshotBatchDto invalid = batch("user-a", 57.0, "steps");
        invalid.snapshots.get(0).status = "AVAILABLE";
        assertThrows(IllegalArgumentException.class, () -> service.persist("user-a", invalid));
    }

    private RdiDailySnapshotBatchDto batch(String userId, double score, String factorCode) {
        RdiDailySnapshotBatchDto.Snapshot snapshot = new RdiDailySnapshotBatchDto.Snapshot();
        snapshot.scoredOn = "2026-08-14";
        snapshot.rawScore = score;
        snapshot.displayScore = score;
        snapshot.dataConfidence = 0.8;
        snapshot.status = "CONFIRMED";
        snapshot.isMock = false;
        snapshot.algorithmVersion = "rdi-rule-1.0.1";
        snapshot.calculationSource = "android_local";
        snapshot.contributions = List.of(contribution(factorCode));
        RdiDailySnapshotBatchDto batch = new RdiDailySnapshotBatchDto();
        batch.userId = userId;
        batch.snapshots = List.of(snapshot);
        return batch;
    }

    private RdiDailySnapshotBatchDto.Contribution contribution(String factorCode) {
        RdiDailySnapshotBatchDto.Contribution value = new RdiDailySnapshotBatchDto.Contribution();
        value.factorCode = factorCode;
        value.domain = "activity";
        value.source = "ROOM_WEARABLE";
        value.currentValue = 5200.0;
        value.baselineValue = 6800.0;
        value.unit = "steps/day";
        value.rawPoints = 0.5;
        value.confidence = 0.8;
        value.finalPoints = 0.4;
        value.sourceFactorId = "wearable:" + factorCode + ":2026-08-14";
        return value;
    }
}
