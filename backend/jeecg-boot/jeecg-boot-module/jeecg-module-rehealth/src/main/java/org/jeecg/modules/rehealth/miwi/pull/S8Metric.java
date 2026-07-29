package org.jeecg.modules.rehealth.miwi.pull;

import org.jeecg.modules.rehealth.miwi.MiwiHealthDataMapper;

import java.util.List;
import java.util.Map;

/**
 * Pollable S8 (云米) metrics and their vendor OpenAPI "bytime" endpoints.
 *
 * Endpoint paths are taken verbatim from vendor OpenAPI doc V1.6.5
 * (https://openapi.miwitracker.com). Each metric is pulled independently so a
 * failing endpoint (e.g. sleep) does not block the others — see the per-metric
 * sync cursor in {@code rehealth_s8_sync_cursor}.
 *
 * Field names are tolerant aliases; the vendor bytime payload is expected to carry
 * a single metric value per record (plus a timestamp). If the real response field
 * names differ, extend the alias lists below — no other code change is required.
 */
public enum S8Metric {
    HEART_RATE(
            "HEART_RATE",
            "/api/heartrate/get_heartrate_bytime",
            600,
            new String[]{"heartRate", "hr", "value"},
            null,
            "bpm",
            25, 250
    ),
    BLOOD_PRESSURE(
            "BLOOD_PRESSURE",
            "/api/bloodpressure/get_bloodpressure_bytime",
            900,
            new String[]{"bloodPressureMax", "sbp", "systolic"},
            new String[]{"bloodPressureMin", "dbp", "diastolic"},
            "mmHg",
            60, 260
    ),
    BLOOD_OXYGEN(
            "BLOOD_OXYGEN",
            "/api/bloodoxygen/get_bloodoxygen_bytime",
            900,
            new String[]{"bloodOxygen", "spo2", "value"},
            null,
            "%",
            50, 100
    ),
    BODY_TEMPERATURE(
            "BODY_TEMPERATURE",
            "/api/temperature/get_temperature_bytime",
            1800,
            new String[]{"temperature", "bodyTemperature", "value"},
            null,
            "celsius",
            30, 45
    ),
    STEPS(
            "STEPS",
            "/api/steps/get_steps_bytime",
            900,
            new String[]{"step", "steps", "value"},
            null,
            "count",
            0, 200_000
    );

    private final String metricType;
    private final String endpoint;
    private final int defaultPollIntervalSeconds;
    private final String[] primaryFieldNames;
    private final String[] secondaryFieldNames;
    private final String unit;
    private final double min;
    private final double max;

    S8Metric(
            String metricType,
            String endpoint,
            int defaultPollIntervalSeconds,
            String[] primaryFieldNames,
            String[] secondaryFieldNames,
            String unit,
            double min,
            double max
    ) {
        this.metricType = metricType;
        this.endpoint = endpoint;
        this.defaultPollIntervalSeconds = defaultPollIntervalSeconds;
        this.primaryFieldNames = primaryFieldNames;
        this.secondaryFieldNames = secondaryFieldNames;
        this.unit = unit;
        this.min = min;
        this.max = max;
    }

    public String metricType() {
        return metricType;
    }

    public String endpoint() {
        return endpoint;
    }

    public int defaultPollIntervalSeconds() {
        return defaultPollIntervalSeconds;
    }

    /**
     * Builds a single normalized measurement map (the shape consumed by
     * {@code HardwareIngestionPort}) from one bytime response record.
     * Returns null when the record carries no plausible value for this metric.
     */
    public Map<String, Object> toMeasurement(
            Map<String, Object> item,
            MiwiHealthDataMapper mapper,
            long measuredAtUtcMillis
    ) {
        Double primary = mapper.number(item, min, max, primaryFieldNames);
        if (primary == null) {
            return null;
        }
        Double secondary = secondaryFieldNames == null ? null : mapper.number(item, min, max, secondaryFieldNames);
        return mapper.measurement(metricType, primary, secondary, unit, measuredAtUtcMillis);
    }

    public static List<S8Metric> all() {
        return List.of(values());
    }
}
