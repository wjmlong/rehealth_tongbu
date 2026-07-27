package org.jeecg.modules.rehealth.kafka;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@ConditionalOnProperty(name = "rehealth.kafka.consumer.enabled", havingValue = "true")
public class JdbcTelemetryProjectionRepository implements TelemetryProjectionRepository {
    private final JdbcTemplate jdbc;

    public JdbcTelemetryProjectionRepository(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void project(TelemetryEvent event) {
        Timestamp now = Timestamp.from(Instant.now());
        int inserted = jdbc.update("""
                INSERT IGNORE INTO rehealth_telemetry_event_projection (
                  event_id, event_type, schema_id, batch_id, tenant_ref, user_ref,
                  device_ref, record_count, persistence_status, quality_status,
                  occurred_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, event.eventId(), event.eventType(), event.schemaId(), event.batchId(),
                event.tenantRef(), event.userRef(), event.deviceRef(), event.recordCount(),
                event.persistenceStatus(), event.qualityStatus(),
                Timestamp.from(event.windowEndedAt()), now);
        if (inserted == 1 && event.isQualityEvent()) {
            jdbc.update("""
                    INSERT IGNORE INTO rehealth_telemetry_quality_case (
                      event_id, batch_id, tenant_ref, device_ref, accepted_count,
                      rejected_count, quality_status, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, event.eventId(), event.batchId(), event.tenantRef(), event.deviceRef(),
                    event.acceptedCount(), event.rejectedCount(), event.qualityStatus(), now);
        }
    }
}
