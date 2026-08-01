package org.jeecg.modules.rehealth.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.BehaviorRecordDto;
import org.jeecg.modules.rehealth.repository.BehaviorRecordRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class JdbcBehaviorRecordRepository implements BehaviorRecordRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcBehaviorRecordRepository(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<BehaviorRecordDto> findByRequestId(String tenantId, String userId, String requestId) {
        requireIdentity(tenantId, userId);
        List<BehaviorRecordDto> records = jdbcTemplate.query("""
                SELECT * FROM rehealth_behavior_record
                WHERE tenant_id = ? AND user_id = ? AND request_id = ?
                LIMIT 1
                """, this::map, tenantId, userId, requestId);
        return records.stream().findFirst();
    }

    @Override
    @Transactional
    public BehaviorRecordDto save(String tenantId, String userId, BehaviorRecordDto record) {
        requireIdentity(tenantId, userId);
        jdbcTemplate.update("""
                INSERT INTO rehealth_behavior_record (
                    id, tenant_id, user_id, request_id, category, title, summary, items_json,
                    calories_kcal, protein_grams, carbohydrate_grams, fat_grams, ocr_text,
                    confidence, model_version, occurred_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                record.id, tenantId, userId, record.requestId, record.category, record.title,
                record.summary, writeItems(record.items), record.caloriesKcal, record.proteinGrams,
                record.carbohydrateGrams, record.fatGrams, record.ocrText, record.confidence,
                record.modelVersion, Timestamp.from(Instant.ofEpochMilli(record.occurredAt)),
                Timestamp.from(Instant.ofEpochMilli(record.createdAt))
        );
        return record;
    }

    @Override
    public List<BehaviorRecordDto> findInWindow(
            String tenantId,
            String userId,
            Instant startInclusive,
            Instant endExclusive
    ) {
        requireIdentity(tenantId, userId);
        return jdbcTemplate.query("""
                SELECT * FROM rehealth_behavior_record
                WHERE tenant_id = ? AND user_id = ? AND occurred_at >= ? AND occurred_at < ?
                ORDER BY occurred_at DESC, created_at DESC
                LIMIT 100
                """, this::map, tenantId, userId, Timestamp.from(startInclusive), Timestamp.from(endExclusive));
    }

    private BehaviorRecordDto map(ResultSet rs, int rowNum) throws SQLException {
        BehaviorRecordDto record = new BehaviorRecordDto();
        record.id = rs.getString("id");
        record.requestId = rs.getString("request_id");
        record.category = rs.getString("category");
        record.title = rs.getString("title");
        record.summary = rs.getString("summary");
        record.items = readItems(rs.getString("items_json"));
        record.caloriesKcal = nullableDouble(rs, "calories_kcal");
        record.proteinGrams = nullableDouble(rs, "protein_grams");
        record.carbohydrateGrams = nullableDouble(rs, "carbohydrate_grams");
        record.fatGrams = nullableDouble(rs, "fat_grams");
        record.ocrText = rs.getString("ocr_text");
        record.confidence = nullableDouble(rs, "confidence");
        record.modelVersion = rs.getString("model_version");
        record.occurredAt = rs.getTimestamp("occurred_at").toInstant().toEpochMilli();
        record.createdAt = rs.getTimestamp("created_at").toInstant().toEpochMilli();
        return record;
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private String writeItems(List<String> items) {
        try {
            return objectMapper.writeValueAsString(items == null ? List.of() : items);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("behavior items are not serializable", failure);
        }
    }

    private List<String> readItems(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException failure) {
            return List.of();
        }
    }

    private void requireIdentity(String tenantId, String userId) {
        if (tenantId == null || tenantId.isBlank() || userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("tenant and user are required");
        }
    }
}
