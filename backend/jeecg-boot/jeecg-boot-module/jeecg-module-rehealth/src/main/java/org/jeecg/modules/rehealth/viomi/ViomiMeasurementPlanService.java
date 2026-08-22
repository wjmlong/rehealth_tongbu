package org.jeecg.modules.rehealth.viomi;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jeecg.modules.rehealth.mobile.dto.ViomiMeasurementPlanRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.ViomiMeasurementPlanResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.ViomiSyncRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.ViomiSyncResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = {"rehealth.software-db.enabled", "rehealth.viomi.enabled"},
        havingValue = "true"
)
public class ViomiMeasurementPlanService {
    private static final Set<String> ALLOWED_METRICS = Set.of("HEART_RATE", "BLOOD_PRESSURE", "BLOOD_OXYGEN");
    private static final Set<String> DEFAULT_METRICS = Set.of("HEART_RATE", "BLOOD_OXYGEN");
    private static final long[] POLL_DELAYS_MILLIS = {5_000L, 10_000L, 15_000L, 30_000L};
    private final JdbcTemplate jdbc;
    private final ViomiPlanSecretCodec secretCodec;
    private final ViomiOpenApiGateway gateway;
    private final ViomiPullService pullService;
    private final ReHealthBusinessRepository businessRepository;

    public ViomiMeasurementPlanService(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            ViomiPlanSecretCodec secretCodec,
            ViomiOpenApiGateway gateway,
            ViomiPullService pullService,
            ReHealthBusinessRepository businessRepository
    ) {
        this.jdbc = jdbc;
        this.secretCodec = secretCodec;
        this.gateway = gateway;
        this.pullService = pullService;
        this.businessRepository = businessRepository;
    }

    public ViomiMeasurementPlanResponseDto save(String userId, ViomiMeasurementPlanRequestDto request) {
        if (request == null) throw new IllegalArgumentException("measurement plan is required");
        String imei = requireImei(request.imei);
        if (request.intervalMinutes < 3 || request.intervalMinutes > 60) {
            throw new IllegalArgumentException("measurement interval must be between 3 and 60 minutes");
        }
        Set<String> metrics = request.metrics == null || request.metrics.isEmpty()
                ? new LinkedHashSet<>(DEFAULT_METRICS) : new LinkedHashSet<>(request.metrics);
        if (!ALLOWED_METRICS.containsAll(metrics)) throw new IllegalArgumentException("unsupported Viomi metric requested");
        String deviceId = ViomiPullService.deviceId(imei);
        if (!businessRepository.hasActiveDeviceBinding(userId, deviceId)) {
            throw new SecurityException("Viomi device is not bound to the current user");
        }
        byte[] encryptedImei = secretCodec.encrypt(imei);
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp next = request.enabled ? now : null;
        String id = UUID.nameUUIDFromBytes(
                (userId + "|" + deviceId).getBytes(StandardCharsets.UTF_8)
        ).toString();
        jdbc.update("""
                INSERT INTO rehealth_viomi_measurement_plan (
                    id, user_id, device_id, imei_ciphertext, enabled, interval_minutes,
                    metrics_csv, last_status, next_run_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?, ?)
                ON DUPLICATE KEY UPDATE imei_ciphertext=VALUES(imei_ciphertext),
                    enabled=VALUES(enabled), interval_minutes=VALUES(interval_minutes),
                    metrics_csv=VALUES(metrics_csv), last_status='PENDING', last_error=NULL,
                    next_run_at=VALUES(next_run_at), updated_at=VALUES(updated_at)
                """, id, userId, deviceId, encryptedImei, request.enabled, request.intervalMinutes,
                String.join(",", metrics), next, now, now);
        ViomiMeasurementPlanResponseDto response = new ViomiMeasurementPlanResponseDto();
        response.deviceId = deviceId;
        response.enabled = request.enabled;
        response.intervalMinutes = request.intervalMinutes;
        response.status = "PENDING";
        response.nextRunAt = next == null ? null : next.getTime();
        return response;
    }

    @Scheduled(fixedDelayString = "${rehealth.viomi.scheduler-poll-ms:30000}")
    public void runDuePlans() {
        List<PlanRow> due = jdbc.query("""
                SELECT id, user_id, device_id, imei_ciphertext, interval_minutes, metrics_csv
                FROM rehealth_viomi_measurement_plan
                WHERE enabled=1 AND next_run_at <= CURRENT_TIMESTAMP(3)
                ORDER BY next_run_at ASC LIMIT 10
                """, ViomiMeasurementPlanService::mapPlan);
        for (PlanRow plan : due) runClaimed(plan);
    }

