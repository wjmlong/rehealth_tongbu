package org.jeecg.modules.rehealth.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewAnswerDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcSoftwareDbReHealthBusinessRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcSoftwareDbReHealthBusinessRepository repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:software-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/software/mysql/V1__create_rehealth_software_tables.sql")
        ).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcSoftwareDbReHealthBusinessRepository(jdbcTemplate, new ObjectMapper());
    }

    @Test
    void persistsDeviceBindingForAuthenticatedUser() {
        DeviceBindRequestDto request = new DeviceBindRequestDto();
        request.deviceId = "ring-001";
        request.model = "MR11";

        assertTrue(repository.recordDeviceBinding("user-a", request).persisted);
        assertEquals(1, count("rehealth_device_binding"));
        assertEquals("user-a", jdbcTemplate.queryForObject(
                "SELECT user_id FROM rehealth_device_binding WHERE device_id = ?",
                String.class,
                "ring-001"
        ));
        assertEquals("MR11", jdbcTemplate.queryForObject(
                "SELECT device_model FROM rehealth_device_binding WHERE device_id = ?",
                String.class,
                "ring-001"
        ));
        assertEquals("MR11", jdbcTemplate.queryForObject(
                "SELECT model FROM rehealth_device_binding WHERE device_id = ?",
                String.class,
                "ring-001"
        ));
    }

    @Test
    void latestRiskAndInterventionAreIsolatedByUser() {
        saveRisk("user-a", "request-a", 0.21);
        saveRisk("user-b", "request-b", 0.73);
        InterventionGenerateResponseDto plan = new InterventionGenerateResponseDto();
        plan.planId = "plan-a";
        plan.modelVersion = "test-v1";
        plan.generatedAt = "2026-07-23T00:00:00Z";
        repository.saveInterventionPlan("user-a", plan);

        assertEquals(0.21, repository.findLatestRiskResult("user-a").orElseThrow().riskScore);
        assertEquals(0.73, repository.findLatestRiskResult("user-b").orElseThrow().riskScore);
        assertEquals("plan-a", repository.findLatestInterventionPlan("user-a").orElseThrow().planId);
        assertTrue(repository.findLatestInterventionPlan("user-b").isEmpty());
    }

    @Test
    void feedbackRetryIsIdempotentAndRequiresAnOwnedPlan() {
        InterventionGenerateResponseDto plan = new InterventionGenerateResponseDto();
        plan.planId = "plan-a";
        plan.modelVersion = "test-v1";
        plan.generatedAt = "2026-07-23T00:00:00Z";
        repository.saveInterventionPlan("user-a", plan);
        FeedbackRequestDto feedback = new FeedbackRequestDto();
        feedback.status = "COMPLETED";
        feedback.adherence = 1.0;
        feedback.checkedAt = 1_753_228_800_000L;

        repository.saveFeedback("user-a", "plan-a", feedback);
        repository.saveFeedback("user-a", "plan-a", feedback);

        assertEquals(1, count("rehealth_intervention_feedback"));
        assertEquals("plan-a", jdbcTemplate.queryForObject(
                "SELECT plan_id FROM rehealth_intervention_feedback",
                String.class
        ));
    }

    @Test
    void readsLegacySnakeCaseInterventionPayload() {
        InterventionGenerateResponseDto plan = new InterventionGenerateResponseDto();
        plan.planId = "plan-legacy";
        plan.modelVersion = "model-legacy";
        plan.generatedAt = "2026-07-14T09:59:38.206Z";
        repository.saveInterventionPlan("user-a", plan);
        jdbcTemplate.update("""
                UPDATE rehealth_intervention_plan
                SET response_json = ?
                WHERE user_id = ? AND plan_id = ?
                """, """
                {"plan_id":"plan-legacy","generated_at":"2026-07-14T09:59:38.206Z",
                 "priority_intervention":"walking","expected_impact":"lower risk",
                 "model_version":"model-legacy","is_mock":false,
                 "medical_disclaimer":"not a diagnosis"}
                """, "user-a", "plan-legacy");

        InterventionGenerateResponseDto restored =
                repository.findLatestInterventionPlan("user-a").orElseThrow();

        assertEquals("plan-legacy", restored.planId);
        assertEquals("model-legacy", restored.modelVersion);
        assertEquals("lower risk", restored.expectedImpact);
        assertEquals(false, restored.isMock);
    }

    @Test
    void profileUpsertAndLatestInterviewAreIsolatedByAuthenticatedUser() {
        PatientProfileDto firstProfile = new PatientProfileDto();
        firstProfile.name = "first";
        repository.savePatientProfile("user-a", firstProfile);
        PatientProfileDto updatedProfile = new PatientProfileDto();
        updatedProfile.name = "updated";
        repository.savePatientProfile("user-a", updatedProfile);

        HealthInterviewSubmitRequestDto interview = new HealthInterviewSubmitRequestDto();
        HealthInterviewAnswerDto answer = new HealthInterviewAnswerDto();
        answer.questionId = "profile";
        answer.topic = "PROFILE";
        answer.content = "32 岁";
        interview.answers = List.of(answer);
        interview.generatedAt = 1_726_000_000_000L;
        repository.saveHealthInterview("user-a", interview);

        assertEquals(1, count("rehealth_patient_profile"));
        assertEquals("updated", repository.findPatientProfile("user-a").orElseThrow().name);
        assertTrue(repository.findPatientProfile("user-b").isEmpty());
        assertEquals(1_726_000_000_000L,
                repository.findLatestHealthInterview("user-a").orElseThrow().generatedAt);
        assertTrue(repository.findLatestHealthInterview("user-b").isEmpty());
    }

    @Test
    void recordsMinimalModelRequestMetadataWithoutHealthPayload() {
        repository.recordModelRequest(
                "user-a",
                new ModelCallAudit(
                        "request-a",
                        "RISK_EVALUATE",
                        "model-v1",
                        "SUCCESS",
                        null,
                        27
                )
        );

        assertEquals(1, count("rehealth_model_request_log"));
        assertEquals("user-a", jdbcTemplate.queryForObject(
                "SELECT user_id FROM rehealth_model_request_log WHERE request_id = ?",
                String.class,
                "request-a"
        ));
        assertEquals("RISK_EVALUATE", jdbcTemplate.queryForObject(
                "SELECT operation FROM rehealth_model_request_log WHERE request_id = ?",
                String.class,
                "request-a"
        ));
        assertEquals("SUCCESS", jdbcTemplate.queryForObject(
                "SELECT outcome FROM rehealth_model_request_log WHERE request_id = ?",
                String.class,
                "request-a"
        ));
        assertEquals(27L, jdbcTemplate.queryForObject(
                "SELECT latency_ms FROM rehealth_model_request_log WHERE request_id = ?",
                Long.class,
                "request-a"
        ));
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'REHEALTH_MODEL_REQUEST_LOG'
                  AND COLUMN_NAME IN ('REQUEST_JSON', 'FEATURE_JSON', 'TOKEN')
                """, Integer.class));
    }

    @Test
    void buildsUserScopedAttributionHistoryAndPersistsProvenanceAudit() {
        saveRisk("user-a", "request-before-plan", 0.31);
        InterventionGenerateResponseDto plan = new InterventionGenerateResponseDto();
        plan.planId = "plan-a";
        plan.modelVersion = "model-v1";
        plan.generatedAt = "2026-07-23T00:00:00Z";
        repository.saveInterventionPlan("user-a", plan);
        saveRisk("user-a", "request-after-plan", 0.27);
        saveRisk("user-b", "request-user-b", 0.81);

        List<AttributionEventsRequestDto.AttributionHistoryPointDto> history =
                repository.findAttributionHistory("user-a");
        AttributionEventsRequestDto request = new AttributionEventsRequestDto();
        request.requestId = "attr-a";
        request.riskHistory = history;
        AttributionResponseDto response = new AttributionResponseDto();
        response.status = "ready";
        response.attributionMode = "pias";
        response.isMock = false;
        response.provider = "pias";
        response.modelVersion = "pias-individual-v2";
        repository.recordAttributionResult("user-a", request, response);

        assertEquals(1, history.size());
        assertEquals(0.27, history.get(0).riskScore);
        assertEquals(1, history.get(0).intervention);
        String responseJson = jdbcTemplate.queryForObject(
                "SELECT response_json FROM rehealth_attribution_result WHERE user_id = ?",
                String.class,
                "user-a"
        );
        assertTrue(responseJson.contains("\"attributionMode\":\"pias\""));
        assertTrue(responseJson.contains("\"isMock\":false"));
        assertTrue(responseJson.contains("\"provider\":\"pias\""));
        assertTrue(repository.findAttributionHistory("missing-user").isEmpty());
    }

    private void saveRisk(String userId, String requestId, double score) {
        RiskEvaluateRequestDto request = new RiskEvaluateRequestDto();
        request.requestId = requestId;
        RiskEvaluateResponseDto response = new RiskEvaluateResponseDto();
        response.riskScore = score;
        response.riskLevel = "moderate";
        response.modelVersion = "test-v1";
        repository.saveRiskResult(userId, requestId, request, response);
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
