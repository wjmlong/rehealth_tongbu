package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InsuranceInterventionWorkbenchAdherenceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private JdbcTemplate jdbc;
    private InsuranceInterventionWorkbenchService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:workbench-adherence-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        createSchema();
        service = new InsuranceInterventionWorkbenchService(
                jdbc,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-20T04:00:00Z"), ZONE),
                ZONE
        );
        seedCurrentPlan();
    }

    @Test
    void versionedOccurrencesUseLatestExecutionAndWeightedDueTaskDenominator() {
        occurrence("occurrence-1", "item-1", "2026-08-01 08:00:00", "2026-08-01 20:00:00");
        occurrence("occurrence-2", "item-1", "2026-08-02 08:00:00", "2026-08-02 20:00:00");
        occurrence("occurrence-3", "item-1", "2026-08-03 08:00:00", "2026-08-03 20:00:00");
        occurrence("occurrence-4", "item-2", "2026-08-04 08:00:00", "2026-08-04 20:00:00");
        occurrence("occurrence-5", "item-2", "2026-08-05 08:00:00", "2026-08-05 20:00:00");
        occurrence("occurrence-6", "item-2", "2026-08-06 08:00:00", "2026-08-06 20:00:00");
        occurrence("occurrence-7", "item-1", "2026-08-07 08:00:00", "2026-08-07 20:00:00");
        occurrence("occurrence-8", "item-1", "2026-08-20 08:00:00", "2026-08-20 20:00:00");

        execution("execution-1-old", "occurrence-1", "skipped", "0", "2026-08-01 09:00:00");
        execution("execution-1-new", "occurrence-1", "completed", "1", "2026-08-01 10:00:00");
        execution("execution-2", "occurrence-2", "partially_completed", "0.5", "2026-08-02 10:00:00");
        execution("execution-3", "occurrence-3", "skipped", "0", "2026-08-03 10:00:00");
        execution("execution-4", "occurrence-4", "completed", "1", "2026-08-04 10:00:00");
        execution("execution-5", "occurrence-5", "partially_completed", "0.5", "2026-08-05 10:00:00");
        execution("execution-6", "occurrence-6", "skipped", "0", "2026-08-06 10:00:00");
        execution("execution-7", "occurrence-7", "not_applicable", null, "2026-08-07 10:00:00");

        InsuranceInterventionWorkbenchService.FeedbackAggregate result =
                service.latestFeedback(9101, "subject-ref", "app-user-1");

        assertEquals(0.5, result.adherence());
        assertEquals(4.5, result.completedCount());
        assertEquals(9.0, result.expectedCount());
        assertEquals("2026-08-07T10:00:00", result.occurredAt());
    }

    @Test
    void activeVersionedPlanWithNoValidDenominatorDoesNotFallBackToZero() {
        occurrence("occurrence-na", "item-1", "2026-08-19 08:00:00", "2026-08-19 20:00:00");
        occurrence("occurrence-future", "item-1", "2026-08-20 08:00:00", "2026-08-20 20:00:00");
        execution("execution-na", "occurrence-na", "not_applicable", null, "2026-08-19 10:00:00");

        InsuranceInterventionWorkbenchService.FeedbackAggregate result =
                service.latestFeedback(9101, "subject-ref", "app-user-1");

        assertNull(result.adherence());
        assertNull(result.completedCount());
        assertNull(result.expectedCount());
    }

    private void seedCurrentPlan() {
        jdbc.update("""
                INSERT INTO rehealth_care_plan(
                  id, tenant_id, owner_type, subject_ref, rehealth_user_id, status
                ) VALUES ('plan-1', 9101, 'insurance', 'subject-ref', 'app-user-1', 'active')
                """);
        jdbc.update("""
                INSERT INTO rehealth_care_plan_revision(
                  id, tenant_id, plan_id, status, effective_from, effective_to
                ) VALUES ('revision-1', 9101, 'plan-1', 'published', '2026-07-01 00:00:00', NULL)
                """);
        jdbc.update("""
                INSERT INTO rehealth_care_plan_item(
                  id, tenant_id, plan_id, revision_id, scoring_weight
                ) VALUES ('item-1', 9101, 'plan-1', 'revision-1', 1.000),
                         ('item-2', 9101, 'plan-1', 'revision-1', 2.000)
                """);
    }

    private void occurrence(String id, String itemId, String scheduledAt, String dueAt) {
        jdbc.update("""
                INSERT INTO rehealth_care_plan_occurrence(
                  id, tenant_id, plan_id, revision_id, plan_item_id, subject_ref,
                  scheduled_at, due_at, status
                ) VALUES (?, 9101, 'plan-1', 'revision-1', ?, 'subject-ref', ?, ?, 'scheduled')
                """, id, itemId, scheduledAt, dueAt);
    }

    private void execution(String id, String occurrenceId, String type, String score, String occurredAt) {
        jdbc.update("""
                INSERT INTO rehealth_care_plan_execution(
                  id, tenant_id, occurrence_id, feedback_type, score_value, occurred_at, created_at
                ) VALUES (?, 9101, ?, ?, ?, ?, ?)
                """, id, occurrenceId, type, score, occurredAt, occurredAt);
    }

    private void createSchema() {
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, owner_type VARCHAR(32),
                  subject_ref VARCHAR(64), rehealth_user_id VARCHAR(64), status VARCHAR(32)
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_revision(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, plan_id VARCHAR(64), status VARCHAR(32),
                  effective_from TIMESTAMP, effective_to TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_item(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, plan_id VARCHAR(64),
                  revision_id VARCHAR(64), scoring_weight DECIMAL(10,3)
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_occurrence(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, plan_id VARCHAR(64), revision_id VARCHAR(64),
                  plan_item_id VARCHAR(64), subject_ref VARCHAR(64), scheduled_at TIMESTAMP,
                  due_at TIMESTAMP, status VARCHAR(32)
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_execution(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, occurrence_id VARCHAR(64),
                  feedback_type VARCHAR(32), score_value DECIMAL(5,4),
                  occurred_at TIMESTAMP, created_at TIMESTAMP
                )
                """);
    }
}
