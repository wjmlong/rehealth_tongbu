package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jeecg.modules.rehealth.ingest.query.HardwareTelemetryQuery;
import org.jeecg.modules.rehealth.ingest.writer.HardwarePersistenceUnavailableException;
import org.jeecg.modules.rehealth.mobile.dto.RecentTelemetryResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceInterventionWorkbenchService {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】工作台范围切到新服务关系表并支持三级范围-----------
    private static String scopeSql(InsuranceAssignmentScope scope) {
        StringBuilder sql = new StringBuilder("""
                FROM rehealth_insurance_subject subject
                INNER JOIN rehealth_insurance_enrollment enrollment
                  ON enrollment.tenant_id = subject.tenant_id
                 AND enrollment.subject_ref = subject.subject_ref
                INNER JOIN rehealth_insurance_user_assignment assignment
                  ON assignment.enrollment_id = enrollment.id
                 AND assignment.status = 'active'
                """);
        if (scope != null) {
            sql.append("""
                      AND (assignment.employee_id = ?
                           OR (? = 'TEAM' AND EXISTS (
                               SELECT 1 FROM sys_user_depart my_dept
                               JOIN sys_user_depart assignee_dept ON assignee_dept.dep_id = my_dept.dep_id
                               WHERE my_dept.user_id = ? AND assignee_dept.user_id = CONVERT(assignment.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci
                           )))
                    """);
        }
        sql.append("""
                LEFT JOIN rehealth_patient_profile profile
                  ON profile.user_id = subject.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
                WHERE subject.tenant_id = ? AND subject.enrollment_status = 'active'
                  AND subject.consent_status = 'granted'
                """);
        return sql.toString();
    }

    private static Object[] scopeArgs(int tenantId, InsuranceAssignmentScope scope) {
        if (scope == null) {
            return new Object[]{tenantId};
        }
        return new Object[]{scope.userId(), scope.mode(), scope.userId(), tenantId};
    }

    private static InsuranceAssignmentScope selfScope(String managerUserId) {
        return managerUserId == null ? null : new InsuranceAssignmentScope(managerUserId, InsuranceAssignmentScope.MODE_SELF);
    }
    //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】工作台范围切到新服务关系表并支持三级范围-----------
    private static final Set<String> ACTION_STATUSES = Set.of("pending", "in_progress", "completed", "cancelled");
    static final int MIN_INTERVENTION_DAYS = 7;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final HardwareTelemetryQuery hardwareTelemetryQuery;
    private final Clock clock;
    private final ZoneId zoneId;

    @Autowired
    public InsuranceInterventionWorkbenchService(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            HardwareTelemetryQuery hardwareTelemetryQuery
    ) {
        this(jdbc, objectMapper, hardwareTelemetryQuery, Clock.systemUTC(), DEFAULT_ZONE);
    }

    InsuranceInterventionWorkbenchService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this(jdbc, objectMapper, emptyTelemetryQuery(), Clock.systemUTC(), DEFAULT_ZONE);
    }

    InsuranceInterventionWorkbenchService(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            Clock clock,
            ZoneId zoneId
    ) {
        this(jdbc, objectMapper, emptyTelemetryQuery(), clock, zoneId);
    }

    InsuranceInterventionWorkbenchService(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            HardwareTelemetryQuery hardwareTelemetryQuery,
            Clock clock,
            ZoneId zoneId
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.hardwareTelemetryQuery = hardwareTelemetryQuery;
        this.clock = clock;
        this.zoneId = zoneId;
    }

    public InsuranceInterventionWorkbenchResponse.Dashboard dashboard(int tenantId, InsuranceAssignmentScope scope) {
        List<Identity> identities = identities(tenantId, scope, null, Integer.MAX_VALUE, 0);
        List<InsuranceInterventionWorkbenchResponse.SubjectSummary> summaries = identities.stream()
                .map(value -> summary(tenantId, value)).toList();
        long pendingAction = summaries.stream().filter(v -> "pending_action".equals(v.workflowStatus())).count();
        long inProgress = summaries.stream().filter(v -> "in_progress".equals(v.workflowStatus())).count();
        long pendingReview = summaries.stream().filter(v -> "pending_review".equals(v.workflowStatus())).count();
        long improved = summaries.stream().filter(v -> "improved".equals(v.workflowStatus())).count();
        BigDecimal completed = summaries.stream()
                .map(InsuranceInterventionWorkbenchResponse.SubjectSummary::adherenceCompletedCount)
                .filter(v -> v != null).map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expected = summaries.stream()
                .map(InsuranceInterventionWorkbenchResponse.SubjectSummary::adherenceExpectedCount)
                .filter(v -> v != null).map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal adherence = expected.signum() > 0
                ? completed.divide(expected, 4, RoundingMode.HALF_UP) : null;
        String updated = summaries.stream().map(InsuranceInterventionWorkbenchResponse.SubjectSummary::updatedAt)
                .filter(v -> v != null).max(String::compareTo).orElse(null);
        return new InsuranceInterventionWorkbenchResponse.Dashboard(
                "assigned_subjects", summaries.size(), pendingAction, inProgress, pendingReview,
                improved, adherence, updated);
    }

    public InsuranceInterventionWorkbenchResponse.Dashboard dashboard(int tenantId, String managerUserId) {
        return dashboard(tenantId, selfScope(managerUserId));
    }

    public InsuranceInterventionWorkbenchResponse.SubjectPage subjects(
            int tenantId, InsuranceAssignmentScope scope, int pageNo, int pageSize, String keyword, String workflowStatus
    ) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        List<Identity> all = identities(tenantId, scope, keyword, Integer.MAX_VALUE, 0);
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

    public InsuranceInterventionWorkbenchResponse.SubjectPage subjects(
            int tenantId, String managerUserId, int pageNo, int pageSize, String keyword, String workflowStatus
    ) {
        return subjects(tenantId, selfScope(managerUserId), pageNo, pageSize, keyword, workflowStatus);
    }

    public InsuranceInterventionWorkbenchResponse.SubjectDetail subject(
            int tenantId, InsuranceAssignmentScope scope, String subjectId
    ) {
        Identity identity = requireIdentity(tenantId, scope, subjectId);
        InsuranceInterventionWorkbenchResponse.SubjectSummary summary = summary(tenantId, identity);
        RiskSnapshot latestRisk = latestRisk(identity.userId());
        InsuranceInterventionWorkbenchResponse.Attribution attribution = attribution(identity.userId());
        return new InsuranceInterventionWorkbenchResponse.SubjectDetail(
                "assigned_subjects", summary, riskTrend(identity.userId()), rhiTrend(identity.userId()),
                rdiTrend(identity.userId()), factors(latestRisk == null ? null : latestRisk.responseJson()),
                rdiContributions(identity.userId()), healthMetrics(identity.userId()), plan(tenantId, identity),
                feedback(tenantId, identity.subjectRef()), actions(tenantId, identity.subjectRef()),
                attribution,
                Boolean.TRUE.equals(summary.riskIsMock())
                        ? "当前风险或干预包含演练数据，不可作为真实改善结论。"
                        : evidenceNotice(attribution)
        );
    }

    public InsuranceInterventionWorkbenchResponse.SubjectDetail subject(
            int tenantId, String managerUserId, String subjectId
    ) {
        return subject(tenantId, selfScope(managerUserId), subjectId);
    }

    List<InsuranceInterventionWorkbenchResponse.HealthMetric> healthMetrics(String userId) {
        RecentTelemetryResponseDto telemetry;
        try {
            telemetry = hardwareTelemetryQuery.recentForUser(userId, 200);
        } catch (HardwarePersistenceUnavailableException | DataAccessException exception) {
            return List.of();
        }
        if (telemetry == null) return List.of();

        Map<String, InsuranceInterventionWorkbenchResponse.HealthMetric> metrics = new LinkedHashMap<>();
        if (telemetry.measurements != null) {
            for (RecentTelemetryResponseDto.Measurement measurement : telemetry.measurements) {
                if (measurement == null || measurement.metricType == null || measurement.primaryValue == null) continue;
                String type = measurement.metricType.trim().toUpperCase(Locale.ROOT);
                boolean synthetic = isSyntheticSource(measurement.source);
                switch (type) {
                    case "HEART_RATE" -> putMetric(metrics, "heart_rate", measurement.primaryValue,
                            defaultUnit(measurement.unit, "bpm"), measurement.measuredAt, synthetic);
                    case "BLOOD_OXYGEN", "SPO2" -> putMetric(metrics, "spo2", measurement.primaryValue,
                            defaultUnit(measurement.unit, "%"), measurement.measuredAt, synthetic);
                    case "BLOOD_PRESSURE" -> {
                        putMetric(metrics, "systolic_bp", measurement.primaryValue,
                                defaultUnit(measurement.unit, "mmHg"), measurement.measuredAt, synthetic);
                        if (measurement.secondaryValue != null) {
                            putMetric(metrics, "diastolic_bp", measurement.secondaryValue,
                                    defaultUnit(measurement.unit, "mmHg"), measurement.measuredAt, synthetic);
                        }
                    }
                    case "STEPS" -> putMetric(metrics, "steps", measurement.primaryValue,
                            defaultUnit(measurement.unit, "步"), measurement.measuredAt, synthetic);
                    case "BLOOD_GLUCOSE" -> putMetric(metrics, "blood_glucose", measurement.primaryValue,
                            defaultUnit(measurement.unit, "mmol/L"), measurement.measuredAt, synthetic);
                    case "WEIGHT" -> putMetric(metrics, "weight", measurement.primaryValue,
                            defaultUnit(measurement.unit, "kg"), measurement.measuredAt, synthetic);
                    default -> {
                        // Only the explicitly whitelisted health metrics reach the insurance workbench.
                    }
                }
            }
        }

        if (telemetry.sleepSessions != null && !telemetry.sleepSessions.isEmpty()) {
            RecentTelemetryResponseDto.SleepSession sleep = telemetry.sleepSessions.get(0);
            Integer minutes = sleepMinutes(sleep);
            if (minutes != null && minutes > 0) {
                putMetric(metrics, "sleep_minutes", minutes.doubleValue(), "分钟", sleep.endedAt,
                        isSyntheticSource(sleep.source));
            }
        }
        if (telemetry.activities != null && !telemetry.activities.isEmpty()) {
            RecentTelemetryResponseDto.Activity activity = telemetry.activities.get(0);
            boolean synthetic = isSyntheticSource(activity.source);
            if (activity.steps != null) {
                putMetric(metrics, "steps", activity.steps.doubleValue(), "步", activity.endedAt, synthetic);
            }
            if (activity.durationMinutes != null) {
                putMetric(metrics, "activity_minutes", activity.durationMinutes.doubleValue(),
                        "分钟", activity.endedAt, synthetic);
            }
            if (activity.caloriesKcal != null) {
                putMetric(metrics, "calories", activity.caloriesKcal, "kcal", activity.endedAt, synthetic);
            }
        }
        return List.copyOf(metrics.values());
    }

    private static void putMetric(
            Map<String, InsuranceInterventionWorkbenchResponse.HealthMetric> metrics,
            String code,
            Double value,
            String unit,
            Long observedAt,
            boolean synthetic
    ) {
        if (value == null || !Double.isFinite(value)) return;
        metrics.putIfAbsent(code, new InsuranceInterventionWorkbenchResponse.HealthMetric(
                code, value, unit, observedAt, "device_telemetry", synthetic));
    }

    private static Integer sleepMinutes(RecentTelemetryResponseDto.SleepSession sleep) {
        if (sleep == null) return null;
        int phaseTotal = valueOrZero(sleep.deepMinutes) + valueOrZero(sleep.lightMinutes)
                + valueOrZero(sleep.remMinutes);
        if (phaseTotal > 0) return phaseTotal;
        if (sleep.startedAt == null || sleep.endedAt == null || sleep.endedAt <= sleep.startedAt) return null;
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, (sleep.endedAt - sleep.startedAt) / 60_000L));
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private static String defaultUnit(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static boolean isSyntheticSource(String source) {
        if (source == null) return false;
        String normalized = source.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("LOCAL_") || normalized.contains("MOCK")
                || normalized.contains("SYNTHETIC") || normalized.contains("DEBUG")
                || normalized.contains("_QA") || normalized.contains("TEST");
    }

    private static HardwareTelemetryQuery emptyTelemetryQuery() {
        return (userId, limit) -> new RecentTelemetryResponseDto();
    }

    private static Object[] concat(Object[] head, Object... tail) {
        Object[] result = new Object[head.length + tail.length];
        System.arraycopy(head, 0, result, 0, head.length);
        System.arraycopy(tail, 0, result, head.length, tail.length);
        return result;
    }

    @Transactional
    public InsuranceInterventionWorkbenchResponse.Action createAction(
            int tenantId, InsuranceAssignmentScope scope, String actorUserId, String subjectId,
            InsuranceInterventionWorkbenchRequest.CreateAction request
    ) {
        Identity identity = requireIdentity(tenantId, scope, subjectId);
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
    public InsuranceInterventionWorkbenchResponse.Action createAction(
            int tenantId, String managerUserId, String actorUserId, String subjectId,
            InsuranceInterventionWorkbenchRequest.CreateAction request
    ) {
        return createAction(tenantId, selfScope(managerUserId), actorUserId, subjectId, request);
    }

    @Transactional
    public InsuranceInterventionWorkbenchResponse.BatchActions createActions(
            int tenantId, InsuranceAssignmentScope scope, String actorUserId,
            InsuranceInterventionWorkbenchRequest.BatchCreateAction request
    ) {
        if (request == null) throw InsuranceApiException.badRequest("batch action request is required");
        if (request.subjectIds() == null || request.subjectIds().isEmpty()) {
            throw InsuranceApiException.badRequest("subject_ids must contain at least one subject");
        }
        List<String> subjectIds = request.subjectIds().stream()
                .map(subjectId -> required(subjectId, "subject_id", 64))
                .distinct()
                .toList();
        if (subjectIds.size() > 100) {
            throw InsuranceApiException.badRequest("subject_ids must not contain more than 100 subjects");
        }
        String requestId = required(request.requestId(), "request_id", 100);
        subjectIds.forEach(subjectId -> requireIdentity(tenantId, scope, subjectId));

        List<InsuranceInterventionWorkbenchResponse.Action> actions = new ArrayList<>(subjectIds.size());
        for (int index = 0; index < subjectIds.size(); index++) {
            InsuranceInterventionWorkbenchRequest.CreateAction action = new InsuranceInterventionWorkbenchRequest.CreateAction(
                    null,
                    request.actionType(),
                    request.title(),
                    request.content(),
                    request.assigneeUserId(),
                    request.dueAt(),
                    requestId + "-" + (index + 1)
            );
            actions.add(createAction(tenantId, scope, actorUserId, subjectIds.get(index), action));
        }
        return new InsuranceInterventionWorkbenchResponse.BatchActions(subjectIds.size(), actions.size(), actions);
    }

    @Transactional
    public InsuranceInterventionWorkbenchResponse.BatchActions createActions(
            int tenantId, String managerUserId, String actorUserId,
            InsuranceInterventionWorkbenchRequest.BatchCreateAction request
    ) {
        return createActions(tenantId, selfScope(managerUserId), actorUserId, request);
    }

    @Transactional
    public InsuranceInterventionWorkbenchResponse.Action updateAction(
            int tenantId, InsuranceAssignmentScope scope, String actorUserId, String actionId,
            InsuranceInterventionWorkbenchRequest.UpdateAction request
    ) {
        String subjectRef = jdbc.query("""
                SELECT subject_ref FROM rehealth_insurance_intervention_action
                WHERE tenant_id=? AND id=? LIMIT 1
                """, (rs, row) -> rs.getString(1), tenantId, required(actionId, "actionId", 64))
                .stream().findFirst().orElseThrow(() -> InsuranceApiException.notFound("intervention action was not found"));
        requireIdentity(tenantId, scope, subjectRef);
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

    @Transactional
    public InsuranceInterventionWorkbenchResponse.Action updateAction(
            int tenantId, String managerUserId, String actorUserId, String actionId,
            InsuranceInterventionWorkbenchRequest.UpdateAction request
    ) {
        return updateAction(tenantId, selfScope(managerUserId), actorUserId, actionId, request);
    }

    //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】包内可见,供人群报告服务复用负责关系范围------------
    List<Identity> identities(int tenantId, InsuranceAssignmentScope scope, String keyword, int limit, int offset) {
    //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】包内可见,供人群报告服务复用负责关系范围------------
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        return jdbc.query("""
                SELECT subject.subject_ref, subject.rehealth_user_id, profile.name,
                       profile.age, profile.gender, profile.bmi
                """ + scopeSql(scope) + """
                  AND (? IS NULL OR profile.name LIKE ?)
                ORDER BY profile.name, subject.subject_ref LIMIT ? OFFSET ?
                """, (rs, row) -> new Identity(rs.getString(1), rs.getString(2), rs.getString(3),
                        nullableInteger(rs, 4), rs.getString(5), rs.getBigDecimal(6)),
                concat(scopeArgs(tenantId, scope), like, like, limit, offset));
    }

    List<Identity> identities(int tenantId, String managerUserId, String keyword, int limit, int offset) {
        return identities(tenantId, selfScope(managerUserId), keyword, limit, offset);
    }

    private Identity requireIdentity(int tenantId, InsuranceAssignmentScope scope, String subjectId) {
        String normalized = required(subjectId, "subjectId", 64);
        return jdbc.query("SELECT subject.subject_ref, subject.rehealth_user_id, profile.name, "
                        + "profile.age, profile.gender, profile.bmi " + scopeSql(scope)
                        + " AND subject.subject_ref = ? LIMIT 1",
                (rs, row) -> new Identity(rs.getString(1), rs.getString(2), rs.getString(3),
                        nullableInteger(rs, 4), rs.getString(5), rs.getBigDecimal(6)),
                concat(scopeArgs(tenantId, scope), normalized)).stream().findFirst()
                .orElseThrow(() -> InsuranceApiException.notFound("assigned insurance subject was not found"));
    }

    private Identity requireIdentity(int tenantId, String managerUserId, String subjectId) {
        return requireIdentity(tenantId, selfScope(managerUserId), subjectId);
    }

    //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】包内可见,供人群报告服务复用主体摘要聚合------------
    InsuranceInterventionWorkbenchResponse.SubjectSummary summary(int tenantId, Identity identity) {
    //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】包内可见,供人群报告服务复用主体摘要聚合------------
        RiskSnapshot risk = latestRisk(identity.userId());
        RhiSnapshot rhi = latestRhi(identity.userId());
        RdiSnapshot rdi = latestRdi(identity.userId());
        List<InsuranceInterventionWorkbenchResponse.Factor> mainFactors = risk == null
                ? List.of()
                : factors(risk.responseJson()).stream().limit(3).toList();
        FeedbackAggregate feedback = latestFeedback(tenantId, identity.subjectRef(), identity.userId());
        AttributionSnapshot attribution = latestAttribution(identity.userId());
        Owner owner = owner(tenantId, identity.subjectRef());
        CurrentIntervention currentIntervention = currentIntervention(tenantId, identity);
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
                rdi == null ? null : rdi.updatedAt(),
                feedback == null ? null : feedback.occurredAt());
        return new InsuranceInterventionWorkbenchResponse.SubjectSummary(
                identity.subjectRef(), identity.name(), identity.age(), identity.gender(), identity.bmi(), workflow,
                risk == null ? null : risk.score(), risk == null ? null : risk.level(),
                risk == null ? null : risk.isMock(), mainFactors, rhi == null ? null : rhi.score(),
                rhi == null ? null : rhi.confidence(), rdi == null ? null : rdi.score(),
                rdi == null ? null : rdi.confidence(), rdi == null ? null : rdi.status(),
                rdi == null ? null : rdi.isMock(), rdi == null ? null : rdi.scoredOn(),
                feedback == null ? null : feedback.adherence(),
                feedback == null ? null : feedback.completedCount(),
                feedback == null ? null : feedback.expectedCount(),
                feedback == null ? null : 28,
                owner == null ? null : owner.name(), owner == null ? null : owner.department(),
                currentIntervention == null ? null : currentIntervention.summary(),
                currentIntervention == null ? null : currentIntervention.dueAt(), updated);
    }

    private String workflowStatus(RiskSnapshot risk, AttributionSnapshot attribution, boolean active) {
        ImprovementEvidenceDecision evidence = evaluateImprovementEvidence(attribution);
        if ("improved".equals(evidence.conclusion())) return "improved";
        if (active) return "in_progress";
        if (risk != null && !Boolean.TRUE.equals(risk.isMock()) && "high".equals(normalizeLevel(risk.level()))) {
            return "pending_action";
        }
        return "pending_review";
    }

    //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】包内可见,供人群报告服务读取主体最新风险------------
    RiskSnapshot latestRisk(String userId) {
    //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】包内可见,供人群报告服务读取主体最新风险------------
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

    private RdiSnapshot latestRdi(String userId) {
        return jdbc.query("""
                SELECT display_score, data_confidence, status, is_mock, scored_on, updated_at
                FROM rehealth_rdi_daily_snapshot
                WHERE user_id=? ORDER BY scored_on DESC LIMIT 1
                """, (rs, row) -> new RdiSnapshot(nullableDouble(rs, 1), nullableDouble(rs, 2),
                rs.getString(3), nullableBoolean(rs, 4), rs.getDate(5).toString(), format(rs.getTimestamp(6))),
                userId).stream().findFirst().orElse(null);
    }

    private List<InsuranceInterventionWorkbenchResponse.TrendPoint> rdiTrend(String userId) {
        return jdbc.query("""
                SELECT scored_on, display_score, status, is_mock FROM rehealth_rdi_daily_snapshot
                WHERE user_id=? AND scored_on >= DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY)
                ORDER BY scored_on
                """, (rs, row) -> new InsuranceInterventionWorkbenchResponse.TrendPoint(
                rs.getDate(1).toString(), nullableDouble(rs, 2), rs.getString(3), nullableBoolean(rs, 4)), userId);
    }

    private List<InsuranceInterventionWorkbenchResponse.RdiContribution> rdiContributions(String userId) {
        return jdbc.query("""
                SELECT contribution.factor_code, contribution.domain_code, contribution.source_code,
                       contribution.current_value, contribution.baseline_value, contribution.unit,
                       contribution.final_points, contribution.confidence
                FROM rehealth_rdi_contribution contribution
                INNER JOIN rehealth_rdi_daily_snapshot snapshot ON snapshot.id=contribution.snapshot_id
                WHERE snapshot.user_id=?
                  AND snapshot.scored_on=(SELECT MAX(latest.scored_on)
                                          FROM rehealth_rdi_daily_snapshot latest WHERE latest.user_id=?)
                ORDER BY ABS(contribution.final_points) DESC, contribution.factor_code
                """, (rs, row) -> new InsuranceInterventionWorkbenchResponse.RdiContribution(
                rs.getString(1), rs.getString(2), rs.getString(3), nullableDouble(rs, 4),
                nullableDouble(rs, 5), rs.getString(6), nullableDouble(rs, 7), nullableDouble(rs, 8)),
                userId, userId);
    }

    FeedbackAggregate latestFeedback(int tenantId, String subjectRef, String userId) {
        LocalDateTime current = LocalDateTime.ofInstant(clock.instant(), zoneId);
        LocalDateTime windowStart = current.toLocalDate().minusDays(27).atStartOfDay();
        LocalDateTime windowEnd = current.toLocalDate().plusDays(1).atStartOfDay();
        VersionedFeedbackAggregate versioned = jdbc.query("""
                SELECT COUNT(occurrence.id),
                       SUM(CASE WHEN execution.feedback_type='not_applicable' THEN 0
                                ELSE COALESCE(item.scoring_weight, 1) * COALESCE(execution.score_value, 0) END),
                       SUM(CASE WHEN execution.feedback_type='not_applicable' THEN 0
                                ELSE COALESCE(item.scoring_weight, 1) END),
                       MAX(COALESCE(execution.occurred_at, occurrence.due_at))
                FROM rehealth_care_plan_occurrence occurrence
                JOIN rehealth_care_plan plan
                  ON plan.tenant_id=occurrence.tenant_id AND plan.id=occurrence.plan_id
                 AND plan.owner_type='insurance' AND plan.subject_ref=? AND plan.rehealth_user_id=?
                JOIN rehealth_care_plan_item item
                  ON item.tenant_id=occurrence.tenant_id AND item.id=occurrence.plan_item_id
                LEFT JOIN rehealth_care_plan_execution execution
                  ON execution.id=(
                    SELECT latest.id FROM rehealth_care_plan_execution latest
                    WHERE latest.tenant_id=occurrence.tenant_id
                      AND latest.occurrence_id=occurrence.id
                    ORDER BY latest.occurred_at DESC, latest.created_at DESC, latest.id DESC
                    LIMIT 1
                  )
                WHERE occurrence.tenant_id=? AND occurrence.subject_ref=?
                  AND occurrence.status='scheduled'
                  AND occurrence.scheduled_at>=? AND occurrence.scheduled_at<?
                  AND (occurrence.due_at<=? OR execution.id IS NOT NULL)
                """, (rs, row) -> new VersionedFeedbackAggregate(
                rs.getLong(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getTimestamp(4)
        ), subjectRef, userId, tenantId, subjectRef, windowStart, windowEnd, current)
                .stream().findFirst().orElse(new VersionedFeedbackAggregate(0, null, null, null));
        Integer activeVersionedPlans = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM rehealth_care_plan plan
                JOIN rehealth_care_plan_revision revision
                  ON revision.tenant_id=plan.tenant_id AND revision.plan_id=plan.id
                 AND revision.status='published' AND revision.effective_from<=?
                 AND (revision.effective_to IS NULL OR revision.effective_to>?)
                WHERE plan.tenant_id=? AND plan.owner_type='insurance' AND plan.status='active'
                  AND plan.subject_ref=? AND plan.rehealth_user_id=?
                """, Integer.class, current, current, tenantId, subjectRef, userId);
        if (versioned.occurrenceCount() > 0 || (activeVersionedPlans != null && activeVersionedPlans > 0)) {
            BigDecimal expected = versioned.expected();
            if (expected == null || expected.signum() == 0) {
                return new FeedbackAggregate(null, null, null, format(versioned.latestActivityAt()));
            }
            BigDecimal completed = versioned.completed() == null ? BigDecimal.ZERO : versioned.completed();
            return new FeedbackAggregate(
                    completed.divide(expected, 4, RoundingMode.HALF_UP).doubleValue(),
                    completed.doubleValue(), expected.doubleValue(), format(versioned.latestActivityAt()));
        }
        return legacyFeedback(tenantId, subjectRef);
    }

    private FeedbackAggregate legacyFeedback(int tenantId, String subjectRef) {
        return jdbc.query("""
                SELECT
                  SUM(COALESCE(feedback.completed_count, feedback.adherence_score))
                    / NULLIF(SUM(COALESCE(feedback.expected_count,
                        CASE WHEN feedback.adherence_score IS NULL THEN 0 ELSE 1 END)), 0),
                  SUM(COALESCE(feedback.completed_count, feedback.adherence_score)),
                  SUM(COALESCE(feedback.expected_count,
                        CASE WHEN feedback.adherence_score IS NULL THEN 0 ELSE 1 END)),
                  MAX(feedback.occurred_at)
                FROM rehealth_insurance_intervention_feedback feedback
                JOIN rehealth_insurance_plan_binding binding
                  ON binding.id=feedback.binding_id
                 AND binding.tenant_id=feedback.tenant_id
                 AND binding.status='active'
                WHERE feedback.tenant_id=? AND feedback.subject_ref=?
                  AND feedback.occurred_at >= DATE_SUB(NOW(3), INTERVAL 28 DAY)
                """, (rs, row) -> new FeedbackAggregate(
                        nullableDouble(rs, 1), nullableDouble(rs, 2), nullableDouble(rs, 3),
                        format(rs.getTimestamp(4))),
                tenantId, subjectRef).stream().findFirst().orElse(null);
    }

    private AttributionSnapshot latestAttribution(String userId) {
        return jdbc.query("""
                SELECT intervention_data_sufficient, is_mock, history_days, min_history_days,
                       intervention_days, adherence_average, individual_att, trend_delta,
                       status, interpretation, created_at
                FROM rehealth_attribution_result WHERE user_id=? ORDER BY created_at DESC, id DESC LIMIT 1
                """, (rs, row) -> new AttributionSnapshot(
                nullableBoolean(rs, 1), nullableBoolean(rs, 2), nullableInteger(rs, 3),
                nullableInteger(rs, 4), nullableInteger(rs, 5), nullableDouble(rs, 6),
                nullableDouble(rs, 7), nullableDouble(rs, 8), rs.getString(9), rs.getString(10),
                rs.getTimestamp(11)),
                userId).stream().findFirst().orElse(null);
    }

    private Owner owner(int tenantId, String subjectRef) {
        //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】负责人展示改读新服务关系表-----------
        return jdbc.query("""
                SELECT account.realname, department.depart_name
                FROM rehealth_insurance_user_assignment assignment
                JOIN rehealth_insurance_enrollment enrollment ON enrollment.id = assignment.enrollment_id
                LEFT JOIN sys_user account
                  ON account.id = CONVERT(assignment.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci
                LEFT JOIN sys_user_depart membership ON membership.user_id = CONVERT(assignment.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci
                LEFT JOIN sys_depart department ON department.id = membership.dep_id AND department.tenant_id = assignment.tenant_id
                WHERE assignment.tenant_id=? AND enrollment.subject_ref=? AND assignment.status='active'
                  AND assignment.role_type='PRIMARY'
                ORDER BY assignment.start_time DESC LIMIT 1
                """, (rs, row) -> new Owner(rs.getString(1), rs.getString(2)), tenantId, subjectRef)
                .stream().findFirst().orElse(null);
        //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】负责人展示改读新服务关系表-----------
    }

    private CurrentIntervention currentIntervention(int tenantId, Identity identity) {
        CurrentIntervention action = jdbc.query("""
                SELECT title, due_at
                FROM rehealth_insurance_intervention_action
                WHERE tenant_id=? AND subject_ref=? AND status IN ('pending','in_progress')
                ORDER BY CASE WHEN status='in_progress' THEN 0 ELSE 1 END, updated_at DESC, id DESC
                LIMIT 1
                """, (rs, row) -> new CurrentIntervention(rs.getString(1), format(rs.getTimestamp(2))),
                tenantId, identity.subjectRef()).stream().findFirst().orElse(null);
        if (action != null) return action;
        InsuranceInterventionWorkbenchResponse.Plan currentPlan = plan(tenantId, identity);
        if (currentPlan == null) return null;
        JsonNode firstItem = currentPlan.items() == null || currentPlan.items().isEmpty()
                ? null : currentPlan.items().get(0);
        String itemTitle = firstItem == null ? null : text(firstItem, "title");
        String dueAt = firstItem == null ? null : text(firstItem, "due_at");
        String summary = itemTitle;
        if (summary == null || summary.isBlank()) summary = currentPlan.title();
        if (summary == null || summary.isBlank()) summary = currentPlan.summary();
        return summary == null || summary.isBlank() ? null : new CurrentIntervention(summary, dueAt);
    }

    private InsuranceInterventionWorkbenchResponse.Plan plan(int tenantId, Identity identity) {
        InsuranceInterventionWorkbenchResponse.Plan institution = institutionPlan(
                tenantId, identity.subjectRef(), identity.userId());
        if (institution != null) return institution;
        return legacyPlan(tenantId, identity);
    }

    InsuranceInterventionWorkbenchResponse.Plan institutionPlan(
            int tenantId, String subjectRef, String userId
    ) {
        return jdbc.query("""
                SELECT plan.id, plan.status, revision.id, revision.revision_no,
                       revision.title, revision.summary, revision.published_at,
                       revision.effective_from, revision.effective_to
                FROM rehealth_care_plan plan
                INNER JOIN rehealth_care_plan_revision revision
                  ON revision.tenant_id=plan.tenant_id AND revision.plan_id=plan.id
                 AND revision.status='published' AND revision.effective_from<=NOW(3)
                 AND (revision.effective_to IS NULL OR revision.effective_to>NOW(3))
                WHERE plan.tenant_id=? AND plan.owner_type='insurance'
                  AND plan.subject_ref=? AND plan.rehealth_user_id=? AND plan.status='active'
                ORDER BY revision.effective_from DESC, revision.revision_no DESC, plan.updated_at DESC
                LIMIT 1
                """, (rs, row) -> new InsuranceInterventionWorkbenchResponse.Plan(
                rs.getString(1), rs.getString(2), "institution", rs.getString(3), rs.getInt(4),
                rs.getString(5), rs.getString(6), institutionPlanItems(
                        tenantId, rs.getString(1), rs.getString(3), subjectRef),
                false, format(rs.getTimestamp(7)), format(rs.getTimestamp(8)), format(rs.getTimestamp(9))
        ), tenantId, subjectRef, userId).stream().findFirst().orElse(null);
    }

    private List<JsonNode> institutionPlanItems(
            int tenantId, String planId, String revisionId, String subjectRef
    ) {
        return jdbc.query("""
                SELECT item.id, item.logical_item_id, item.category, item.title,
                       item.instructions, item.schedule_json, item.scoring_weight,
                       item.allow_not_applicable, item.display_order,
                       occurrence.id, occurrence.due_at,
                       execution.feedback_type
                FROM rehealth_care_plan_item item
                LEFT JOIN rehealth_care_plan_occurrence occurrence
                  ON occurrence.tenant_id=item.tenant_id
                 AND occurrence.plan_id=item.plan_id
                 AND occurrence.revision_id=item.revision_id
                 AND occurrence.plan_item_id=item.id
                 AND occurrence.subject_ref=?
                 AND occurrence.status='scheduled'
                 AND DATE(occurrence.scheduled_at)=CURRENT_DATE
                LEFT JOIN rehealth_care_plan_execution execution
                  ON execution.id=(
                    SELECT latest.id FROM rehealth_care_plan_execution latest
                    WHERE latest.tenant_id=occurrence.tenant_id
                      AND latest.occurrence_id=occurrence.id
                    ORDER BY latest.occurred_at DESC, latest.created_at DESC, latest.id DESC
                    LIMIT 1
                  )
                WHERE item.tenant_id=? AND item.plan_id=? AND item.revision_id=?
                ORDER BY item.display_order, item.id
                """, (rs, row) -> {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("item_id", rs.getString(1));
            item.put("logical_item_id", rs.getString(2));
            item.put("category", rs.getString(3));
            item.put("title", rs.getString(4));
            put(item, "action", rs.getString(5));
            JsonNode schedule = tree(rs.getString(6));
            if (schedule != null) item.set("schedule", schedule);
            if (rs.getBigDecimal(7) != null) item.put("scoring_weight", rs.getBigDecimal(7));
            item.put("allow_not_applicable", rs.getBoolean(8));
            item.put("display_order", rs.getInt(9));
            put(item, "occurrence_id", rs.getString(10));
            put(item, "due_at", format(rs.getTimestamp(11)));
            put(item, "feedback_type", rs.getString(12));
            return item;
        }, subjectRef, tenantId, planId, revisionId);
    }

    private InsuranceInterventionWorkbenchResponse.Plan legacyPlan(int tenantId, Identity identity) {
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
            return new InsuranceInterventionWorkbenchResponse.Plan(
                    rs.getString(1), rs.getString(2), "personal", null, null, null,
                    rs.getString(3), items, nullableBoolean(rs, 5), format(rs.getTimestamp(6)), null, null);
        }, identity.userId(), tenantId, identity.subjectRef()).stream().findFirst().orElse(null);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || !value.isTextual() || value.asText().isBlank()
                ? null : value.asText().trim();
    }

    private static void put(ObjectNode node, String field, String value) {
        if (value != null && !value.isBlank()) node.put(field, value);
    }

    private List<InsuranceInterventionWorkbenchResponse.Feedback> feedback(int tenantId, String subjectRef) {
        return jdbc.query("""
                SELECT id, feedback_type, intervention_id, plan_item_id,
                       completion_rate, adherence_score, expected_count, completed_count,
                       verification_type, calculation_version, occurred_at, outcome_summary_json
                FROM rehealth_insurance_intervention_feedback
                WHERE tenant_id=? AND subject_ref=?
                  AND occurred_at >= DATE_SUB(NOW(3), INTERVAL 28 DAY)
                ORDER BY occurred_at DESC LIMIT 100
                """, (rs, row) -> new InsuranceInterventionWorkbenchResponse.Feedback(
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                nullableDouble(rs, 5), nullableDouble(rs, 6), nullableDouble(rs, 7),
                nullableDouble(rs, 8), rs.getString(9), rs.getString(10),
                format(rs.getTimestamp(11)), tree(rs.getString(12))), tenantId, subjectRef);
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
        ImprovementEvidenceDecision decision = evaluateImprovementEvidence(value);
        return new InsuranceInterventionWorkbenchResponse.Attribution(value.status(), value.dataSufficient(),
                value.isMock(), value.historyDays(), value.minHistoryDays(), value.interventionDays(),
                MIN_INTERVENTION_DAYS, value.adherenceAverage(), value.individualAtt(), value.trendDelta(),
                decision.conclusive(), decision.conclusion(), decision.effectMetric(), decision.effectValue(),
                value.interpretation(), format(value.createdAt()));
    }

    private static String evidenceNotice(InsuranceInterventionWorkbenchResponse.Attribution attribution) {
        if (attribution == null) {
            return "尚无归因结果；需要真实非模拟数据、至少 14 天历史基线、至少 7 天干预执行和可计算的效果信号。";
        }
        return switch (attribution.conclusion()) {
            case "improved" -> "证据门槛已满足，当前支持阶段性改善；该结论不等于诊断、长期疗效或必然因果。";
            case "not_improved" -> "证据门槛已满足，但当前效果方向未显示改善；继续执行或调整行动后复评。";
            default -> "证据尚未达到结论门槛：需真实非模拟数据、足量历史基线和干预执行记录，并形成可计算的效果信号。";
        };
    }

    static ImprovementEvidenceDecision evaluateImprovementEvidence(AttributionSnapshot value) {
        if (value == null || Boolean.TRUE.equals(value.isMock())
                || !Boolean.TRUE.equals(value.dataSufficient())) {
            return ImprovementEvidenceDecision.insufficient();
        }
        if (value.historyDays() == null || value.minHistoryDays() == null
                || value.historyDays() < value.minHistoryDays()) {
            return ImprovementEvidenceDecision.insufficient();
        }
        if (value.interventionDays() == null || value.interventionDays() < MIN_INTERVENTION_DAYS) {
            return ImprovementEvidenceDecision.insufficient();
        }
        String metric = value.individualAtt() != null ? "individual_att"
                : value.trendDelta() != null ? "trend_delta" : null;
        Double effect = value.individualAtt() != null ? value.individualAtt() : value.trendDelta();
        if (effect == null || !Double.isFinite(effect)) {
            return ImprovementEvidenceDecision.insufficient();
        }
        return new ImprovementEvidenceDecision(true, effect < 0 ? "improved" : "not_improved", metric, effect);
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

    //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】包内可见,供人群报告服务归一化风险等级------------
    static String normalizeLevel(String value) {
    //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】包内可见,供人群报告服务归一化风险等级------------
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

    private static Integer nullableInteger(ResultSet rs, int column) throws SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.intValue();
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

    //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】包内可见,供人群报告服务读取负责关系主体------------
    record Identity(String subjectRef, String userId, String name, Integer age, String gender, BigDecimal bmi) {}
    //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】包内可见,供人群报告服务读取负责关系主体------------
    private record RiskSnapshot(Double score, String level, Boolean isMock, String responseJson, Timestamp evaluatedAt) {}
    private record RhiSnapshot(Double score, Double confidence, String updatedAt) {}
    private record RdiSnapshot(Double score, Double confidence, String status, Boolean isMock,
                               String scoredOn, String updatedAt) {}
    record FeedbackAggregate(
            Double adherence, Double completedCount, Double expectedCount, String occurredAt
    ) {}
    private record VersionedFeedbackAggregate(
            long occurrenceCount, BigDecimal completed, BigDecimal expected, Timestamp latestActivityAt
    ) {}
    static record AttributionSnapshot(
            Boolean dataSufficient,
            Boolean isMock,
            Integer historyDays,
            Integer minHistoryDays,
            Integer interventionDays,
            Double adherenceAverage,
            Double individualAtt,
            Double trendDelta,
            String status,
            String interpretation,
            Timestamp createdAt
    ) {}

    static record ImprovementEvidenceDecision(
            boolean conclusive,
            String conclusion,
            String effectMetric,
            Double effectValue
    ) {
        static ImprovementEvidenceDecision insufficient() {
            return new ImprovementEvidenceDecision(false, "insufficient", null, null);
        }
    }
    private record Owner(String name, String department) {}
    private record CurrentIntervention(String summary, String dueAt) {}

    private static final class StreamDates {
        private StreamDates() {}
        static String max(String... values) {
            String result = null;
            for (String value : values) if (value != null && (result == null || value.compareTo(result) > 0)) result = value;
            return result;
        }
    }
}
