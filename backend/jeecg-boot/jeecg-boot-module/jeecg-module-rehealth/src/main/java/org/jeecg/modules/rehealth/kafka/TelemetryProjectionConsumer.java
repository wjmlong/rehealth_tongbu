package org.jeecg.modules.rehealth.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rehealth.kafka.consumer.enabled", havingValue = "true")
public class TelemetryProjectionConsumer {
    private final TelemetryProjectionService service;

    public TelemetryProjectionConsumer(TelemetryProjectionService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = {
                    "rehealth.telemetry.persisted.v1",
                    "rehealth.telemetry.quality.v1"
            },
            groupId = "${rehealth.kafka.consumer.group-id:rehealth-jeecg-projection-v1}",
            concurrency = "1",
            containerFactory = "rehealthKafkaListenerContainerFactory"
    )
    public void consume(String payload) {
        service.project(payload);
    }
}
