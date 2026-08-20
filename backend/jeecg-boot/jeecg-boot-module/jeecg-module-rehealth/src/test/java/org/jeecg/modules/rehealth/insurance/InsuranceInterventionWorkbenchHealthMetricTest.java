package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.ingest.writer.HardwarePersistenceUnavailableException;
import org.jeecg.modules.rehealth.mobile.dto.RecentTelemetryResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsuranceInterventionWorkbenchHealthMetricTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    void exposesOnlyWhitelistedLatestTelemetryAndMarksSyntheticQaData() {
        RecentTelemetryResponseDto telemetry = new RecentTelemetryResponseDto();
        telemetry.measurements.add(measurement("HEART_RATE", 68.0, null, "bpm", 20L, "MRD_RING"));
        telemetry.measurements.add(measurement("HEART_RATE", 79.0, null, "bpm", 10L, "MRD_RING"));
        telemetry.measurements.add(measurement("BLOOD_PRESSURE", 122.0, 78.0, "mmHg", 18L, "MRD_RING"));
        telemetry.measurements.add(measurement("UNSUPPORTED_RAW_METRIC", 999.0, null, "raw", 19L, "MRD_RING"));

        RecentTelemetryResponseDto.SleepSession sleep = new RecentTelemetryResponseDto.SleepSession();
        sleep.endedAt = 30L;
        sleep.deepMinutes = 90;
        sleep.lightMinutes = 260;
        sleep.awakeMinutes = 20;
        sleep.remMinutes = 80;
        sleep.source = "LOCAL_MULTI_INSURER_APP_QA";
        telemetry.sleepSessions.add(sleep);

        RecentTelemetryResponseDto.Activity activity = new RecentTelemetryResponseDto.Activity();
        activity.endedAt = 40L;
        activity.steps = 7654;
        activity.durationMinutes = 36;
        activity.caloriesKcal = 238.5;
        activity.source = "LOCAL_MULTI_INSURER_APP_QA";
        telemetry.activities.add(activity);

        InsuranceInterventionWorkbenchService service = service(telemetry);
        Map<String, InsuranceInterventionWorkbenchResponse.HealthMetric> metrics = service.healthMetrics("app-user-1")
                .stream()
                .collect(Collectors.toMap(
                        InsuranceInterventionWorkbenchResponse.HealthMetric::metricCode,
                        Function.identity()
                ));

        assertEquals(7, metrics.size());
        assertEquals(68.0, metrics.get("heart_rate").value());
        assertEquals(122.0, metrics.get("systolic_bp").value());
        assertEquals(78.0, metrics.get("diastolic_bp").value());
        assertEquals(430.0, metrics.get("sleep_minutes").value());
        assertEquals(7654.0, metrics.get("steps").value());
        assertEquals(36.0, metrics.get("activity_minutes").value());
        assertEquals(238.5, metrics.get("calories").value());
        assertFalse(metrics.containsKey("unsupported_raw_metric"));
        assertFalse(metrics.get("heart_rate").synthetic());
        assertTrue(metrics.get("sleep_minutes").synthetic());
        assertTrue(metrics.get("activity_minutes").synthetic());
        assertEquals("device_telemetry", metrics.get("sleep_minutes").dataSource());
    }

    @Test
    void returnsNoMetricsWhenHardwarePersistenceIsUnavailable() {
        InsuranceInterventionWorkbenchService service = new InsuranceInterventionWorkbenchService(
                new JdbcTemplate(),
                new ObjectMapper(),
                (userId, limit) -> {
                    throw new HardwarePersistenceUnavailableException("hardware persistence unavailable");
                },
                Clock.systemUTC(),
                ZONE
        );

        assertEquals(List.of(), service.healthMetrics("app-user-1"));
    }

    private InsuranceInterventionWorkbenchService service(RecentTelemetryResponseDto telemetry) {
        return new InsuranceInterventionWorkbenchService(
                new JdbcTemplate(),
                new ObjectMapper(),
                (userId, limit) -> telemetry,
                Clock.systemUTC(),
                ZONE
        );
    }

    private RecentTelemetryResponseDto.Measurement measurement(
            String type,
            Double primary,
            Double secondary,
            String unit,
            Long observedAt,
            String source
    ) {
        RecentTelemetryResponseDto.Measurement measurement = new RecentTelemetryResponseDto.Measurement();
        measurement.metricType = type;
        measurement.primaryValue = primary;
        measurement.secondaryValue = secondary;
        measurement.unit = unit;
        measurement.measuredAt = observedAt;
        measurement.source = source;
        return measurement;
    }
}
