package org.jeecg.modules.rehealth.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewAnswerDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewBaselineItemDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.ModelTraceDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import java.util.Set;
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
        validateProfile(profile);
        Timestamp now = Timestamp.from(Instant.now());
        profile.patientId = userId;
        profile.updatedAt = now.getTime();
        profile.bmi = calculatedBmi(profile.heightCm, profile.weightKg);
        List<ProfileIdentity> existing = jdbcTemplate.query(
                "SELECT id, profile_version FROM rehealth_patient_profile WHERE user_id = ?",
                (resultSet, rowNum) -> new ProfileIdentity(
                        resultSet.getString("id"),
                        resultSet.getLong("profile_version")
                ),
                userId
        );
        String profileId;
        if (existing.isEmpty()) {
            profileId = UUID.randomUUID().toString();
            profile.version = 1L;
            jdbcTemplate.update("""
                    INSERT INTO rehealth_patient_profile (
                        id, user_id, name, gender, age, height_cm, weight_kg, bmi,
                        family_history, smoking, drinking, diabetes_history, hypertension_history,
                        profile_version, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, profileId, userId, profile.name, profile.gender, profile.age,
                    profile.heightCm, profile.weightKg, profile.bmi, profile.familyHistory,
                    profile.smoking, profile.drinking, profile.diabetesHistory,
                    profile.hypertensionHistory, profile.version, now, now);
        } else {
            ProfileIdentity identity = existing.get(0);
            if (profile.version != null && profile.version.longValue() != identity.version()) {
                throw new OptimisticLockingFailureException("patient profile was updated by another request");
            }
            profileId = identity.id();
            profile.version = identity.version() + 1;
            int updated = jdbcTemplate.update("""
                    UPDATE rehealth_patient_profile
                    SET name = ?, gender = ?, age = ?, height_cm = ?, weight_kg = ?, bmi = ?,
                        family_history = ?, smoking = ?, drinking = ?, diabetes_history = ?,
                        hypertension_history = ?, profile_version = ?, updated_at = ?
                    WHERE id = ? AND profile_version = ?
                    """, profile.name, profile.gender, profile.age, profile.heightCm,
                    profile.weightKg, profile.bmi, profile.familyHistory, profile.smoking,
                    profile.drinking, profile.diabetesHistory, profile.hypertensionHistory,
                    profile.version, now, profileId, identity.version());
            if (updated == 0) {
                throw new OptimisticLockingFailureException("patient profile was updated by another request");
            }
        }
        replaceProfileItems("rehealth_patient_diagnosis", profileId, profile.diagnoses, now);
        replaceProfileItems("rehealth_patient_medication", profileId, profile.medications, now);
        replaceProfileItems("rehealth_patient_allergy", profileId, profile.allergies, now);
        return profile;
    }

    @Override
    public Optional<PatientProfileDto> findPatientProfile(String userId) {
        requireUser(userId);
        List<PersistedProfile> rows = jdbcTemplate.query("""
                        SELECT id, user_id, name, gender, age, height_cm, weight_kg, bmi,
                               family_history, smoking, drinking, diabetes_history,
                               hypertension_history, profile_version, updated_at
                        FROM rehealth_patient_profile
                        WHERE user_id = ?
                        ORDER BY updated_at DESC, id DESC
                        LIMIT 1
                        """,
                (resultSet, rowNum) -> mapProfile(resultSet),
                userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        PersistedProfile persisted = rows.get(0);
        PatientProfileDto profile = persisted.profile();
        profile.diagnoses = profileItems("rehealth_patient_diagnosis", persisted.id());
        profile.medications = profileItems("rehealth_patient_medication", persisted.id());
        profile.allergies = profileItems("rehealth_patient_allergy", persisted.id());
        return Optional.of(profile);
    }

    @Override
    @Transactional
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
        String interviewId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO rehealth_health_interview (
                    id, user_id, generated_at, created_at
                ) VALUES (?, ?, ?, ?)
                """, interviewId, userId, new Timestamp(request.generatedAt), now);
        for (int index = 0; index < request.answers.size(); index++) {
            HealthInterviewAnswerDto answer = request.answers.get(index);
            if (answer == null || answer.content == null || answer.content.isBlank()) {
                throw new IllegalArgumentException("interview answer content is required");
            }
            jdbcTemplate.update("""
                    INSERT INTO rehealth_health_interview_answer (
                        id, interview_id, question_id, topic, content, sort_order
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), interviewId, answer.questionId,
                    answer.topic, answer.content, index);
        }
        List<HealthInterviewBaselineItemDto> baselineItems =
                request.baselineItems == null ? List.of() : request.baselineItems;
        for (int index = 0; index < baselineItems.size(); index++) {
            HealthInterviewBaselineItemDto item = baselineItems.get(index);
            if (item == null) {
                continue;
            }
            jdbcTemplate.update("""
                    INSERT INTO rehealth_health_interview_baseline (
                        id, interview_id, label, item_value, sort_order
                    ) VALUES (?, ?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), interviewId, item.label, item.value, index);
        }
        List<String> focusAreas = request.focusAreas == null ? List.of() : request.focusAreas;
        for (int index = 0; index < focusAreas.size(); index++) {
            String focusArea = focusAreas.get(index);
            if (focusArea == null || focusArea.isBlank()) {
                continue;
            }
            jdbcTemplate.update("""
                    INSERT INTO rehealth_health_interview_focus (
                        id, interview_id, focus_area, sort_order
                    ) VALUES (?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), interviewId, focusArea.strip(), index);
        }
        if (request.profile != null) {
            request.profile = savePatientProfile(
                    userId,
                    mergeProfile(findPatientProfile(userId).orElse(null), request.profile)
            );
        }
        return request;
    }

    @Override
    public Optional<HealthInterviewSubmitRequestDto> findLatestHealthInterview(String userId) {
        requireUser(userId);
        List<InterviewIdentity> rows = jdbcTemplate.query("""
                        SELECT id, generated_at
                        FROM rehealth_health_interview
                        WHERE user_id = ?
                        ORDER BY created_at DESC, id DESC
                        LIMIT 1
                        """,
                (resultSet, rowNum) -> new InterviewIdentity(
                        resultSet.getString("id"), resultSet.getTimestamp("generated_at")
                ),
                userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        InterviewIdentity interview = rows.get(0);
        HealthInterviewSubmitRequestDto restored = new HealthInterviewSubmitRequestDto();
        restored.generatedAt = interview.generatedAt().getTime();
        restored.answers = jdbcTemplate.query("""
                        SELECT question_id, topic, content
                        FROM rehealth_health_interview_answer
                        WHERE interview_id = ?
                        ORDER BY sort_order
                        """, (resultSet, rowNum) -> {
                    HealthInterviewAnswerDto answer = new HealthInterviewAnswerDto();
                    answer.questionId = resultSet.getString("question_id");
                    answer.topic = resultSet.getString("topic");
                    answer.content = resultSet.getString("content");
                    return answer;
                }, interview.id());
        restored.baselineItems = jdbcTemplate.query("""
                        SELECT label, item_value
                        FROM rehealth_health_interview_baseline
                        WHERE interview_id = ?
                        ORDER BY sort_order
                        """, (resultSet, rowNum) -> {
                    HealthInterviewBaselineItemDto item = new HealthInterviewBaselineItemDto();
                    item.label = resultSet.getString("label");
                    item.value = resultSet.getString("item_value");
                    return item;
                }, interview.id());
        restored.focusAreas = jdbcTemplate.query("""
                        SELECT focus_area
                        FROM rehealth_health_interview_focus
                        WHERE interview_id = ?
                        ORDER BY sort_order
                        """, (resultSet, rowNum) -> resultSet.getString("focus_area"), interview.id());
        restored.profile = findPatientProfile(userId).orElse(null);
        return Optional.of(restored);
    }

    private PatientProfileDto mergeProfile(PatientProfileDto existing, PatientProfileDto incoming) {
        if (existing == null) {
            return incoming;
        }
        PatientProfileDto merged = new PatientProfileDto();
        merged.name = incoming.name != null ? incoming.name : existing.name;
        merged.gender = incoming.gender != null ? incoming.gender : existing.gender;
        merged.age = incoming.age != null ? incoming.age : existing.age;
        merged.heightCm = incoming.heightCm != null ? incoming.heightCm : existing.heightCm;
        merged.weightKg = incoming.weightKg != null ? incoming.weightKg : existing.weightKg;
        merged.diagnoses = incoming.diagnoses != null ? incoming.diagnoses : existing.diagnoses;
        merged.medications = incoming.medications != null ? incoming.medications : existing.medications;
        merged.allergies = incoming.allergies != null ? incoming.allergies : existing.allergies;
        merged.familyHistory = incoming.familyHistory != null ? incoming.familyHistory : existing.familyHistory;
        merged.smoking = incoming.smoking != null ? incoming.smoking : existing.smoking;
        merged.drinking = incoming.drinking != null ? incoming.drinking : existing.drinking;
        merged.diabetesHistory = incoming.diabetesHistory != null
                ? incoming.diabetesHistory
                : existing.diabetesHistory;
        merged.hypertensionHistory = incoming.hypertensionHistory != null
                ? incoming.hypertensionHistory
                : existing.hypertensionHistory;
        merged.version = existing.version;
        return merged;
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
                    model_version, scorer_mode, is_mock, artifact_name, fallback_reason, contribution_method,
                    factor_contribution_version, risk_score, risk_level, contribution_json,
                    factor_contribution_json, factor_measured_component_json, factor_control_support_json, missing_fields_json,
                    quality_warnings_json, summary, response_json, evaluated_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), featureId, userId, effectiveRequestId,
                featureSchemaVersion, modelVersion,
                response.modelTrace == null ? null : response.modelTrace.scorerMode,
                response.isMock,
                response.modelTrace == null ? null : response.modelTrace.artifactName,
                response.modelTrace == null ? null : response.modelTrace.fallbackReason,
                null,
                response.factorContributionVersion,
                response.riskScore,
                response.riskLevel,
                json(response.featureContributions),
                json(response.factorContributions),
                json(response.factorMeasuredComponents),
                json(response.factorControlSupportComponents),
                json(response.missingFields),
                json(response.qualityWarnings),
                response.summary,
                json(response), now, now);
    }

    @Override
    public Optional<RiskEvaluateResponseDto> findLatestRiskResult(String userId) {
        requireUser(userId);
        List<RiskEvaluateResponseDto> rows = jdbcTemplate.query("""
                        SELECT request_id, feature_schema_version, model_version, scorer_mode,
                               is_mock, artifact_name, fallback_reason, risk_score, risk_level,
                               contribution_json, factor_contribution_version, factor_contribution_json,
                               factor_measured_component_json, factor_control_support_json,
                               missing_fields_json, quality_warnings_json, summary
                        FROM rehealth_cvd_risk_result
                        WHERE user_id = ?
                        ORDER BY evaluated_at DESC, id DESC
                        LIMIT 1
                        """, (resultSet, rowNum) -> mapRiskResult(resultSet), userId);
        return rows.stream().findFirst();
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
    @Transactional
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
        String planRecordId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO rehealth_intervention_plan (
                    id, user_id, plan_id, source_request_id, feature_schema_version,
                    model_version, scorer_mode, is_mock, artifact_name,
                    priority_intervention, rationale, expected_impact, confidence, medical_disclaimer,
                    generated_at, response_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, planRecordId, userId, planId, null, null,
                modelVersion, null, response.isMock, null,
                response.priorityIntervention, response.rationale, response.expectedImpact,
                response.confidence, response.medicalDisclaimer,
                parseTimestamp(response.generatedAt, now), json(response), now);
        List<String> contraindications = response.contraindications == null ? List.of() : response.contraindications;
        for (int index = 0; index < contraindications.size(); index++) {
            String item = contraindications.get(index);
            if (item == null || item.isBlank()) {
                continue;
            }
            jdbcTemplate.update("""
                    INSERT INTO rehealth_intervention_contraindication (
                        id, plan_record_id, item_value, sort_order
                    ) VALUES (?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), planRecordId, item.strip(), index);
        }
    }

    @Override
    public Optional<InterventionGenerateResponseDto> findLatestInterventionPlan(String userId) {
        requireUser(userId);
        return findIntervention("""
                SELECT id, plan_id, model_version, is_mock, priority_intervention, rationale,
                       expected_impact, confidence, medical_disclaimer, generated_at, response_json
                FROM rehealth_intervention_plan
                WHERE user_id = ?
                ORDER BY generated_at DESC, id DESC
                LIMIT 1
                """, userId);
    }

    @Override
    public Optional<InterventionGenerateResponseDto> findInterventionPlanInWindow(
            String userId,
            Instant startInclusive,
            Instant endExclusive
    ) {
        requireUser(userId);
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("valid intervention time window is required");
        }
        return findIntervention("""
                        SELECT id, plan_id, model_version, is_mock, priority_intervention, rationale,
                               expected_impact, confidence, medical_disclaimer, generated_at, response_json
                        FROM rehealth_intervention_plan
                        WHERE user_id = ? AND generated_at >= ? AND generated_at < ?
                        ORDER BY generated_at DESC, id DESC
                        LIMIT 1
                        """,
                userId,
                Timestamp.from(startInclusive),
                Timestamp.from(endExclusive));
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
                    id, user_id, status, model_version, request_id, attribution_mode, is_mock,
                    provider, history_days, min_history_days, intervention_days,
                    intervention_data_sufficient, current_risk_score, current_risk_level,
                    current_trend, individual_att, trend_delta, adherence_average,
                    interpretation, error_code, retryable, request_json, response_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), userId,
                response == null ? null : response.status,
                response == null ? null : response.modelVersion,
                response == null ? null : response.requestId,
                response == null ? null : response.attributionMode,
                response == null ? null : response.isMock,
                response == null ? null : response.provider,
                response == null ? null : response.historyDays,
                response == null ? null : response.minHistoryDays,
                response == null ? null : response.interventionDays,
                response == null ? null : response.interventionDataSufficient,
                response == null || response.currentState == null ? null : response.currentState.riskScore,
                response == null || response.currentState == null ? null : response.currentState.riskLevel,
                response == null || response.currentState == null ? null : response.currentState.trend,
                response == null || response.interventionEffect == null ? null : response.interventionEffect.individualAtt,
                response == null ? null : response.trendDelta,
                response == null ? null : response.adherenceAverage,
                response == null ? null : response.interpretation,
                response == null ? null : response.errorCode,
                response == null ? null : response.retryable,
                json(request), json(response), Timestamp.from(Instant.now()));
    }

    private PersistedProfile mapProfile(ResultSet resultSet) throws SQLException {
        PatientProfileDto profile = new PatientProfileDto();
        profile.patientId = resultSet.getString("user_id");
        profile.name = resultSet.getString("name");
        profile.gender = resultSet.getString("gender");
        profile.age = nullableInteger(resultSet, "age");
        profile.heightCm = nullableDouble(resultSet, "height_cm");
        profile.weightKg = nullableDouble(resultSet, "weight_kg");
        profile.bmi = nullableDouble(resultSet, "bmi");
        profile.familyHistory = nullableBoolean(resultSet, "family_history");
        profile.smoking = nullableBoolean(resultSet, "smoking");
        profile.drinking = nullableBoolean(resultSet, "drinking");
        profile.diabetesHistory = nullableBoolean(resultSet, "diabetes_history");
        profile.hypertensionHistory = nullableBoolean(resultSet, "hypertension_history");
        profile.version = resultSet.getLong("profile_version");
        profile.updatedAt = resultSet.getTimestamp("updated_at").getTime();
        return new PersistedProfile(resultSet.getString("id"), profile);
    }

    private void replaceProfileItems(String table, String profileId, List<String> values, Timestamp now) {
        Set<String> allowedTables = Set.of(
                "rehealth_patient_diagnosis",
                "rehealth_patient_medication",
                "rehealth_patient_allergy"
        );
        if (!allowedTables.contains(table)) {
            throw new IllegalArgumentException("unsupported patient profile item table");
        }
        List<String> normalized = normalizeProfileItems(values);
        jdbcTemplate.update("DELETE FROM " + table + " WHERE profile_id = ?", profileId);
        for (int index = 0; index < normalized.size(); index++) {
            jdbcTemplate.update(
                    "INSERT INTO " + table +
                            " (id, profile_id, item_value, sort_order, created_at) VALUES (?, ?, ?, ?, ?)",
                    UUID.randomUUID().toString(), profileId, normalized.get(index), index, now
            );
        }
    }

    private List<String> profileItems(String table, String profileId) {
        Set<String> allowedTables = Set.of(
                "rehealth_patient_diagnosis",
                "rehealth_patient_medication",
                "rehealth_patient_allergy"
        );
        if (!allowedTables.contains(table)) {
            throw new IllegalArgumentException("unsupported patient profile item table");
        }
        return jdbcTemplate.query(
                "SELECT item_value FROM " + table + " WHERE profile_id = ? ORDER BY sort_order",
                (resultSet, rowNum) -> resultSet.getString("item_value"),
                profileId
        );
    }

    private void validateProfile(PatientProfileDto profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile is required");
        }
        if (profile.name != null) {
            profile.name = profile.name.strip();
            if (profile.name.length() > 128) {
                throw new IllegalArgumentException("profile name exceeds 128 characters");
            }
        }
        if (profile.gender != null) {
            profile.gender = profile.gender.strip().toLowerCase();
            if (!Set.of("male", "female").contains(profile.gender)) {
                throw new IllegalArgumentException("profile gender must be male or female");
            }
        }
        if (profile.age != null && (profile.age < 1 || profile.age > 120)) {
            throw new IllegalArgumentException("profile age must be between 1 and 120");
        }
        if (profile.heightCm != null && (profile.heightCm < 50.0 || profile.heightCm > 250.0)) {
            throw new IllegalArgumentException("profile heightCm must be between 50 and 250");
        }
        if (profile.weightKg != null && (profile.weightKg < 2.0 || profile.weightKg > 500.0)) {
            throw new IllegalArgumentException("profile weightKg must be between 2 and 500");
        }
        normalizeProfileItems(profile.diagnoses);
        normalizeProfileItems(profile.medications);
        normalizeProfileItems(profile.allergies);
    }

    private List<String> normalizeProfileItems(List<String> values) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > 100) {
            throw new IllegalArgumentException("patient profile item list exceeds 100 entries");
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String item = value.strip();
            if (item.length() > 512) {
                throw new IllegalArgumentException("patient profile item exceeds 512 characters");
            }
            normalized.add(item);
        }
        return normalized;
    }

    private Double calculatedBmi(Double heightCm, Double weightKg) {
        if (heightCm == null || weightKg == null) {
            return null;
        }
        double heightMeters = heightCm / 100.0;
        return Math.round(weightKg / (heightMeters * heightMeters) * 100.0) / 100.0;
    }

    private RiskEvaluateResponseDto mapRiskResult(ResultSet resultSet) throws SQLException {
        RiskEvaluateResponseDto response = new RiskEvaluateResponseDto();
        response.riskScore = nullableDouble(resultSet, "risk_score");
        response.riskLevel = resultSet.getString("risk_level");
        response.modelVersion = resultSet.getString("model_version");
        response.isMock = nullableBoolean(resultSet, "is_mock");
        response.summary = resultSet.getString("summary");
        response.featureContributions = readJson(
                resultSet.getString("contribution_json"),
                new TypeReference<Map<String, Double>>() {},
                new LinkedHashMap<>()
        );
        response.factorContributionVersion = resultSet.getString("factor_contribution_version");
        response.factorContributions = readJson(
                resultSet.getString("factor_contribution_json"),
                new TypeReference<Map<String, Double>>() {},
                new LinkedHashMap<>()
        );
        response.factorMeasuredComponents = readJson(
                resultSet.getString("factor_measured_component_json"),
                new TypeReference<Map<String, Double>>() {},
                new LinkedHashMap<>()
        );
        response.factorControlSupportComponents = readJson(
                resultSet.getString("factor_control_support_json"),
                new TypeReference<Map<String, Double>>() {},
                new LinkedHashMap<>()
        );
        response.missingFields = readJson(
                resultSet.getString("missing_fields_json"),
                new TypeReference<List<String>>() {},
                List.of()
        );
        response.qualityWarnings = readJson(
                resultSet.getString("quality_warnings_json"),
                new TypeReference<List<String>>() {},
                List.of()
        );
        ModelTraceDto trace = new ModelTraceDto();
        trace.requestId = resultSet.getString("request_id");
        trace.featureSchemaVersion = resultSet.getString("feature_schema_version");
        trace.modelVersion = resultSet.getString("model_version");
        trace.scorerMode = resultSet.getString("scorer_mode");
        trace.artifactName = resultSet.getString("artifact_name");
        trace.fallbackReason = resultSet.getString("fallback_reason");
        response.modelTrace = trace;
        return response;
    }

    private Optional<InterventionGenerateResponseDto> findIntervention(String sql, Object... arguments) {
        List<PersistedIntervention> rows = jdbcTemplate.query(sql, (resultSet, rowNum) -> {
            InterventionGenerateResponseDto response = new InterventionGenerateResponseDto();
            response.planId = resultSet.getString("plan_id");
            response.modelVersion = resultSet.getString("model_version");
            response.isMock = nullableBoolean(resultSet, "is_mock");
            response.priorityIntervention = resultSet.getString("priority_intervention");
            response.rationale = resultSet.getString("rationale");
            response.expectedImpact = resultSet.getString("expected_impact");
            response.confidence = nullableDouble(resultSet, "confidence");
            response.medicalDisclaimer = resultSet.getString("medical_disclaimer");
            response.generatedAt = resultSet.getTimestamp("generated_at").toInstant().toString();
            return new PersistedIntervention(
                    resultSet.getString("id"), response, resultSet.getString("response_json")
            );
        }, arguments);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        PersistedIntervention persisted = rows.get(0);
        InterventionGenerateResponseDto response = persisted.response();
        response.contraindications = jdbcTemplate.query("""
                        SELECT item_value
                        FROM rehealth_intervention_contraindication
                        WHERE plan_record_id = ?
                        ORDER BY sort_order
                        """, (resultSet, rowNum) -> resultSet.getString("item_value"), persisted.id());
        mergeLegacyIntervention(response, persisted.snapshotJson());
        return Optional.of(response);
    }

    private void mergeLegacyIntervention(InterventionGenerateResponseDto target, String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return;
        }
        try {
            InterventionGenerateResponseDto legacy =
                    objectMapper.readValue(snapshotJson, InterventionGenerateResponseDto.class);
            target.priorityIntervention = firstText(target.priorityIntervention, legacy.priorityIntervention);
            target.rationale = firstText(target.rationale, legacy.rationale);
            target.expectedImpact = firstText(target.expectedImpact, legacy.expectedImpact);
            target.confidence = target.confidence == null ? legacy.confidence : target.confidence;
            target.medicalDisclaimer = firstText(target.medicalDisclaimer, legacy.medicalDisclaimer);
            target.isMock = target.isMock == null ? legacy.isMock : target.isMock;
            if ((target.contraindications == null || target.contraindications.isEmpty())
                    && legacy.contraindications != null) {
                target.contraindications = legacy.contraindications;
            }
        } catch (JsonProcessingException ignored) {
            // The structured columns remain readable even if a legacy evidence snapshot is malformed.
        }
    }

    private <T> T readJson(String value, TypeReference<T> type, T fallback) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return fallback;
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("software_db contains unreadable structured JSON metadata", e);
        }
    }

    private Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        Number value = (Number) resultSet.getObject(column);
        return value == null ? null : value.intValue();
    }

    private Double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        Number value = (Number) resultSet.getObject(column);
        return value == null ? null : value.doubleValue();
    }

    private Boolean nullableBoolean(ResultSet resultSet, String column) throws SQLException {
        Object value = resultSet.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return ((Number) value).intValue() != 0;
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

    private record ProfileIdentity(String id, long version) {
    }

    private record PersistedProfile(String id, PatientProfileDto profile) {
    }

    private record InterviewIdentity(String id, Timestamp generatedAt) {
    }

    private record PersistedIntervention(
            String id,
            InterventionGenerateResponseDto response,
            String snapshotJson
    ) {
    }
}
