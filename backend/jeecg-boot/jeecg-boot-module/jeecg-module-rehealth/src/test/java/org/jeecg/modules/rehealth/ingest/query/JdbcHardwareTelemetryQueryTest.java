package org.jeecg.modules.rehealth.ingest.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.jeecg.modules.rehealth.ingest.writer.JdbcHardwareTelemetryWriter;
import org.jeecg.modules.rehealth.mobile.dto.RecentTelemetryResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcHardwareTelemetryQueryTest {
    private JdbcHardwareTelemetryWriter writer;
    private JdbcHardwareTelemetryQuery query;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:hardware-query-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/hardware/mysql/V1__create_hardware_telemetry_tables.sql")
        ).execute(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        writer = new JdbcHardwareTelemetryWriter(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)),
                new ObjectMapper()
        );
        query = new JdbcHardwareTelemetryQuery(jdbcTemplate);
    }

    @Test
    void returnsOnlyAuthenticatedUsersRecentNormalizedData() {
        writer.write(batch("user-a", "batch-a", 1_720_000_001_000L, 72));
        writer.write(batch("user-b", "batch-b", 1_720_000_002_000L, 99));
        writer.write(batch("user-a", "batch-c", 1_720_000_003_000L, 75));

        RecentTelemetryResponseDto response = query.recentForUser("user-a", 1);

        assertEquals(1, response.measurements.size());
        assertEquals(75.0, response.measurements.get(0).primaryValue);
        assertEquals("user-a", response.userId);
        assertTrue(response.sleepSessions.isEmpty());
        assertTrue(response.activities.isEmpty());
    }

    private TelemetryBatchRequestDto batch(String userId, String batchId, long measuredAt, int value) {
        TelemetryBatchRequestDto request = new TelemetryBatchRequestDto();
        request.userId = userId;
        request.batchId = batchId;
        request.deviceId = "ring-001";
        request.source = "ANDROID_ROOM";
        request.measurements.add(Map.of(
                "id", batchId + "-measurement",
                "metricType", "HEART_RATE",
                "measuredAt", measuredAt,
                "primaryValue", value,
                "unit", "bpm",
                "source", "MRD"
        ));
        return request;
    }
}
