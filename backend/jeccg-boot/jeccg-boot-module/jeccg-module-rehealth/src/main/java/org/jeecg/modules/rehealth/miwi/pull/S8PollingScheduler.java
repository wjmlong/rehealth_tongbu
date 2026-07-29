package org.jeccg.modules.rehealth.miwi.pull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Schedules each S8 metric at its own cadence. A failing metric is retried on its
 * next tick; the per-(device, metric) cursor in {@code rehealth_s8_sync_cursor}
 * guarantees no data is skipped and no data is double-counted.
 *
 * Intervals default to {@link S8Metric} values but are overridable per metric.
 *
 * NOTE: for multi-instance deployment add a distributed lock (DB row lock on
 * {@code rehealth_s8_sync_cursor} or a leased lock) around {@code pullMetric} so two
 * nodes do not pull the same window concurrently. Single-instance 8 月初 launch is fine.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "rehealth.miwi.pull.enabled", havingValue = "true")
public class S8PollingScheduler {

    private final S8PollingService service;

    @Value("${rehealth.miwi.pull.heart-rate-interval-ms:600000}")
    private long heartRateIntervalMs;
    @Value("${rehealth.miwi.pull.blood-pressure-interval-ms:900000}")
    private long bloodPressureIntervalMs;
    @Value("${rehealth.miwi.pull.blood-oxygen-interval-ms:900000}")
    private long bloodOxygenIntervalMs;
    @Value("${rehealth.miwi.pull.body-temperature-interval-ms:1800000}")
    private long bodyTemperatureIntervalMs;
    @Value("${rehealth.miwi.pull.steps-interval-ms:900000}")
    private long stepsIntervalMs;

    public S8PollingScheduler(S8PollingService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "#{@s8PollingScheduler.heartRateIntervalMs}")
    public void heartRate() {
        service.pullMetric(S8Metric.HEART_RATE);
    }

    @Scheduled(fixedDelayString = "#{@s8PollingScheduler.bloodPressureIntervalMs}")
    public void bloodPressure() {
        service.pullMetric(S8Metric.BLOOD_PRESSURE);
    }

    @Scheduled(fixedDelayString = "#{@s8PollingScheduler.bloodOxygenIntervalMs}")
    public void bloodOxygen() {
        service.pullMetric(S8Metric.BLOOD_OXYGEN);
    }

    @Scheduled(fixedDelayString = "#{@s8PollingScheduler.bodyTemperatureIntervalMs}")
    public void bodyTemperature() {
        service.pullMetric(S8Metric.BODY_TEMPERATURE);
    }

    @Scheduled(fixedDelayString = "#{@s8PollingScheduler.stepsIntervalMs}")
    public void steps() {
        service.pullMetric(S8Metric.STEPS);
    }
}
