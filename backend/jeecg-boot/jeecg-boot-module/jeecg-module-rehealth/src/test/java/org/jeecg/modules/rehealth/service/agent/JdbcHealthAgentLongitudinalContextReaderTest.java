package org.jeecg.modules.rehealth.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JdbcHealthAgentLongitudinalContextReaderTest {
    @Test
    void readsOnlyTheRequestedUsersBoundedLongitudinalProjections() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:health_agent_context;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        createSchema(jdbc);
        seed(jdbc, "user-a", 78.0, 54.0);
        seed(jdbc, "user-b", 99.0, 88.0);
        JdbcHealthAgentLongitudinalContextReader reader =
                new JdbcHealthAgentLongitudinalContextReader(jdbc, new ObjectMapper());

        Map<String, Object> context = reader.read("user-a");

        Map<?, ?> rhi = (Map<?, ?>) context.get("rhi");
        Map<?, ?> latestRhi = (Map<?, ?>) rhi.get("latest");
        assertEquals(78.0, latestRhi.get("displayScore"));
        assertEquals(1, ((List<?>) rhi.get("recentTrendNewestFirst")).size());
        Map<?, ?> rdi = (Map<?, ?>) context.get("rdi");
        Map<?, ?> latestRdi = (Map<?, ?>) rdi.get("latest");
        assertEquals(54.0, latestRdi.get("displayScore"));
        assertEquals(false, latestRdi.get("isMock"));
        assertEquals(1, ((List<?>) latestRdi.get("contributions")).size());
        assertEquals("insufficient_data", ((Map<?, ?>) context.get("latestAttribution")).get("status"));
        assertEquals(1, ((List<?>) context.get("recentInterventionFeedback")).size());
        assertFalse(context.toString().contains("99.0"));
        assertFalse(context.toString().contains("88.0"));
    }

    private void createSchema(JdbcTemplate jdbc) {
        jdbc.execute("""
                CREATE TABLE rehealth_rhi_daily_snapshot (
                  id VARCHAR(64), user_id VARCHAR(64), scored_on DATE, raw_score DOUBLE,
                  display_score DOUBLE, data_confidence DOUBLE, status VARCHAR(32),
                  product_tier VARCHAR(32), available_days INT, available_feature_count INT,
                  algorithm_version VARCHAR(128), calculation_source VARCHAR(64),
                  domains_json CLOB, features_json CLOB, quality_json CLOB, updated_at TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_rdi_daily_snapshot (
                  id VARCHAR(64), user_id VARCHAR(64), scored_on DATE, raw_score DOUBLE,
                  display_score DOUBLE, data_confidence DOUBLE, status VARCHAR(32),
                  is_mock BOOLEAN, algorithm_version VARCHAR(128), calculation_source VARCHAR(64),
                  updated_at TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_rdi_contribution (
                  snapshot_id VARCHAR(64), factor_code VARCHAR(64), domain_code VARCHAR(64),
                  source_code VARCHAR(64), current_value DOUBLE, baseline_value DOUBLE,
                  unit VARCHAR(32), final_points DOUBLE, confidence DOUBLE
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_attribution_result (
                  id VARCHAR(64), user_id VARCHAR(64), intervention_data_sufficient BOOLEAN,
                  is_mock BOOLEAN, history_days INT, min_history_days INT, intervention_days INT,
                  adherence_average DOUBLE, individual_att DOUBLE, trend_delta DOUBLE,
                  status VARCHAR(64), interpretation VARCHAR(1000), model_version VARCHAR(128),
                  created_at TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_intervention_feedback (
                  user_id VARCHAR(64), status VARCHAR(64), adherence DOUBLE, note VARCHAR(2000),
                  checked_at TIMESTAMP, created_at TIMESTAMP
                )
                """);
    }

    private void seed(JdbcTemplate jdbc, String userId, double rhi, double rdi) {
        String suffix = userId.substring(userId.length() - 1);
        jdbc.update("""
                INSERT INTO rehealth_rhi_daily_snapshot VALUES (
                  ?, ?, DATE '2026-08-21', ?, ?, 0.8, 'CONFIRMED', 'standard', 30, 16,
                  'rhi-v2', 'android', '[{\"domain\":\"activity\"}]',
                  '[{\"feature\":\"steps\"}]', '{}', TIMESTAMP '2026-08-21 08:00:00')
                """, "rhi-" + suffix, userId, rhi, rhi);
        jdbc.update("""
                INSERT INTO rehealth_rdi_daily_snapshot VALUES (
                  ?, ?, DATE '2026-08-21', ?, ?, 0.7, 'CONFIRMED', FALSE,
                  'rdi-v1', 'android', TIMESTAMP '2026-08-21 08:00:00')
                """, "rdi-" + suffix, userId, rdi, rdi);
        jdbc.update("""
                INSERT INTO rehealth_rdi_contribution VALUES (
                  ?, 'sleep', 'recovery', 'wearable', 420, 450, 'minutes', -1.2, 0.8)
                """, "rdi-" + suffix);
        jdbc.update("""
                INSERT INTO rehealth_attribution_result VALUES (
                  ?, ?, FALSE, FALSE, 12, 30, 2, 0.6, NULL, -0.01,
                  'insufficient_data', '需要更多数据', 'pias-v1', TIMESTAMP '2026-08-21 08:00:00')
                """, "attr-" + suffix, userId);
        jdbc.update("""
                INSERT INTO rehealth_intervention_feedback VALUES (
                  ?, 'completed', 1.0, '已完成', TIMESTAMP '2026-08-20 08:00:00',
                  TIMESTAMP '2026-08-20 08:00:00')
                """, userId);
    }
}
