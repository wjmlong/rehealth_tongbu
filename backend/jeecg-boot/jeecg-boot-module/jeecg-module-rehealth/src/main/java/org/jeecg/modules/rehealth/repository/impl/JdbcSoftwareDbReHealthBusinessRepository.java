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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class JdbcSoftwareDbReHealthBusinessRepository implements ReHealthBusinessRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcSoftwareDbReHealthBusinessRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
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
    public void recordModelRequest(
            String userId,
            String requestId,
            String operation,
            String modelVersion,
            String outcome
    ) {
        requireUser(userId);
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("model operation is required");
        }
        if (outcome == null || outcome.isBlank()) {
            throw new IllegalArgumentException("model outcome is required");
        }
        jdbcTemplate.update("""
                INSERT INTO rehealth_model_request_log (
                    id, user_id, request_id, operation, model_version, outcome, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId, requestId, operation,
                modelVersion, outcome, Timestamp.from(Instant.now()));
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
                SET device_name = ?, manufacturer = ?, model = ?, firmware_version = ?,
                    hardware_address_hash = ?, status = 'BOUND', updated_at = ?
                WHERE user_id = ? AND device_id = ?
                """, request.deviceName, request.manufacturer, request.model, request.firmwareVersion,
                request.hardwareAddressHash, now, userId, request.deviceId);
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO rehealth_device_binding (
                        id, user_id, device_id, device_name, manufacturer, model,
                        firmware_version, hardware_address_hash, status, bound_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'BOUND', ?, ?)
                    """, UUID.randomUUID().toString(), userId, request.deviceId, request.deviceName,
                    request.manufacturer, request.model, request.firmwareVersion,
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
    @Transactional
    public void saveRiskResult(
            String userId,
            String requestId,
            RiskEvaluateRequestDto request,
            RiskEvaluateResponseDto response
    ) {
        requireUser(userId);
        Timestamp now = Timestamp.from(Instant.now());
        String effectiveRequestId = requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString()
                : requestId;
        jdbcTemplate.update("""
                INSERT INTO rehealth_cvd_feature_vector (id, user_id, request_id, payload_json, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId, effectiveRequestId, json(request), now);
        jdbcTemplate.update("""
                INSERT INTO rehealth_cvd_risk_result (
                    id, user_id, request_id, risk_score, risk_level, model_version, response_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId, effectiveRequestId,
                response == null ? null : response.riskScore,
                response == null ? null : response.riskLevel,
                response == null ? null : response.modelVersion,
                json(response), now);
    }

    @Override
    public Optional<RiskEvaluateResponseDto> findLatestRiskResult(String userId) {
        requireUser(userId);
        return latestJson(
                "SELECT response_json FROM rehealth_cvd_risk_result WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                userId,
                RiskEvaluateResponseDto.class
        );
    }

    @Override
    public void saveInterventionPlan(String userId, InterventionGenerateResponseDto response) {
        requireUser(userId);
        jdbcTemplate.update("""
                INSERT INTO rehealth_intervention_plan (
                    id, user_id, plan_id, model_version, response_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId,
                response == null ? null : response.planId,
                response == null ? null : response.modelVersion,
                json(response), Timestamp.from(Instant.now()));
    }

    @Override
    public Optional<InterventionGenerateResponseDto> findLatestInterventionPlan(String userId) {
        requireUser(userId);
        return latestJson(
                "SELECT response_json FROM rehealth_intervention_plan WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                userId,
                InterventionGenerateResponseDto.class
        );
    }

    @Override
    public void saveFeedback(String userId, String interventionId, FeedbackRequestDto request) {
        requireUser(userId);
        jdbcTemplate.update("""
                INSERT INTO rehealth_intervention_feedback (
                    id, user_id, intervention_id, status, adherence, note, checked_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId, interventionId,
                request == null ? null : request.status,
                request == null ? null : request.adherence,
                request == null ? null : request.note,
                request == null || request.checkedAt == null ? null : new Timestamp(request.checkedAt),
                Timestamp.from(Instant.now()));
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

    private void requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("authenticated userId is required");
        }
    }
}
