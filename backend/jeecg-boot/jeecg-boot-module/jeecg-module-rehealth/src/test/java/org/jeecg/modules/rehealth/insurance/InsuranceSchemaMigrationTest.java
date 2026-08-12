package org.jeecg.modules.rehealth.insurance;

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
}
