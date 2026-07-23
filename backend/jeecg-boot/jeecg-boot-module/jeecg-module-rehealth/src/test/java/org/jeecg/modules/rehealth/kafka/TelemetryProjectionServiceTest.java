package org.jeecg.modules.rehealth.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TelemetryProjectionServiceTest {
    private JdbcTemplate jdbc;
    private TelemetryProjectionService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:kafka-projection;MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE rehealth_telemetry_event_projection (
                  event_id VARCHAR(128) PRIMARY KEY, event_type VARCHAR(128) NOT NULL,
                  schema_id VARCHAR(128) NOT NULL, batch_id VARCHAR(128) NOT NULL,
                  tenant_ref VARCHAR(160) NOT NULL, user_ref VARCHAR(160) NOT NULL,
                  device_ref VARCHAR(160) NOT NULL, record_count INT NOT NULL,
                  persistence_status VARCHAR(32) NOT NULL, quality_status VARCHAR(64),
                  occurred_at TIMESTAMP(3) NOT NULL, created_at TIMESTAMP(3) NOT NULL)
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_telemetry_quality_case (
                  event_id VARCHAR(128) PRIMARY KEY, batch_id VARCHAR(128) NOT NULL,
                  tenant_ref VARCHAR(160) NOT NULL, device_ref VARCHAR(160) NOT NULL,
                  accepted_count INT NOT NULL, rejected_count INT NOT NULL,
                  quality_status VARCHAR(64) NOT NULL, created_at TIMESTAMP(3) NOT NULL)
                """);
        service = new TelemetryProjectionService(
                new TelemetryEventParser(new ObjectMapper()),
                new JdbcTelemetryProjectionRepository(jdbc));
    }

    @Test
    void duplicateDeliveryCreatesOneProjection() {
        service.project(persisted());
        service.project(persisted());

        assertEquals(1, count("rehealth_telemetry_event_projection"));
    }

    @Test
    void qualityEventCreatesMinimalQualityCase() {
        service.project(quality());

        assertEquals(1, count("rehealth_telemetry_quality_case"));
    }

    @Test
    void poisonIsRejectedBeforeDatabaseMutation() {
        String poison = persisted().replace("}", ",\"phone\":\"13000000000\"}");

        assertThrows(TelemetryEventContractException.class, () -> service.project(poison));
        assertEquals(0, count("rehealth_telemetry_event_projection"));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private String persisted() {
        return """
                {"event_type":"rehealth.telemetry.persisted.v1","event_id":"event_12345678",
                "batch_id":"batch_12345678","schema_id":"rehealth.telemetry.persisted.v1",
                "tenant_ref":"opaque_tenant123","user_ref":"opaque_user12345",
                "device_ref":"opaque_device123","window_started_at":"2026-07-23T00:00:00Z",
                "window_ended_at":"2026-07-23T00:01:00Z","record_count":1,
                "quality_status":"accepted","persistence_status":"persisted"}
                """;
    }

    private String quality() {
        return persisted()
                .replace("persisted.v1", "quality.v1")
                .replace("\"record_count\":1", "\"record_count\":1,\"accepted_count\":1,\"rejected_count\":0")
                .replace("\"event_id\":\"event_12345678\"", "\"event_id\":\"quality_12345678\"");
    }
}
