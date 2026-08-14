package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceInterventionWorkbenchService {
    private static final String SCOPE_SQL = """
            FROM rehealth_insurance_subject subject
            INNER JOIN rehealth_insurance_subject_manager scope
              ON scope.tenant_id = subject.tenant_id
             AND scope.subject_ref = subject.subject_ref
             AND scope.manager_user_id = ?
             AND scope.status = 'active'
            LEFT JOIN rehealth_patient_profile profile
              ON profile.user_id = subject.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
            WHERE subject.tenant_id = ? AND subject.enrollment_status = 'active'
            """;
    private static final Set<String> ACTION_STATUSES = Set.of("pending", "in_progress", "completed", "cancelled");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public InsuranceInterventionWorkbenchService(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            ObjectMapper objectMapper
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public InsuranceInterventionWorkbenchResponse.Dashboard dashboard(int tenantId, String managerUserId) {
        List<Identity> identities = identities(tenantId, managerUserId, null, Integer.MAX_VALUE, 0);
        List<InsuranceInterventionWorkbenchResponse.SubjectSummary> summaries = identities.stream()
                .map(value -> summary(tenantId, value)).toList();
        long pendingAction = summaries.stream().filter(v -> "pending_action".equals(v.workflowStatus())).count();
        long inProgress = summaries.stream().filter(v -> "in_progress".equals(v.workflowStatus())).count();
        long pendingReview = summaries.stream().filter(v -> "pending_review".equals(v.workflowStatus())).count();
        long improved = summaries.stream().filter(v -> "improved".equals(v.workflowStatus())).count();
        BigDecimal adherence = summaries.stream().map(InsuranceInterventionWorkbenchResponse.SubjectSummary::adherenceScore)
                .filter(v -> v != null).map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        long adherenceCount = summaries.stream().filter(v -> v.adherenceScore() != null).count();
        if (adherenceCount > 0) adherence = adherence.divide(BigDecimal.valueOf(adherenceCount), 4, RoundingMode.HALF_UP);
        else adherence = null;
        String updated = summaries.stream().map(InsuranceInterventionWorkbenchResponse.SubjectSummary::updatedAt)
                .filter(v -> v != null).max(String::compareTo).orElse(null);
        return new InsuranceInterventionWorkbenchResponse.Dashboard(
                "assigned_subjects", summaries.size(), pendingAction, inProgress, pendingReview,
                improved, adherence, updated);
    }

    public InsuranceInterventionWorkbenchResponse.SubjectPage subjects(
            int tenantId, String managerUserId, int pageNo, int pageSize, String keyword, String workflowStatus
    ) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        List<Identity> all = identities(tenantId, managerUserId, keyword, Integer.MAX_VALUE, 0);
        List<InsuranceInterventionWorkbenchResponse.SubjectSummary> summaries = all.stream()
                .map(value -> summary(tenantId, value))
                .filter(value -> workflowStatus == null || workflowStatus.isBlank()
                        || workflowStatus.trim().equals(value.workflowStatus()))
                .sorted(Comparator.comparing(InsuranceInterventionWorkbenchResponse.SubjectSummary::updatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int from = Math.min((safePage - 1) * safeSize, summaries.size());
        int to = Math.min(from + safeSize, summaries.size());
        return new InsuranceInterventionWorkbenchResponse.SubjectPage(
                "assigned_subjects", safePage, safeSize, summaries.size(), summaries.subList(from, to));
    }

    public InsuranceInterventionWorkbenchResponse.SubjectDetail subject(
            int tenantId, String managerUserId, String subjectId
    ) {
        Identity identity = requireIdentity(tenantId, managerUserId, subjectId);
        InsuranceInterventionWorkbenchResponse.SubjectSummary summary = summary(tenantId, identity);
        RiskSnapshot latestRisk = latestRisk(identity.userId());
        return new InsuranceInterventionWorkbenchResponse.SubjectDetail(
                "assigned_subjects", summary, riskTrend(identity.userId()), rhiTrend(identity.userId()),
                factors(latestRisk == null ? null : latestRisk.responseJson()), plan(tenantId, identity),
                feedback(tenantId, identity.subjectRef()), actions(tenantId, identity.subjectRef()),
                attribution(identity.userId()),
                Boolean.TRUE.equals(summary.riskIsMock())
                        ? "当前风险或干预包含演练数据，不可作为真实改善结论。"
                        : "趋势为描述性证据；只有非 Mock 且数据充分的归因结果才显示为改善。"
        );
    }

    @Transactional
    public InsuranceInterventionWorkbenchResponse.Action createAction(
            int tenantId, String managerUserId, String actorUserId, String subjectId,
            InsuranceInterventionWorkbenchRequest.CreateAction request
    ) {
        Identity identity = requireIdentity(tenantId, managerUserId, subjectId);
        if (request == null) throw InsuranceApiException.badRequest("action request is required");
        String type = required(request.actionType(), "action_type", 32);
        String title = required(request.title(), "title", 255);
        String content = optional(request.content(), 2000);
        String assignee = optional(request.assigneeUserId(), 32);
        if (assignee != null) requireTenantStaff(tenantId, assignee);
        String requestId = optional(request.requestId(), 128);
        String id = uuid();
        LocalDateTime now = LocalDateTime.now();
        try {
            jdbc.update("""
                    INSERT INTO rehealth_insurance_intervention_action (
                      id, tenant_id, subject_ref, plan_id, action_type, title, content,
                      assignee_user_id, status, due_at, completed_at, result_json,
                      created_by, request_id, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?, NULL, NULL, ?, ?, ?, ?)
                    """, id, tenantId, identity.subjectRef(), optional(request.planId(), 128), type, title, content,
                    assignee, request.dueAt(), actorUserId, requestId, now, now);
        } catch (DuplicateKeyException e) {
            if (requestId == null) throw e;
            String existing = jdbc.queryForObject(
                    "SELECT id FROM rehealth_insurance_intervention_action WHERE tenant_id=? AND request_id=?",
                    String.class, tenantId, requestId);
            return actionById(tenantId, existing);
        }
        audit(tenantId, actorUserId, "intervention_action.create", id, requestId, null,
                Map.of("subjectRef", identity.subjectRef(), "status", "pending"));
        return actionById(tenantId, id);
    }

    @Transactional
    public InsuranceInterventionWorkbenchResponse.Action updateAction(
            int tenantId, String managerUserId, String actorUserId, String actionId,
            InsuranceInterventionWorkbenchRequest.UpdateAction request
    ) {
        String subjectRef = jdbc.query("""
                SELECT subject_ref FROM rehealth_insurance_intervention_action
                WHERE tenant_id=? AND id=? LIMIT 1
                """, (rs, row) -> rs.getString(1), tenantId, required(actionId, "actionId", 64))
                .stream().findFirst().orElseThrow(() -> InsuranceApiException.notFound("intervention action was not found"));
        requireIdentity(tenantId, managerUserId, subjectRef);
        if (request == null) throw InsuranceApiException.badRequest("action request is required");
        String status = required(request.status(), "status", 32).toLowerCase();
        if (!ACTION_STATUSES.contains(status)) throw InsuranceApiException.badRequest("unsupported action status");
        String assignee = optional(request.assigneeUserId(), 32);
        if (assignee != null) requireTenantStaff(tenantId, assignee);
        InsuranceInterventionWorkbenchResponse.Action before = actionById(tenantId, actionId);
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                UPDATE rehealth_insurance_intervention_action
                SET status=?, assignee_user_id=COALESCE(?, assignee_user_id),
                    due_at=COALESCE(?, due_at), completed_at=?, result_json=?, updated_at=?
                WHERE tenant_id=? AND id=?
                """, status, assignee, request.dueAt(), "completed".equals(status) ? now : null,
                request.result() == null ? null : json(request.result()), now, tenantId, actionId);
        InsuranceInterventionWorkbenchResponse.Action after = actionById(tenantId, actionId);
        audit(tenantId, actorUserId, "intervention_action.update", actionId, null, before, after);
        return after;
    }

    private List<Identity> identities(int tenantId, String managerUserId, String keyword, int limit, int offset) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        return jdbc.query("""
                SELECT subject.subject_ref, subject.rehealth_user_id, profile.name
                """ + SCOPE_SQL + """
                  AND (? IS NULL OR profile.name LIKE ? OR subject.subject_ref = ?)
                ORDER BY profile.name, subject.subject_ref LIMIT ? OFFSET ?
                """, (rs, row) -> new Identity(rs.getString(1), rs.getString(2), rs.getString(3)),
                managerUserId, tenantId, like, like, keyword, limit, offset);
    }

    private Identity requireIdentity(int tenantId, String managerUserId, String subjectId) {
        String normalized = required(subjectId, "subjectId", 64);
        return jdbc.query("SELECT subject.subject_ref, subject.rehealth_user_id, profile.name " + SCOPE_SQL
                        + " AND subject.subject_ref = ? LIMIT 1",
                (rs, row) -> new Identity(rs.getString(1), rs.getString(2), rs.getString(3)),
                managerUserId, tenantId, normalized).stream().findFirst()
                .orElseThrow(() -> InsuranceApiException.notFound("assigned insurance subject was not found"));
    }

    private InsuranceInterventionWorkbenchResponse.SubjectSummary summary(int tenantId, Identity identity) {
        RiskSnapshot risk = latestRisk(identity.userId());
        RhiSnapshot rhi = latestRhi(identity.userId());
        FeedbackAggregate feedback = latestFeedback(tenantId, identity.subjectRef());
        AttributionSnapshot attribution = latestAttribution(identity.userId());
        Owner owner = owner(tenantId, identity.subjectRef());
        Integer openActions = jdbc.queryForObject("""
                SELECT COUNT(*) FROM rehealth_insurance_intervention_action
                WHERE tenant_id=? AND subject_ref=? AND status IN ('pending','in_progress')
                """, Integer.class, tenantId, identity.subjectRef());
        Integer activePrograms = jdbc.queryForObject("""
                SELECT COUNT(*) FROM rehealth_insurance_intervention
                WHERE tenant_id=? AND subject_ref=? AND status IN ('active','enrolled','in_progress')
                """, Integer.class, tenantId, identity.subjectRef());
        String workflow = workflowStatus(risk, attribution,
                (openActions != null && openActions > 0) || (activePrograms != null && activePrograms > 0));
        String updated = StreamDates.max(
                risk == null ? null : format(risk.evaluatedAt()),
                rhi == null ? null : rhi.updatedAt(),
                feedback == null ? null : feedback.occurredAt());
        return new InsuranceInterventionWorkbenchResponse.SubjectSummary(
                identity.subjectRef(), identity.name(), workflow,
                risk == null ? null : risk.score(), risk == null ? null : risk.level(),
                risk == null ? null : risk.isMock(), rhi == null ? null : rhi.score(),
                rhi == null ? null : rhi.confidence(), feedback == null ? null : feedback.adherence(),
                owner == null ? null : owner.name(), owner == null ? null : owner.department(), updated);
    }

    private String workflowStatus(RiskSnapshot risk, AttributionSnapshot attribution, boolean active) {
        boolean improved = attribution != null && !Boolean.TRUE.equals(attribution.isMock())
                && Boolean.TRUE.equals(attribution.dataSufficient())
                && ((attribution.individualAtt() != null && attribution.individualAtt() < 0)
                    || (attribution.trendDelta() != null && attribution.trendDelta() < 0));
        if (improved) return "improved";
        if (active) return "in_progress";
        if (risk != null && !Boolean.TRUE.equals(risk.isMock()) && "high".equals(normalizeLevel(risk.level()))) {
            return "pending_action";
        }
        return "pending_review";
    }

    private RiskSnapshot latestRisk(String userId) {
        return jdbc.query("""
                SELECT risk_score, risk_level, is_mock, response_json, evaluated_at
                FROM rehealth_cvd_risk_result WHERE user_id=?
                ORDER BY evaluated_at DESC, id DESC LIMIT 1
                """, (rs, row) -> new RiskSnapshot(nullableDouble(rs, 1), rs.getString(2), nullableBoolean(rs, 3),
                rs.getString(4), rs.getTimestamp(5)), userId).stream().findFirst().orElse(null);
    }

    private List<InsuranceInterventionWorkbenchResponse.TrendPoint> riskTrend(String userId) {
        return jdbc.query("""
                SELECT DATE(evaluated_at), risk_score, risk_level, is_mock
                FROM rehealth_cvd_risk_result WHERE user_id=?
                  AND evaluated_at >= DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY)
                ORDER BY evaluated_at
                """, (rs, row) -> new InsuranceInterventionWorkbenchResponse.TrendPoint(
                rs.getDate(1).toString(), nullableDouble(rs, 2), rs.getString(3), nullableBoolean(rs, 4)), userId);
    }

    private RhiSnapshot latestRhi(String userId) {
        return jdbc.query("""
                SELECT display_score, data_confidence, updated_at FROM rehealth_rhi_daily_snapshot
                WHERE user_id=? ORDER BY scored_on DESC LIMIT 1
                """, (rs, row) -> new RhiSnapshot(nullableDouble(rs, 1), nullableDouble(rs, 2), format(rs.getTimestamp(3))),
                userId).stream().findFirst().orElse(null);
    }

    private List<InsuranceInterventionWorkbenchResponse.TrendPoint> rhiTrend(String userId) {
        return jdbc.query("""
                SELECT scored_on, display_score, status FROM rehealth_rhi_daily_snapshot
                WHERE user_id=? AND scored_on >= DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY)
                ORDER BY scored_on
                """, (rs, row) -> new InsuranceInterventionWorkbenchResponse.TrendPoint(
                rs.getDate(1).toString(), nullableDouble(rs, 2), rs.getString(3), false), userId);
    }

    private FeedbackAggregate latestFeedback(int tenantId, String subjectRef) {
        return jdbc.query("""
                SELECT AVG(adherence_score), MAX(occurred_at)
                FROM rehealth_insurance_intervention_feedback WHERE tenant_id=? AND subject_ref=?
                """, (rs, row) -> new FeedbackAggregate(nullableDouble(rs, 1), format(rs.getTimestamp(2))),
                tenantId, subjectRef).stream().findFirst().orElse(null);
    }

    private AttributionSnapshot latestAttribution(String userId) {
        return jdbc.query("""
                SELECT intervention_data_sufficient, is_mock, individual_att, trend_delta,
                       status, interpretation, created_at
                FROM rehealth_attribution_result WHERE user_id=? ORDER BY created_at DESC, id DESC LIMIT 1
                """, (rs, row) -> new AttributionSnapshot(nullableBoolean(rs, 1), nullableBoolean(rs, 2),
                nullableDouble(rs, 3), nullableDouble(rs, 4), rs.getString(5), rs.getString(6), rs.getTimestamp(7)),
                userId).stream().findFirst().orElse(null);
    }

    private Owner owner(int tenantId, String subjectRef) {
        return jdbc.query("""
                SELECT account.realname, department.depart_name
                FROM rehealth_insurance_subject_manager manager
                LEFT JOIN sys_user account
                  ON account.id = CONVERT(manager.manager_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                LEFT JOIN sys_depart department
                  ON department.id = CONVERT(manager.department_id USING utf8mb3) COLLATE utf8mb3_general_ci
                WHERE manager.tenant_id=? AND manager.subject_ref=? AND manager.status='active'
                ORDER BY manager.updated_at DESC LIMIT 1
                """, (rs, row) -> new Owner(rs.getString(1), rs.getString(2)), tenantId, subjectRef)
                .stream().findFirst().orElse(null);
    }

    private InsuranceInterventionWorkbenchResponse.Plan plan(int tenantId, Identity identity) {
        return jdbc.query("""
                SELECT binding.plan_id, binding.status, plan.priority_intervention, plan.response_json,
                       plan.is_mock, plan.generated_at
                FROM rehealth_insurance_plan_binding binding
                LEFT JOIN rehealth_intervention_plan plan
                  ON plan.user_id = ? COLLATE utf8mb4_0900_ai_ci AND plan.plan_id = binding.plan_id
                WHERE binding.tenant_id=? AND binding.subject_ref=?
                ORDER BY binding.updated_at DESC, plan.generated_at DESC LIMIT 1
                """, (rs, row) -> {
            JsonNode json = tree(rs.getString(4));
            List<JsonNode> items = new ArrayList<>();
            if (json != null && json.path("items").isArray()) json.path("items").forEach(items::add);
            return new InsuranceInterventionWorkbenchResponse.Plan(rs.getString(1), rs.getString(2),
                    rs.getString(3), items, nullableBoolean(rs, 5), format(rs.getTimestamp(6)));
        }, identity.userId(), tenantId, identity.subjectRef()).stream().findFirst().orElse(null);
    }

    private List<InsuranceInterventionWorkbenchResponse.Feedback> feedback(int tenantId, String subjectRef) {
        return jdbc.query("""
                SELECT id, feedback_type, intervention_id, completion_rate, adherence_score,
                       occurred_at, outcome_summary_json
                FROM rehealth_insurance_intervention_feedback
                WHERE tenant_id=? AND subject_ref=? ORDER BY occurred_at DESC LIMIT 100
                """, (rs, row) -> new InsuranceInterventionWorkbenchResponse.Feedback(
                rs.getString(1), rs.getString(2), rs.getString(3), nullableDouble(rs, 4),
                nullableDouble(rs, 5), format(rs.getTimestamp(6)), tree(rs.getString(7))), tenantId, subjectRef);
    }

    private List<InsuranceInterventionWorkbenchResponse.Action> actions(int tenantId, String subjectRef) {
        return jdbc.query("""
                SELECT action.id, action.action_type, action.title, action.content,
                       action.assignee_user_id, account.realname, action.status, action.due_at,
                       action.completed_at, action.result_json, action.updated_at
                FROM rehealth_insurance_intervention_action action
                LEFT JOIN sys_user account
                  ON account.id = CONVERT(action.assignee_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                WHERE action.tenant_id=? AND action.subject_ref=?
                ORDER BY action.updated_at DESC LIMIT 100
                """, this::mapAction, tenantId, subjectRef);
    }

    private InsuranceInterventionWorkbenchResponse.Action actionById(int tenantId, String id) {
        return jdbc.query("""
                SELECT action.id, action.action_type, action.title, action.content,
                       action.assignee_user_id, account.realname, action.status, action.due_at,
                       action.completed_at, action.result_json, action.updated_at
                FROM rehealth_insurance_intervention_action action
                LEFT JOIN sys_user account
                  ON account.id = CONVERT(action.assignee_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                WHERE action.tenant_id=? AND action.id=? LIMIT 1
                """, this::mapAction, tenantId, id).stream().findFirst()
                .orElseThrow(() -> InsuranceApiException.notFound("intervention action was not found"));
    }

    private InsuranceInterventionWorkbenchResponse.Action mapAction(ResultSet rs, int row) throws SQLException {
        return new InsuranceInterventionWorkbenchResponse.Action(
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getString(7), format(rs.getTimestamp(8)), format(rs.getTimestamp(9)),
                tree(rs.getString(10)), format(rs.getTimestamp(11)));
    }

    private InsuranceInterventionWorkbenchResponse.Attribution attribution(String userId) {
        AttributionSnapshot value = latestAttribution(userId);
        if (value == null) return null;
        return new InsuranceInterventionWorkbenchResponse.Attribution(value.status(), value.dataSufficient(),
                value.isMock(), value.individualAtt(), value.trendDelta(), value.interpretation(),
                format(value.createdAt()));
    }

    private List<InsuranceInterventionWorkbenchResponse.Factor> factors(String responseJson) {
        JsonNode root = tree(responseJson);
        if (root == null) return List.of();
        JsonNode contributions = root.path("factor_contributions");
        JsonNode measured = root.path("factor_measured_components");
        if (!contributions.isObject()) return List.of();
        List<InsuranceInterventionWorkbenchResponse.Factor> result = new ArrayList<>();
        contributions.fields().forEachRemaining(entry -> result.add(new InsuranceInterventionWorkbenchResponse.Factor(
                entry.getKey(), entry.getValue().isNumber() ? entry.getValue().doubleValue() : null,
                measured.path(entry.getKey()).isNumber() ? measured.path(entry.getKey()).doubleValue() : null)));
        result.sort(Comparator.comparing(v -> Math.abs(v.contribution() == null ? 0 : v.contribution()), Comparator.reverseOrder()));
        return result;
    }

    private void requireTenantStaff(int tenantId, String userId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_user_tenant membership
                JOIN sys_user account ON account.id=CONVERT(membership.user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                WHERE membership.tenant_id=? AND membership.user_id=? AND membership.status='1'
                  AND account.status=1 AND account.del_flag=0
                """, Integer.class, tenantId, userId);
        if (count == null || count < 1) throw InsuranceApiException.badRequest("assignee is not active staff in this tenant");
    }

    private void audit(int tenantId, String actor, String action, String resourceId,
                       String requestId, Object before, Object after) {
        jdbc.update("""
                INSERT INTO rehealth_insurance_audit_event (
                  id, tenant_id, actor_user_id, action, resource_type, resource_id,
                  request_id, before_hash, after_hash, metadata_json, created_at
                ) VALUES (?, ?, ?, ?, 'intervention_action', ?, ?, ?, ?, ?, ?)
                """, uuid(), tenantId, actor, action, resourceId, requestId,
                before == null ? null : sha256(json(before)), after == null ? null : sha256(json(after)),
                json(Map.of("source", "insurance_workbench")), LocalDateTime.now());
    }

    private JsonNode tree(String json) {
        if (json == null || json.isBlank()) return null;
        try { return objectMapper.readTree(json); } catch (JsonProcessingException e) { return null; }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw InsuranceApiException.badRequest("JSON value is invalid"); }
    }

    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 is unavailable", e); }
    }

    private static String normalizeLevel(String value) {
        if (value == null) return null;
        String level = value.trim().toLowerCase();
        if (Set.of("high", "very_high", "severe").contains(level)) return "high";
        if (Set.of("medium", "moderate").contains(level)) return "medium";
        return level;
    }

    private static String required(String value, String field, int max) {
        String result = optional(value, max);
        if (result == null) throw InsuranceApiException.badRequest(field + " is required");
        return result;
    }

    private static String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim();
        if (result.length() > max) throw InsuranceApiException.badRequest("value exceeds maximum length " + max);
        return result;
    }

    private static Double nullableDouble(ResultSet rs, int column) throws SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.doubleValue();
    }

    private static Boolean nullableBoolean(ResultSet rs, int column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) return null;
        return value instanceof Boolean b ? b : ((Number) value).intValue() != 0;
    }

    private static String format(Timestamp value) {
        return value == null ? null : value.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static String uuid() { return UUID.randomUUID().toString().replace("-", ""); }

    private record Identity(String subjectRef, String userId, String name) {}
    private record RiskSnapshot(Double score, String level, Boolean isMock, String responseJson, Timestamp evaluatedAt) {}
    private record RhiSnapshot(Double score, Double confidence, String updatedAt) {}
    private record FeedbackAggregate(Double adherence, String occurredAt) {}
    private record AttributionSnapshot(Boolean dataSufficient, Boolean isMock, Double individualAtt,
                                       Double trendDelta, String status, String interpretation, Timestamp createdAt) {}
    private record Owner(String name, String department) {}

    private static final class StreamDates {
        private StreamDates() {}
        static String max(String... values) {
            String result = null;
            for (String value : values) if (value != null && (result == null || value.compareTo(result) > 0)) result = value;
            return result;
        }
    }
}
