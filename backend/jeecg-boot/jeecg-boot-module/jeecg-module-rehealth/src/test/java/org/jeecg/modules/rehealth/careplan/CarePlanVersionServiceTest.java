package org.jeecg.modules.rehealth.careplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarePlanVersionServiceTest {
    private JdbcTemplate jdbc;
    private CarePlanVersionService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:care-plan-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        createSchema();
        service = new CarePlanVersionService(jdbc, new ObjectMapper());
    }

    @Test
    void draftCanBeEditedPublishedAndNeverOverwrittenInPlace() {
        CarePlanVersionResponse.Plan created = createDraft("exercise");
        String logicalItemId = created.revisions().get(0).items().get(0).logicalItemId();

        CarePlanVersionResponse.Plan edited = service.updateDraft(
                1001, "insurance", created.planId(), "editor-1",
                new CarePlanVersionRequest.UpdateDraft(
                        0L, "近四周运动计划", "逐步增加步行", "补充执行说明",
                        List.of(item(logicalItemId, "exercise", "晚饭后步行 20 分钟"))
                )
        );
        assertEquals(1, edited.lockVersion());
        assertEquals("晚饭后步行 20 分钟", edited.revisions().get(0).items().get(0).title());

        CarePlanVersionResponse.Plan published = service.publish(
                1001, "insurance", created.planId(), "publisher-1",
                new CarePlanVersionRequest.Publish(1L, null)
        );
        assertEquals("active", published.status());
        assertEquals(2, published.lockVersion());
        assertEquals("published", published.revisions().get(0).status());
        assertNotNull(published.revisions().get(0).publishedAt());

        CarePlanVersionException error = assertThrows(
                CarePlanVersionException.class,
                () -> service.updateDraft(
                        1001, "insurance", created.planId(), "editor-1",
                        new CarePlanVersionRequest.UpdateDraft(
                                2L, "禁止覆盖", null, null,
                                List.of(item(logicalItemId, "exercise", "禁止覆盖"))
                        )
                )
        );
        assertEquals(HttpStatus.CONFLICT, error.status());
    }

    @Test
    void publishingANewRevisionPreservesLogicalItemAndCancelsSupersededFutureOccurrences() {
        CarePlanVersionResponse.Plan created = createDraft("sleep");
        CarePlanVersionResponse.Plan firstPublished = service.publish(
                1001, "insurance", created.planId(), "publisher-1",
                new CarePlanVersionRequest.Publish(0L, null)
        );
        String firstRevisionId = firstPublished.currentRevisionId();
        String firstItemId = firstPublished.revisions().get(0).items().get(0).itemId();
        String logicalItemId = firstPublished.revisions().get(0).items().get(0).logicalItemId();

        CarePlanVersionResponse.Plan cloned = service.cloneRevision(
                1001, "insurance", created.planId(), "editor-1",
                new CarePlanVersionRequest.CreateRevision(1L, "调整睡眠提醒时间")
        );
        assertEquals(logicalItemId, cloned.revisions().get(0).items().get(0).logicalItemId());
        CarePlanVersionResponse.Plan edited = service.updateDraft(
                1001, "insurance", created.planId(), "editor-1",
                new CarePlanVersionRequest.UpdateDraft(
                        2L, "睡眠计划", null, "调整睡眠提醒时间",
                        List.of(item(logicalItemId, "sleep", "22:30 开始睡前准备"))
                )
        );

        LocalDateTime effectiveAt = LocalDateTime.now().plusMinutes(5);
        jdbc.update("""
                INSERT INTO rehealth_care_plan_occurrence (
                  id, tenant_id, plan_id, revision_id, plan_item_id, logical_item_id,
                  subject_ref, scheduled_at, due_at, status, exclusion_reason, created_at, updated_at
                ) VALUES ('occurrence-1', 1001, ?, ?, ?, ?, 'subject-1', ?, ?, 'scheduled', NULL, ?, ?)
                """, created.planId(), firstRevisionId, firstItemId, logicalItemId,
                effectiveAt.plusHours(1), effectiveAt.plusHours(2), LocalDateTime.now(), LocalDateTime.now());

        CarePlanVersionResponse.Plan secondPublished = service.publish(
                1001, "insurance", created.planId(), "publisher-1",
                new CarePlanVersionRequest.Publish(edited.lockVersion(), effectiveAt)
        );
        assertEquals(2, secondPublished.revisions().get(0).revisionNo());
        assertEquals("cancelled", jdbc.queryForObject(
                "SELECT status FROM rehealth_care_plan_occurrence WHERE id='occurrence-1'", String.class));
        assertEquals("superseded_by_revision", jdbc.queryForObject(
                "SELECT exclusion_reason FROM rehealth_care_plan_occurrence WHERE id='occurrence-1'", String.class));
    }

    @Test
    void optimisticLockRejectsStaleDraftUpdates() {
        CarePlanVersionResponse.Plan created = createDraft("nutrition");
        service.updateDraft(
                1001, "insurance", created.planId(), "editor-1",
                new CarePlanVersionRequest.UpdateDraft(
                        0L, "饮食计划", null, null,
                        List.of(item(created.revisions().get(0).items().get(0).logicalItemId(),
                                "nutrition", "记录晚餐"))
                )
        );

        CarePlanVersionException error = assertThrows(
                CarePlanVersionException.class,
                () -> service.publish(
                        1001, "insurance", created.planId(), "publisher-1",
                        new CarePlanVersionRequest.Publish(0L, null)
                )
        );
        assertEquals(HttpStatus.CONFLICT, error.status());
    }

    @Test
    void insuranceAdapterCannotCreateMedicationOrDiagnosisItems() {
        CarePlanVersionException error = assertThrows(
                CarePlanVersionException.class,
                () -> createDraft("medication")
        );
        assertEquals(HttpStatus.BAD_REQUEST, error.status());
    }

    @Test
    void withdrawalFreezesThePublishedRevisionAndCancelsFutureOccurrences() {
        CarePlanVersionResponse.Plan created = createDraft("follow_up");
        CarePlanVersionResponse.Plan published = service.publish(
                1001, "insurance", created.planId(), "publisher-1",
                new CarePlanVersionRequest.Publish(0L, null)
        );

        CarePlanVersionResponse.Plan withdrawn = service.withdraw(
                1001, "insurance", created.planId(), "publisher-1",
                new CarePlanVersionRequest.Withdraw(published.lockVersion(), "服务关系结束")
        );
        assertEquals("withdrawn", withdrawn.status());
        assertEquals("withdrawn", withdrawn.revisions().get(0).status());
        assertNotNull(withdrawn.revisions().get(0).effectiveTo());
    }

    @Test
    void clonedDraftCanBeDiscardedWithoutChangingPublishedContent() {
        CarePlanVersionResponse.Plan created = createDraft("education");
        CarePlanVersionResponse.Plan published = service.publish(
                1001, "insurance", created.planId(), "publisher-1",
                new CarePlanVersionRequest.Publish(0L, null)
        );
        CarePlanVersionResponse.Plan cloned = service.cloneRevision(
                1001, "insurance", created.planId(), "editor-1",
                new CarePlanVersionRequest.CreateRevision(published.lockVersion(), "准备调整")
        );

        CarePlanVersionResponse.Plan discarded = service.discardDraft(
                1001, "insurance", created.planId(), "editor-1",
                new CarePlanVersionRequest.DiscardDraft(cloned.lockVersion(), "本次不调整")
        );
        assertEquals("active", discarded.status());
        assertEquals(published.currentRevisionId(), discarded.currentRevisionId());
        assertEquals(null, discarded.draftRevisionId());
        assertEquals("withdrawn", discarded.revisions().get(0).status());
        assertEquals("published", discarded.revisions().get(1).status());
    }

    private CarePlanVersionResponse.Plan createDraft(String category) {
        return service.createDraft(
                1001, "insurance", "1001", "subject-1", "app-user-1", "editor-1",
                new CarePlanVersionRequest.CreateDraft(
                        "四周健康计划", "生活方式干预", "首次创建",
                        List.of(item(null, category, "每日执行一项任务"))
                )
        );
    }

    private CarePlanVersionRequest.Item item(String logicalItemId, String category, String title) {
        return new CarePlanVersionRequest.Item(
                logicalItemId, category, title, "仅作为健康管理建议，不替代医疗诊断。",
                Map.of("frequency", "daily"), BigDecimal.ONE, true
        );
    }

    private void createSchema() {
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan (
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT NOT NULL, owner_type VARCHAR(32) NOT NULL,
                  owner_org_ref VARCHAR(64) NOT NULL, subject_ref VARCHAR(64) NOT NULL,
                  rehealth_user_id VARCHAR(64) NOT NULL, source_plan_id VARCHAR(128),
                  status VARCHAR(32) NOT NULL, current_revision_id VARCHAR(64), draft_revision_id VARCHAR(64),
                  lock_version BIGINT NOT NULL, created_by VARCHAR(64) NOT NULL, created_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(64) NOT NULL, updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_revision (
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT NOT NULL, plan_id VARCHAR(64) NOT NULL,
                  revision_no INT NOT NULL, status VARCHAR(32) NOT NULL, title VARCHAR(255) NOT NULL,
                  summary VARCHAR(2000), change_reason VARCHAR(1000), content_hash VARCHAR(64) NOT NULL,
                  effective_from TIMESTAMP, effective_to TIMESTAMP, published_by VARCHAR(64),
                  published_at TIMESTAMP, withdrawn_by VARCHAR(64), withdrawn_at TIMESTAMP,
                  created_by VARCHAR(64) NOT NULL, created_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(64) NOT NULL, updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_item (
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT NOT NULL, plan_id VARCHAR(64) NOT NULL,
                  revision_id VARCHAR(64) NOT NULL, logical_item_id VARCHAR(64) NOT NULL,
                  category VARCHAR(32) NOT NULL, title VARCHAR(255) NOT NULL, instructions VARCHAR(4000),
                  schedule_json LONGTEXT, scoring_weight DECIMAL(10,3) NOT NULL,
                  allow_not_applicable BOOLEAN NOT NULL, display_order INT NOT NULL, created_at TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_occurrence (
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT NOT NULL, plan_id VARCHAR(64) NOT NULL,
                  revision_id VARCHAR(64) NOT NULL, plan_item_id VARCHAR(64) NOT NULL,
                  logical_item_id VARCHAR(64) NOT NULL, subject_ref VARCHAR(64) NOT NULL,
                  scheduled_at TIMESTAMP NOT NULL, due_at TIMESTAMP NOT NULL, status VARCHAR(32) NOT NULL,
                  exclusion_reason VARCHAR(128), created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_audit_event (
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT NOT NULL, owner_type VARCHAR(32) NOT NULL,
                  actor_user_id VARCHAR(64) NOT NULL, action VARCHAR(64) NOT NULL,
                  plan_id VARCHAR(64) NOT NULL, revision_id VARCHAR(64), before_hash VARCHAR(64),
                  after_hash VARCHAR(64), reason VARCHAR(1000), created_at TIMESTAMP NOT NULL
                )
                """);
    }
}
