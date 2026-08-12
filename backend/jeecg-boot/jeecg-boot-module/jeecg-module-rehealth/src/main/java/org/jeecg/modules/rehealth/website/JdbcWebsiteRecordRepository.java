package org.jeecg.modules.rehealth.website;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.system.vo.LoginUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class JdbcWebsiteRecordRepository implements WebsiteRecordRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public JdbcWebsiteRecordRepository(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            ObjectMapper mapper
    ) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public WebsiteRecord save(LoginUser user, String tenantId, String resource, JsonNode payload) {
        String id = text(payload, "id");
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        String status = text(payload, "status");
        if (status == null || status.isBlank()) status = "active";
        Timestamp now = Timestamp.from(Instant.now());
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid website record payload", e);
        }
        jdbc.update("""
                INSERT INTO rehealth_website_record
                    (id, tenant_id, resource_type, status, payload_json, created_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE payload_json = VALUES(payload_json), status = VALUES(status), updated_at = VALUES(updated_at)
                """, id, tenantId, resource, status, json, user.getId(), now, now);
        return find(tenantId, resource, id).orElseThrow();
    }

    @Override
    public List<WebsiteRecord> list(String tenantId, String resource, int pageNo, int pageSize, String keyword, String status) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        return jdbc.query("""
                SELECT id, tenant_id, resource_type, status, payload_json, created_by, created_at, updated_at
                FROM rehealth_website_record
                WHERE tenant_id = ? AND resource_type = ?
                  AND (? IS NULL OR status = ?)
                  AND (? IS NULL OR payload_json LIKE ?)
                ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?
                """, (rs, row) -> map(rs.getString("id"), rs.getString("tenant_id"), rs.getString("resource_type"),
                        rs.getString("status"), rs.getString("payload_json"), rs.getString("created_by"),
                        rs.getTimestamp("created_at"), rs.getTimestamp("updated_at")),
                tenantId, resource, status, status, like, like, pageSize, (pageNo - 1) * pageSize);
    }

    @Override
    public long count(String tenantId, String resource, String keyword, String status) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        Long value = jdbc.queryForObject("""
                SELECT COUNT(*) FROM rehealth_website_record
                WHERE tenant_id = ? AND resource_type = ?
                  AND (? IS NULL OR status = ?) AND (? IS NULL OR payload_json LIKE ?)
                """, Long.class, tenantId, resource, status, status, like, like);
        return value == null ? 0 : value;
    }

    @Override
    public Optional<WebsiteRecord> find(String tenantId, String resource, String id) {
        return jdbc.query("""
                SELECT id, tenant_id, resource_type, status, payload_json, created_by, created_at, updated_at
                FROM rehealth_website_record WHERE tenant_id = ? AND resource_type = ? AND id = ?
                """, (rs, row) -> map(rs.getString("id"), rs.getString("tenant_id"), rs.getString("resource_type"),
                        rs.getString("status"), rs.getString("payload_json"), rs.getString("created_by"),
                        rs.getTimestamp("created_at"), rs.getTimestamp("updated_at")), tenantId, resource, id).stream().findFirst();
    }

    @Override
    public boolean delete(String tenantId, String resource, String id) {
        return jdbc.update("DELETE FROM rehealth_website_record WHERE tenant_id = ? AND resource_type = ? AND id = ?", tenantId, resource, id) > 0;
    }

    private WebsiteRecord map(String id, String tenantId, String resource, String status, String payload, String createdBy, Timestamp createdAt, Timestamp updatedAt) {
        try {
            return new WebsiteRecord(id, tenantId, resource, status, mapper.readTree(payload), createdBy, createdAt, updatedAt);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("invalid persisted website record", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
