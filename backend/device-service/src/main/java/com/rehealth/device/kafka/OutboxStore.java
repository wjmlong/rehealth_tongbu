package com.rehealth.device.kafka;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OutboxStore {
    Optional<OutboxEvent> claimNext();
    void markPublished(UUID eventId);
    void markRetryable(UUID eventId, String errorCode, Instant availableAt);
    void quarantine(UUID eventId, String errorCode);
}
