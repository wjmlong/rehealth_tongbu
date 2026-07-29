package org.jeecg.modules.rehealth.miwi.pull;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Schedules each S8 metric at its own cadence (see {@link S8Metric}). A failing
 * metric is retried on its next tick; the per-(device, metric) cursor in
 * {@code rehealth_s8_sync_cursor} guarantees no data is skipped and no data is
 * double-counted.
 *
 * NOTE: for multi-instance deployment add a distributed lock (e.g. DB row lock on
 * {@code rehealth_s8_sync_cursor} or a leased lock) around {@code pullMetric} so two
 * nodes do not pull the same window concurrently. Single-instance 8 月初 launch is fine.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "rehealth.miwi.pull.enabled", havingValue = "true")
public class S8PollingScheduler {

    private final S8PollingService service;

    public S8PollingScheduler(S8PollingService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "#{T(org.jeccg.modules.rehealth.miwi.pull.S8Metric).HEART_RATE.defaultPollIntervalSeconds() * 1000}")
    public void heartRate() {
        service.pullMetric(S8Metric.HEART_RATE);
    }

    @Scheduled(fixedDelayString = "#{T(org.jeccg.modules.rehealth.miwi.pull.S8Metric).BLOOD_PRESSURE.defaultPollIntervalSeconds() * 1000}")
    public void bloodPressure() {
        service.pullMetric(S8Metric.BLOOD_PRESSURE);
    }

    @Scheduled(fixedDelayString = "#{T(org.jeccg.modules.rehealth.miwi.pull.S8Metric).BLOOD_OXYGEN.defaultPollIntervalSeconds() * 1000}")
    public void bloodOxygen() {
        service.pullMetric(S8Metric.BLOOD_OXYGEN);
    }

    @Scheduled(fixedDelayString = "#{T(org.jeccg.modules.rehealth.miwi.pull.S8Metric).BODY_TEMPERATURE.defaultPollIntervalSeconds() * 1000}")
    public void bodyTemperature() {
        service.pullMetric(S8Metric.BODY_TEMPERATURE);
    }

    @Scheduled(fixedDelayString = "#{T(org.jeccg.modules.rehealth.miwi.pull.S8Metric).STEPS.defaultPollIntervalSeconds() * 1000}")
    public void steps() {
        service.pullMetric(S8Metric.STEPS);
    }
}
