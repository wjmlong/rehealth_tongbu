package org.jeecg.modules.rehealth.mobile.service;

import org.jeecg.modules.rehealth.mobile.dto.RdiDailySnapshotBatchDto;
import org.jeecg.modules.rehealth.mobile.dto.RdiDailySnapshotResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class RdiDailySnapshotService {
    private static final int MAX_SNAPSHOTS = 120;
    private static final int MAX_CONTRIBUTIONS = 64;
    private static final Set<String> STATUSES = Set.of(
            "NO_DATA", "BASELINE_BUILDING", "PRELIMINARY", "CONFIRMED",
            "STALE", "INVALID", "DEBUG_MOCK");

    private final JdbcTemplate jdbc;

    public RdiDailySnapshotService(@Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public RdiDailySnapshotResponseDto persist(String authenticatedUserId, RdiDailySnapshotBatchDto batch) {
        if (batch == null || batch.snapshots == null || batch.snapshots.isEmpty()
                || batch.snapshots.size() > MAX_SNAPSHOTS) {
            throw new IllegalArgumentException("snapshots must contain between 1 and 120 items");
        }
        if (batch.userId != null && !batch.userId.isBlank() && !authenticatedUserId.equals(batch.userId)) {
            throw new SecurityException("RDI snapshot userId does not match the authenticated user");
        }
        LocalDateTime now = LocalDateTime.now();
        for (RdiDailySnapshotBatchDto.Snapshot snapshot : batch.snapshots) {
            validate(snapshot);
            jdbc.update("""
                    INSERT INTO rehealth_rdi_daily_snapshot (
                        id, user_id, scored_on, raw_score, display_score, data_confidence,
                        status, is_mock, algorithm_version, calculation_source, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        raw_score = VALUES(raw_score), display_score = VALUES(display_score),
                        data_confidence = VALUES(data_confidence), status = VALUES(status),
                        is_mock = VALUES(is_mock), algorithm_version = VALUES(algorithm_version),
                        calculation_source = VALUES(calculation_source), updated_at = VALUES(updated_at)
                    """, uuid(), authenticatedUserId, Date.valueOf(snapshot.scoredOn), snapshot.rawScore,
                    snapshot.displayScore, snapshot.dataConfidence, snapshot.status.trim(), snapshot.isMock,
                    snapshot.algorithmVersion.trim(), snapshot.calculationSource.trim(),
                    Timestamp.valueOf(now), Timestamp.valueOf(now));
            String snapshotId = jdbc.queryForObject("""
                    SELECT id FROM rehealth_rdi_daily_snapshot WHERE user_id=? AND scored_on=?
                    """, String.class, authenticatedUserId, Date.valueOf(snapshot.scoredOn));
            jdbc.update("DELETE FROM rehealth_rdi_contribution WHERE snapshot_id=?", snapshotId);
            for (RdiDailySnapshotBatchDto.Contribution contribution : snapshot.contributions) {
                jdbc.update("""
                        INSERT INTO rehealth_rdi_contribution (
                            id, snapshot_id, factor_code, domain_code, source_code, current_value,
                            baseline_value, unit, raw_points, confidence, final_points,
                            source_factor_id, algorithm_version, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, uuid(), snapshotId, contribution.factorCode.trim(), contribution.domain.trim(),
                        contribution.source.trim(), contribution.currentValue, contribution.baselineValue,
                        contribution.unit.trim(), contribution.rawPoints, contribution.confidence,
                        contribution.finalPoints, contribution.sourceFactorId.trim(),
                        snapshot.algorithmVersion.trim(), Timestamp.valueOf(now));
            }
        }
        return new RdiDailySnapshotResponseDto(true, true, "ACCEPTED_PERSISTED");
    }

    private void validate(RdiDailySnapshotBatchDto.Snapshot value) {
        if (value == null) throw new IllegalArgumentException("snapshot is required");
        try { LocalDate.parse(required(value.scoredOn, "scored_on", 10)); }
        catch (RuntimeException e) { throw new IllegalArgumentException("scored_on must be an ISO date"); }
        range(value.rawScore, 0, 100, "raw_score");
        range(value.displayScore, 0, 100, "display_score");
        range(value.dataConfidence, 0, 1, "data_confidence");
        String status = required(value.status, "status", 32);
        if (!STATUSES.contains(status)) throw new IllegalArgumentException("unsupported RDI status");
        if (value.isMock == null) throw new IllegalArgumentException("is_mock is required");
        required(value.algorithmVersion, "algorithm_version", 128);
        required(value.calculationSource, "calculation_source", 64);
        List<RdiDailySnapshotBatchDto.Contribution> contributions = value.contributions;
        if (contributions == null || contributions.size() > MAX_CONTRIBUTIONS) {
            throw new IllegalArgumentException("contributions must contain between 0 and 64 items");
        }
        for (RdiDailySnapshotBatchDto.Contribution contribution : contributions) validate(contribution);
    }

    private void validate(RdiDailySnapshotBatchDto.Contribution value) {
        if (value == null) throw new IllegalArgumentException("contribution is required");
        required(value.factorCode, "factor_code", 64);
        required(value.domain, "domain", 64);
        required(value.source, "source", 64);
        finite(value.currentValue, "current_value");
        if (value.baselineValue != null) finite(value.baselineValue, "baseline_value");
        required(value.unit, "unit", 32);
        finite(value.rawPoints, "raw_points");
        range(value.confidence, 0, 1, "confidence");
        finite(value.finalPoints, "final_points");
        required(value.sourceFactorId, "source_factor_id", 255);
    }

    private static void range(Double value, double min, double max, String field) {
        finite(value, field);
        if (value < min || value > max) throw new IllegalArgumentException(field + " is out of range");
    }

    private static void finite(Double value, String field) {
        if (value == null || !Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
    }

    private static String required(String value, String field, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new IllegalArgumentException(field + " is required and must not exceed " + max + " characters");
        }
        return value.trim();
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
