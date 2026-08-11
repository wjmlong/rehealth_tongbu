package org.jeecg.modules.rehealth.insurance;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsurancePermissionSeedTest {
    private static final String RESOURCE =
            "db/software/mysql/V20260811_1__seed_insurance_risk_permission.sql";

    @Test
    void seedDefinesPermissionButNeverAutoGrantsARole() throws Exception {
        String sql = new ClassPathResource(RESOURCE).getContentAsString(StandardCharsets.UTF_8);

        assertTrue(sql.contains("rehealth:insurance:risk:view"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertFalse(Pattern.compile(
                "(?im)^\\s*INSERT\\s+INTO\\s+sys_role_permission"
        ).matcher(sql).find());
    }
}
