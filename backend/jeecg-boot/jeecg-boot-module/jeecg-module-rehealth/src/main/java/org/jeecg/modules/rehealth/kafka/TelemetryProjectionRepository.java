package org.jeecg.modules.rehealth.kafka;

public interface TelemetryProjectionRepository {
    void project(TelemetryEvent event);
}
