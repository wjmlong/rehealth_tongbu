package org.jeecg.modules.rehealth.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class JdbcSoftwareDbReHealthBusinessRepository implements ReHealthBusinessRepository {
    private static final String DEFAULT_FEATURE_SCHEMA = "cvd-16-v1";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcSoftwareDbReHealthBusinessRepository(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PatientProfileDto savePatientProfile(String userId, PatientProfileDto profile) {
        requireUser(userId);
        if (profile == null) {
            throw new IllegalArgumentException("profile is required");
        }
        Timestamp now = Timestamp.from(Instant.now());
        profile.patientId = userId;
        profile.updatedAt = now.getTime();
        String profileJson = json(profile);
        int updated = jdbcTemplate.update("""
                UPDATE rehealth_patient_profile
                SET profile_json = ?, updated_at = ?
                WHERE user_id = ?
                """, profileJson, now, userId);
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO rehealth_patient_profile (id, user_id, profile_json, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), userId, profileJson, now, now);
        }
        return profile;
    }

    @Override
    public Optional<PatientProfileDto> findPatientProfile(String userId) {
        requireUser(userId);
        return latestJson(
                "SELECT profile_json FROM rehealth_patient_profile WHERE user_id = ? ORDER BY updated_at DESC, id DESC",
                userId,
                PatientProfileDto.class
        );
    }

    @Override
    public HealthInterviewSubmitRequestDto saveHealthInterview(
            String userId,
            HealthInterviewSubmitRequestDto request
    ) {
        requireUser(userId);
        if (request == null || request.answers == null || request.answers.isEmpty()) {
            throw new IllegalArgumentException("interview answers are required");
        }
        Timestamp now = Timestamp.from(Instant.now());
        if (request.generatedAt == null) {
            request.generatedAt = now.getTime();
        }
        jdbcTemplate.update("""
                INSERT INTO rehealth_health_interview (
                    id, user_id, answers_json, baseline_json, created_at
                ) VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId, json(request.answers), json(request), now);
        return request;
    }

    @Override
    public Optional<HealthInterviewSubmitRequestDto> findLatestHealthInterview(String userId) {
        requireUser(userId);
        return latestJson(
                "SELECT baseline_json FROM rehealth_health_interview WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                userId,
                HealthInterviewSubmitRequestDto.class
        );
    }

    @Override
    public void recordModelRequest(String userId, ModelCallAudit audit) {
        requireUser(userId);
        if (audit == null) {
            throw new IllegalArgumentException("model audit is required");
        }
        if (audit.operation() == null || audit.operation().isBlank()) {
            throw new IllegalArgumentException("model operation is required");
        }
        if (audit.outcome() == null || audit.outcome().isBlank()) {
            throw new IllegalArgumentException("model outcome is required");
        }
        jdbcTemplate.update("""
                INSERT INTO rehealth_model_request_log (
                    id, user_id, request_id, operation, model_version, outcome,
                    error_code, latency_ms, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId, audit.correlationId(), audit.operation(),
                audit.modelVersion(), audit.outcome(), audit.errorCode(), audit.latencyMillis(),
                Timestamp.from(Instant.now()));
    }

    @Override
    @Transactional
    public DeviceBindResponseDto recordDeviceBinding(String userId, DeviceBindRequestDto request) {
        requireUser(userId);
        if (request == null || request.deviceId == null || request.deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId is required");
        }
        Timestamp now = Timestamp.from(Instant.now());
        int updated = jdbcTemplate.update("""
                UPDATE rehealth_device_binding
                SET device_name = ?, manufacturer = ?, device_model = ?, model = ?, firmware_version = ?,
                    hardware_address_hash = ?, status = 'BOUND', updated_at = ?
                WHERE user_id = ? AND device_id = ?
                """, request.deviceName, request.manufacturer, request.model, request.model, request.firmwareVersion,
                request.hardwareAddressHash, now, userId, request.deviceId);
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO rehealth_device_binding (
                        id, user_id, device_id, device_name, manufacturer, device_model, model,
                        firmware_version, hardware_address_hash, status, bound_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'BOUND', ?, ?)
                    """, UUID.randomUUID().toString(), userId, request.deviceId, request.deviceName,
                    request.manufacturer, request.model, request.model, request.firmwareVersion,
                    request.hardwareAddressHash, now, now);
        }
        DeviceBindResponseDto response = new DeviceBindResponseDto();
        response.deviceId = request.deviceId;
        response.status = "BOUND_PERSISTED";
        response.persisted = true;
        response.persistenceStage = "SOFTWARE_DB_COMMITTED";
        return response;
    }

    @Override
    public boolean hasActiveDeviceBinding(String userId, String deviceId) {
        requireUser(userId);
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM rehealth_device_binding
                WHERE user_id = ? AND device_id = ? AND status = 'BOUND'
                """, Integer.class, userId, deviceId);
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void saveRiskResult(
            String userId,
            String requestId,
            RiskEvaluateRequestDto request,
            RiskEvaluateResponseDto response
    ) {
        requireUser(userId);
        if (request == null || response == null) {
            throw new IllegalArgumentException("risk request and response are required");
        }
        Timestamp now = Timestamp.from(Instant.now());
        String effectiveRequestId = requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString()
                : requestId;
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rehealth_cvd_risk_result WHERE user_id = ? AND request_id = ?",
                Integer.class,
                userId,
                effectiveRequestId
        );
        if (existing != null && existing > 0) {
            return;
        }
        String featureSchemaVersion = firstText(
                response.modelTrace == null ? null : response.modelTrace.featureSchemaVersion,
                DEFAULT_FEATURE_SCHEMA
        );
        String modelVersion = requireText(firstText(
                response.modelVersion,
                response.modelTrace == null ? null : response.modelTrace.modelVersion
        ), "model version is required");
        if (response.riskScore == null || response.riskLevel == null || response.riskLevel.isBlank()) {
            throw new IllegalArgumentException("risk score and level are required");
        }
        String featureId = UUID.randomUUID().toString();
        String featureJson = json(request.featureVector);
        String qualityJson = request.featureVector == null ? null : json(request.featureVector.featureQuality);
        jdbcTemplate.update("""
                INSERT INTO rehealth_cvd_feature_vector (
                    id, user_id, request_id, feature_schema_version,
                    feature_json, quality_json, payload_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, featureId, userId, effectiveRequestId, featureSchemaVersion,
                featureJson, qualityJson, json(request), now);
        jdbcTemplate.update("""
                INSERT INTO rehealth_cvd_risk_result (
                    id, feature_vector_id, user_id, request_id, feature_schema_version,
                    model_version, scorer_mode, is_mock, artifact_name, contribution_method,
                    risk_score, risk_level, contribution_json, missing_fields_json,
                    quality_warnings_json, summary, response_json, evaluated_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), featureId, userId, effectiveRequestId,
                featureSchemaVersion, modelVersion,
                response.modelTrace == null ? null : response.modelTrace.scorerMode,
                response.isMock,
                response.modelTrace == null ? null : response.modelTrace.artifactName,
                null,
                response.riskScore,
                response.riskLevel,
                json(response.featureContributions),
                json(response.missingFields),
                json(response.qualityWarnings),
                response.summary,
                json(response), now, now);
    }

    @Override
    public Optional<RiskEvaluateResponseDto> findLatestRiskResult(String userId) {
        requireUser(userId);
        return latestJson(
                "SELECT response_json FROM rehealth_cvd_risk_result WHERE user_id = ? ORDER BY evaluated_at DESC, id DESC",
                userId,
                RiskEvaluateResponseDto.class
        );
    }

    @Override
    public List<AttributionEventsRequestDto.AttributionHistoryPointDto> findAttributionHistory(String userId) {
        requireUser(userId);
        List<PersistedRiskPoint> risks = jdbcTemplate.query("""
                SELECT evaluated_at, risk_score
                FROM rehealth_cvd_risk_result
                WHERE user_id = ?
                ORDER BY evaluated_at DESC, id DESC
                LIMIT 90
                """, (resultSet, rowNum) -> new PersistedRiskPoint(
                resultSet.getTimestamp("evaluated_at"),
                resultSet.getDouble("risk_score")
        ), userId);
        Collections.reverse(risks);
        Timestamp firstPlanAt = jdbcTemplate.query(
                "SELECT MIN(generated_at) FROM rehealth_intervention_plan WHERE user_id = ?",
                resultSet -> resultSet.next() ? resultSet.getTimestamp(1) : null,
                userId
        );
        Map<String, AttributionEventsRequestDto.AttributionHistoryPointDto> byDate = new LinkedHashMap<>();
        for (PersistedRiskPoint risk : risks) {
            if (risk.evaluatedAt() == null) {
                continue;
            }
            AttributionEventsRequestDto.AttributionHistoryPointDto point =
                    new AttributionEventsRequestDto.AttributionHistoryPointDto();
            point.date = risk.evaluatedAt().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString();
            point.riskScore = risk.riskScore();
            point.intervention = firstPlanAt != null && !risk.evaluatedAt().before(firstPlanAt) ? 1 : 0;
            byDate.put(point.date, point);
        }
        return new ArrayList<>(byDate.values());
    }

    @Override
    public void saveInterventionPlan(String userId, InterventionGenerateResponseDto response) {
        requireUser(userId);
        if (response == null) {
            throw new IllegalArgumentException("intervention response is required");
        }
        String planId = requireText(response.planId, "plan id is required");
        String modelVersion = requireText(response.modelVersion, "model version is required");
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rehealth_intervention_plan WHERE user_id = ? AND plan_id = ?",
                Integer.class,
                userId,
                planId
        );
        if (existing != null && existing > 0) {
            return;
        }
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO rehealth_intervention_plan (
                    id, user_id, plan_id, source_request_id, feature_schema_version,
                    model_version, scorer_mode, is_mock, artifact_name,
                    generated_at, response_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId, planId, null, null,
                modelVersion, null, response.isMock, null,
                parseTimestamp(response.generatedAt, now), json(response), now);
    }

    @Override
    public Optional<InterventionGenerateResponseDto> findLatestInterventionPlan(String userId) {
        requireUser(userId);
        return latestJson(
                "SELECT response_json FROM rehealth_intervention_plan WHERE user_id = ? ORDER BY generated_at DESC, id DESC",
                userId,
                InterventionGenerateResponseDto.class
        );
    }

    @Override
    public void saveFeedback(String userId, String interventionId, FeedbackRequestDto request) {
        requireUser(userId);
        String planId = requireText(interventionId, "intervention id is required");
        if (request == null) {
            throw new IllegalArgumentException("feedback request is required");
        }
        String status = requireText(request.status, "feedback status is required");
        List<String> planRecords = jdbcTemplate.query(
                "SELECT id FROM rehealth_intervention_plan WHERE user_id = ? AND plan_id = ?",
                (resultSet, rowNum) -> resultSet.getString(1),
                userId,
                planId
        );
        if (planRecords.isEmpty()) {
            throw new IllegalArgumentException("intervention plan is not owned by the authenticated user");
        }
        Timestamp now = Timestamp.from(Instant.now());
        String idempotencyKey = feedbackKey(userId, planId, request);
        try {
            jdbcTemplate.update("""
                    INSERT INTO rehealth_intervention_feedback (
                        id, user_id, plan_record_id, plan_id, intervention_id,
                        idempotency_key, status, adherence, note, checked_at, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), userId, planRecords.get(0), planId, planId,
                    idempotencyKey, status, request.adherence, request.note,
                    request.checkedAt == null ? null : new Timestamp(request.checkedAt), now);
        } catch (DuplicateKeyException race) {
            Integer duplicateCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM rehealth_intervention_feedback WHERE user_id = ? AND idempotency_key = ?",
                    Integer.class,
                    userId,
                    idempotencyKey
            );
            if (duplicateCount == null || duplicateCount == 0) {
                throw race;
            }
        }
    }

    @Override
    public void recordAttributionResult(
            String userId,
            AttributionEventsRequestDto request,
            AttributionResponseDto response
    ) {
        requireUser(userId);
        jdbcTemplate.update("""
                INSERT INTO rehealth_attribution_result (
                    id, user_id, status, model_version, request_json, response_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId,
                response == null ? null : response.status,
                response == null ? null : response.modelVersion,
                json(request), json(response), Timestamp.from(Instant.now()));
    }

    private <T> Optional<T> latestJson(String sql, String userId, Class<T> type) {
        List<String> rows = jdbcTemplate.query(sql, (resultSet, rowNum) -> resultSet.getString(1), userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readValue(rows.get(0), type));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("software_db contains an unreadable persisted response", e);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("software_db payload must be JSON serializable", e);
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private Timestamp parseTimestamp(String value, Timestamp fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Timestamp.from(Instant.parse(value));
        } catch (RuntimeException ignored) {
            try {
                return Timestamp.from(OffsetDateTime.parse(value).toInstant());
            } catch (RuntimeException invalidTimestamp) {
                throw new IllegalArgumentException("generatedAt must be an ISO-8601 timestamp", invalidTimestamp);
            }
        }
    }

    private String feedbackKey(String userId, String planId, FeedbackRequestDto request) {
        String material = String.join("|",
                userId,
                planId,
                String.valueOf(request.checkedAt),
                String.valueOf(request.status),
                String.valueOf(request.adherence),
                String.valueOf(request.note)
        );
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private void requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("authenticated userId is required");
        }
    }

    private record PersistedRiskPoint(Timestamp evaluatedAt, double riskScore) {
    }
}
