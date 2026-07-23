package com.rehealth.device.adapter;

import com.rehealth.contracts.telemetry.v1.TelemetryBatchRequest;
import com.rehealth.contracts.telemetry.v1.TelemetryValidationResult;
import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.domain.DeviceClaims;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class TimescaleBatchRepository {
    private final JdbcTemplate jdbc;
    private final TelemetryPayloadCodec payloads;

    TimescaleBatchRepository(JdbcTemplate jdbc, TelemetryPayloadCodec payloads) {
        this.jdbc = jdbc;
        this.payloads = payloads;
    }

    void lock(DeviceClaims claims, String batchId) {
        jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                (resultSet, rowNumber) -> 0,
                claims.tenantId() + "\u001f" + claims.userId() + "\u001f"
                        + claims.deviceId() + "\u001f" + batchId);
    }

    StoredTelemetryBatch findReplay(DeviceClaims claims, String batchId) {
        List<StoredTelemetryBatch> matches = jdbc.query("""
                        SELECT receipt_id::text, measurement_count, sleep_session_count,
                               activity_count, signal_metadata_count, quality_summary
                        FROM hardware_upload_batch
                        WHERE tenant_id = ? AND user_id = ? AND device_id = ? AND batch_id = ?
                        """,
                (row, index) -> new StoredTelemetryBatch(
                        row.getString("receipt_id"),
                        new TelemetryValidationResult(
                                List.of(),
                                row.getInt("measurement_count"),
                                row.getInt("sleep_session_count"),
                                row.getInt("activity_count"),
                                row.getInt("signal_metadata_count")),
                        payloads.rejectedCount(row.getString("quality_summary"))),
                claims.tenantId(), claims.userId(), claims.deviceId(), batchId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    boolean insertReceived(
            UUID batchDatabaseId,
            UUID receiptId,
            DeviceClaims claims,
            TelemetryBatchRequest request,
            TelemetryValidationResult validation,
            Instant receivedAt
    ) {
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
                request.batchId, request.source, payloads.timestamp(request.collectedFrom),
                payloads.timestamp(request.collectedTo), Timestamp.from(receivedAt), validation.recordCount(),
                validation.measurementCount(), validation.sleepSessionCount(),
                validation.activitySessionCount(), validation.signalChunkCount(),
                payloads.json(request.quality == null ? Map.of() : request.quality));
        return inserted != 0;
    }

    void createReconciliation(UUID batchDatabaseId, String tenantId) {
        jdbc.update("""
                        INSERT INTO hardware_reconciliation (
                          id, upload_batch_id, tenant_id, state
                        ) VALUES (?, ?, ?, 'RECEIVED')
                        """,
                UUID.randomUUID(), batchDatabaseId, tenantId);
    }

    void markPersisted(UUID batchDatabaseId) {
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
    }

    void writeOutbox(
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
        boolean quality = "rehealth.telemetry.quality.v1".equals(eventType);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("event_type", eventType);
        metadata.put("event_id", eventId.toString());
        metadata.put("batch_id", receiptId.toString());
        metadata.put("schema_id", eventType);
        metadata.put("tenant_ref", payloads.opaque(claims.tenantId()));
        metadata.put("user_ref", payloads.opaque(claims.userId()));
        metadata.put("device_ref", payloads.opaque(claims.deviceId()));
        metadata.put("window_started_at", payloads.instant(request.collectedFrom, receivedAt).toString());
        metadata.put("window_ended_at", payloads.instant(request.collectedTo, receivedAt).toString());
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
                eventType, payloads.json(metadata));
    }

    void markEventPending(UUID batchDatabaseId) {
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
    }

    String replay(String tenantId, String receiptId, String actorId, String reason) {
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
    }
}

record StoredTelemetryBatch(
        String receiptId,
        TelemetryValidationResult counts,
        int rejectedCount
) {
}
