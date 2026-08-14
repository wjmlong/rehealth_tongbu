package org.jeecg.modules.rehealth.mobile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.RhiDailySnapshotBatchDto;
import org.jeecg.modules.rehealth.mobile.dto.RhiDailySnapshotResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class RhiDailySnapshotService {
    private static final int MAX_SNAPSHOTS = 120;
    private static final int MAX_JSON_LENGTH = 2_000_000;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public RhiDailySnapshotService(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            ObjectMapper objectMapper
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RhiDailySnapshotResponseDto persist(String authenticatedUserId, RhiDailySnapshotBatchDto batch) {
        if (batch == null || batch.snapshots == null || batch.snapshots.isEmpty()
                || batch.snapshots.size() > MAX_SNAPSHOTS) {
            throw new IllegalArgumentException("snapshots must contain between 1 and 120 items");
        }
        if (batch.userId != null && !batch.userId.isBlank() && !authenticatedUserId.equals(batch.userId)) {
            throw new SecurityException("RHI snapshot userId does not match the authenticated user");
        }
        LocalDateTime now = LocalDateTime.now();
        for (RhiDailySnapshotBatchDto.Snapshot snapshot : batch.snapshots) {
            validate(snapshot);
            String domains = json(snapshot.domains, "[]");
            String features = json(snapshot.features, "[]");
            String quality = snapshot.quality == null ? null : json(snapshot.quality, null);
            jdbc.update("""
                    INSERT INTO rehealth_rhi_daily_snapshot (
                        id, user_id, scored_on, raw_score, display_score, data_confidence,
                        status, product_tier, available_days, available_feature_count,
                        smoothing_alpha, algorithm_version, calculation_source,
                        domains_json, features_json, quality_json, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        raw_score = VALUES(raw_score), display_score = VALUES(display_score),
                        data_confidence = VALUES(data_confidence), status = VALUES(status),
                        product_tier = VALUES(product_tier), available_days = VALUES(available_days),
                        available_feature_count = VALUES(available_feature_count),
                        smoothing_alpha = VALUES(smoothing_alpha), algorithm_version = VALUES(algorithm_version),
                        calculation_source = VALUES(calculation_source), domains_json = VALUES(domains_json),
                        features_json = VALUES(features_json), quality_json = VALUES(quality_json),
                        updated_at = VALUES(updated_at)
                    """, uuid(), authenticatedUserId, Date.valueOf(snapshot.scoredOn),
                    snapshot.rawScore, snapshot.displayScore, snapshot.dataConfidence,
                    snapshot.status.trim(), snapshot.productTier.trim(), snapshot.availableDays,
                    snapshot.availableFeatureCount, snapshot.smoothingAlpha,
                    snapshot.algorithmVersion.trim(), snapshot.calculationSource.trim(),
                    domains, features, quality, Timestamp.valueOf(now), Timestamp.valueOf(now));
        }
        return new RhiDailySnapshotResponseDto(true, true, "ACCEPTED_PERSISTED");
    }

    private void validate(RhiDailySnapshotBatchDto.Snapshot value) {
        if (value == null) throw new IllegalArgumentException("snapshot is required");
        try { LocalDate.parse(required(value.scoredOn, "scored_on", 10)); }
        catch (RuntimeException e) { throw new IllegalArgumentException("scored_on must be an ISO date"); }
        range(value.rawScore, 0, 100, "raw_score");
        range(value.displayScore, 0, 100, "display_score");
        range(value.dataConfidence, 0, 1, "data_confidence");
        range(value.smoothingAlpha, 0, 1, "smoothing_alpha");
        required(value.status, "status", 32);
        required(value.productTier, "product_tier", 32);
        required(value.algorithmVersion, "algorithm_version", 128);
        required(value.calculationSource, "calculation_source", 64);
        if (value.availableDays == null || value.availableDays < 0
                || value.availableFeatureCount == null || value.availableFeatureCount < 0) {
            throw new IllegalArgumentException("availability counts must be non-negative");
        }
    }

    private String json(JsonNode value, String fallback) {
        if (value == null) return fallback;
        try {
            String result = objectMapper.writeValueAsString(value);
            if (result.length() > MAX_JSON_LENGTH) throw new IllegalArgumentException("RHI snapshot JSON is too large");
            return result;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("RHI snapshot JSON is invalid", e);
        }
    }

    private static void range(Double value, double min, double max, String field) {
        if (value == null || !Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(field + " is out of range");
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
