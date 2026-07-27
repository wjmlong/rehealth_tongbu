package com.rehealth.device;

import com.rehealth.device.config.TimescaleDatabaseProperties;
import com.rehealth.device.config.TimescaleMigrationConfiguration;
import org.flywaydb.core.Flyway;
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
    private static String timescaleAdminUrl;
    private static String postgresAdminUrl;

    @BeforeAll
    static void startTimescale() {
        if (caseEnabled("unsupported_extension")) {
            postgresAdminUrl = System.getenv("POSTGRES_TEST_JDBC_URL");
            if (postgresAdminUrl == null || postgresAdminUrl.isBlank()) {
                postgres = TimescaleTestDatabase.start(TimescaleTestDatabase.POSTGRES_IMAGE);
            }
        }
        if (caseEnabled("happy")
                || caseEnabled("duplicate_source")
                || caseEnabled("timezone_roundtrip")) {
            timescaleAdminUrl = System.getenv("TIMESCALE_TEST_JDBC_URL");
            if (timescaleAdminUrl == null || timescaleAdminUrl.isBlank()) {
                timescale = TimescaleTestDatabase.start(TimescaleTestDatabase.TIMESCALE_IMAGE);
            }
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
    void cleanAndUpgradedMigrationsValidateAndExposeExpectedPolicies() throws Exception {
        requireCase("happy");
        TimescaleTestDatabase clean = createTimescaleDatabase();

        MigrateResult first = configuredFlyway(clean).migrate();
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

        TimescaleTestDatabase upgraded = createTimescaleDatabase();
        assertEquals(2, upgraded.flywayAtVersion("2").migrate().migrationsExecuted);
        assertEquals(1, upgraded.flyway().migrate().migrationsExecuted);
        upgraded.flyway().validate();
    }

    @Test
    void duplicateSourceAndBatchKeysAreRejected() throws SQLException {
        requireCase("duplicate_source");
        TimescaleTestDatabase database = createTimescaleDatabase();
        database.flyway().migrate();

        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.execute(batchInsert(
                    "00000000-0000-0000-0000-000000000001",
                    "batch-1",
                    "10000000-0000-0000-0000-000000000001"));
            SQLException duplicateBatch = assertThrows(
                    SQLException.class,
                    () -> statement.execute(batchInsert(
                            "00000000-0000-0000-0000-000000000002",
                            "batch-1",
                            "10000000-0000-0000-0000-000000000002"))
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
    void unsupportedTimescaleExtensionFailsBeforeSchemaWrites() throws Exception {
        requireCase("unsupported_extension");
        TimescaleTestDatabase database = postgres == null
                ? TimescaleTestDatabase.create(postgresAdminUrl)
                : TimescaleTestDatabase.create(postgres);

        assertThrows(
                IllegalStateException.class,
                () -> configuredFlyway(database)
        );

        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            assertEquals(0, scalarInt(statement, """
                    SELECT count(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                    """));
            assertFalse(scalarBoolean(statement,
                    "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL"));
        }
    }

    @Test
    void legacyMysqlDatetimeIsInterpretedAsUtc() throws SQLException {
        requireCase("timezone_roundtrip");
        TimescaleTestDatabase database = createTimescaleDatabase();
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

    private static TimescaleTestDatabase createTimescaleDatabase() throws SQLException {
        return timescale == null
                ? TimescaleTestDatabase.create(timescaleAdminUrl)
                : TimescaleTestDatabase.create(timescale);
    }

    private static Flyway configuredFlyway(TimescaleTestDatabase database) throws Exception {
        TimescaleDatabaseProperties properties = new TimescaleDatabaseProperties();
        properties.setUrl(database.url());
        properties.setUsername(TimescaleTestDatabase.USER);
        properties.setPassword(TimescaleTestDatabase.PASSWORD);
        return new TimescaleMigrationConfiguration().hardwareDatabaseFlyway(properties);
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
