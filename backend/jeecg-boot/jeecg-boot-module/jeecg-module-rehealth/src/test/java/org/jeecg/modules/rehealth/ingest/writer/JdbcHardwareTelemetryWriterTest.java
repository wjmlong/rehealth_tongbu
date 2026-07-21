package org.jeecg.modules.rehealth.ingest.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcHardwareTelemetryWriterTest {
    private JdbcDataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private JdbcHardwareTelemetryWriter writer;

    @BeforeEach
    void setUp() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:hardware-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/hardware/mysql/V1__create_hardware_telemetry_tables.sql")
        ).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        writer = new JdbcHardwareTelemetryWriter(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)),
                new ObjectMapper()
        );
    }

    @Test
    void persistsBatchAndAllNormalizedRows() {
        HardwareWriteResult result = writer.write(fullBatch());

        assertTrue(result.persisted);
        assertEquals("ACCEPTED_PERSISTED", result.status);
        assertEquals("HARDWARE_DB_COMMITTED", result.stage);
        assertEquals(1, count("hardware_upload_batch"));
        assertEquals(1, count("hardware_measurement"));
        assertEquals(1, count("hardware_sleep_session"));
        assertEquals(1, count("hardware_activity"));
    }

    @Test
    void duplicateBatchReturnsExistingReceiptWithoutDuplicatingRows() {
        HardwareWriteResult first = writer.write(fullBatch());
        JdbcHardwareTelemetryWriter restartedWriter = new JdbcHardwareTelemetryWriter(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)),
                new ObjectMapper()
        );

        HardwareWriteResult duplicate = restartedWriter.write(fullBatch());

        assertTrue(duplicate.persisted);
        assertTrue(duplicate.duplicate);
        assertEquals("ACCEPTED_DUPLICATE", duplicate.status);
        assertEquals(first.receiptId, duplicate.receiptId);
        assertEquals(1, count("hardware_upload_batch"));
        assertEquals(1, count("hardware_measurement"));
        assertEquals(1, count("hardware_sleep_session"));
        assertEquals(1, count("hardware_activity"));
    }

    @Test
    void partialWriteFailureRollsBackBatchAndPreviouslyInsertedRows() {
        TelemetryBatchRequestDto request = fullBatch();
        request.measurements = new ArrayList<>(request.measurements);
        Map<String, Object> invalid = new LinkedHashMap<>(request.measurements.get(0));
        invalid.put("id", "measurement-invalid");
        invalid.remove("unit");
        request.measurements.add(invalid);

        assertThrows(HardwarePersistenceUnavailableException.class, () -> writer.write(request));

        assertEquals(0, count("hardware_upload_batch"));
        assertEquals(0, count("hardware_measurement"));
        assertEquals(0, count("hardware_sleep_session"));
        assertEquals(0, count("hardware_activity"));
    }

    private TelemetryBatchRequestDto fullBatch() {
        TelemetryBatchRequestDto request = new TelemetryBatchRequestDto();
        request.batchId = "batch-001";
        request.userId = "authenticated-user-001";
        request.deviceId = "ring-001";
        request.collectedFrom = 1720000000000L;
        request.collectedTo = 1720003600000L;
        request.source = "ANDROID_ROOM";
        request.quality.put("schemaVersion", "d2-v1");
        request.measurements.add(Map.of(
                "id", "measurement-001",
                "metricType", "HEART_RATE",
                "measuredAt", 1720000010000L,
                "primaryValue", 72.0,
                "unit", "bpm",
                "quality", 1,
                "source", "MRD"
        ));
        request.sleepSessions.add(Map.of(
                "id", "sleep-001",
                "startedAt", 1720000000000L,
                "endedAt", 1720003600000L,
                "deepMinutes", 20,
                "lightMinutes", 30,
                "awakeMinutes", 5,
                "remMinutes", 10,
                "interruptionMinutes", 2,
                "source", "MRD"
        ));
        request.activitySessions.add(Map.ofEntries(
                Map.entry("id", "activity-001"),
                Map.entry("startedAt", 1720001000000L),
                Map.entry("endedAt", 1720002000000L),
                Map.entry("activityType", "DAILY"),
                Map.entry("steps", 1200),
                Map.entry("distanceMeters", 850.5),
                Map.entry("caloriesKcal", 65.0),
                Map.entry("durationMinutes", 30),
                Map.entry("averageHeartRate", 88.0),
                Map.entry("source", "MRD")
        ));
        return request;
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
