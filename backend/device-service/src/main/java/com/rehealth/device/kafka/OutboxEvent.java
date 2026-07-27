package com.rehealth.device.kafka;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        String metadataJson,
        int attemptCount,
        Instant availableAt
) {
}
