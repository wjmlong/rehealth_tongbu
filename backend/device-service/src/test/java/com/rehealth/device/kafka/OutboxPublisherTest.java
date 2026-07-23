package com.rehealth.device.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxPublisherTest {
    @Test
    void marksPublishedOnlyAfterBrokerAcknowledgement() {
        FakeStore store = new FakeStore(validEvent());
        OutboxPublisher publisher = new OutboxPublisher(
                store, (topic, key, value) -> {
                }, new OutboxEventSerializer(new ObjectMapper()), properties());

        publisher.publishAvailable();

        assertEquals(List.of("published"), store.transitions);
    }

    @Test
    void leavesRowRetryableWhenBrokerDoesNotAcknowledge() {
        FakeStore store = new FakeStore(validEvent());
        OutboxPublisher publisher = new OutboxPublisher(
                store, (topic, key, value) -> {
                    throw new BrokerPublishException("broker unavailable");
                }, new OutboxEventSerializer(new ObjectMapper()), properties());

        publisher.publishAvailable();

        assertEquals(List.of("retry:BROKER_UNAVAILABLE"), store.transitions);
    }

    @Test
    void quarantinesContractPoisonWithoutSendingIt() {
        OutboxEvent poison = new OutboxEvent(
                UUID.randomUUID(), "rehealth.telemetry.persisted.v1", 1,
                "{\"event_type\":\"rehealth.telemetry.persisted.v1\",\"token\":\"forbidden\"}",
                0, Instant.now());
        FakeStore store = new FakeStore(poison);
        OutboxPublisher publisher = new OutboxPublisher(
                store, (topic, key, value) -> {
                    throw new AssertionError("poison must not reach Kafka");
                }, new OutboxEventSerializer(new ObjectMapper()), properties());

        publisher.publishAvailable();

        assertEquals(List.of("quarantine:CONTRACT_POISON"), store.transitions);
    }

    @Test
    void rejectsMetricValuesAtTheSerializationBoundary() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), "rehealth.telemetry.persisted.v1", 1,
                validJson().replace("\"record_count\":1", "\"record_count\":1,\"heart_rate\":72"),
                0, Instant.now());

        assertThrows(OutboxContractException.class,
                () -> new OutboxEventSerializer(new ObjectMapper()).serialize(event));
    }

    private static KafkaPublisherProperties properties() {
        KafkaPublisherProperties properties = new KafkaPublisherProperties();
        properties.setEnabled(true);
        properties.setBatchSize(10);
        return properties;
    }

    private static OutboxEvent validEvent() {
        UUID eventId = UUID.randomUUID();
        return new OutboxEvent(eventId, "rehealth.telemetry.persisted.v1", 1,
                validJson().replace("event_12345678", eventId.toString()), 0, Instant.now());
    }

    private static String validJson() {
        return """
                {"event_type":"rehealth.telemetry.persisted.v1","event_id":"event_12345678",
                "batch_id":"batch_12345678","schema_id":"rehealth.telemetry.persisted.v1",
                "tenant_ref":"opaque_tenant123","user_ref":"opaque_user12345",
                "device_ref":"opaque_device123","window_started_at":"2026-07-23T00:00:00Z",
                "window_ended_at":"2026-07-23T00:01:00Z","record_count":1,
                "quality_status":"accepted","persistence_status":"persisted"}
                """;
    }

    private static final class FakeStore implements OutboxStore {
        private final OutboxEvent event;
        private final List<String> transitions = new ArrayList<>();
        private boolean claimed;

        private FakeStore(OutboxEvent event) {
            this.event = event;
        }

        @Override
        public Optional<OutboxEvent> claimNext() {
            if (claimed) {
                return Optional.empty();
            }
            claimed = true;
            return Optional.of(event);
        }

        @Override
        public void markPublished(UUID eventId) {
            transitions.add("published");
        }

        @Override
        public void markRetryable(UUID eventId, String errorCode, Instant availableAt) {
            transitions.add("retry:" + errorCode);
        }

        @Override
        public void quarantine(UUID eventId, String errorCode) {
            transitions.add("quarantine:" + errorCode);
        }
    }
}
