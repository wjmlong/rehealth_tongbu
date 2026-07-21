package org.jeecg.modules.rehealth.ingest.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.hardware-db.enabled", havingValue = "true")
public class JdbcHardwareTelemetryWriter implements HardwareTelemetryWriter {
    private static final String WRITER_TYPE = "jdbc-hardware-db";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public JdbcHardwareTelemetryWriter(
            @Qualifier("rehealthHardwareJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("rehealthHardwareTransactionTemplate") TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public HardwareWriteResult write(TelemetryBatchRequestDto request) {
        try {
            HardwareWriteResult result = transactionTemplate.execute(status -> writeInTransaction(request));
            if (result == null) {
                throw new HardwarePersistenceUnavailableException("hardware_db transaction returned no result");
            }
            return result;
        } catch (DuplicateKeyException race) {
            HardwareWriteResult duplicate = transactionTemplate.execute(status -> findDuplicate(request));
            if (duplicate != null) {
                return duplicate;
            }
            throw new HardwarePersistenceUnavailableException("hardware_db idempotency conflict could not be resolved", race);
        } catch (HardwarePersistenceUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new HardwarePersistenceUnavailableException("hardware_db transaction failed; telemetry was not accepted", e);
        }
    }

    private HardwareWriteResult writeInTransaction(TelemetryBatchRequestDto request) {
        HardwareWriteResult duplicate = findDuplicate(request);
        if (duplicate != null) {
            return duplicate;
        }

        String uploadBatchId = UUID.randomUUID().toString();
        String receiptId = UUID.randomUUID().toString();
        Timestamp now = Timestamp.from(Instant.now());
        int measurementCount = sizeOf(request.measurements);
        int sleepCount = sizeOf(request.sleepSessions);
        int activityCount = sizeOf(request.activitySessions);
        int signalCount = sizeOf(request.signalChunks);
        int recordCount = measurementCount + sleepCount + activityCount + signalCount;

        jdbcTemplate.update("""
                INSERT INTO hardware_upload_batch (
                    id, receipt_id, batch_id, user_id, device_id, source,
                    collected_from, collected_to, received_at, committed_at, status,
                    record_count, measurement_count, sleep_session_count, activity_count,
                    signal_chunk_count, quality_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                uploadBatchId, receiptId, request.batchId, request.userId, request.deviceId, request.source,
                timestamp(request.collectedFrom), timestamp(request.collectedTo), now, now, "COMMITTED",
                recordCount, measurementCount, sleepCount, activityCount, signalCount, qualityJson(request.quality)
        );

        writeMeasurements(uploadBatchId, request, now);
        writeSleepSessions(uploadBatchId, request, now);
        writeActivities(uploadBatchId, request, now);

        HardwareWriteResult result = new HardwareWriteResult();
        result.persisted = true;
        result.duplicate = false;
        result.receiptId = receiptId;
        result.status = "ACCEPTED_PERSISTED";
        result.stage = "HARDWARE_DB_COMMITTED";
        result.writerType = WRITER_TYPE;
        return result;
    }

    private HardwareWriteResult findDuplicate(TelemetryBatchRequestDto request) {
        List<HardwareWriteResult> existing = jdbcTemplate.query("""
                        SELECT receipt_id
                        FROM hardware_upload_batch
                        WHERE user_id = ? AND device_id = ? AND batch_id = ?
                        """,
                (resultSet, rowNum) -> {
                    HardwareWriteResult result = new HardwareWriteResult();
                    result.persisted = true;
                    result.duplicate = true;
                    result.receiptId = resultSet.getString("receipt_id");
                    result.status = "ACCEPTED_DUPLICATE";
                    result.stage = "HARDWARE_DB_IDEMPOTENT_REPLAY";
                    result.writerType = WRITER_TYPE;
                    result.warnings.add("batch already persisted; existing receipt returned without duplicate rows");
                    return result;
                },
                request.userId, request.deviceId, request.batchId
        );
        return existing.isEmpty() ? null : existing.get(0);
    }

    private void writeMeasurements(String uploadBatchId, TelemetryBatchRequestDto request, Timestamp now) {
        for (Map<String, Object> record : valuesOrEmpty(request.measurements)) {
            jdbcTemplate.update("""
                    INSERT INTO hardware_measurement (
                        id, upload_batch_id, client_record_id, user_id, device_id,
                        metric_type, measured_at, primary_value, secondary_value,
                        unit, quality_code, source, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID().toString(), uploadBatchId,
                    TelemetryRecordReader.optionalString(record, "id"), request.userId, request.deviceId,
                    TelemetryRecordReader.requiredString(record, "metricType", "metric_type"),
                    timestamp(TelemetryRecordReader.requiredLong(record, "measuredAt", "measured_at")),
                    TelemetryRecordReader.requiredDouble(record, "primaryValue", "primary_value", "value", "value_num"),
                    TelemetryRecordReader.optionalDouble(record, "secondaryValue", "secondary_value"),
                    TelemetryRecordReader.requiredString(record, "unit"),
                    TelemetryRecordReader.optionalString(record, "quality", "qualityCode", "quality_code"),
                    TelemetryRecordReader.optionalString(record, "source"), now
            );
        }
    }

    private void writeSleepSessions(String uploadBatchId, TelemetryBatchRequestDto request, Timestamp now) {
        for (Map<String, Object> record : valuesOrEmpty(request.sleepSessions)) {
            jdbcTemplate.update("""
                    INSERT INTO hardware_sleep_session (
                        id, upload_batch_id, client_record_id, user_id, device_id,
                        started_at, ended_at, deep_minutes, light_minutes, awake_minutes,
                        rem_minutes, interruption_minutes, source, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID().toString(), uploadBatchId,
                    TelemetryRecordReader.optionalString(record, "id"), request.userId, request.deviceId,
                    timestamp(TelemetryRecordReader.requiredLong(record, "startedAt", "started_at")),
                    timestamp(TelemetryRecordReader.requiredLong(record, "endedAt", "ended_at")),
                    TelemetryRecordReader.integerOrDefault(record, 0, "deepMinutes", "deep_minutes"),
                    TelemetryRecordReader.integerOrDefault(record, 0, "lightMinutes", "light_minutes"),
                    TelemetryRecordReader.integerOrDefault(record, 0, "awakeMinutes", "awake_minutes"),
                    TelemetryRecordReader.integerOrDefault(record, 0, "remMinutes", "rem_minutes"),
                    TelemetryRecordReader.integerOrDefault(record, 0, "interruptionMinutes", "interruption_minutes"),
                    TelemetryRecordReader.optionalString(record, "source"), now
            );
        }
    }

    private void writeActivities(String uploadBatchId, TelemetryBatchRequestDto request, Timestamp now) {
        for (Map<String, Object> record : valuesOrEmpty(request.activitySessions)) {
            jdbcTemplate.update("""
                    INSERT INTO hardware_activity (
                        id, upload_batch_id, client_record_id, user_id, device_id,
                        started_at, ended_at, activity_type, steps, distance_meters,
                        calories_kcal, duration_minutes, average_heart_rate, source, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID().toString(), uploadBatchId,
                    TelemetryRecordReader.optionalString(record, "id"), request.userId, request.deviceId,
                    timestamp(TelemetryRecordReader.requiredLong(record, "startedAt", "started_at")),
                    optionalTimestamp(record, "endedAt", "ended_at"),
                    TelemetryRecordReader.requiredString(record, "activityType", "activity_type"),
                    TelemetryRecordReader.integerOrDefault(record, 0, "steps"),
                    doubleOrDefault(record, 0, "distanceMeters", "distance_meters"),
                    doubleOrDefault(record, 0, "caloriesKcal", "calories_kcal", "calories"),
                    TelemetryRecordReader.integerOrDefault(record, 0, "durationMinutes", "duration_minutes"),
                    TelemetryRecordReader.optionalDouble(record, "averageHeartRate", "average_heart_rate"),
                    TelemetryRecordReader.optionalString(record, "source"), now
            );
        }
    }

    private Timestamp optionalTimestamp(Map<String, Object> record, String... keys) {
        Double value = TelemetryRecordReader.optionalDouble(record, keys);
        return value == null ? null : timestamp(value.longValue());
    }

    private double doubleOrDefault(Map<String, Object> record, double defaultValue, String... keys) {
        Double value = TelemetryRecordReader.optionalDouble(record, keys);
        return value == null ? defaultValue : value;
    }

    private String qualityJson(Map<String, Object> quality) {
        if (quality == null || quality.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(quality);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("batch quality metadata must be JSON serializable", e);
        }
    }

    private Timestamp timestamp(Long epochMillis) {
        return epochMillis == null ? null : new Timestamp(epochMillis);
    }

    private int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private <T> List<T> valuesOrEmpty(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    @Override
    public boolean isDurable() {
        return true;
    }

    @Override
    public String writerType() {
        return WRITER_TYPE;
    }
}
