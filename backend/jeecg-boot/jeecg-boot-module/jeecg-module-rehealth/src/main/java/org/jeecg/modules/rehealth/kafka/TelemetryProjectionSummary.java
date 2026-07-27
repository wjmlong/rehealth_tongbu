package org.jeecg.modules.rehealth.kafka;

import java.time.Instant;

public record TelemetryProjectionSummary(
        String eventId,
        String batchId,
        String tenantRef,
        String deviceRef,
        int recordCount,
        String persistenceStatus,
        String qualityStatus,
        Instant occurredAt
) {
}
