package com.rehealth.device.application;

import java.util.List;

/**
 * Aggregated, read-only health summary for one user, derived from the
 * telemetry stored in the hardware (TimescaleDB) tables. This is the
 * "software-extracted data" surfaced to administrative callers.
 * Timestamps follow the codebase convention of epoch milliseconds (Long).
 */
public record UserHealthSummary(
        String userId,
        List<String> devices,
        Long firstSeenAt,
        Long lastSeenAt,
        long measurementCount,
        long sleepSessionCount,
        long activityCount,
        List<MetricSummary> latestMetrics
) {
}
