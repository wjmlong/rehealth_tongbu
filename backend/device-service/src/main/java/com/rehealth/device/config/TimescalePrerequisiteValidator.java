package com.rehealth.device.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

public final class TimescalePrerequisiteValidator {
    private static final int MINIMUM_POSTGRES_VERSION = 170000;
    private static final int[] MINIMUM_TIMESCALE_VERSION = {2, 18, 0};

    private TimescalePrerequisiteValidator() {
    }

    public static void validate(String url, String username, String password) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            validatePostgres(statement);
            validateTimescale(statement);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Unable to validate hardware DB prerequisites before Flyway startup",
                    exception
            );
        }
    }

    private static void validatePostgres(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery(
                "SELECT current_setting('server_version_num')::integer")) {
            if (!result.next() || result.getInt(1) < MINIMUM_POSTGRES_VERSION) {
                throw new IllegalStateException(
                        "ReHealth hardware_db requires PostgreSQL 17 or newer");
            }
        }
    }

    private static void validateTimescale(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery(
                "SELECT extversion FROM pg_extension WHERE extname = 'timescaledb'")) {
            if (!result.next()) {
                throw new IllegalStateException(
                        "ReHealth hardware_db requires TimescaleDB 2.18 or newer");
            }
            int[] installed = parseVersion(result.getString(1));
            if (compare(installed, MINIMUM_TIMESCALE_VERSION) < 0) {
                throw new IllegalStateException(
                        "ReHealth hardware_db requires TimescaleDB 2.18 or newer");
            }
        }
    }

    private static int[] parseVersion(String version) {
        String numeric = version == null ? "" : version.replaceFirst("[^0-9.].*$", "");
        try {
            int[] parts = Arrays.stream(numeric.split("\\."))
                    .limit(3)
                    .mapToInt(Integer::parseInt)
                    .toArray();
            if (parts.length < 2) {
                throw new NumberFormatException("missing major or minor version");
            }
            return parts;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Cannot parse installed TimescaleDB version: " + version,
                    exception
            );
        }
    }

    private static int compare(int[] left, int[] right) {
        for (int index = 0; index < right.length; index++) {
            int leftPart = index < left.length ? left[index] : 0;
            int comparison = Integer.compare(leftPart, right[index]);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }
}
