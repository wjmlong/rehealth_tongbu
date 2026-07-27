package org.jeecg.modules.rehealth.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rehealth.device.kafka.BrokerSender;
import com.rehealth.device.kafka.JdbcOutboxStore;
import com.rehealth.device.kafka.KafkaPublisherConfiguration;
import com.rehealth.device.kafka.KafkaPublisherProperties;
import com.rehealth.device.kafka.OutboxEventSerializer;
import com.rehealth.device.kafka.OutboxPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaLifecycleIT {
    private static final String PERSISTED = "rehealth.telemetry.persisted.v1";
    private static final String DLQ = "rehealth.telemetry.dlq.v1";
    private static final String DEVICE_REF = "opaque_device_t9_12345678";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void productionPublisherAndConsumerCompleteTheRealLifecycle() throws Exception {
        String bootstrap = requiredProperty("kafka.bootstrap");
        Path fixture = Path.of(requiredProperty("gate.fixture"));
        mapper.readTree(Files.readString(fixture));

        JdbcTemplate hardware = jdbc(
                requiredProperty("timescale.url"),
                requiredProperty("timescale.username"),
                requiredProperty("timescale.password"));
        JdbcTemplate software = jdbc(
                requiredProperty("mysql.url"),
                requiredProperty("mysql.username"),
                requiredProperty("mysql.password"));
        prepareHardwareSchema(hardware);
        prepareSoftwareSchema(software);

        UUID eventId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        String eventPayload = persistedPayload(eventId.toString());
        seedOutbox(hardware, eventId, batchId, eventPayload);

        KafkaPublisherProperties publisherProperties = publisherProperties();
        try (KafkaTemplateHolder unavailableKafka = kafkaTemplate("kafka:65530")) {
            OutboxPublisher unavailablePublisher = publisher(
                    hardware, publisherProperties, unavailableKafka.template());
            unavailablePublisher.publishAvailable();
        }
        int outagePending = hardware.queryForObject(
                "SELECT count(*) FROM hardware_outbox WHERE status <> 'PUBLISHED'", Integer.class);
        assertEquals(1, outagePending, "broker outage must leave the Outbox retryable");
        assertEquals("FAILED", hardware.queryForObject(
                "SELECT status FROM hardware_outbox WHERE id = ?", String.class, eventId));

        try (KafkaTemplateHolder liveKafka = kafkaTemplate(bootstrap)) {
            OutboxPublisher recoveredPublisher = publisher(
                    hardware, publisherProperties, liveKafka.template());
            recoveredPublisher.publishAvailable();
            assertEquals("PUBLISHED", hardware.queryForObject(
                    "SELECT status FROM hardware_outbox WHERE id = ?", String.class, eventId));

            List<ConsumerRecord<String, String>> publisherRecords =
                    readTopic(bootstrap, PERSISTED, Duration.ofSeconds(10));
            long recoveredEvents = publisherRecords.stream()
                    .filter(record -> record.value().contains(eventId.toString()))
                    .count();
            assertEquals(1L, recoveredEvents, "publisher recovery must emit one logical event");
            String observedDeviceKey = publisherRecords.stream()
                    .filter(record -> record.value().contains(eventId.toString()))
                    .map(ConsumerRecord::key)
                    .findFirst()
                    .orElseThrow();
            assertEquals(DEVICE_REF, observedDeviceKey);

            UUID poisonedOutboxId = UUID.randomUUID();
            UUID poisonedBatchId = UUID.randomUUID();
            ObjectNode poisonedOutboxNode = (ObjectNode) mapper.readTree(
                    persistedPayload(poisonedOutboxId.toString()));
            poisonedOutboxNode.put(
                    "token", "publisher-secret-must-not-leave-timescale");
            String poisonedOutbox = poisonedOutboxNode.toString();
            seedOutbox(hardware, poisonedOutboxId, poisonedBatchId, poisonedOutbox);
            recoveredPublisher.publishAvailable();
            boolean publisherPoisonQuarantined = "DLQ_REVIEW".equals(
                    hardware.queryForObject(
                    "SELECT status FROM hardware_outbox WHERE id = ?",
                    String.class, poisonedOutboxId));
            assertTrue(publisherPoisonQuarantined);

            ConsumerOutcome consumerOutcome = runConsumerLifecycle(
                    bootstrap, software, liveKafka.template(), eventId, eventPayload);

            Map<String, Object> results = new LinkedHashMap<>();
            results.put("broker_outage_pending_rows", outagePending);
            results.put("recovery_logical_events", recoveredEvents);
            results.put("idempotent_projection_rows", software.queryForObject(
                    "SELECT count(*) FROM rehealth_telemetry_event_projection WHERE event_id = ?",
                    Integer.class, eventId.toString()));
            results.put("publisher_poison_quarantined", publisherPoisonQuarantined);
            results.put("consumer_poison_dlq", consumerOutcome.poisonDlqObserved());
            results.put("later_event_progress", consumerOutcome.laterEventProjected());
            results.put("redacted", consumerOutcome.redacted());
            results.put("per_device_key", observedDeviceKey);
            writeReport(results);
        }
    }

    private ConsumerOutcome runConsumerLifecycle(
            String bootstrap,
            JdbcTemplate software,
            KafkaTemplate<String, String> producer,
            UUID originalEventId,
            String originalPayload
    ) throws Exception {
        Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "t9-projection-" + UUID.randomUUID());
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        TelemetryProjectionRepository repository =
                new JdbcTelemetryProjectionRepository(software);
        TelemetryProjectionConsumer productionConsumer = new TelemetryProjectionConsumer(
                new TelemetryProjectionService(new TelemetryEventParser(mapper), repository));
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ReHealthKafkaConfiguration().rehealthKafkaListenerContainerFactory(
                        new DefaultKafkaConsumerFactory<>(consumerProperties),
                        producer,
                        new DlqEnvelopeFactory(mapper));
        ConcurrentMessageListenerContainer<String, String> container =
                factory.createContainer(PERSISTED);
        container.setupMessageListener((MessageListener<String, String>)
                record -> productionConsumer.consume(record.value()));
        container.start();
        try {
            await("initial production projection", Duration.ofSeconds(30), () ->
                    countProjection(software, originalEventId.toString()) == 1);
            producer.send(PERSISTED, DEVICE_REF, originalPayload).get();

            String poisonMarker = "consumer-secret-must-never-reach-dlq";
            ObjectNode poisonNode = (ObjectNode) mapper.readTree(
                    persistedPayload("poison_" + UUID.randomUUID()));
            poisonNode.put("token", poisonMarker);
            poisonNode.put("heart_rate", 72);
            String poison = poisonNode.toString();
            String laterEventId = "later_" + UUID.randomUUID();
            producer.send(PERSISTED, DEVICE_REF, poison).get();
            producer.send(PERSISTED, DEVICE_REF, persistedPayload(laterEventId)).get();

            await("idempotent duplicate and later projection", Duration.ofSeconds(30), () ->
                    countProjection(software, originalEventId.toString()) == 1
                            && countProjection(software, laterEventId) == 1);
            List<ConsumerRecord<String, String>> dlqRecords =
                    readTopic(requiredProperty("kafka.bootstrap"), DLQ, Duration.ofSeconds(30));
            ConsumerRecord<String, String> poisonDlq = dlqRecords.stream()
                    .filter(record -> DEVICE_REF.equals(record.key()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("consumer poison was not sent to DLQ"));
            JsonNode envelope = mapper.readTree(poisonDlq.value());
            assertEquals(3, envelope.path("attempt_count").asInt());
            assertEquals("retry_exhausted", envelope.path("failure_code").asText());
            boolean redacted = !poisonDlq.value().contains(poisonMarker)
                    && !poisonDlq.value().contains("heart_rate")
                    && !poisonDlq.value().contains("\"token\"");
            assertTrue(redacted);
            boolean laterEventProjected = countProjection(software, laterEventId) == 1;
            assertTrue(laterEventProjected);
            return new ConsumerOutcome(true, laterEventProjected, redacted);
        } finally {
            container.stop();
        }
    }

    private OutboxPublisher publisher(
            JdbcTemplate hardware,
            KafkaPublisherProperties properties,
            KafkaTemplate<String, String> template
    ) {
        DataSource dataSource = hardware.getDataSource();
        TransactionTemplate transactions = new TransactionTemplate(
                new DataSourceTransactionManager(dataSource));
        BrokerSender sender = new KafkaPublisherConfiguration().brokerSender(template, properties);
        return new OutboxPublisher(
                new JdbcOutboxStore(hardware, transactions),
                sender,
                new OutboxEventSerializer(mapper),
                properties);
    }

    private KafkaTemplateHolder kafkaTemplate(String bootstrap) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1_000);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1_000);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 2_000);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<String, String> factory =
                new DefaultKafkaProducerFactory<>(properties);
        return new KafkaTemplateHolder(factory, new KafkaTemplate<>(factory));
    }

    private KafkaPublisherProperties publisherProperties() {
        KafkaPublisherProperties properties = new KafkaPublisherProperties();
        properties.setBatchSize(10);
        properties.setAckTimeout(Duration.ofSeconds(3));
        properties.setInitialBackoff(Duration.ZERO);
        properties.setMaximumBackoff(Duration.ofSeconds(1));
        return properties;
    }

    private JdbcTemplate jdbc(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        return new JdbcTemplate(dataSource);
    }

    private void prepareHardwareSchema(JdbcTemplate jdbc) {
        jdbc.execute("DROP TABLE IF EXISTS hardware_outbox");
        jdbc.execute("DROP TABLE IF EXISTS hardware_reconciliation");
        jdbc.execute("DROP TABLE IF EXISTS hardware_upload_batch");
        jdbc.execute("""
                CREATE TABLE hardware_upload_batch (
                  id uuid PRIMARY KEY, status text NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE hardware_reconciliation (
                  upload_batch_id uuid PRIMARY KEY, state text NOT NULL,
                  last_error_code text, updated_at timestamptz NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE hardware_outbox (
                  id uuid PRIMARY KEY, upload_batch_id uuid NOT NULL,
                  event_type text NOT NULL, event_version integer NOT NULL,
                  event_metadata jsonb NOT NULL, status text NOT NULL,
                  attempt_count integer NOT NULL, available_at timestamptz NOT NULL,
                  published_at timestamptz, last_error_code text,
                  created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL
                )
                """);
    }

    private void prepareSoftwareSchema(JdbcTemplate jdbc) {
        jdbc.execute("DROP TABLE IF EXISTS rehealth_telemetry_quality_case");
        jdbc.execute("DROP TABLE IF EXISTS rehealth_telemetry_event_projection");
        jdbc.execute("""
                CREATE TABLE rehealth_telemetry_event_projection (
                  event_id varchar(128) PRIMARY KEY, event_type varchar(128) NOT NULL,
                  schema_id varchar(128) NOT NULL, batch_id varchar(128) NOT NULL,
                  tenant_ref varchar(160) NOT NULL, user_ref varchar(160) NOT NULL,
                  device_ref varchar(160) NOT NULL, record_count int NOT NULL,
                  persistence_status varchar(32) NOT NULL, quality_status varchar(64),
                  occurred_at datetime(3) NOT NULL, created_at datetime(3) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_telemetry_quality_case (
                  event_id varchar(128) PRIMARY KEY, batch_id varchar(128) NOT NULL,
                  tenant_ref varchar(160) NOT NULL, device_ref varchar(160) NOT NULL,
                  accepted_count int NOT NULL, rejected_count int NOT NULL,
                  quality_status varchar(64) NOT NULL, created_at datetime(3) NOT NULL
                )
                """);
    }

    private void seedOutbox(
            JdbcTemplate jdbc,
            UUID eventId,
            UUID batchId,
            String metadata
    ) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO hardware_upload_batch(id,status) VALUES (?,?)",
                batchId, "PERSISTED");
        jdbc.update("""
                INSERT INTO hardware_reconciliation(
                  upload_batch_id,state,updated_at) VALUES (?,?,?)
                """, batchId, "PERSISTED", now);
        jdbc.update("""
                INSERT INTO hardware_outbox(
                  id,upload_batch_id,event_type,event_version,event_metadata,status,
                  attempt_count,available_at,created_at,updated_at)
                VALUES (?,?,?,?,?::jsonb,'PENDING',0,?,?,?)
                """, eventId, batchId, PERSISTED, 1, metadata, now, now, now);
    }

    private String persistedPayload(String eventId) throws Exception {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event_type", PERSISTED);
        event.put("event_id", eventId);
        event.put("batch_id", "batch_t9_12345678");
        event.put("schema_id", PERSISTED);
        event.put("tenant_ref", "opaque_tenant_t9_12345678");
        event.put("user_ref", "opaque_user_t9_12345678");
        event.put("device_ref", DEVICE_REF);
        event.put("window_started_at", "2026-07-23T00:00:00Z");
        event.put("window_ended_at", "2026-07-23T00:01:00Z");
        event.put("record_count", 1);
        event.put("quality_status", "accepted");
        event.put("persistence_status", "persisted");
        return mapper.writeValueAsString(event);
    }

    private int countProjection(JdbcTemplate jdbc, String eventId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM rehealth_telemetry_event_projection WHERE event_id = ?",
                Integer.class, eventId);
    }

    private List<ConsumerRecord<String, String>> readTopic(
            String bootstrap,
            String topic,
            Duration timeout
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "t9-observer-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        List<ConsumerRecord<String, String>> result = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plus(timeout);
            int emptyPolls = 0;
            boolean receivedRecord = false;
            while (Instant.now().isBefore(deadline) && emptyPolls < 3) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(result::add);
                if (!records.isEmpty()) {
                    receivedRecord = true;
                    emptyPolls = 0;
                } else if (receivedRecord) {
                    emptyPolls++;
                }
            }
        }
        return result;
    }

    private void await(String description, Duration timeout, BooleanSupplier condition)
            throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("timed out waiting for " + description);
    }

    private void writeReport(Map<String, Object> results) throws Exception {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("passed", true);
        report.put("runtime_verified", true);
        report.put("fixture", requiredProperty("gate.fixture"));
        String selected = System.getProperty("gate.cases", "");
        report.put("selected_cases", selected.isBlank() ? List.of() : List.of(selected.split(",")));
        report.put("results", results);
        Path reportPath = Path.of(requiredProperty("gate.report"));
        Files.createDirectories(reportPath.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
    }

    private String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing system property: " + name);
        }
        return value;
    }

    private record KafkaTemplateHolder(
            DefaultKafkaProducerFactory<String, String> factory,
            KafkaTemplate<String, String> template
    ) implements AutoCloseable {
        @Override
        public void close() {
            factory.destroy();
        }
    }

    private record ConsumerOutcome(
            boolean poisonDlqObserved,
            boolean laterEventProjected,
            boolean redacted
    ) {
    }
}
