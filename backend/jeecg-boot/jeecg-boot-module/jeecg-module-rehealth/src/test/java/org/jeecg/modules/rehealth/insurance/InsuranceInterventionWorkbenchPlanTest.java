package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class InsuranceInterventionWorkbenchPlanTest {
    private JdbcTemplate jdbc;
    private InsuranceInterventionWorkbenchService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:workbench-plan-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        createSchema();
        service = new InsuranceInterventionWorkbenchService(jdbc, new ObjectMapper());
    }

    @Test
    void institutionPlanExposesPublishedRevisionItemsAsWorkbenchActions() {
        seedPublishedPlan();

        InsuranceInterventionWorkbenchResponse.Plan plan =
                service.institutionPlan(9101, "subject-ref", "app-user-1");

        assertEquals("institution", plan.sourceType());
        assertEquals("plan-1", plan.planId());
        assertEquals("revision-2", plan.revisionId());
        assertEquals(2, plan.revisionNo());
        assertEquals("心血管健康管理计划", plan.title());
        assertEquals(2, plan.items().size());
        JsonNode first = plan.items().get(0);
        assertEquals("规律舒缓活动", first.path("title").asText());
        assertEquals("量力而行", first.path("action").asText());
        assertEquals("daily", first.path("schedule").path("type").asText());
        assertEquals("completed", first.path("feedback_type").asText());
        assertFalse(plan.synthetic());
    }

    @Test
    void institutionPlanRejectsOtherTenantSubjectOrUser() {
        seedPublishedPlan();

        assertNull(service.institutionPlan(9102, "subject-ref", "app-user-1"));
        assertNull(service.institutionPlan(9101, "other-subject", "app-user-1"));
        assertNull(service.institutionPlan(9101, "subject-ref", "other-user"));
    }

    private void seedPublishedPlan() {
        jdbc.update("""
                INSERT INTO rehealth_care_plan(
                  id, tenant_id, owner_type, subject_ref, rehealth_user_id, status, updated_at
                ) VALUES ('plan-1', 9101, 'insurance', 'subject-ref', 'app-user-1', 'active', CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO rehealth_care_plan_revision(
                  id, tenant_id, plan_id, revision_no, status, title, summary,
                  published_at, effective_from, effective_to
                ) VALUES ('revision-2', 9101, 'plan-1', 2, 'published', '心血管健康管理计划',
                          '逐步形成可持续的健康习惯', CURRENT_TIMESTAMP,
                          DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL)
                """);
        jdbc.update("""
                INSERT INTO rehealth_care_plan_item(
                  id, tenant_id, plan_id, revision_id, logical_item_id, category, title,
                  instructions, schedule_json, scoring_weight, allow_not_applicable, display_order
                ) VALUES
                  ('item-1', 9101, 'plan-1', 'revision-2', 'logical-1', 'exercise',
                   '规律舒缓活动', '量力而行', '{"type":"daily","time":"19:00"}', 1.000, TRUE, 1),
                  ('item-2', 9101, 'plan-1', 'revision-2', 'logical-2', 'nutrition',
                   '均衡饮食记录', '记录主要餐食', '{"type":"daily","time":"20:00"}', 1.000, TRUE, 2)
                """);
        jdbc.update("""
                INSERT INTO rehealth_care_plan_occurrence(
                  id, tenant_id, plan_id, revision_id, plan_item_id, subject_ref,
                  scheduled_at, due_at, status
                ) VALUES ('occurrence-1', 9101, 'plan-1', 'revision-2', 'item-1', 'subject-ref',
                          CURRENT_DATE, DATEADD('HOUR', 23, CURRENT_DATE), 'scheduled')
                """);
        jdbc.update("""
                INSERT INTO rehealth_care_plan_execution(
                  id, tenant_id, occurrence_id, feedback_type, occurred_at, created_at
                ) VALUES ('execution-1', 9101, 'occurrence-1', 'completed', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
    }

    private void createSchema() {
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, owner_type VARCHAR(32), subject_ref VARCHAR(64),
                  rehealth_user_id VARCHAR(64), status VARCHAR(32), updated_at TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_revision(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, plan_id VARCHAR(64), revision_no INT,
                  status VARCHAR(32), title VARCHAR(255), summary VARCHAR(2000), published_at TIMESTAMP,
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
                  plan_item_id VARCHAR(64), subject_ref VARCHAR(64), scheduled_at TIMESTAMP,
                  due_at TIMESTAMP, status VARCHAR(32)
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_execution(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, occurrence_id VARCHAR(64), feedback_type VARCHAR(32),
                  occurred_at TIMESTAMP, created_at TIMESTAMP
                )
                """);
    }
}
