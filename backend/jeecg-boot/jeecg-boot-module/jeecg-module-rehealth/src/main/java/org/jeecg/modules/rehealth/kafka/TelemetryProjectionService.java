package org.jeecg.modules.rehealth.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "rehealth.kafka.consumer.enabled", havingValue = "true")
public class TelemetryProjectionService {
    private final TelemetryEventParser parser;
    private final TelemetryProjectionRepository repository;

    public TelemetryProjectionService(
            TelemetryEventParser parser,
            TelemetryProjectionRepository repository
    ) {
        this.parser = parser;
        this.repository = repository;
    }

    public void project(String payload) {
        repository.project(parser.parse(payload));
    }
}
