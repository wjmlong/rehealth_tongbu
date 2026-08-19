package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsuranceMobileCarePlanServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private JdbcTemplate jdbc;
    private InsuranceMobileCarePlanService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:mobile-care-plan-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        createSchema();
        Clock clock = Clock.fixed(Instant.parse("2026-08-19T04:00:00Z"), ZONE);
        service = new InsuranceMobileCarePlanService(jdbc, new ObjectMapper(), clock, ZONE);
        seedPlan();
    }

    @Test
    void currentExpandsDailyTasksAndReturnsTaskDenominatorAdherence() {
        List<InsuranceMobileCarePlanResponse.Plan> plans = service.current("app-user-1");

        assertEquals(1, plans.size());
        InsuranceMobileCarePlanResponse.Plan plan = plans.get(0);
        assertEquals("健康险机构", plan.organizationName());
        assertEquals(1, plan.items().size());
        assertNotNull(plan.items().get(0).todayOccurrence());
        assertTrue(plan.items().get(0).scheduleSupported());
        assertEquals(28, jdbc.queryForObject(
                "SELECT COUNT(*) FROM rehealth_care_plan_occurrence WHERE plan_id='plan-1'", Integer.class));
        assertEquals(27, plan.adherence28d().expectedCount());
        assertEquals(0, plan.adherence28d().scoredCount());
        assertEquals("0.0", plan.adherence28d().scorePercent().toPlainString());
    }

    @Test
    void occurrenceFeedbackIsIdempotentAndUpdatesRollingAdherence() {
        InsuranceMobileCarePlanResponse.Plan initial = service.current("app-user-1").get(0);
        String occurrenceId = initial.items().get(0).todayOccurrence().occurrenceId();
        InsuranceMobilePlanRequest.OccurrenceFeedback request = new InsuranceMobilePlanRequest.OccurrenceFeedback(
                "partially_completed", LocalDateTime.of(2026, 8, 19, 11, 0),
                "mobile-feedback-1", "self_report", null
        );

        Map<String, Object> first = service.feedback("app-user-1", occurrenceId, request);
        Map<String, Object> replay = service.feedback("app-user-1", occurrenceId, request);
        String otherOccurrenceId = jdbc.queryForObject(
                "SELECT id FROM rehealth_care_plan_occurrence WHERE id<>? ORDER BY scheduled_at LIMIT 1",
                String.class,
                occurrenceId
        );
        InsuranceApiException reusedForAnotherTask = assertThrows(
                InsuranceApiException.class,
                () -> service.feedback("app-user-1", otherOccurrenceId, request)
        );
        InsuranceMobileCarePlanResponse.Plan refreshed = service.current("app-user-1").get(0);

        assertEquals(false, first.get("idempotentReplay"));
        assertEquals(true, replay.get("idempotentReplay"));
        assertEquals(409, reusedForAnotherTask.status().value());
        assertEquals(28, refreshed.adherence28d().expectedCount());
        assertEquals(1, refreshed.adherence28d().scoredCount());
        assertEquals("1.8", refreshed.adherence28d().scorePercent().toPlainString());
        assertEquals("partially_completed", refreshed.items().get(0).todayOccurrence().feedbackType());
    }

    private void seedPlan() {
        jdbc.update("INSERT INTO sys_tenant(id, name, status, del_flag) VALUES (1001, '健康险机构', 1, 0)");
        jdbc.update("""
                INSERT INTO rehealth_insurance_subject(
                  id, tenant_id, subject_ref, rehealth_user_id, enrollment_status, consent_status
                ) VALUES ('subject-id', 1001, 'subject-ref', 'app-user-1', 'active', 'granted')
                """);
        jdbc.update("""
                INSERT INTO rehealth_care_plan(
                  id, tenant_id, owner_type, subject_ref, rehealth_user_id, status, updated_at
                ) VALUES ('plan-1', 1001, 'insurance', 'subject-ref', 'app-user-1', 'active', '2026-08-01 09:00:00')
                """);
        jdbc.update("""
                INSERT INTO rehealth_care_plan_revision(
                  id, tenant_id, plan_id, revision_no, status, title, summary, effective_from, effective_to
                ) VALUES ('revision-1', 1001, 'plan-1', 1, 'published', '每日健康计划', '循序渐进',
                          '2026-07-01 00:00:00', NULL)
                """);
        jdbc.update("""
                INSERT INTO rehealth_care_plan_item(
                  id, tenant_id, plan_id, revision_id, logical_item_id, category, title,
                  instructions, schedule_json, scoring_weight, allow_not_applicable, display_order
                ) VALUES ('item-1', 1001, 'plan-1', 'revision-1', 'logical-1', 'exercise', '晚间步行',
                          '量力而行', '{"type":"daily","time":"08:00"}', 1.000, TRUE, 1)
                """);
    }

    private void createSchema() {
        jdbc.execute("CREATE TABLE sys_tenant(id INT PRIMARY KEY, name VARCHAR(100), status INT, del_flag INT)");
        jdbc.execute("""
                CREATE TABLE rehealth_insurance_subject(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, subject_ref VARCHAR(64), rehealth_user_id VARCHAR(64),
                  enrollment_status VARCHAR(32), consent_status VARCHAR(32)
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, owner_type VARCHAR(32), subject_ref VARCHAR(64),
                  rehealth_user_id VARCHAR(64), status VARCHAR(32), updated_at TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_revision(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, plan_id VARCHAR(64), revision_no INT,
                  status VARCHAR(32), title VARCHAR(255), summary VARCHAR(2000),
                  effective_from TIMESTAMP, effective_to TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_item(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, plan_id VARCHAR(64), revision_id VARCHAR(64),
                  logical_item_id VARCHAR(64), category VARCHAR(32), title VARCHAR(255), instructions VARCHAR(4000),
                  schedule_json LONGTEXT, scoring_weight DECIMAL(10,3), allow_not_applicable BOOLEAN, display_order INT
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_occurrence(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, plan_id VARCHAR(64), revision_id VARCHAR(64),
                  plan_item_id VARCHAR(64), logical_item_id VARCHAR(64), subject_ref VARCHAR(64),
                  scheduled_at TIMESTAMP, due_at TIMESTAMP, status VARCHAR(32), exclusion_reason VARCHAR(128),
                  created_at TIMESTAMP, updated_at TIMESTAMP,
                  UNIQUE(tenant_id, plan_item_id, scheduled_at)
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_execution(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, occurrence_id VARCHAR(64), plan_id VARCHAR(64),
                  revision_id VARCHAR(64), plan_item_id VARCHAR(64), logical_item_id VARCHAR(64), subject_ref VARCHAR(64),
                  feedback_type VARCHAR(32), score_value DECIMAL(5,4), verification_type VARCHAR(32), note VARCHAR(1000),
                  occurred_at TIMESTAMP, source_system VARCHAR(64), source_record_id VARCHAR(128), created_at TIMESTAMP,
                  UNIQUE(tenant_id, source_system, source_record_id)
                )
                """);
    }
}
