package com.rehealth.device.application;

/**
 * A single latest metric value extracted by the device pipeline for a user.
 * Timestamps follow the codebase convention of epoch milliseconds (Long).
 */
public record MetricSummary(String metricType, Double value, String unit, Long measuredAt) {
}
