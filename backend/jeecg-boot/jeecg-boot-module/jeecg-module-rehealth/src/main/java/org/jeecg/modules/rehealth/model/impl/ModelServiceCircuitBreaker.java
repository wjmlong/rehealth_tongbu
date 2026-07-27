package org.jeecg.modules.rehealth.model.impl;

import org.jeecg.modules.rehealth.model.ModelServiceErrorCode;
import org.jeecg.modules.rehealth.model.ModelServiceException;

import java.time.Duration;
import java.time.Instant;

final class ModelServiceCircuitBreaker {
    private final int failureThreshold;
    private final Duration resetDuration;
    private int consecutiveFailures;
    private Instant openedUntil = Instant.EPOCH;

    ModelServiceCircuitBreaker(int failureThreshold, long resetSeconds) {
        if (failureThreshold < 1 || resetSeconds < 1) {
            throw new IllegalArgumentException("circuit threshold and reset seconds must be positive");
        }
        this.failureThreshold = failureThreshold;
        this.resetDuration = Duration.ofSeconds(resetSeconds);
    }

    synchronized void beforeCall(String correlationId) {
        if (Instant.now().isBefore(openedUntil)) {
            throw new ModelServiceException(
                    ModelServiceErrorCode.CIRCUIT_OPEN,
                    "model-service circuit is open",
                    0,
                    correlationId,
                    null
            );
        }
    }

    synchronized void success() {
        consecutiveFailures = 0;
        openedUntil = Instant.EPOCH;
    }

    synchronized void failure() {
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold) {
            openedUntil = Instant.now().plus(resetDuration);
        }
    }
}
