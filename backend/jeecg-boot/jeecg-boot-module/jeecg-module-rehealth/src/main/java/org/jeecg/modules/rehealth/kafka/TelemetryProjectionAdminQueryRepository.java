package org.jeecg.modules.rehealth.kafka;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "rehealth.kafka.consumer.enabled", havingValue = "true")
public class TelemetryProjectionAdminQueryRepository {
    private final JdbcTemplate jdbc;

    public TelemetryProjectionAdminQueryRepository(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
    }

    public List<TelemetryProjectionSummary> findRecent(String tenantRef, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 200));
        return jdbc.query("""
                SELECT event_id, batch_id, tenant_ref, device_ref, record_count,
                       persistence_status, quality_status, occurred_at
                FROM rehealth_telemetry_event_projection
                WHERE tenant_ref = ?
                ORDER BY occurred_at DESC, event_id DESC
                LIMIT ?
                """, (row, index) -> new TelemetryProjectionSummary(
                row.getString("event_id"), row.getString("batch_id"),
                row.getString("tenant_ref"), row.getString("device_ref"),
                row.getInt("record_count"), row.getString("persistence_status"),
                row.getString("quality_status"), row.getTimestamp("occurred_at").toInstant()),
                tenantRef, boundedLimit);
    }
}
