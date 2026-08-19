package org.jeecg.modules.rehealth.insurance;

import org.jeecg.config.mybatis.MybatisPlusSaasConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsuranceSchemaMigrationTest {
    private static String read(String resource) throws Exception {
        return new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8);
    }

    private static void assertEveryTableAndColumnHasAComment(String sql, String table) {
        String createPrefix = "CREATE TABLE IF NOT EXISTS " + table + " (";
        int createAt = sql.indexOf(createPrefix);
        assertTrue(createAt >= 0, table);

        int bodyAt = createAt + createPrefix.length();
        int bodyEnd = sql.indexOf(") ENGINE=", bodyAt);
        assertTrue(bodyEnd > bodyAt, table + " table body");
        for (String rawLine : sql.substring(bodyAt, bodyEnd).lines().toList()) {
            String line = rawLine.trim();
            if (line.isEmpty()
                    || line.startsWith("PRIMARY KEY")
                    || line.startsWith("UNIQUE KEY")
                    || line.startsWith("KEY ")
                    || line.startsWith("CONSTRAINT ")) {
                continue;
            }
            assertTrue(line.contains(" COMMENT '"), table + " column comment: " + line);
            assertTrue(line.matches(".*[\\u4E00-\\u9FFF].*"), table + " Chinese column comment: " + line);
        }

        int statementEnd = sql.indexOf(';', bodyEnd);
        assertTrue(statementEnd > bodyEnd, table + " statement end");
        String tableOptions = sql.substring(bodyEnd, statementEnd);
        assertTrue(tableOptions.contains(" COMMENT='"), table + " table comment");
        assertTrue(tableOptions.matches("(?s).*[\\u4E00-\\u9FFF].*"), table + " Chinese table comment");
    }

    @Test
    void businessSchemaIsTenantScopedAndDoesNotPersistRawTelemetry() throws Exception {
        String sql = read("db/software/mysql/V20260812_2__create_insurance_business_schema.sql");

        List<String> tables = List.of(
                "rehealth_insurance_subject",
                "rehealth_insurance_policy",
                "rehealth_insurance_coverage",
                "rehealth_insurance_consent",
                "rehealth_insurance_intervention",
                "rehealth_insurance_claim",
                "rehealth_insurance_study",
                "rehealth_insurance_study_snapshot",
                "rehealth_insurance_study_member",
                "rehealth_insurance_study_result",
                "rehealth_insurance_rwe_report",
                "rehealth_insurance_settlement_package",
                "rehealth_insurance_settlement_approval",
                "rehealth_insurance_audit_event"
        );

        for (String table : tables) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table), table);
        }
        assertTrue(sql.contains("tenant_id INT NOT NULL"));
        assertTrue(sql.contains("subject_ref CHAR(64)"));
        assertTrue(sql.contains("snapshot_hash CHAR(64)"));
        assertFalse(sql.toLowerCase().contains("raw_ppg"));
        assertFalse(sql.toLowerCase().contains("raw_rri"));
        assertTrue(sql.contains("software-V20260812.2"));
    }

    @Test
    void roleSeedCreatesTemplatesAndOnlyGrantsTheReadPermission() throws Exception {
        String sql = read("db/software/mysql/V20260812_3__seed_insurer_roles.sql");

        assertTrue(sql.contains("role_code = 'insurer_analyst'"));
        assertTrue(sql.contains("role_code = 'insurance_operator'"));
        assertTrue(sql.contains("rehealth:insurance:risk:view"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertFalse(sql.contains("sys_user_role"));
        assertTrue(sql.contains("software-V20260812.3"));
    }

    @Test
    void workflowExtensionAddsImportJobsAndAppFeedbackWithoutRawTelemetry() throws Exception {
        String sql = read("db/software/mysql/V20260813_1__extend_insurance_workflow.sql");

        for (String table : List.of(
                "rehealth_insurance_import_batch",
                "rehealth_insurance_study_job",
                "rehealth_insurance_plan_binding",
                "rehealth_insurance_intervention_feedback"
        )) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table), table);
        }
        assertTrue(sql.contains("idempotency_key"));
        assertTrue(sql.contains("source_record_id"));
        assertTrue(sql.contains("covariate_json"));
        assertFalse(sql.toLowerCase().contains("raw_ppg"));
        assertFalse(sql.toLowerCase().contains("raw_rri"));
        assertTrue(sql.contains("software-V20260813.1"));
    }

    @Test
    void workflowPermissionSeedDefinesSegregatedRolesWithoutAssigningUsers() throws Exception {
        String sql = read("db/software/mysql/V20260813_2__seed_insurer_workflow_permissions.sql");

        for (String role : List.of("insurer_viewer", "insurer_analyst", "insurance_operator", "insurer_auditor")) {
            assertTrue(sql.contains("'" + role + "'"), role);
        }
        for (String permission : List.of(
                "rehealth:insurance:business:import",
                "rehealth:insurance:study:manage",
                "rehealth:insurance:report:manage",
                "rehealth:insurance:settlement:operate",
                "rehealth:insurance:audit:view"
        )) {
            assertTrue(sql.contains(permission), permission);
        }
        assertFalse(sql.contains("sys_user_role"));
        assertTrue(sql.contains("software-V20260813.2"));
    }

    @Test
    void adminGrantKeepsLocalAcceptanceUsableWithoutAssigningRolesToUsers() throws Exception {
        String sql = read("db/software/mysql/V20260813_3__grant_insurance_workflow_to_admin.sql");

        assertTrue(sql.contains("role.role_code = 'admin'"));
        assertTrue(sql.contains("rehealth:insurance:business:import"));
        assertTrue(sql.contains("rehealth:insurance:settlement:operate"));
        assertFalse(sql.contains("sys_user_role"));
        assertTrue(sql.contains("software-V20260813.3"));
    }

    @Test
    void settingsMigrationCreatesOrganizationProfileAndSeparatedRoles() throws Exception {
        String sql = read("db/software/mysql/V20260813_6__create_insurance_settings.sql");
        assertTrue(sql.contains("rehealth_insurance_tenant_profile"));
        assertTrue(sql.contains("rehealth:insurance:organization:view"));
        assertTrue(sql.contains("insurance_org_admin"));
        assertTrue(sql.contains("insurance_department_manager"));
        assertTrue(sql.contains("software-V20260813.6"));
    }

    @Test
    void settingsAdminGrantKeepsLocalAcceptanceAccountUsable() throws Exception {
        String sql = read("db/software/mysql/V20260813_7__grant_insurance_settings_to_admin.sql");
        assertTrue(sql.contains("role.role_code = 'admin'"));
        for (String permission : List.of(
                "rehealth:insurance:organization:view",
                "rehealth:insurance:organization:edit",
                "rehealth:insurance:department:manage",
                "rehealth:insurance:member:view",
                "rehealth:insurance:member:manage",
                "rehealth:insurance:role:assign",
                "rehealth:insurance:assignment:manage")) {
            assertTrue(sql.contains(permission), permission);
        }
        assertTrue(sql.contains("software-V20260813.7"));
    }

    @Test
    void insurerRolesCanReadOrganizationAndMemberSettings() throws Exception {
        String sql = read("db/software/mysql/V20260814_1__grant_insurance_settings_view.sql");

        for (String role : List.of(
                "insurer_viewer",
                "insurer_analyst",
                "insurance_operator",
                "insurer_auditor")) {
            assertTrue(sql.contains(role), role);
        }
        assertTrue(sql.contains("rehealth:insurance:organization:view"));
        assertTrue(sql.contains("rehealth:insurance:member:view"));
        assertFalse(sql.contains("rehealth:insurance:member:manage"));
        assertTrue(sql.contains("software-V20260814.1"));
    }

    @Test
    void departmentMigrationUsesTenantScopedCodesAndNormalizesTheLocalTree() throws Exception {
        String sql = read("db/software/mysql/V20260813_8__isolate_department_codes_by_tenant.sql");

        assertTrue(sql.contains("uniq_depart_tenant_org_code (tenant_id, org_code)"));
        assertTrue(sql.contains("DROP INDEX uniq_depart_org_code"));
        assertTrue(sql.contains("'iqdep000000000000000000000001' THEN 'A01'"));
        assertTrue(sql.contains("'iqdep000000000000000000000002' THEN 'A01A01'"));
        assertTrue(sql.contains("software-V20260813.8"));
        assertTrue(MybatisPlusSaasConfig.TENANT_TABLE.contains("sys_depart"));
        assertFalse(MybatisPlusSaasConfig.OPEN_SYSTEM_TENANT_CONTROL);
    }

    @Test
    void interventionWorkbenchMigrationAddsAuditedActionsAndSeparatedWritePermission() throws Exception {
        String sql = read("db/software/mysql/V20260814_2__create_insurance_intervention_actions.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS rehealth_insurance_intervention_action"));
        assertTrue(sql.contains("tenant_id INT NOT NULL"));
        assertTrue(sql.contains("rehealth:insurance:intervention:manage"));
        assertTrue(sql.contains("insurance_org_admin"));
        assertTrue(sql.contains("insurance_department_manager"));
        assertTrue(sql.contains("insurance_operator"));
        assertFalse(sql.contains("insurer_viewer', 'rehealth:insurance:intervention:manage"));
        assertTrue(sql.contains("software-V20260814.2"));
    }

    @Test
    void rhiSnapshotMigrationStoresOnlyDailyAggregates() throws Exception {
        String sql = read("db/software/mysql/V20260814_3__create_rhi_daily_snapshot.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS rehealth_rhi_daily_snapshot"));
        assertTrue(sql.contains("UNIQUE KEY uk_rhi_daily_user_date (user_id, scored_on)"));
        assertTrue(sql.contains("domains_json"));
        assertTrue(sql.contains("quality_json"));
        assertFalse(sql.toLowerCase().contains("raw_ppg"));
        assertFalse(sql.toLowerCase().contains("raw_rri"));
        assertTrue(sql.contains("software-V20260814.3"));
    }

    @Test
    void rdiSnapshotMigrationSeparatesDailyAggregateAndStructuredContributions() throws Exception {
        String sql = read("db/software/mysql/V20260814_4__create_rdi_daily_snapshot.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS rehealth_rdi_daily_snapshot"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS rehealth_rdi_contribution"));
        assertTrue(sql.contains("UNIQUE KEY uk_rdi_daily_user_date (user_id, scored_on)"));
        assertTrue(sql.contains("source_factor_id"));
        assertFalse(sql.toLowerCase().contains("evidence_text"));
        assertFalse(sql.toLowerCase().contains("raw_ppg"));
        assertFalse(sql.toLowerCase().contains("raw_rri"));
        assertTrue(sql.contains("software-V20260814.4"));
    }

    @Test
    void versionedCarePlanMigrationFreezesPublishedContentAndBindsTaskOccurrences() throws Exception {
        String sql = read("db/software/mysql/V20260819_1__create_versioned_care_plans.sql");
        for (String table : List.of(
                "rehealth_care_plan",
                "rehealth_care_plan_revision",
                "rehealth_care_plan_item",
                "rehealth_care_plan_occurrence",
                "rehealth_care_plan_audit_event"
        )) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table), table);
            assertEveryTableAndColumnHasAComment(sql, table);
        }
        assertTrue(sql.contains("lock_version BIGINT NOT NULL"));
        assertTrue(sql.contains("revision_id VARCHAR(64) NOT NULL"));
        assertTrue(sql.contains("logical_item_id VARCHAR(64) NOT NULL"));
        assertTrue(sql.contains("COMMENT='发布后内容不可变的机构关怀计划版本表'"));
        assertTrue(sql.contains("rehealth:insurance:care-plan:manage"));
        assertTrue(sql.contains("rehealth:insurance:care-plan:publish"));
        assertTrue(sql.contains("software-V20260819.1"));
        assertFalse(sql.toLowerCase().contains("raw_ppg"));
        assertFalse(sql.toLowerCase().contains("raw_rri"));
    }
}
