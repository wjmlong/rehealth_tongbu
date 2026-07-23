package com.rehealth.device.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;
import java.util.UUID;

@Component
@Primary
@ConditionalOnProperty(name = "rehealth.hardware-db.enabled", havingValue = "true")
public class TimescaleTelemetryStore implements TelemetryWritePort {
    private static final String PERSISTED_EVENT = "rehealth.telemetry.persisted.v1";
    private static final String QUALITY_EVENT = "rehealth.telemetry.quality.v1";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final TimescaleBatchRepository batches;
    private final TimescaleRecordWriter records;
    private final TelemetryPayloadCodec payloads;

    public TimescaleTelemetryStore(
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            ObjectMapper objectMapper
    ) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.payloads = new TelemetryPayloadCodec(objectMapper);
        this.batches = new TimescaleBatchRepository(jdbc, payloads);
        this.records = new TimescaleRecordWriter(jdbc, payloads);
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
            String result = transactions.execute(
                    status -> batches.replay(tenantId, receiptId, actorId, reason));
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
        batches.lock(claims, request.batchId);
        StoredTelemetryBatch replay = batches.findReplay(claims, request.batchId);
        if (replay != null) {
            return response(request.batchId, replay.receiptId(), true,
                    replay.counts(), replay.rejectedCount());
        }

        UUID batchDatabaseId = UUID.randomUUID();
        UUID receiptId = UUID.randomUUID();
        Instant receivedAt = Instant.now();
        int rejectedCount = payloads.rejectedCount(request.quality);
        boolean inserted = batches.insertReceived(
                batchDatabaseId, receiptId, claims, request, validation, receivedAt);
        if (!inserted) {
            StoredTelemetryBatch concurrentReplay = batches.findReplay(claims, request.batchId);
            if (concurrentReplay != null) {
                return response(request.batchId, concurrentReplay.receiptId(), true,
                        concurrentReplay.counts(), concurrentReplay.rejectedCount());
            }
            throw unavailable();
        }

        batches.createReconciliation(batchDatabaseId, claims.tenantId());
        int qualityEventCount = records.write(batchDatabaseId, claims, request, receivedAt);
        batches.markPersisted(batchDatabaseId);

        batches.writeOutbox(batchDatabaseId, receiptId, claims, request, validation,
                receivedAt, PERSISTED_EVENT, rejectedCount, qualityEventCount);
        if (qualityEventCount > 0 || rejectedCount > 0) {
            batches.writeOutbox(batchDatabaseId, receiptId, claims, request, validation,
                    receivedAt, QUALITY_EVENT, rejectedCount, qualityEventCount);
        }

        batches.markEventPending(batchDatabaseId);
        return response(request.batchId, receiptId.toString(), false, validation, rejectedCount);
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

    private DeviceRequestException unavailable() {
        return new DeviceRequestException(
                HttpStatus.SERVICE_UNAVAILABLE, "HARDWARE_PERSISTENCE_UNAVAILABLE");
    }

    private DeviceRequestException unavailable(Throwable cause) {
        return new DeviceRequestException(
                HttpStatus.SERVICE_UNAVAILABLE, "HARDWARE_PERSISTENCE_UNAVAILABLE", cause);
    }
}
