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
import org.jeecg.modules.rehealth.mobile.dto.RhiManualHealthInputDto;
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.UUID;
import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        jdbcTemplate.execute("ALTER TABLE rehealth_cvd_risk_result ADD COLUMN factor_contribution_version VARCHAR(64)");
        jdbcTemplate.execute("ALTER TABLE rehealth_cvd_risk_result ADD COLUMN factor_contribution_json LONGTEXT");
        jdbcTemplate.execute("ALTER TABLE rehealth_cvd_risk_result ADD COLUMN factor_measured_component_json LONGTEXT");
        jdbcTemplate.execute("ALTER TABLE rehealth_cvd_risk_result ADD COLUMN factor_control_support_json LONGTEXT");
        jdbcTemplate.execute("""
                CREATE TABLE rehealth_insurance_subject (
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT NOT NULL, subject_ref VARCHAR(64) NOT NULL,
                  rehealth_user_id VARCHAR(64) NOT NULL, enrollment_status VARCHAR(32) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE rehealth_insurance_plan_binding (
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT NOT NULL, subject_ref VARCHAR(64) NOT NULL,
                  plan_id VARCHAR(128) NOT NULL, status VARCHAR(32) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE rehealth_insurance_intervention_feedback (
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT NOT NULL, binding_id VARCHAR(64) NOT NULL,
                  subject_ref VARCHAR(64) NOT NULL, intervention_id VARCHAR(128), feedback_type VARCHAR(64) NOT NULL,
                  occurred_at DATETIME(3) NOT NULL, completion_rate DECIMAL(8,6), adherence_score DECIMAL(8,6),
                  outcome_summary_json LONGTEXT, source_system VARCHAR(64) NOT NULL,
                  source_record_id VARCHAR(128) NOT NULL, created_at DATETIME(3) NOT NULL,
                  UNIQUE(tenant_id, source_system, source_record_id)
                )
                """);
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
    void readsInterventionOnlyInsideRequestedDayWindow() {
        InterventionGenerateResponseDto plan = new InterventionGenerateResponseDto();
        plan.planId = "plan-day";
        plan.modelVersion = "test-v1";
        plan.generatedAt = "2026-07-23T02:00:00Z";
        repository.saveInterventionPlan("user-a", plan);

        assertEquals(
                "plan-day",
                repository.findInterventionPlanInWindow(
                        "user-a",
                        Instant.parse("2026-07-23T00:00:00Z"),
                        Instant.parse("2026-07-24T00:00:00Z")
                ).orElseThrow().planId
        );
        assertTrue(repository.findInterventionPlanInWindow(
                "user-a",
                Instant.parse("2026-07-24T00:00:00Z"),
                Instant.parse("2026-07-25T00:00:00Z")
        ).isEmpty());
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
        firstProfile.heightCm = 170.0;
        firstProfile.weightKg = 68.0;
        firstProfile.diagnoses = List.of("hypertension");
        firstProfile.medications = List.of("medication-a");
        firstProfile.allergies = List.of("penicillin");
        repository.savePatientProfile("user-a", firstProfile);
        PatientProfileDto updatedProfile = new PatientProfileDto();
        updatedProfile.name = "updated";
        updatedProfile.heightCm = 170.0;
        updatedProfile.weightKg = 68.0;
        updatedProfile.diagnoses = firstProfile.diagnoses;
        updatedProfile.medications = firstProfile.medications;
        updatedProfile.allergies = firstProfile.allergies;
        repository.savePatientProfile("user-a", updatedProfile);

        HealthInterviewSubmitRequestDto interview = new HealthInterviewSubmitRequestDto();
        HealthInterviewAnswerDto answer = new HealthInterviewAnswerDto();
        answer.questionId = "profile";
        answer.topic = "PROFILE";
        answer.content = "32 岁";
        interview.answers = List.of(answer);
        interview.focusAreas = List.of("sleep");
        interview.generatedAt = 1_726_000_000_000L;
        interview.profile = new PatientProfileDto();
        interview.profile.age = 32;
        repository.saveHealthInterview("user-a", interview);

        assertEquals(1, count("rehealth_patient_profile"));
        PatientProfileDto restored = repository.findPatientProfile("user-a").orElseThrow();
        assertEquals("updated", restored.name);
        assertEquals(23.53, restored.bmi);
        assertEquals(3L, restored.version);
        assertEquals(32, restored.age);
        assertEquals(List.of("hypertension"), restored.diagnoses);
        assertEquals(List.of("medication-a"), restored.medications);
        assertEquals(List.of("penicillin"), restored.allergies);
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'REHEALTH_PATIENT_PROFILE' AND COLUMN_NAME = 'PROFILE_JSON'
                """, Integer.class));
        assertTrue(repository.findPatientProfile("user-b").isEmpty());
        assertEquals(1_726_000_000_000L,
                repository.findLatestHealthInterview("user-a").orElseThrow().generatedAt);
        assertEquals(List.of("sleep"),
                repository.findLatestHealthInterview("user-a").orElseThrow().focusAreas);
        assertTrue(repository.findLatestHealthInterview("user-b").isEmpty());
    }

    @Test
    void genericFeedbackProjectsToEveryActiveInsurerBinding() {
        InterventionGenerateResponseDto plan = new InterventionGenerateResponseDto();
        plan.planId = "shared-plan";
        plan.modelVersion = "test-v1";
        plan.generatedAt = "2026-08-14T00:00:00Z";
        repository.saveInterventionPlan("app-user", plan);
        for (int tenant : List.of(9101, 9102)) {
            jdbcTemplate.update("INSERT INTO rehealth_insurance_subject VALUES (?,?,?,?,?)",
                    "subject-row-" + tenant, tenant, "subject-" + tenant, "app-user", "active");
            jdbcTemplate.update("INSERT INTO rehealth_insurance_plan_binding VALUES (?,?,?,?,?)",
                    "binding-" + tenant, tenant, "subject-" + tenant, "shared-plan", "active");
        }
        FeedbackRequestDto feedback = new FeedbackRequestDto();
        feedback.status = "completed";
        feedback.adherence = 0.9;

        repository.saveFeedback("app-user", "shared-plan", feedback);
        repository.saveFeedback("app-user", "shared-plan", feedback);

        assertEquals(2, count("rehealth_insurance_intervention_feedback"));
    }

    @Test
    void persistsManualRhiInputAndRejectsAnOlderClientCopy() {
        RhiManualHealthInputDto current = new RhiManualHealthInputDto();
        current.sedentaryHoursPerDay = 8.5;
        current.waistCircumferenceCm = 92.0;
        current.cuffConfirmed = false;
        current.labConfirmed = false;
        current.updatedAt = 2_000L;

        repository.saveRhiManualHealthInput("user-a", current);

        RhiManualHealthInputDto stale = new RhiManualHealthInputDto();
        stale.sedentaryHoursPerDay = 1.0;
        stale.updatedAt = 1_000L;
        RhiManualHealthInputDto persisted = repository.saveRhiManualHealthInput("user-a", stale);

        assertEquals(1, count("rehealth_rhi_manual_health_input"));
        assertEquals(8.5, persisted.sedentaryHoursPerDay);
        assertEquals(92.0, repository.findRhiManualHealthInput("user-a").orElseThrow().waistCircumferenceCm);
        assertTrue(repository.findRhiManualHealthInput("user-b").isEmpty());
    }

    @Test
    void rejectsStaleProfileUpdates() {
        PatientProfileDto initial = new PatientProfileDto();
        initial.name = "initial";
        repository.savePatientProfile("user-a", initial);

        PatientProfileDto current = repository.findPatientProfile("user-a").orElseThrow();
        PatientProfileDto stale = repository.findPatientProfile("user-a").orElseThrow();
        current.name = "current";
        repository.savePatientProfile("user-a", current);
        stale.name = "stale";

        assertThrows(
                OptimisticLockingFailureException.class,
                () -> repository.savePatientProfile("user-a", stale)
        );
        assertEquals("current", repository.findPatientProfile("user-a").orElseThrow().name);
    }

    @Test
    void structuredRiskAndInterventionRemainReadableWhenSnapshotsAreMalformed() {
        saveRisk("user-a", "request-a", 0.42);
        jdbcTemplate.update(
                "UPDATE rehealth_cvd_risk_result SET response_json = 'invalid' WHERE user_id = ?",
                "user-a"
        );
        assertEquals(0.42, repository.findLatestRiskResult("user-a").orElseThrow().riskScore);

        InterventionGenerateResponseDto plan = new InterventionGenerateResponseDto();
        plan.planId = "plan-structured";
        plan.modelVersion = "model-v1";
        plan.priorityIntervention = "walk";
        plan.expectedImpact = "lower risk";
        plan.contraindications = List.of("stop if unwell");
        repository.saveInterventionPlan("user-a", plan);
        jdbcTemplate.update(
                "UPDATE rehealth_intervention_plan SET response_json = 'invalid' WHERE user_id = ?",
                "user-a"
        );

        InterventionGenerateResponseDto restoredPlan =
                repository.findLatestInterventionPlan("user-a").orElseThrow();
        assertEquals("walk", restoredPlan.priorityIntervention);
        assertEquals("lower risk", restoredPlan.expectedImpact);
        assertEquals(List.of("stop if unwell"), restoredPlan.contraindications);
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
        assertTrue(responseJson.contains("\"attribution_mode\":\"pias\""));
        assertTrue(responseJson.contains("\"is_mock\":false"));
        assertTrue(responseJson.contains("\"provider\":\"pias\""));
        assertEquals("pias", jdbcTemplate.queryForObject(
                "SELECT attribution_mode FROM rehealth_attribution_result WHERE user_id = ?",
                String.class,
                "user-a"
        ));
        assertEquals(false, jdbcTemplate.queryForObject(
                "SELECT is_mock FROM rehealth_attribution_result WHERE user_id = ?",
                Boolean.class,
                "user-a"
        ));
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
