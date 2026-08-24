package org.jeecg.modules.rehealth.insurance;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InsuranceInterventionReportServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int TENANT = 9101;
    private static final String MANAGER = "manager-1";

    private JdbcTemplate jdbc;
    private InsuranceInterventionWorkbenchService workbench;
    private InsuranceInterventionReportService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:intervention-report-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        createSchema();
        workbench = mock(InsuranceInterventionWorkbenchService.class);
        service = new InsuranceInterventionReportService(
                workbench,
                jdbc,
                Clock.fixed(Instant.parse("2026-08-24T04:00:00Z"), ZONE),
                ZONE
        );
    }

    @Test
    void movementCountsRiskLevelTransitionsInsideWindowOnly() {
        // user-a: high before window start -> medium now = down
        risk("user-a", "high", "2026-07-10 08:00:00");
        risk("user-a", "medium", "2026-08-20 08:00:00");
        // user-b: medium before window start -> high now = up
        risk("user-b", "medium", "2026-07-20 08:00:00");
        risk("user-b", "high", "2026-08-21 08:00:00");
        // user-c: low now, no history before window start = not counted
        risk("user-c", "low", "2026-08-22 08:00:00");

        stubWorkbench(List.of(
                identity("subject-a", "user-a"),
                identity("subject-b", "user-b"),
                identity("subject-c", "user-c")
        ), Map.of(
                "user-a", summary("pending_action", "medium", 0.2, 60.0, 30.0, 0.8, false, false, null),
                "user-b", summary("in_progress", "high", 0.6, 40.0, 70.0, 0.5, false, false, "每周随访"),
                "user-c", summary("improved", "low", 0.05, 80.0, 10.0, 0.9, false, false, null)
        ));

        InsuranceInterventionReportResponse.ReportData data = service.reportData(TENANT, MANAGER, 30);

        assertEquals("1", data.movement().get("down").get("value"));
        assertEquals("1", data.movement().get("up").get("value"));
        assertEquals("0", data.movement().get("net").get("value"));
        assertEquals(1, data.highRiskWaiting());
        assertEquals(1, data.activeInterventions());
        assertEquals(1, data.improved());
        assertEquals("", data.dataStatusLabel());
    }

    @Test
    void adherenceFunnelUsesExecutionFactThresholds() {
        stubWorkbench(List.of(
                identity("subject-1", "user-1"),
                identity("subject-2", "user-2"),
                identity("subject-3", "user-3"),
                identity("subject-4", "user-4")
        ), Map.of(
                "user-1", summary("pending_review", "low", 0.1, 60.0, 20.0, 0.5, false, false, null),
                "user-2", summary("pending_review", "low", 0.1, 60.0, 20.0, 0.5, false, false, null),
                "user-3", summary("pending_review", "low", 0.1, 60.0, 20.0, 0.5, false, false, null),
                "user-4", summary("pending_review", "low", 0.1, 60.0, 20.0, 0.5, false, false, null)
        ));
        occurrence("occurrence-1", "subject-1", "2026-08-01 08:00:00");
        occurrence("occurrence-2", "subject-2", "2026-08-01 08:00:00");
        occurrence("occurrence-3", "subject-3", "2026-08-01 08:00:00");
        occurrence("occurrence-4", "subject-4", "2026-08-01 08:00:00");
        executions("subject-1", 3);
        executions("subject-2", 7);
        executions("subject-3", 28);

        InsuranceInterventionReportResponse.ReportData data = service.reportData(TENANT, MANAGER, 30);

        List<InsuranceInterventionReportResponse.AdherenceRow> rows = data.adherence();
        assertEquals("收到行动建议", rows.get(0).name());
        assertEquals("4", rows.get(0).count());
        assertEquals("开始行动", rows.get(1).name());
        assertEquals("3", rows.get(1).count());
        assertEquals("75%", rows.get(1).share());
        assertEquals("连续记录 7 天", rows.get(2).name());
        assertEquals("2", rows.get(2).count());
        assertEquals("50%", rows.get(2).share());
        assertEquals("连续记录 30 天", rows.get(3).name());
        assertEquals("1", rows.get(3).count());
        assertEquals("25%", rows.get(3).share());
        assertEquals("待真实接口聚合", rows.get(5).count());
    }

    @Test
    void mockDataSetsCoverageWarningLabel() {
        stubWorkbench(List.of(identity("subject-1", "user-1")), Map.of(
                "user-1", summary("pending_action", "high", 0.7, 40.0, 60.0, 0.3, true, false, null)
        ));
        InsuranceInterventionReportResponse.ReportData data = service.reportData(TENANT, MANAGER, 30);
        assertTrue(data.dataStatusLabel().contains("演练数据"));
        assertEquals("1", data.riskDistribution().get("高风险").get("count"));
    }

    @Test
    void riskDistributionGroupsNormalizedLevelsWithShares() {
        stubWorkbench(List.of(
                identity("subject-1", "user-1"),
                identity("subject-2", "user-2"),
                identity("subject-3", "user-3")
        ), Map.of(
                "user-1", summary("pending_review", "high", 0.6, 40.0, 60.0, 0.5, false, false, null),
                "user-2", summary("pending_review", "medium", 0.3, 60.0, 40.0, 0.5, false, false, null),
                "user-3", summary("pending_review", "low", 0.1, 80.0, 20.0, 0.5, false, false, null)
        ));
        InsuranceInterventionReportResponse.ReportData data = service.reportData(TENANT, MANAGER, 30);
        assertEquals("1", data.riskDistribution().get("高风险").get("count"));
        assertEquals("33%", data.riskDistribution().get("高风险").get("share"));
        assertEquals("1", data.riskDistribution().get("中风险").get("count"));
        assertEquals("1", data.riskDistribution().get("低风险").get("count"));
        assertEquals(3, data.totalManaged());
    }

    @Test
    void outcomesUseLatestVersusBaselineMeanDelta() {
        stubWorkbench(List.of(identity("subject-1", "user-1")), Map.of(
                "user-1", summary("pending_review", "medium", 0.3, 60.0, 40.0, 0.5, false, false, null)
        ));
        rhi("rhi-1", "user-1", "2026-07-01", 50.0);
        rhi("rhi-2", "user-1", "2026-08-20", 60.0);
        rdi("rdi-1", "user-1", "2026-07-01", 40.0, 0);
        rdi("rdi-2", "user-1", "2026-08-20", 30.0, 0);
        rdi("rdi-mock", "user-1", "2026-08-21", 10.0, 1);

        InsuranceInterventionReportResponse.ReportData data = service.reportData(TENANT, MANAGER, 30);

        InsuranceInterventionReportResponse.Outcome rhiOutcome = data.outcomes().get(0);
        assertEquals("RHI 综合健康状态", rhiOutcome.name());
        assertEquals("+10.0 分", rhiOutcome.change());
        assertTrue(rhiOutcome.meaning().contains("样本 1 人"));
        InsuranceInterventionReportResponse.Outcome rdiOutcome = data.outcomes().get(1);
        assertEquals("RDI 近期风险负荷", rdiOutcome.name());
        assertEquals("-10.0 分", rdiOutcome.change());
        assertEquals("待复测口径", data.outcomes().get(2).change());
        assertEquals("未接入", data.outcomes().get(4).change());
    }

    @Test
    void factorsAverageOnlyLatestNonMockRdiContributions() {
        stubWorkbench(List.of(identity("subject-1", "user-1")), Map.of(
                "user-1", summary("pending_review", "medium", 0.3, 60.0, 40.0, 0.5, false, false, null)
        ));
        String oldSnapshot = "snapshot-old";
        String latestSnapshot = "snapshot-latest";
        rdiSnapshot(oldSnapshot, "user-1", "2026-07-01", 0);
        rdiSnapshot(latestSnapshot, "user-1", "2026-08-20", 0);
        contribution("c-1", oldSnapshot, "steps", "activity", -100.0);
        contribution("c-2", latestSnapshot, "steps", "activity", -3.0);
        contribution("c-3", latestSnapshot, "sleep", "sleep", 2.0);

        InsuranceInterventionReportResponse.ReportData data = service.reportData(TENANT, MANAGER, 30);

        assertEquals(2, data.factors().size());
        assertEquals("步数", data.factors().get(0).name());
        assertEquals("-3.00", data.factors().get(0).contribution());
        assertEquals("睡眠", data.factors().get(1).name());
        assertEquals("+2.00", data.factors().get(1).contribution());
    }

    @Test
    void factorsPlaceholderWhenOnlyMockSnapshotsExist() {
        stubWorkbench(List.of(identity("subject-1", "user-1")), Map.of(
                "user-1", summary("pending_review", "medium", 0.3, 60.0, 40.0, 0.5, false, true, null)
        ));
        String snapshot = "snapshot-mock";
        rdiSnapshot(snapshot, "user-1", "2026-08-20", 1);
        contribution("c-1", snapshot, "steps", "activity", -3.0);

        InsuranceInterventionReportResponse.ReportData data = service.reportData(TENANT, MANAGER, 30);

        assertEquals(1, data.factors().size());
        assertEquals("暂无贡献数据", data.factors().get(0).name());
        assertEquals("—", data.factors().get(0).contribution());
        assertTrue(data.factors().get(0).meaning().contains("演练数据"));
        assertTrue(data.dataStatusLabel().contains("演练数据"));
        assertEquals("数据不足", data.outcomes().get(1).change());
        assertTrue(data.outcomes().get(1).meaning().contains("演练快照不计入统计"));
    }

    @Test
    void includeMockFlagIncludesMockSnapshotsButKeepsCoverageLabel() {
        InsuranceInterventionReportService includeMockService = new InsuranceInterventionReportService(
                workbench,
                jdbc,
                Clock.fixed(Instant.parse("2026-08-24T04:00:00Z"), ZONE),
                ZONE,
                true
        );
        stubWorkbench(List.of(identity("subject-1", "user-1")), Map.of(
                "user-1", summary("pending_review", "medium", 0.3, 60.0, 40.0, 0.5, false, true, null)
        ));
        rdi("rdi-base", "user-1", "2026-07-01", 40.0, 1);
        rdi("rdi-latest", "user-1", "2026-08-20", 30.0, 1);
        contribution("c-1", "rdi-latest", "steps", "activity", -3.0);
        contribution("c-2", "rdi-latest", "sleep_duration", "sleep", 2.0);

        InsuranceInterventionReportResponse.ReportData data = includeMockService.reportData(TENANT, MANAGER, 30);

        assertEquals(2, data.factors().size());
        assertEquals("步数", data.factors().get(0).name());
        assertEquals("-3.00", data.factors().get(0).contribution());
        assertEquals("RDI 近期风险负荷", data.outcomes().get(1).name());
        assertEquals("-10.0 分", data.outcomes().get(1).change());
        assertTrue(data.dataStatusLabel().contains("演练数据"));
    }

    @Test
    void reportDataUsesTenantAndManagerScope() {
        stubWorkbench(List.of(), Map.of());
        service.reportData(9102, "manager-2", 30);
        verify(workbench).identities(9102, "manager-2", null, Integer.MAX_VALUE, 0);
    }

    private void stubWorkbench(
            List<InsuranceInterventionWorkbenchService.Identity> identities,
            Map<String, InsuranceInterventionWorkbenchResponse.SubjectSummary> summaries
    ) {
        when(workbench.identities(anyInt(), anyString(), any(), anyInt(), anyInt()))
                .thenReturn(identities);
        when(workbench.summary(anyInt(), any())).thenAnswer(invocation -> {
            InsuranceInterventionWorkbenchService.Identity identity = invocation.getArgument(1);
            return summaries.get(identity.userId());
        });
    }

    private static InsuranceInterventionWorkbenchService.Identity identity(String subjectRef, String userId) {
        return new InsuranceInterventionWorkbenchService.Identity(subjectRef, userId, "name-" + userId, 45, "M", null);
    }

    private static InsuranceInterventionWorkbenchResponse.SubjectSummary summary(
            String workflow, String level, Double riskScore, Double rhi, Double rdi, Double adherence,
            Boolean riskMock, Boolean rdiMock, String intervention
    ) {
        return new InsuranceInterventionWorkbenchResponse.SubjectSummary(
                "subject", "name", 45, "M", null, workflow, riskScore, level, riskMock, List.of(),
                rhi, null, rdi, null, null, rdiMock, null, adherence, null, null, 28,
                null, null, intervention, null, "2026-08-24T00:00:00");
    }

    private void risk(String userId, String level, String evaluatedAt) {
        jdbc.update("INSERT INTO rehealth_cvd_risk_result(id, user_id, risk_level, evaluated_at) VALUES (?,?,?,?)",
                UUID.randomUUID().toString(), userId, level, evaluatedAt);
    }

    private void occurrence(String id, String subjectRef, String scheduledAt) {
        jdbc.update("""
                INSERT INTO rehealth_care_plan_occurrence(id, tenant_id, subject_ref, status, scheduled_at)
                VALUES (?,?,?, 'scheduled', ?)
                """, id, TENANT, subjectRef, scheduledAt);
    }

    private void executions(String subjectRef, int distinctDays) {
        for (int day = 1; day <= distinctDays; day++) {
            jdbc.update("""
                    INSERT INTO rehealth_care_plan_execution(id, tenant_id, subject_ref, occurred_at)
                    VALUES (?,?,?,?)
                    """, UUID.randomUUID().toString(), TENANT, subjectRef,
                    "2026-08-" + String.format("%02d", day) + " 10:00:00");
        }
    }

    private void rhi(String id, String userId, String scoredOn, double score) {
        jdbc.update("INSERT INTO rehealth_rhi_daily_snapshot(id, user_id, scored_on, display_score) VALUES (?,?,?,?)",
                id, userId, scoredOn, score);
    }

    private void rdi(String id, String userId, String scoredOn, double score, int mock) {
        jdbc.update("INSERT INTO rehealth_rdi_daily_snapshot(id, user_id, scored_on, display_score, is_mock) VALUES (?,?,?,?,?)",
                id, userId, scoredOn, score, mock);
    }

    private void rdiSnapshot(String id, String userId, String scoredOn, int mock) {
        rdi(id, userId, scoredOn, 10.0, mock);
    }

    private void contribution(String id, String snapshotId, String factorCode, String domainCode, double points) {
        jdbc.update("""
                INSERT INTO rehealth_rdi_contribution(id, snapshot_id, factor_code, domain_code, final_points)
                VALUES (?,?,?,?,?)
                """, id, snapshotId, factorCode, domainCode, points);
    }

    private void createSchema() {
        jdbc.execute("""
                CREATE TABLE rehealth_cvd_risk_result(
                  id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64), risk_level VARCHAR(32),
                  evaluated_at TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, owner_type VARCHAR(32),
                  subject_ref VARCHAR(64), status VARCHAR(32)
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_occurrence(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, subject_ref VARCHAR(64),
                  status VARCHAR(32), scheduled_at TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_care_plan_execution(
                  id VARCHAR(64) PRIMARY KEY, tenant_id INT, subject_ref VARCHAR(64),
                  occurred_at TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_rhi_daily_snapshot(
                  id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64), scored_on DATE,
                  display_score DECIMAL(8,4)
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_rdi_daily_snapshot(
                  id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64), scored_on DATE,
                  display_score DECIMAL(8,4), is_mock TINYINT
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_rdi_contribution(
                  id VARCHAR(64) PRIMARY KEY, snapshot_id VARCHAR(64), factor_code VARCHAR(64),
                  domain_code VARCHAR(64), final_points DECIMAL(10,6)
                )
                """);
    }
}
