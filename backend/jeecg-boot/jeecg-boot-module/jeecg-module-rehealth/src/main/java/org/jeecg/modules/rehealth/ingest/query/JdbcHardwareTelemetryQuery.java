package org.jeecg.modules.rehealth.ingest.query;

import org.jeecg.modules.rehealth.mobile.dto.RecentTelemetryResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
@ConditionalOnProperty(name = "rehealth.hardware-db.enabled", havingValue = "true")
public class JdbcHardwareTelemetryQuery implements HardwareTelemetryQuery {
    private final JdbcTemplate jdbcTemplate;

    public JdbcHardwareTelemetryQuery(
            @Qualifier("rehealthHardwareJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RecentTelemetryResponseDto recentForUser(String userId, int limit) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("authenticated userId is required");
        }
        int boundedLimit = Math.max(1, Math.min(limit, 200));
        RecentTelemetryResponseDto response = new RecentTelemetryResponseDto();
        response.userId = userId;
        response.limit = boundedLimit;
        response.measurements = jdbcTemplate.query("""
                        SELECT device_id, metric_type, measured_at, primary_value, secondary_value,
                               unit, quality_code, source
                        FROM hardware_measurement
                        WHERE user_id = ?
                        ORDER BY measured_at DESC, id DESC
                        LIMIT ?
                        """,
                (resultSet, rowNum) -> {
                    RecentTelemetryResponseDto.Measurement item = new RecentTelemetryResponseDto.Measurement();
                    item.deviceId = resultSet.getString("device_id");
                    item.metricType = resultSet.getString("metric_type");
                    item.measuredAt = epochMillis(resultSet.getTimestamp("measured_at"));
                    item.primaryValue = resultSet.getDouble("primary_value");
                    item.secondaryValue = nullableDouble(resultSet, "secondary_value");
                    item.unit = resultSet.getString("unit");
                    item.qualityCode = resultSet.getString("quality_code");
                    item.source = resultSet.getString("source");
                    return item;
                }, userId, boundedLimit);
        response.sleepSessions = jdbcTemplate.query("""
                        SELECT device_id, started_at, ended_at, deep_minutes, light_minutes,
                               awake_minutes, rem_minutes, interruption_minutes, source
                        FROM hardware_sleep_session
                        WHERE user_id = ?
                        ORDER BY started_at DESC, id DESC
                        LIMIT ?
                        """,
                (resultSet, rowNum) -> {
                    RecentTelemetryResponseDto.SleepSession item = new RecentTelemetryResponseDto.SleepSession();
                    item.deviceId = resultSet.getString("device_id");
                    item.startedAt = epochMillis(resultSet.getTimestamp("started_at"));
                    item.endedAt = epochMillis(resultSet.getTimestamp("ended_at"));
                    item.deepMinutes = resultSet.getInt("deep_minutes");
                    item.lightMinutes = resultSet.getInt("light_minutes");
                    item.awakeMinutes = resultSet.getInt("awake_minutes");
                    item.remMinutes = resultSet.getInt("rem_minutes");
                    item.interruptionMinutes = resultSet.getInt("interruption_minutes");
                    item.source = resultSet.getString("source");
                    return item;
                }, userId, boundedLimit);
        response.activities = jdbcTemplate.query("""
                        SELECT device_id, started_at, ended_at, activity_type, steps,
                               distance_meters, calories_kcal, duration_minutes,
                               average_heart_rate, source
                        FROM hardware_activity
                        WHERE user_id = ?
                        ORDER BY started_at DESC, id DESC
                        LIMIT ?
                        """,
                (resultSet, rowNum) -> {
                    RecentTelemetryResponseDto.Activity item = new RecentTelemetryResponseDto.Activity();
                    item.deviceId = resultSet.getString("device_id");
                    item.startedAt = epochMillis(resultSet.getTimestamp("started_at"));
                    item.endedAt = epochMillis(resultSet.getTimestamp("ended_at"));
                    item.activityType = resultSet.getString("activity_type");
                    item.steps = resultSet.getInt("steps");
                    item.distanceMeters = resultSet.getDouble("distance_meters");
                    item.caloriesKcal = resultSet.getDouble("calories_kcal");
                    item.durationMinutes = resultSet.getInt("duration_minutes");
                    item.averageHeartRate = nullableDouble(resultSet, "average_heart_rate");
                    item.source = resultSet.getString("source");
                    return item;
                }, userId, boundedLimit);
        return response;
    }

    private Long epochMillis(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.getTime();
    }

    private Double nullableDouble(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }
}
