package com.rehealth.device.adapter;

import com.rehealth.contracts.telemetry.v1.ActivitySessionRecord;
import com.rehealth.contracts.telemetry.v1.MeasurementRecord;
import com.rehealth.contracts.telemetry.v1.RecentTelemetryResponse;
import com.rehealth.contracts.telemetry.v1.SleepSessionRecord;
import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.application.InterventionTelemetryContext;
import com.rehealth.device.application.MetricSummary;
import com.rehealth.device.application.UserHealthSummary;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@Primary
@ConditionalOnProperty(name = "rehealth.hardware-db.enabled", havingValue = "true")
public class TimescaleTelemetryReader implements TelemetryReadPort {
    static final List<String> USER_HEALTH_SQL = List.of(
            "SELECT DISTINCT device_id FROM hardware_measurement WHERE tenant_id = ? AND user_id = ? ORDER BY device_id",
            "SELECT count(*) FROM hardware_measurement WHERE tenant_id = ? AND user_id = ?",
            "SELECT count(*) FROM hardware_sleep_session WHERE tenant_id = ? AND user_id = ?",
            "SELECT count(*) FROM hardware_activity WHERE tenant_id = ? AND user_id = ?",
            "SELECT min(observed_at) FROM hardware_measurement WHERE tenant_id = ? AND user_id = ?",
            "SELECT max(observed_at) FROM hardware_measurement WHERE tenant_id = ? AND user_id = ?",
            "SELECT DISTINCT ON (metric_type) metric_type, primary_value, unit, observed_at "
                    + "FROM hardware_measurement WHERE tenant_id = ? AND user_id = ? ORDER BY metric_type, observed_at DESC",
            "SELECT DISTINCT source FROM ("
                    + "SELECT source FROM hardware_measurement WHERE tenant_id = ? AND user_id = ? "
                    + "UNION SELECT source FROM hardware_sleep_session WHERE tenant_id = ? AND user_id = ? "
                    + "UNION SELECT source FROM hardware_activity WHERE tenant_id = ? AND user_id = ?"
                    + ") scoped_sources WHERE source IS NOT NULL ORDER BY source"
    );
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
    private static final String TODAY_ACTIVITY_SQL = """
            SELECT COALESCE(sum(steps), 0) AS steps,
                   COALESCE(sum(duration_minutes), 0) AS active_minutes,
                   COALESCE(sum(calories_kcal), 0) AS calories_kcal,
                   avg(average_heart_rate) AS average_heart_rate,
                   max(started_at) AS latest_at
            FROM hardware_activity
            WHERE tenant_id = ? AND user_id = ?
              AND started_at >= ? AND started_at < ?
            """;
    private static final String TODAY_SLEEP_SQL = """
            SELECT round(extract(epoch FROM (ended_at - started_at)) / 60.0)::integer AS sleep_minutes,
                   ended_at
            FROM hardware_sleep_session
            WHERE tenant_id = ? AND user_id = ?
              AND ended_at >= ? AND ended_at < ?
            ORDER BY ended_at DESC
            LIMIT 1
            """;
    private static final String TODAY_MEASUREMENTS_SQL = """
            SELECT metric_type,
                   (array_agg(primary_value ORDER BY observed_at DESC))[1] AS latest_value,
                   avg(primary_value) AS average_value,
                   min(primary_value) AS minimum_value,
                   max(primary_value) AS maximum_value,
                   (array_agg(unit ORDER BY observed_at DESC))[1] AS unit,
                   count(*) AS sample_count,
                   max(observed_at) AS latest_at
            FROM hardware_measurement
            WHERE tenant_id = ? AND user_id = ?
              AND observed_at >= ? AND observed_at < ?
            GROUP BY metric_type
            ORDER BY metric_type
            """;
    private static final String TODAY_DIET_SQL = """
            SELECT meal_type, description, consumed_at, calories_kcal,
                   protein_grams, carbohydrate_grams, fat_grams,
                   fiber_grams, sodium_milligrams, source
            FROM hardware_diet_record
            WHERE tenant_id = ? AND user_id = ?
              AND consumed_at >= ? AND consumed_at < ?
            ORDER BY consumed_at ASC
            LIMIT 12
            """;
    private static final String RECENT_MEASUREMENT_CHANGES_SQL = """
            SELECT metric_type,
                   (array_agg(unit ORDER BY observed_at DESC))[1] AS unit,
                   avg(primary_value) FILTER (WHERE observed_at >= ?) AS recent_average,
                   avg(primary_value) FILTER (
                       WHERE observed_at >= ? AND observed_at < ?
                   ) AS previous_average,
                   count(*) FILTER (WHERE observed_at >= ?) AS recent_count,
                   count(*) FILTER (
                       WHERE observed_at >= ? AND observed_at < ?
                   ) AS previous_count
            FROM hardware_measurement
            WHERE tenant_id = ? AND user_id = ?
              AND observed_at >= ? AND observed_at < ?
            GROUP BY metric_type
            HAVING count(*) FILTER (WHERE observed_at >= ?) > 0
               AND count(*) FILTER (
                   WHERE observed_at >= ? AND observed_at < ?
               ) > 0
            ORDER BY metric_type
            """;
    private static final String ACTIVITY_CHANGE_SQL = """
            SELECT sum(steps) FILTER (WHERE started_at >= ?) / 7.0 AS recent_steps,
                   sum(steps) FILTER (WHERE started_at >= ? AND started_at < ?) / 7.0 AS previous_steps,
                   sum(duration_minutes) FILTER (WHERE started_at >= ?) / 7.0 AS recent_minutes,
                   sum(duration_minutes) FILTER (
                       WHERE started_at >= ? AND started_at < ?
                   ) / 7.0 AS previous_minutes,
                   count(*) FILTER (WHERE started_at >= ?) AS recent_count,
                   count(*) FILTER (
                       WHERE started_at >= ? AND started_at < ?
                   ) AS previous_count
            FROM hardware_activity
            WHERE tenant_id = ? AND user_id = ?
              AND started_at >= ? AND started_at < ?
            """;
    private static final String SLEEP_CHANGE_SQL = """
            SELECT avg(extract(epoch FROM (ended_at - started_at)) / 60.0)
                       FILTER (WHERE ended_at >= ?) AS recent_average,
                   avg(extract(epoch FROM (ended_at - started_at)) / 60.0)
                       FILTER (WHERE ended_at >= ? AND ended_at < ?) AS previous_average,
                   count(*) FILTER (WHERE ended_at >= ?) AS recent_count,
                   count(*) FILTER (WHERE ended_at >= ? AND ended_at < ?) AS previous_count
            FROM hardware_sleep_session
            WHERE tenant_id = ? AND user_id = ?
              AND ended_at >= ? AND ended_at < ?
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

    @Override
    public UserHealthSummary healthSummaryForUser(String tenantId, String userId) {
        try {
            List<String> devices = jdbc.queryForList(
                    USER_HEALTH_SQL.get(0),
                    String.class,
                    tenantId,
                    userId
            );
            Long measurementCount = jdbc.queryForObject(
                    USER_HEALTH_SQL.get(1), Long.class, tenantId, userId);
            Long sleepSessionCount = jdbc.queryForObject(
                    USER_HEALTH_SQL.get(2), Long.class, tenantId, userId);
            Long activityCount = jdbc.queryForObject(
                    USER_HEALTH_SQL.get(3), Long.class, tenantId, userId);
            Timestamp firstSeen = jdbc.queryForObject(
                    USER_HEALTH_SQL.get(4), Timestamp.class, tenantId, userId);
            Timestamp lastSeen = jdbc.queryForObject(
                    USER_HEALTH_SQL.get(5), Timestamp.class, tenantId, userId);
            List<MetricSummary> latestMetrics = jdbc.query(
                    USER_HEALTH_SQL.get(6),
                    (result, rowNumber) -> new MetricSummary(
                            result.getString("metric_type"),
                            decimal(result, "primary_value"),
                            result.getString("unit"),
                            epochMillis(result.getTimestamp("observed_at"))
                    ),
                    tenantId,
                    userId
            );
            List<String> provenance = jdbc.queryForList(
                    USER_HEALTH_SQL.get(7),
                    String.class,
                    tenantId, userId,
                    tenantId, userId,
                    tenantId, userId
            );
            boolean isSynthetic = isSyntheticProvenance(provenance);
            return new UserHealthSummary(
                    userId,
                    devices,
                    epochMillis(firstSeen),
                    epochMillis(lastSeen),
                    measurementCount == null ? 0L : measurementCount,
                    sleepSessionCount == null ? 0L : sleepSessionCount,
                    activityCount == null ? 0L : activityCount,
                    provenance,
                    isSynthetic,
                    latestMetrics
            );
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    static boolean isSyntheticProvenance(List<String> provenance) {
        return provenance.stream()
                .map(source -> source.toLowerCase(java.util.Locale.ROOT))
                .anyMatch(source -> source.contains("synthetic")
                        || source.contains("mock")
                        || source.contains("test_seed")
                        || source.contains("ring_sim")
                        || source.contains("demo")
                        || source.contains("sample"));
    }

    @Override
    public InterventionTelemetryContext interventionContext(
            String tenantId,
            String userId,
            ZoneId timeZone
    ) {
        LocalDate today = LocalDate.now(timeZone);
        Instant todayStart = today.atStartOfDay(timeZone).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(timeZone).toInstant();
        Instant recentStart = today.minusDays(6).atStartOfDay(timeZone).toInstant();
        Instant previousStart = today.minusDays(13).atStartOfDay(timeZone).toInstant();
        Timestamp todayTimestamp = Timestamp.from(todayStart);
        Timestamp tomorrowTimestamp = Timestamp.from(tomorrowStart);
        Timestamp recentTimestamp = Timestamp.from(recentStart);
        Timestamp previousTimestamp = Timestamp.from(previousStart);
        try {
            ActivityAggregate activity = jdbc.queryForObject(
                    TODAY_ACTIVITY_SQL,
                    (result, rowNumber) -> new ActivityAggregate(
                            result.getInt("steps"),
                            result.getInt("active_minutes"),
                            decimal(result, "calories_kcal"),
                            decimal(result, "average_heart_rate"),
                            epochMillis(result.getTimestamp("latest_at"))
                    ),
                    tenantId,
                    userId,
                    todayTimestamp,
                    tomorrowTimestamp
            );
            List<SleepAggregate> sleepRows = jdbc.query(
                    TODAY_SLEEP_SQL,
                    (result, rowNumber) -> new SleepAggregate(
                            integer(result, "sleep_minutes"),
                            epochMillis(result.getTimestamp("ended_at"))
                    ),
                    tenantId,
                    userId,
                    todayTimestamp,
                    tomorrowTimestamp
            );
            SleepAggregate sleep = sleepRows.isEmpty() ? null : sleepRows.get(0);
            List<InterventionTelemetryContext.MetricSnapshot> measurements = jdbc.query(
                    TODAY_MEASUREMENTS_SQL,
                    (result, rowNumber) -> new InterventionTelemetryContext.MetricSnapshot(
                            result.getString("metric_type"),
                            decimal(result, "latest_value"),
                            decimal(result, "average_value"),
                            decimal(result, "minimum_value"),
                            decimal(result, "maximum_value"),
                            result.getString("unit"),
                            result.getInt("sample_count"),
                            epochMillis(result.getTimestamp("latest_at"))
                    ),
                    tenantId,
                    userId,
                    todayTimestamp,
                    tomorrowTimestamp
            );
            List<InterventionTelemetryContext.DietSnapshot> dietRecords = jdbc.query(
                    TODAY_DIET_SQL,
                    (result, rowNumber) -> new InterventionTelemetryContext.DietSnapshot(
                            result.getString("meal_type"),
                            result.getString("description"),
                            epochMillis(result.getTimestamp("consumed_at")),
                            decimal(result, "calories_kcal"),
                            decimal(result, "protein_grams"),
                            decimal(result, "carbohydrate_grams"),
                            decimal(result, "fat_grams"),
                            decimal(result, "fiber_grams"),
                            decimal(result, "sodium_milligrams"),
                            result.getString("source")
                    ),
                    tenantId,
                    userId,
                    todayTimestamp,
                    tomorrowTimestamp
            );
            List<InterventionTelemetryContext.RecentChange> changes =
                    new ArrayList<>(measurementChanges(
                            tenantId,
                            userId,
                            previousTimestamp,
                            recentTimestamp,
                            tomorrowTimestamp
                    ));
            appendActivityChanges(
                    changes,
                    tenantId,
                    userId,
                    previousTimestamp,
                    recentTimestamp,
                    tomorrowTimestamp
            );
            appendSleepChange(
                    changes,
                    tenantId,
                    userId,
                    previousTimestamp,
                    recentTimestamp,
                    tomorrowTimestamp
            );
            changes.sort(Comparator.comparing(InterventionTelemetryContext.RecentChange::metricType));

            long generatedAt = Instant.now().toEpochMilli();
            Long latestDataAt = measurements.stream()
                    .map(InterventionTelemetryContext.MetricSnapshot::latestObservedAt)
                    .filter(value -> value != null)
                    .max(Long::compareTo)
                    .orElse(null);
            latestDataAt = maximum(
                    latestDataAt,
                    activity == null ? null : activity.latestAt(),
                    sleep == null ? null : sleep.endedAt(),
                    dietRecords.stream()
                            .map(InterventionTelemetryContext.DietSnapshot::consumedAt)
                            .filter(value -> value != null)
                            .max(Long::compareTo)
                            .orElse(null)
            );
            InterventionTelemetryContext.TodayBehavior todayBehavior =
                    new InterventionTelemetryContext.TodayBehavior(
                            activity == null ? 0 : activity.steps(),
                            activity == null ? 0 : activity.activeMinutes(),
                            activity == null || activity.caloriesKcal() == null
                                    ? 0.0
                                    : activity.caloriesKcal(),
                            activity == null ? null : activity.averageHeartRate(),
                            sleep == null ? null : sleep.sleepMinutes(),
                            sleep == null ? null : sleep.endedAt(),
                            List.copyOf(dietRecords),
                            List.copyOf(measurements)
                    );
            return new InterventionTelemetryContext(
                    generatedAt,
                    today.toString(),
                    timeZone.getId(),
                    latestDataAt,
                    todayBehavior,
                    List.copyOf(changes)
            );
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    private List<InterventionTelemetryContext.RecentChange> measurementChanges(
            String tenantId,
            String userId,
            Timestamp previousStart,
            Timestamp recentStart,
            Timestamp windowEnd
    ) {
        return jdbc.query(
                RECENT_MEASUREMENT_CHANGES_SQL,
                (result, rowNumber) -> change(
                        result.getString("metric_type"),
                        result.getString("unit"),
                        decimal(result, "recent_average"),
                        decimal(result, "previous_average"),
                        result.getInt("recent_count"),
                        result.getInt("previous_count")
                ),
                recentStart,
                previousStart,
                recentStart,
                recentStart,
                previousStart,
                recentStart,
                tenantId,
                userId,
                previousStart,
                windowEnd,
                recentStart,
                previousStart,
                recentStart
        );
    }

    private void appendActivityChanges(
            List<InterventionTelemetryContext.RecentChange> changes,
            String tenantId,
            String userId,
            Timestamp previousStart,
            Timestamp recentStart,
            Timestamp windowEnd
    ) {
        ActivityChanges activity = jdbc.queryForObject(
                ACTIVITY_CHANGE_SQL,
                (result, rowNumber) -> new ActivityChanges(
                        decimal(result, "recent_steps"),
                        decimal(result, "previous_steps"),
                        decimal(result, "recent_minutes"),
                        decimal(result, "previous_minutes"),
                        result.getInt("recent_count"),
                        result.getInt("previous_count")
                ),
                recentStart,
                previousStart,
                recentStart,
                recentStart,
                previousStart,
                recentStart,
                recentStart,
                previousStart,
                recentStart,
                tenantId,
                userId,
                previousStart,
                windowEnd
        );
        if (activity == null || activity.recentCount() == 0 || activity.previousCount() == 0) {
            return;
        }
        changes.add(change(
                "daily_steps",
                "steps/day",
                activity.recentSteps(),
                activity.previousSteps(),
                activity.recentCount(),
                activity.previousCount()
        ));
        changes.add(change(
                "daily_active_minutes",
                "min/day",
                activity.recentMinutes(),
                activity.previousMinutes(),
                activity.recentCount(),
                activity.previousCount()
        ));
    }

    private void appendSleepChange(
            List<InterventionTelemetryContext.RecentChange> changes,
            String tenantId,
            String userId,
            Timestamp previousStart,
            Timestamp recentStart,
            Timestamp windowEnd
    ) {
        SleepChanges sleep = jdbc.queryForObject(
                SLEEP_CHANGE_SQL,
                (result, rowNumber) -> new SleepChanges(
                        decimal(result, "recent_average"),
                        decimal(result, "previous_average"),
                        result.getInt("recent_count"),
                        result.getInt("previous_count")
                ),
                recentStart,
                previousStart,
                recentStart,
                recentStart,
                previousStart,
                recentStart,
                tenantId,
                userId,
                previousStart,
                windowEnd
        );
        if (sleep != null && sleep.recentCount() > 0 && sleep.previousCount() > 0) {
            changes.add(change(
                    "sleep_minutes",
                    "min/session",
                    sleep.recentAverage(),
                    sleep.previousAverage(),
                    sleep.recentCount(),
                    sleep.previousCount()
            ));
        }
    }

    private InterventionTelemetryContext.RecentChange change(
            String metricType,
            String unit,
            Double recent,
            Double previous,
            int recentCount,
            int previousCount
    ) {
        Double delta = recent == null || previous == null ? null : recent - previous;
        String trend = delta == null || Math.abs(delta) < 0.000001
                ? "stable"
                : delta > 0.0 ? "up" : "down";
        return new InterventionTelemetryContext.RecentChange(
                metricType,
                unit,
                recent,
                previous,
                delta,
                trend,
                recentCount,
                previousCount
        );
    }

    private Long maximum(Long... values) {
        Long maximum = null;
        for (Long value : values) {
            if (value != null && (maximum == null || value > maximum)) {
                maximum = value;
            }
        }
        return maximum;
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

    private record ActivityAggregate(
            int steps,
            int activeMinutes,
            Double caloriesKcal,
            Double averageHeartRate,
            Long latestAt
    ) {
    }

    private record SleepAggregate(Integer sleepMinutes, Long endedAt) {
    }

    private record ActivityChanges(
            Double recentSteps,
            Double previousSteps,
            Double recentMinutes,
            Double previousMinutes,
            int recentCount,
            int previousCount
    ) {
    }

    private record SleepChanges(
            Double recentAverage,
            Double previousAverage,
            int recentCount,
            int previousCount
    ) {
    }
}
