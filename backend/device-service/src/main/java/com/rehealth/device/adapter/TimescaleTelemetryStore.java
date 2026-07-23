package com.rehealth.device.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehealth.contracts.telemetry.v1.ActivitySessionRecord;
import com.rehealth.contracts.telemetry.v1.MeasurementRecord;
import com.rehealth.contracts.telemetry.v1.SleepSessionRecord;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchRequest;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryValidationResult;
import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.domain.DeviceClaims;
import com.rehealth.device.port.TelemetryWritePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Primary
@ConditionalOnProperty(name = "rehealth.hardware-db.enabled", havingValue = "true")
public class TimescaleTelemetryStore implements TelemetryWritePort {
    private static final String PERSISTED_EVENT = "rehealth.telemetry.persisted.v1";
    private static final String QUALITY_EVENT = "rehealth.telemetry.quality.v1";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper objectMapper;

    public TimescaleTelemetryStore(
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            ObjectMapper objectMapper
    ) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.objectMapper = objectMapper;
    }

    @Override
    public TelemetryBatchResponse write(
            DeviceClaims claims,
            TelemetryBatchRequest request,
            TelemetryValidationResult validation
    ) {
        try {
            TelemetryBatchResponse response =
                    transactions.execute(status -> writeTransaction(claims, request, validation));
            if (response == null) {
                throw unavailable();
            }
            return response;
        } catch (DeviceRequestException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        } catch (TransactionException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public boolean ready() {
        try {
            Integer value = jdbc.queryForObject("SELECT 1", Integer.class);
            return value != null && value == 1;
        } catch (DataAccessException exception) {
            return false;
        }
    }

    public String replay(String tenantId, String receiptId, String actorId, String reason) {
        try {
            String result = transactions.execute(status -> {
                List<String> receipts = jdbc.query("""
                                SELECT batch.receipt_id::text
                                FROM hardware_upload_batch batch
                                JOIN hardware_reconciliation reconciliation
                                  ON reconciliation.upload_batch_id = batch.id
                                WHERE batch.tenant_id = ? AND batch.receipt_id = ?::uuid
                                FOR UPDATE
                                """,
                        (row, index) -> row.getString(1),
                        tenantId, receiptId);
                if (receipts.isEmpty()) {
                    throw new DeviceRequestException(HttpStatus.NOT_FOUND, "TELEMETRY_RECEIPT_NOT_FOUND");
                }
                jdbc.update("""
                                UPDATE hardware_reconciliation reconciliation
                                SET state = 'EVENT_PENDING',
                                    attempt_count = attempt_count
                                      + CASE WHEN operator_actor_id IS DISTINCT FROM ?
                                                OR operator_reason IS DISTINCT FROM ? THEN 1 ELSE 0 END,
                                    operator_actor_id = ?,
                                    operator_reason = ?,
                                    updated_at = now()
                                FROM hardware_upload_batch batch
                                WHERE reconciliation.upload_batch_id = batch.id
                                  AND batch.tenant_id = ?
                                  AND batch.receipt_id = ?::uuid
                                """,
                        actorId, reason, actorId, reason, tenantId, receiptId);
                jdbc.update("""
                                UPDATE hardware_outbox outbox
                                SET status = CASE
                                      WHEN outbox.status IN ('FAILED', 'DLQ_REVIEW') THEN 'PENDING'
                                      ELSE outbox.status
                                    END,
                                    available_at = CASE
                                      WHEN outbox.status IN ('FAILED', 'DLQ_REVIEW') THEN now()
                                      ELSE outbox.available_at
                                    END,
                                    updated_at = now()
                                FROM hardware_upload_batch batch
                                WHERE outbox.upload_batch_id = batch.id
                                  AND batch.tenant_id = ?
                                  AND batch.receipt_id = ?::uuid
                                  AND outbox.status <> 'PUBLISHED'
                                """,
                        tenantId, receiptId);
                return receipts.get(0);
            });
            if (result == null) {
                throw unavailable();
            }
            return result;
        } catch (DeviceRequestException exception) {
            throw exception;
        } catch (DataAccessException | TransactionException exception) {
            throw unavailable(exception);
        }
    }

    private TelemetryBatchResponse writeTransaction(
            DeviceClaims claims,
            TelemetryBatchRequest request,
            TelemetryValidationResult validation
    ) {
        jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                (resultSet, rowNumber) -> 0,
                claims.tenantId() + "\u001f" + claims.userId() + "\u001f"
                        + claims.deviceId() + "\u001f" + request.batchId);
        TelemetryBatchResponse replay = findReplay(claims, request.batchId);
        if (replay != null) {
            return replay;
        }

        UUID batchDatabaseId = UUID.randomUUID();
        UUID receiptId = UUID.randomUUID();
        Instant receivedAt = Instant.now();
        int rejectedCount = rejectedCount(request.quality);
        int inserted = jdbc.update("""
                        INSERT INTO hardware_upload_batch (
                          id, receipt_id, tenant_id, user_id, device_id, batch_id,
                          source, collected_from, collected_to, received_at, status,
                          record_count, measurement_count, sleep_session_count,
                          activity_count, signal_metadata_count, quality_summary
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'RECEIVED', ?, ?, ?, ?, ?, ?::jsonb)
                        ON CONFLICT (tenant_id, user_id, device_id, batch_id) DO NOTHING
                        """,
                batchDatabaseId, receiptId, claims.tenantId(), claims.userId(), claims.deviceId(),
                request.batchId, request.source, timestamp(request.collectedFrom),
                timestamp(request.collectedTo), Timestamp.from(receivedAt), validation.recordCount(),
                validation.measurementCount(), validation.sleepSessionCount(),
                validation.activitySessionCount(), validation.signalChunkCount(),
                json(request.quality == null ? Map.of() : request.quality));
        if (inserted == 0) {
            TelemetryBatchResponse concurrentReplay = findReplay(claims, request.batchId);
            if (concurrentReplay != null) {
                return concurrentReplay;
            }
            throw unavailable();
        }

        jdbc.update("""
                        INSERT INTO hardware_reconciliation (
                          id, upload_batch_id, tenant_id, state
                        ) VALUES (?, ?, ?, 'RECEIVED')
                        """,
                UUID.randomUUID(), batchDatabaseId, claims.tenantId());

        writeMeasurements(batchDatabaseId, claims, request);
        writeSleep(batchDatabaseId, claims, request);
        writeActivities(batchDatabaseId, claims, request);
        int qualityEventCount = writeQualityEvents(batchDatabaseId, claims, request, receivedAt);

        jdbc.update("""
                        UPDATE hardware_upload_batch
                        SET status = 'PERSISTED', committed_at = now()
                        WHERE id = ?
                        """,
                batchDatabaseId);
        jdbc.update("""
                        UPDATE hardware_reconciliation
                        SET state = 'PERSISTED', updated_at = now()
                        WHERE upload_batch_id = ?
                        """,
                batchDatabaseId);

        writeOutbox(batchDatabaseId, receiptId, claims, request, validation,
                receivedAt, PERSISTED_EVENT, rejectedCount, qualityEventCount);
        if (qualityEventCount > 0 || rejectedCount > 0) {
            writeOutbox(batchDatabaseId, receiptId, claims, request, validation,
                    receivedAt, QUALITY_EVENT, rejectedCount, qualityEventCount);
        }

        jdbc.update("""
                        UPDATE hardware_upload_batch
                        SET status = 'EVENT_PENDING'
                        WHERE id = ?
                        """,
                batchDatabaseId);
        jdbc.update("""
                        UPDATE hardware_reconciliation
                        SET state = 'EVENT_PENDING', updated_at = now()
                        WHERE upload_batch_id = ?
                        """,
                batchDatabaseId);
        return response(request.batchId, receiptId.toString(), false, validation, rejectedCount);
    }

    private TelemetryBatchResponse findReplay(DeviceClaims claims, String batchId) {
        List<TelemetryBatchResponse> matches = jdbc.query("""
                        SELECT receipt_id::text, record_count, measurement_count,
                               sleep_session_count, activity_count, signal_metadata_count,
                               quality_summary
                        FROM hardware_upload_batch
                        WHERE tenant_id = ? AND user_id = ? AND device_id = ? AND batch_id = ?
                        """,
                (row, index) -> {
                    TelemetryValidationResult storedCounts = new TelemetryValidationResult(
                            List.of(),
                            row.getInt("measurement_count"),
                            row.getInt("sleep_session_count"),
                            row.getInt("activity_count"),
                            row.getInt("signal_metadata_count"));
                    return response(
                            batchId,
                            row.getString("receipt_id"),
                            true,
                            storedCounts,
                            rejectedCount(row.getString("quality_summary")));
                },
                claims.tenantId(), claims.userId(), claims.deviceId(), batchId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private void writeMeasurements(
            UUID batchId,
            DeviceClaims claims,
            TelemetryBatchRequest request
    ) {
        List<MeasurementRecord> records = request.measurements == null ? List.of() : request.measurements;
        for (int index = 0; index < records.size(); index++) {
            MeasurementRecord record = records.get(index);
            jdbc.update("""
                            INSERT INTO hardware_measurement (
                              id, upload_batch_id, tenant_id, user_id, device_id,
                              source_record_id, metric_type, observed_at, primary_value,
                              secondary_value, unit, quality_code, source
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    UUID.randomUUID(), batchId, claims.tenantId(), claims.userId(), claims.deviceId(),
                    sourceId(record.id, request.batchId, "measurement", index), record.metricType,
                    timestamp(record.measuredAt), record.primaryValue, record.secondaryValue,
                    record.unit, record.qualityCode, record.source);
        }
    }

    private void writeSleep(UUID batchId, DeviceClaims claims, TelemetryBatchRequest request) {
        List<SleepSessionRecord> records =
                request.sleepSessions == null ? List.of() : request.sleepSessions;
        for (int index = 0; index < records.size(); index++) {
            SleepSessionRecord record = records.get(index);
            jdbc.update("""
                            INSERT INTO hardware_sleep_session (
                              id, upload_batch_id, tenant_id, user_id, device_id,
                              source_record_id, started_at, ended_at, deep_minutes,
                              light_minutes, awake_minutes, rem_minutes,
                              interruption_minutes, source
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    UUID.randomUUID(), batchId, claims.tenantId(), claims.userId(), claims.deviceId(),
                    sourceId(record.id, request.batchId, "sleep", index), timestamp(record.startedAt),
                    timestamp(record.endedAt), value(record.deepMinutes), value(record.lightMinutes),
                    value(record.awakeMinutes), value(record.remMinutes),
                    value(record.interruptionMinutes), record.source);
        }
    }

    private void writeActivities(UUID batchId, DeviceClaims claims, TelemetryBatchRequest request) {
        List<ActivitySessionRecord> records =
                request.activitySessions == null ? List.of() : request.activitySessions;
        for (int index = 0; index < records.size(); index++) {
            ActivitySessionRecord record = records.get(index);
            jdbc.update("""
                            INSERT INTO hardware_activity (
                              id, upload_batch_id, tenant_id, user_id, device_id,
                              source_record_id, started_at, ended_at, activity_type,
                              steps, distance_meters, calories_kcal, duration_minutes,
                              average_heart_rate, source
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    UUID.randomUUID(), batchId, claims.tenantId(), claims.userId(), claims.deviceId(),
                    sourceId(record.id, request.batchId, "activity", index),
                    timestamp(record.startedAt), timestamp(record.endedAt), record.activityType,
                    value(record.steps), value(record.distanceMeters), value(record.caloriesKcal),
                    value(record.durationMinutes), record.averageHeartRate, record.source);
        }
    }

    private int writeQualityEvents(
            UUID batchId,
            DeviceClaims claims,
            TelemetryBatchRequest request,
            Instant receivedAt
    ) {
        int count = 0;
        List<MeasurementRecord> records = request.measurements == null ? List.of() : request.measurements;
        for (int index = 0; index < records.size(); index++) {
            MeasurementRecord record = records.get(index);
            if (record.qualityCode == null || record.qualityCode.isBlank()) {
                continue;
            }
            insertQuality(batchId, claims,
                    sourceId(record.id, request.batchId, "measurement", index),
                    "NORMALIZED_RECORD_QUALITY", "WARN", record.qualityCode,
                    Instant.ofEpochMilli(record.measuredAt));
            count++;
        }
        int rejectedCount = rejectedCount(request.quality);
        if (rejectedCount > 0) {
            Object code = request.quality == null ? null : request.quality.get("rejectionCode");
            insertQuality(batchId, claims, digest(request.batchId + ":rejection"),
                    "RECORD_REJECTED", "WARN",
                    code == null ? "CLIENT_REPORTED_REJECTION" : String.valueOf(code),
                    instant(request.collectedTo, receivedAt));
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

    private void writeOutbox(
            UUID batchDatabaseId,
            UUID receiptId,
            DeviceClaims claims,
            TelemetryBatchRequest request,
            TelemetryValidationResult validation,
            Instant receivedAt,
            String eventType,
            int rejectedCount,
            int qualityEventCount
    ) {
        UUID eventId = UUID.randomUUID();
        boolean quality = QUALITY_EVENT.equals(eventType);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("event_type", eventType);
        metadata.put("event_id", eventId.toString());
        metadata.put("batch_id", receiptId.toString());
        metadata.put("schema_id", eventType);
        metadata.put("tenant_ref", opaque(claims.tenantId()));
        metadata.put("user_ref", opaque(claims.userId()));
        metadata.put("device_ref", opaque(claims.deviceId()));
        metadata.put("window_started_at", instant(request.collectedFrom, receivedAt).toString());
        metadata.put("window_ended_at", instant(request.collectedTo, receivedAt).toString());
        metadata.put("record_count", validation.recordCount());
        if (quality) {
            metadata.put("accepted_count", validation.recordCount());
            metadata.put("rejected_count", rejectedCount);
        }
        metadata.put("quality_status",
                qualityEventCount > 0 || rejectedCount > 0 ? "accepted_with_warnings" : "accepted");
        metadata.put("persistence_status", "persisted");
        jdbc.update("""
                        INSERT INTO hardware_outbox (
                          id, upload_batch_id, tenant_id, aggregate_type, aggregate_id,
                          event_type, event_version, status, event_metadata
                        ) VALUES (?, ?, ?, 'TELEMETRY_BATCH', ?, ?, 1, 'PENDING', ?::jsonb)
                        """,
                eventId, batchDatabaseId, claims.tenantId(), receiptId.toString(),
                eventType, json(metadata));
    }

    private TelemetryBatchResponse response(
            String batchId,
            String receiptId,
            boolean duplicate,
            TelemetryValidationResult counts,
            int rejectedCount
    ) {
        TelemetryBatchResponse response = new TelemetryBatchResponse();
        response.batchId = batchId;
        response.receiptId = receiptId;
        response.status = duplicate ? "ACCEPTED_DUPLICATE" : "ACCEPTED_PERSISTED";
        response.accepted = true;
        response.persisted = true;
        response.queued = true;
        response.durableQueue = true;
        response.hardwareDbEnabled = true;
        response.rawSignalUploadEnabled = false;
        response.ingestStage = "EVENT_PENDING";
        response.ingestMode = "durable-outbox";
        response.queueType = "timescale-outbox";
        response.recordCount = counts.recordCount();
        response.measurementCount = counts.measurementCount();
        response.sleepSessionCount = counts.sleepSessionCount();
        response.activitySessionCount = counts.activitySessionCount();
        response.signalChunkCount = counts.signalChunkCount();
        response.rejectedCount = rejectedCount;
        if (duplicate) {
            response.warnings.add(
                    "batch already persisted; original receipt returned without duplicate rows or events");
        }
        return response;
    }

    private int rejectedCount(Map<String, Object> quality) {
        if (quality == null) {
            return 0;
        }
        Object value = quality.get("rejectedCount");
        return value instanceof Number number ? Math.max(number.intValue(), 0) : 0;
    }

    private int rejectedCount(String qualityJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> quality = objectMapper.readValue(qualityJson, Map.class);
            return rejectedCount(quality);
        } catch (JsonProcessingException exception) {
            throw unavailable();
        }
    }

    private String sourceId(String supplied, String batchId, String kind, int index) {
        if (supplied != null && !supplied.isBlank() && supplied.length() <= 128) {
            return supplied;
        }
        return digest((supplied == null ? "" : supplied) + ":" + batchId + ":" + kind + ":" + index);
    }

    private String opaque(String value) {
        return "opaque_" + digest(value);
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw unavailable();
        }
    }

    private Timestamp timestamp(Long epochMillis) {
        return epochMillis == null ? null : Timestamp.from(Instant.ofEpochMilli(epochMillis));
    }

    private Instant instant(Long epochMillis, Instant fallback) {
        return epochMillis == null ? fallback : Instant.ofEpochMilli(epochMillis);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private double value(Double value) {
        return value == null ? 0 : value;
    }

    private DeviceRequestException unavailable() {
        return new DeviceRequestException(
                HttpStatus.SERVICE_UNAVAILABLE, "HARDWARE_PERSISTENCE_UNAVAILABLE");
    }

    private DeviceRequestException unavailable(Throwable cause) {
        return new DeviceRequestException(
                HttpStatus.SERVICE_UNAVAILABLE, "HARDWARE_PERSISTENCE_UNAVAILABLE", cause);
    }
}
