package org.jeecg.modules.rehealth.kafka;

import java.time.Instant;

public record TelemetryEvent(
        String eventType,
        String eventId,
        String batchId,
        String schemaId,
        String tenantRef,
        String userRef,
        String deviceRef,
        Instant windowEndedAt,
        int recordCount,
        int acceptedCount,
        int rejectedCount,
        String qualityStatus,
        String persistenceStatus
) {
    public boolean isQualityEvent() {
        return "rehealth.telemetry.quality.v1".equals(eventType);
    }
}