    private void runClaimed(PlanRow plan) {
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp leaseUntil = Timestamp.from(Instant.now().plus(Duration.ofMinutes(10)));
        int claimed = jdbc.update("""
                UPDATE rehealth_viomi_measurement_plan
                SET last_status='RUNNING', last_error=NULL, last_run_at=?, next_run_at=?, updated_at=?
                WHERE id=? AND enabled=1 AND next_run_at <= ?
                """, now, leaseUntil, now, plan.id, now);
        if (claimed != 1) return;
        try {
            ViomiSyncResponseDto result = activeMeasure(
                    plan.userId,
                    secretCodec.decrypt(plan.encryptedImei),
                    plan.metrics
            );
            jdbc.update("""
                    UPDATE rehealth_viomi_measurement_plan SET last_status=?, last_error=NULL,
                        next_run_at=?, updated_at=CURRENT_TIMESTAMP(3) WHERE id=?
                    """, result.status, nextRun(plan.intervalMinutes), plan.id);
        } catch (SecurityException e) {
            jdbc.update("""
                    UPDATE rehealth_viomi_measurement_plan
                    SET enabled=0, last_status='DISABLED', last_error=?, next_run_at=NULL,
                        updated_at=CURRENT_TIMESTAMP(3) WHERE id=?
                    """, safeError(e), plan.id);
        } catch (Exception e) {
            jdbc.update("""
                    UPDATE rehealth_viomi_measurement_plan
                    SET last_status='FAILED', last_error=?, next_run_at=?,
                        updated_at=CURRENT_TIMESTAMP(3) WHERE id=?
                    """, safeError(e), nextRun(plan.intervalMinutes), plan.id);
        }
    }

    ViomiSyncResponseDto activeMeasure(String userId, String imei, Set<String> metrics) {
        String deviceId = ViomiPullService.deviceId(imei);
        if (!businessRepository.hasActiveDeviceBinding(userId, deviceId)) {
            throw new SecurityException("Viomi device binding is no longer active");
        }
        ViomiSyncResponseDto aggregate = new ViomiSyncResponseDto();
        aggregate.deviceId = deviceId;
        int failedMetrics = 0;
        for (String metric : metrics) {
            Instant commandAt = Instant.now();
            long baseline = latestTimestamp(metric, imei, commandAt.minusSeconds(60), commandAt);
            gateway.sendMeasurementCommand(metric, imei);
            boolean hasNewResult = false;
            for (long delay : POLL_DELAYS_MILLIS) {
                sleep(delay);
                Instant end = Instant.now();
                if (latestTimestamp(metric, imei, commandAt.minusSeconds(30), end) > baseline) {
                    hasNewResult = true;
                    break;
                }
            }
            if (!hasNewResult) {
                failedMetrics++;
                continue;
            }
            ViomiSyncRequestDto request = new ViomiSyncRequestDto();
            request.imei = imei;
            request.beginAt = commandAt.minusSeconds(60).toEpochMilli();
            request.endAt = Instant.now().plusSeconds(1).toEpochMilli();
            request.metrics = new LinkedHashSet<>(Set.of(metric));
            ViomiSyncResponseDto result = pullService.sync(userId, request);
            aggregate.measurements.addAll(result.measurements);
        }
        aggregate.recordCount = aggregate.measurements.size();
        aggregate.persisted = aggregate.recordCount > 0;
        if (!aggregate.persisted) throw new IllegalStateException("Viomi measurement result timeout");
        aggregate.status = failedMetrics == 0 ? "SUCCESS" : "PARTIAL";
        return aggregate;
    }

    private long latestTimestamp(String metric, String imei, Instant begin, Instant end) {
        long latest = 0L;
        for (JsonNode row : gateway.history(metric, imei, begin, end)) {
            ViomiSyncResponseDto.Measurement normalized = ViomiPullService.normalize(
                    ViomiPullService.deviceId(imei), metric, row);
            if (normalized != null) latest = Math.max(latest, normalized.measuredAt);
        }
        return latest;
    }

    private static PlanRow mapPlan(ResultSet rs, int ignored) throws SQLException {
        return new PlanRow(
                rs.getString("id"), rs.getString("user_id"), rs.getString("device_id"),
                rs.getBytes("imei_ciphertext"), rs.getInt("interval_minutes"),
                new LinkedHashSet<>(List.of(rs.getString("metrics_csv").split(",")))
        );
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Viomi measurement polling interrupted", e);
        }
    }

    private static Timestamp nextRun(int intervalMinutes) {
        return Timestamp.from(Instant.now().plus(Duration.ofMinutes(intervalMinutes)));
    }

    private static String requireImei(String value) {
        String imei = value == null ? "" : value.trim();
        if (!imei.matches("[0-9]{8,32}")) throw new IllegalArgumentException("IMEI must contain 8 to 32 digits");
        return imei;
    }

    private static String safeError(Exception error) {
        String value = error.getMessage();
        if (value == null || value.isBlank()) value = error.getClass().getSimpleName();
        String sanitized = value.replaceAll("[0-9]{8,32}", "[device]");
        return sanitized.substring(0, Math.min(512, sanitized.length()));
    }

    private record PlanRow(
            String id, String userId, String deviceId, byte[] encryptedImei,
            int intervalMinutes, Set<String> metrics
    ) {}
}
