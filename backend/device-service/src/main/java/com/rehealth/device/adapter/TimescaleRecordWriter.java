package com.rehealth.device.adapter;

import com.rehealth.contracts.telemetry.v1.ActivitySessionRecord;
import com.rehealth.contracts.telemetry.v1.MeasurementRecord;
import com.rehealth.contracts.telemetry.v1.SleepSessionRecord;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchRequest;
import com.rehealth.device.domain.DeviceClaims;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class TimescaleRecordWriter {
    private final JdbcTemplate jdbc;
    private final TelemetryPayloadCodec payloads;

    TimescaleRecordWriter(JdbcTemplate jdbc, TelemetryPayloadCodec payloads) {
        this.jdbc = jdbc;
        this.payloads = payloads;
    }

    int write(
            UUID batchId,
            DeviceClaims claims,
            TelemetryBatchRequest request,
            Instant receivedAt
    ) {
        writeMeasurements(batchId, claims, request);
        writeSleep(batchId, claims, request);
        writeActivities(batchId, claims, request);
        return writeQualityEvents(batchId, claims, request, receivedAt);
    }

    private void writeMeasurements(
            UUID batchId,
            DeviceClaims claims,
            TelemetryBatchRequest request
    ) {
        List<MeasurementRecord> measurements =
                request.measurements == null ? List.of() : request.measurements;
        for (int index = 0; index < measurements.size(); index++) {
            MeasurementRecord record = measurements.get(index);
            jdbc.update("""
                            INSERT INTO hardware_measurement (
                              id, upload_batch_id, tenant_id, user_id, device_id,
                              source_record_id, metric_type, observed_at, primary_value,
                              secondary_value, unit, quality_code, source
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    UUID.randomUUID(), batchId, claims.tenantId(), claims.userId(), claims.deviceId(),
                    payloads.sourceId(record.id, request.batchId, "measurement", index), record.metricType,
                    payloads.timestamp(record.measuredAt), record.primaryValue, record.secondaryValue,
                    record.unit, record.qualityCode, record.source);
        }
    }

    private void writeSleep(UUID batchId, DeviceClaims claims, TelemetryBatchRequest request) {
        List<SleepSessionRecord> sleepSessions =
                request.sleepSessions == null ? List.of() : request.sleepSessions;
        for (int index = 0; index < sleepSessions.size(); index++) {
            SleepSessionRecord record = sleepSessions.get(index);
            jdbc.update("""
                            INSERT INTO hardware_sleep_session (
                              id, upload_batch_id, tenant_id, user_id, device_id,
                              source_record_id, started_at, ended_at, deep_minutes,
                              light_minutes, awake_minutes, rem_minutes,
                              interruption_minutes, source
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    UUID.randomUUID(), batchId, claims.tenantId(), claims.userId(), claims.deviceId(),
                    payloads.sourceId(record.id, request.batchId, "sleep", index),
                    payloads.timestamp(record.startedAt), payloads.timestamp(record.endedAt),
                    payloads.value(record.deepMinutes), payloads.value(record.lightMinutes),
                    payloads.value(record.awakeMinutes), payloads.value(record.remMinutes),
                    payloads.value(record.interruptionMinutes), record.source);
        }
    }

    private void writeActivities(UUID batchId, DeviceClaims claims, TelemetryBatchRequest request) {
        List<ActivitySessionRecord> activitySessions =
                request.activitySessions == null ? List.of() : request.activitySessions;
        for (int index = 0; index < activitySessions.size(); index++) {
            ActivitySessionRecord record = activitySessions.get(index);
            jdbc.update("""
                            INSERT INTO hardware_activity (
                              id, upload_batch_id, tenant_id, user_id, device_id,
                              source_record_id, started_at, ended_at, activity_type,
                              steps, distance_meters, calories_kcal, duration_minutes,
                              average_heart_rate, source
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    UUID.randomUUID(), batchId, claims.tenantId(), claims.userId(), claims.deviceId(),
                    payloads.sourceId(record.id, request.batchId, "activity", index),
                    payloads.timestamp(record.startedAt), payloads.timestamp(record.endedAt), record.activityType,
                    payloads.value(record.steps), payloads.value(record.distanceMeters),
                    payloads.value(record.caloriesKcal), payloads.value(record.durationMinutes),
                    record.averageHeartRate, record.source);
        }
    }

    private int writeQualityEvents(
            UUID batchId,
            DeviceClaims claims,
            TelemetryBatchRequest request,
            Instant receivedAt
    ) {
        int count = 0;
        List<MeasurementRecord> measurements =
                request.measurements == null ? List.of() : request.measurements;
        for (int index = 0; index < measurements.size(); index++) {
            MeasurementRecord record = measurements.get(index);
            if (record.qualityCode == null || record.qualityCode.isBlank()) {
                continue;
            }
            insertQuality(batchId, claims,
                    payloads.sourceId(record.id, request.batchId, "measurement", index),
                    "NORMALIZED_RECORD_QUALITY", "WARN", record.qualityCode,
                    Instant.ofEpochMilli(record.measuredAt));
            count++;
        }
        int rejectedCount = payloads.rejectedCount(request.quality);
        if (rejectedCount > 0) {
            Object code = request.quality == null ? null : request.quality.get("rejectionCode");
            insertQuality(batchId, claims, payloads.digest(request.batchId + ":rejection"),
                    "RECORD_REJECTED", "WARN",
                    code == null ? "CLIENT_REPORTED_REJECTION" : String.valueOf(code),
                    payloads.instant(request.collectedTo, receivedAt));
            count++;
        }
        return count;
    }

    private void insertQuality(
            UUID batchId,
            DeviceClaims claims,
            String sourceRecordId,
            String eventType,
            String severity,
            String detailCode,
            Instant eventAt
    ) {
        jdbc.update("""
                        INSERT INTO hardware_data_quality_event (
                          id, upload_batch_id, tenant_id, user_id, device_id,
                          source_record_id, event_type, severity, detail_code, event_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(), batchId, claims.tenantId(), claims.userId(), claims.deviceId(),
                sourceRecordId, eventType, severity, detailCode, Timestamp.from(eventAt));
    }
}
