package org.jeecg.modules.rehealth.service.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class JdbcHealthAgentLongitudinalContextReader implements HealthAgentLongitudinalContextReader {
    private static final int HISTORY_LIMIT = 30;
    private static final int JSON_LIMIT = 16_000;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcHealthAgentLongitudinalContextReader(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            ObjectMapper objectMapper
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> read(String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new IllegalArgumentException("authenticated user is required");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> coverage = new LinkedHashMap<>();
        section(result, coverage, "rhi", () -> rhi(authenticatedUserId));
        section(result, coverage, "rdi", () -> rdi(authenticatedUserId));
        section(result, coverage, "latestAttribution", () -> latestAttribution(authenticatedUserId));
        section(result, coverage, "recentInterventionFeedback", () -> recentFeedback(authenticatedUserId));
        result.put("coverage", coverage);
        return result;
    }

    private Map<String, Object> rhi(String userId) {
        Map<String, Object> section = new LinkedHashMap<>();
        jdbc.query("""
                SELECT scored_on, raw_score, display_score, data_confidence, status,
                       product_tier, available_days, available_feature_count,
                       algorithm_version, calculation_source, domains_json, features_json,
                       quality_json, updated_at
                FROM rehealth_rhi_daily_snapshot
                WHERE user_id=? ORDER BY scored_on DESC LIMIT 1
                """, rs -> {
            Map<String, Object> latest = new LinkedHashMap<>();
            put(latest, "date", rs.getDate(1) == null ? null : rs.getDate(1).toString());
            put(latest, "rawScore", nullableDouble(rs, 2));
            put(latest, "displayScore", nullableDouble(rs, 3));
            put(latest, "dataConfidence", nullableDouble(rs, 4));
            put(latest, "status", rs.getString(5));
            put(latest, "productTier", rs.getString(6));
            put(latest, "availableDays", nullableInteger(rs, 7));
            put(latest, "availableFeatureCount", nullableInteger(rs, 8));
            put(latest, "algorithmVersion", rs.getString(9));
            put(latest, "calculationSource", rs.getString(10));
            put(latest, "domains", parseJson(rs.getString(11)));
            put(latest, "features", parseJson(rs.getString(12)));
            put(latest, "quality", parseJson(rs.getString(13)));
            put(latest, "updatedAt", instant(rs.getTimestamp(14)));
            section.put("latest", latest);
        }, userId);
        List<Map<String, Object>> trend = jdbc.query("""
                SELECT scored_on, display_score, data_confidence, status
                FROM rehealth_rhi_daily_snapshot
                WHERE user_id=? ORDER BY scored_on DESC LIMIT ?
                """, (rs, rowNum) -> mapTrend(rs, false), userId, HISTORY_LIMIT);
        if (!trend.isEmpty()) section.put("recentTrendNewestFirst", trend);
        return section;
    }

    private Map<String, Object> rdi(String userId) {
        Map<String, Object> section = new LinkedHashMap<>();
        jdbc.query("""
                SELECT id, scored_on, raw_score, display_score, data_confidence, status,
                       is_mock, algorithm_version, calculation_source, updated_at
                FROM rehealth_rdi_daily_snapshot
                WHERE user_id=? ORDER BY scored_on DESC LIMIT 1
                """, rs -> {
            Map<String, Object> latest = new LinkedHashMap<>();
            String snapshotId = rs.getString(1);
            put(latest, "date", rs.getDate(2) == null ? null : rs.getDate(2).toString());
            put(latest, "rawScore", nullableDouble(rs, 3));
            put(latest, "displayScore", nullableDouble(rs, 4));
            put(latest, "dataConfidence", nullableDouble(rs, 5));
            put(latest, "status", rs.getString(6));
            put(latest, "isMock", nullableBoolean(rs, 7));
            put(latest, "algorithmVersion", rs.getString(8));
            put(latest, "calculationSource", rs.getString(9));
            put(latest, "updatedAt", instant(rs.getTimestamp(10)));
            List<Map<String, Object>> contributions = jdbc.query("""
                    SELECT factor_code, domain_code, source_code, current_value,
                           baseline_value, unit, final_points, confidence
                    FROM rehealth_rdi_contribution
                    WHERE snapshot_id=?
                    ORDER BY ABS(final_points) DESC, factor_code
                    LIMIT 64
                    """, this::mapContribution, snapshotId);
            if (!contributions.isEmpty()) latest.put("contributions", contributions);
            section.put("latest", latest);
        }, userId);
        List<Map<String, Object>> trend = jdbc.query("""
                SELECT scored_on, display_score, data_confidence, status, is_mock
                FROM rehealth_rdi_daily_snapshot
                WHERE user_id=? ORDER BY scored_on DESC LIMIT ?
                """, (rs, rowNum) -> mapTrend(rs, true), userId, HISTORY_LIMIT);
        if (!trend.isEmpty()) section.put("recentTrendNewestFirst", trend);
        return section;
    }

    private Map<String, Object> latestAttribution(String userId) {
        return jdbc.query("""
                SELECT intervention_data_sufficient, is_mock, history_days, min_history_days,
                       intervention_days, adherence_average, individual_att, trend_delta,
                       status, interpretation, model_version, created_at
                FROM rehealth_attribution_result
                WHERE user_id=? ORDER BY created_at DESC, id DESC LIMIT 1
                """, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            put(item, "dataSufficient", nullableBoolean(rs, 1));
            put(item, "isMock", nullableBoolean(rs, 2));
            put(item, "historyDays", nullableInteger(rs, 3));
            put(item, "minimumHistoryDays", nullableInteger(rs, 4));
            put(item, "interventionDays", nullableInteger(rs, 5));
            put(item, "adherenceAverage", nullableDouble(rs, 6));
            put(item, "individualEffect", nullableDouble(rs, 7));
            put(item, "trendDelta", nullableDouble(rs, 8));
            put(item, "status", rs.getString(9));
            put(item, "interpretation", bounded(rs.getString(10), 600));
            put(item, "modelVersion", rs.getString(11));
            put(item, "createdAt", instant(rs.getTimestamp(12)));
            return item;
        }, userId).stream().findFirst().orElseGet(Collections::emptyMap);
    }

    private List<Map<String, Object>> recentFeedback(String userId) {
        return jdbc.query("""
                SELECT status, adherence, note, checked_at, created_at
                FROM rehealth_intervention_feedback
                WHERE user_id=? ORDER BY created_at DESC LIMIT 20
                """, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            put(item, "status", rs.getString(1));
            put(item, "adherence", nullableDouble(rs, 2));
            put(item, "note", bounded(rs.getString(3), 500));
            put(item, "checkedAt", instant(rs.getTimestamp(4)));
            put(item, "createdAt", instant(rs.getTimestamp(5)));
            return item;
        }, userId);
    }

    private Map<String, Object> mapTrend(ResultSet rs, boolean hasMock) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        put(item, "date", rs.getDate(1) == null ? null : rs.getDate(1).toString());
        put(item, "displayScore", nullableDouble(rs, 2));
        put(item, "dataConfidence", nullableDouble(rs, 3));
        put(item, "status", rs.getString(4));
        if (hasMock) put(item, "isMock", nullableBoolean(rs, 5));
        return item;
    }

    private Map<String, Object> mapContribution(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        put(item, "factor", rs.getString(1));
        put(item, "domain", rs.getString(2));
        put(item, "source", rs.getString(3));
        put(item, "currentValue", nullableDouble(rs, 4));
        put(item, "baselineValue", nullableDouble(rs, 5));
        put(item, "unit", rs.getString(6));
        put(item, "finalPoints", nullableDouble(rs, 7));
        put(item, "confidence", nullableDouble(rs, 8));
        return item;
    }

    private void section(
            Map<String, Object> result,
            Map<String, Object> coverage,
            String name,
            Supplier<Object> supplier
    ) {
        try {
            Object value = supplier.get();
            if (empty(value)) {
                coverage.put(name, "no_data");
            } else {
                result.put(name, value);
                coverage.put(name, "available");
            }
        } catch (RuntimeException failure) {
            coverage.put(name, "unavailable");
        }
    }

    private JsonNode parseJson(String value) {
        if (value == null || value.isBlank() || value.length() > JSON_LIMIT) return null;
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private static boolean empty(Object value) {
        return value == null
                || value instanceof Map<?, ?> map && map.isEmpty()
                || value instanceof List<?> list && list.isEmpty();
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (value != null && (!(value instanceof String text) || !text.isBlank())) target.put(key, value);
    }

    private static Double nullableDouble(ResultSet rs, int column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInteger(ResultSet rs, int column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Boolean nullableBoolean(ResultSet rs, int column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private static String instant(Timestamp value) {
        return value == null ? null : value.toInstant().toString();
    }

    private static String bounded(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.strip();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
