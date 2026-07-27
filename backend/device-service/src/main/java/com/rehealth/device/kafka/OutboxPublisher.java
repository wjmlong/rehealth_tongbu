package com.rehealth.device.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "rehealth.kafka.publisher.enabled", havingValue = "true")
public final class OutboxPublisher {
    private static final String PERSISTED = "rehealth.telemetry.persisted.v1";
    private final OutboxStore store;
    private final BrokerSender sender;
    private final OutboxEventSerializer serializer;
    private final KafkaPublisherProperties properties;

    public OutboxPublisher(
            OutboxStore store,
            BrokerSender sender,
            OutboxEventSerializer serializer,
            KafkaPublisherProperties properties
    ) {
        this.store = store;
        this.sender = sender;
        this.serializer = serializer;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${rehealth.kafka.publisher.poll-interval:500ms}")
    public void publishAvailable() {
        for (int index = 0; index < properties.getBatchSize(); index++) {
            Optional<OutboxEvent> event = store.claimNext();
            if (event.isEmpty()) {
                return;
            }
            publish(event.get());
        }
    }

    private void publish(OutboxEvent event) {
        try {
            SerializedOutboxEvent serialized = serializer.serialize(event);
            String topic = PERSISTED.equals(event.eventType())
                    ? properties.getPersistedTopic()
                    : properties.getQualityTopic();
            sender.send(topic, serialized.deviceKey(), serialized.json());
            store.markPublished(event.eventId());
        } catch (OutboxContractException exception) {
            store.quarantine(event.eventId(), "CONTRACT_POISON");
        } catch (BrokerPublishException exception) {
            store.markRetryable(event.eventId(), "BROKER_UNAVAILABLE", retryAt(event.attemptCount()));
        }
    }

    private Instant retryAt(int attemptCount) {
        int exponent = Math.min(Math.max(attemptCount, 0), 20);
        Duration delay;
        try {
            delay = properties.getInitialBackoff().multipliedBy(1L << exponent);
        } catch (ArithmeticException overflow) {
            delay = properties.getMaximumBackoff();
        }
        if (delay.compareTo(properties.getMaximumBackoff()) > 0) {
            delay = properties.getMaximumBackoff();
        }
        return Instant.now().plus(delay);
    }
}
