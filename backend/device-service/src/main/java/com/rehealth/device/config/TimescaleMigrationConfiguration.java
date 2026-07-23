package com.rehealth.device.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "rehealth.hardware-db.enabled", havingValue = "true")
@EnableConfigurationProperties(TimescaleDatabaseProperties.class)
public class TimescaleMigrationConfiguration {
    @Bean(name = "hardwareDataSource")
    public DataSource hardwareDataSource(TimescaleDatabaseProperties properties) throws IOException {
        String url = required(properties.getUrl(), "REHEALTH_HARDWARE_DB_URL");
        String username = required(
                properties.getUsername(), "REHEALTH_HARDWARE_DB_USERNAME");
        String password = databasePassword(properties);
        TimescalePrerequisiteValidator.validate(url, username, password);
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean(initMethod = "migrate")
    public Flyway hardwareDatabaseFlyway(
            @Qualifier("hardwareDataSource") DataSource dataSource,
            TimescaleDatabaseProperties properties
    ) {
        TimescaleDatabaseProperties.Retention retention = properties.getRetention();
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/timescale")
                .placeholders(Map.of(
                        "measurementRetentionDays", positive(retention.getMeasurementDays()),
                        "signalMetadataRetentionDays", positive(retention.getSignalMetadataDays()),
                        "operationalRetentionDays", positive(retention.getOperationalDays()),
                        "publishedOutboxRetentionDays", positive(retention.getPublishedOutboxDays())
                ))
                .failOnMissingLocations(true)
                .validateMigrationNaming(true)
                .load();
    }

    public Flyway hardwareDatabaseFlyway(
            TimescaleDatabaseProperties properties
    ) throws IOException {
        return hardwareDatabaseFlyway(hardwareDataSource(properties), properties);
    }

    private String databasePassword(TimescaleDatabaseProperties properties) throws IOException {
        if (!properties.getPassword().isBlank()) {
            return properties.getPassword();
        }
        String passwordFile = required(
                properties.getPasswordFile(), "REHEALTH_HARDWARE_DB_PASSWORD_FILE");
        return required(Files.readString(Path.of(passwordFile)).trim(), "hardware DB password");
    }

    private String required(String value, String setting) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(setting + " is required when hardware_db is enabled");
        }
        return value;
    }

    private String positive(int days) {
        if (days < 1) {
            throw new IllegalStateException("hardware DB retention days must be positive");
        }
        return Integer.toString(days);
    }
}
