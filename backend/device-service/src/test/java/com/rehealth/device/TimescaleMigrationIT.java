package com.rehealth.device;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimescaleMigrationIT {
    private static GenericContainer<?> timescale;
    private static GenericContainer<?> postgres;

    @BeforeAll
    static void startTimescale() {
        if (caseEnabled("unsupported_extension")) {
            postgres = TimescaleTestDatabase.start(TimescaleTestDatabase.POSTGRES_IMAGE);
        } else {
            timescale = TimescaleTestDatabase.start(TimescaleTestDatabase.TIMESCALE_IMAGE);
        }
    }

    @AfterAll
    static void stopDatabases() {
        if (timescale != null) {
            timescale.stop();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Test
    void cleanAndUpgradedMigrationsValidateAndExposeExpectedPolicies() throws SQLException {
        requireCase("happy");
        TimescaleTestDatabase clean = TimescaleTestDatabase.create(timescale);

        MigrateResult first = clean.flyway().migrate();
        clean.flyway().validate();
        MigrateResult second = clean.flyway().migrate();

        assertEquals(3, first.migrationsExecuted);
        assertEquals(0, second.migrationsExecuted);
        try (Connection connection = clean.connect(); Statement statement = connection.createStatement()) {
            assertEquals(4, scalarInt(statement, """
                    SELECT count(*) FROM timescaledb_information.hypertables
                    WHERE hypertable_name IN (
                      'hardware_measurement', 'hardware_sleep_session',
                      'hardware_activity', 'hardware_data_quality_event'
                    )
                    """));
            assertEquals(4, scalarInt(statement, """
                    SELECT count(*) FROM timescaledb_information.jobs
                    WHERE proc_name = 'policy_compression'
                    """));
            assertEquals(4, scalarInt(statement, """
                    SELECT count(*) FROM timescaledb_information.jobs
                    WHERE proc_name = 'policy_retention'
                    """));
            assertEquals(1, scalarInt(statement, """
                    SELECT count(*) FROM timescaledb_information.jobs
                    WHERE proc_name = 'rehealth_apply_ordinary_retention'
                    """));
            assertTrue(scalarBoolean(statement, """
                    SELECT EXISTS (
                      SELECT 1 FROM pg_indexes
                      WHERE tablename = 'hardware_measurement'
                        AND indexdef LIKE '%tenant_id, user_id, device_id, observed_at DESC%'
                    )
                    """));
        }

        TimescaleTestDatabase upgraded = TimescaleTestDatabase.create(timescale);
        assertEquals(2, upgraded.flywayAtVersion("2").migrate().migrationsExecuted);
        assertEquals(1, upgraded.flyway().migrate().migrationsExecuted);
        upgraded.flyway().validate();
    }

    @Test
    void duplicateSourceAndBatchKeysAreRejected() throws SQLException {
        requireCase("duplicate_source");
        TimescaleTestDatabase database = TimescaleTestDatabase.create(timescale);
        database.flyway().migrate();

        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.execute(batchInsert(
                    "00000000-0000-0000-0000-000000000001", "batch-1", "receipt-1"));
            SQLException duplicateBatch = assertThrows(
                    SQLException.class,
                    () -> statement.execute(batchInsert(
                            "00000000-0000-0000-0000-000000000002", "batch-1", "receipt-2"))
            );
            assertEquals("23505", duplicateBatch.getSQLState());

            statement.execute(measurementInsert("00000000-0000-0000-0000-000000000010"));
            SQLException duplicateMeasurement = assertThrows(
                    SQLException.class,
                    () -> statement.execute(measurementInsert(
                            "00000000-0000-0000-0000-000000000011"))
            );
            assertEquals("23505", duplicateMeasurement.getSQLState());

            statement.execute(qualityInsert("00000000-0000-0000-0000-000000000020"));
            SQLException duplicateQuality = assertThrows(
                    SQLException.class,
                    () -> statement.execute(qualityInsert(
                            "00000000-0000-0000-0000-000000000021"))
            );
            assertEquals("23505", duplicateQuality.getSQLState());
        }
    }

    @Test
    void unsupportedTimescaleExtensionFailsBeforeSchemaWrites() throws SQLException {
        requireCase("unsupported_extension");
        TimescaleTestDatabase database = TimescaleTestDatabase.create(postgres);

        assertThrows(FlywayException.class, () -> database.flyway().migrate());

        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            assertFalse(scalarBoolean(statement,
                    "SELECT to_regclass('public.hardware_upload_batch') IS NOT NULL"));
        }
    }

    @Test
    void legacyMysqlDatetimeIsInterpretedAsUtc() throws SQLException {
        requireCase("timezone_roundtrip");
        TimescaleTestDatabase database = TimescaleTestDatabase.create(timescale);
        database.flyway().migrate();

        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.execute("SET TIME ZONE 'Asia/Shanghai'");
            try (ResultSet result = statement.executeQuery("""
                    SELECT rehealth_legacy_mysql_datetime_utc(
                      TIMESTAMP '2024-07-03 09:46:40.123'
                    )
                    """)) {
                assertTrue(result.next());
                assertEquals(
                        Instant.parse("2024-07-03T09:46:40.123Z"),
                        result.getTimestamp(1).toInstant()
                );
            }
        }
    }

    private static boolean caseEnabled(String name) {
        String configured = System.getProperty("cases", "").trim();
        if (configured.isEmpty()) {
            return name.equals("happy");
        }
        Set<String> cases = Arrays.stream(configured.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        return cases.contains(name);
    }

    private static void requireCase(String name) {
        if (!caseEnabled(name)) {
            throw new TestAbortedException("case not selected: " + name);
        }
    }

    private static int scalarInt(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static boolean scalarBoolean(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getBoolean(1);
        }
    }

    private static String batchInsert(String id, String batchId, String receiptId) {
        return """
                INSERT INTO hardware_upload_batch (
                  id, receipt_id, tenant_id, user_id, device_id, batch_id,
                  received_at, status, record_count
                ) VALUES (
                  '%s', '%s',
                  'tenant-1', 'user-1', 'device-1', '%s',
                  '2024-07-03T09:46:40.123Z', 'PERSISTED', 1
                )
                """.formatted(id, receiptId, batchId);
    }

    private static String measurementInsert(String id) {
        return """
                INSERT INTO hardware_measurement (
                  id, upload_batch_id, tenant_id, user_id, device_id,
                  source_record_id, metric_type, observed_at, primary_value, unit
                ) VALUES (
                  '%s',
                  '00000000-0000-0000-0000-000000000001',
                  'tenant-1', 'user-1', 'device-1', 'source-1',
                  'HEART_RATE', '2024-07-03T09:46:41.123Z', 72, 'bpm'
                )
                """.formatted(id);
    }

    private static String qualityInsert(String id) {
        return """
                INSERT INTO hardware_data_quality_event (
                  id, upload_batch_id, tenant_id, user_id, device_id,
                  source_record_id, event_type, severity, event_at
                ) VALUES (
                  '%s',
                  '00000000-0000-0000-0000-000000000001',
                  'tenant-1', 'user-1', 'device-1', 'source-quality-1',
                  'MISSING_WINDOW', 'WARN', '2024-07-03T09:46:42.123Z'
                )
                """.formatted(id);
    }
}
