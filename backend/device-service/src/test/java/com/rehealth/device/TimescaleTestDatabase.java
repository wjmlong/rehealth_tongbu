package com.rehealth.device;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

final class TimescaleTestDatabase {
    static final String TIMESCALE_IMAGE =
            "timescale/timescaledb:2.21.1-pg17@sha256:c17f60ac41a9b5c529af918e8827156bb02faeddb3fab1c961b4eebe52b25d83";
    static final String POSTGRES_IMAGE = "postgres:17";
    static final String USER = "rehealth_test";
    static final String PASSWORD = "rehealth_test";
    static final Map<String, String> RETENTION_PLACEHOLDERS = Map.of(
            "measurementRetentionDays", "730",
            "signalMetadataRetentionDays", "90",
            "operationalRetentionDays", "1095",
            "publishedOutboxRetentionDays", "30"
    );

    private final GenericContainer<?> container;
    private final String database;

    private TimescaleTestDatabase(GenericContainer<?> container, String database) {
        this.container = container;
        this.database = database;
    }

    static GenericContainer<?> start(String image) {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(image))
                .withEnv("POSTGRES_DB", "postgres")
                .withEnv("POSTGRES_USER", USER)
                .withEnv("POSTGRES_PASSWORD", PASSWORD)
                .withExposedPorts(5432)
                .waitingFor(Wait.forListeningPort());
        container.start();
        return container;
    }

    static TimescaleTestDatabase create(GenericContainer<?> container) throws SQLException {
        String database = "rehealth_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl(container, "postgres"), USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        return new TimescaleTestDatabase(container, database);
    }

    Flyway flyway() {
        return Flyway.configure()
                .dataSource(url(), USER, PASSWORD)
                .locations("classpath:db/migration/timescale")
                .placeholders(RETENTION_PLACEHOLDERS)
                .failOnMissingLocations(true)
                .validateMigrationNaming(true)
                .load();
    }

    Flyway flywayAtVersion(String version) {
        return Flyway.configure()
                .dataSource(url(), USER, PASSWORD)
                .locations("classpath:db/migration/timescale")
                .placeholders(RETENTION_PLACEHOLDERS)
                .target(MigrationVersion.fromVersion(version))
                .failOnMissingLocations(true)
                .validateMigrationNaming(true)
                .load();
    }

    Connection connect() throws SQLException {
        return DriverManager.getConnection(url(), USER, PASSWORD);
    }

    String url() {
        return jdbcUrl(container, database);
    }

    private static String jdbcUrl(GenericContainer<?> container, String database) {
        return "jdbc:postgresql://%s:%d/%s".formatted(
                container.getHost(), container.getMappedPort(5432), database);
    }
}
