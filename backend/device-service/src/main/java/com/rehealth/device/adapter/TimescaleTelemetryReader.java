package com.rehealth.device.adapter;

import com.rehealth.contracts.telemetry.v1.ActivitySessionRecord;
import com.rehealth.contracts.telemetry.v1.MeasurementRecord;
import com.rehealth.contracts.telemetry.v1.RecentTelemetryResponse;
import com.rehealth.contracts.telemetry.v1.SleepSessionRecord;
import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.domain.DeviceClaims;
import com.rehealth.device.port.TelemetryReadPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Component
@Primary
@ConditionalOnProperty(name = "rehealth.hardware-db.enabled", havingValue = "true")
public class TimescaleTelemetryReader implements TelemetryReadPort {
    private static final String MEASUREMENTS_SQL = """
            SELECT source_record_id, metric_type, observed_at, primary_value,
                   secondary_value, unit, quality_code, source
            FROM hardware_measurement
            WHERE tenant_id = ? AND user_id = ? AND device_id = ?
            ORDER BY observed_at DESC
            LIMIT ?
            """;
    private static final String SLEEP_SQL = """
            SELECT source_record_id, started_at, ended_at, deep_minutes,
                   light_minutes, awake_minutes, rem_minutes,
                   interruption_minutes, source
            FROM hardware_sleep_session
            WHERE tenant_id = ? AND user_id = ? AND device_id = ?
            ORDER BY started_at DESC
            LIMIT ?
            """;
    private static final String ACTIVITIES_SQL = """
            SELECT source_record_id, started_at, ended_at, activity_type,
                   steps, distance_meters, calories_kcal, duration_minutes,
                   average_heart_rate, source
            FROM hardware_activity
            WHERE tenant_id = ? AND user_id = ? AND device_id = ?
            ORDER BY started_at DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbc;

    public TimescaleTelemetryReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public RecentTelemetryResponse recent(DeviceClaims claims, int limit) {
        try {
            RecentTelemetryResponse response = new RecentTelemetryResponse();
            response.userId = claims.userId();
            response.limit = limit;
            Object[] scope = {
                    claims.tenantId(), claims.userId(), claims.deviceId(), limit
            };
            response.measurements = jdbc.query(MEASUREMENTS_SQL, this::measurement, scope);
            response.sleepSessions = jdbc.query(SLEEP_SQL, this::sleep, scope);
            response.activities = jdbc.query(ACTIVITIES_SQL, this::activity, scope);
            return response;
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public boolean ready() {
        try {
            Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (DataAccessException exception) {
            return false;
        }
    }

    private MeasurementRecord measurement(ResultSet result, int rowNumber) throws SQLException {
        MeasurementRecord record = new MeasurementRecord();
        record.id = result.getString("source_record_id");
        record.metricType = result.getString("metric_type");
        record.measuredAt = epochMillis(result.getTimestamp("observed_at"));
        record.primaryValue = decimal(result, "primary_value");
        record.secondaryValue = decimal(result, "secondary_value");
        record.unit = result.getString("unit");
        record.qualityCode = result.getString("quality_code");
        record.source = result.getString("source");
        return record;
    }

    private SleepSessionRecord sleep(ResultSet result, int rowNumber) throws SQLException {
        SleepSessionRecord record = new SleepSessionRecord();
        record.id = result.getString("source_record_id");
        record.startedAt = epochMillis(result.getTimestamp("started_at"));
        record.endedAt = epochMillis(result.getTimestamp("ended_at"));
        record.deepMinutes = integer(result, "deep_minutes");
        record.lightMinutes = integer(result, "light_minutes");
        record.awakeMinutes = integer(result, "awake_minutes");
        record.remMinutes = integer(result, "rem_minutes");
        record.interruptionMinutes = integer(result, "interruption_minutes");
        record.source = result.getString("source");
        return record;
    }

    private ActivitySessionRecord activity(ResultSet result, int rowNumber) throws SQLException {
        ActivitySessionRecord record = new ActivitySessionRecord();
        record.id = result.getString("source_record_id");
        record.startedAt = epochMillis(result.getTimestamp("started_at"));
        record.endedAt = epochMillis(result.getTimestamp("ended_at"));
        record.activityType = result.getString("activity_type");
        record.steps = integer(result, "steps");
        record.distanceMeters = decimal(result, "distance_meters");
        record.caloriesKcal = decimal(result, "calories_kcal");
        record.durationMinutes = integer(result, "duration_minutes");
        record.averageHeartRate = decimal(result, "average_heart_rate");
        record.source = result.getString("source");
        return record;
    }

    private Long epochMillis(Timestamp value) {
        return value == null ? null : value.toInstant().toEpochMilli();
    }

    private Double decimal(ResultSet result, String column) throws SQLException {
        BigDecimal value = result.getBigDecimal(column);
        return value == null ? null : value.doubleValue();
    }

    private Integer integer(ResultSet result, String column) throws SQLException {
        Integer value = result.getObject(column, Integer.class);
        return value;
    }

    private DeviceRequestException unavailable(Throwable cause) {
        return new DeviceRequestException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "HARDWARE_PERSISTENCE_UNAVAILABLE",
                cause
        );
    }
}
