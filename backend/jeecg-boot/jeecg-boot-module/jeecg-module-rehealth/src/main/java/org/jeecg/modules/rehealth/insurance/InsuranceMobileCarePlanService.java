package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceMobileCarePlanService {
    static final String CALCULATION_VERSION = "care-plan-occurrence-adherence-28d-v1";
    private static final Set<String> FEEDBACK_TYPES = Set.of(
            "completed", "partially_completed", "skipped", "not_applicable"
    );
    private static final Set<String> VERIFICATION_TYPES = Set.of(
            "self_report", "device_verified", "staff_confirmed"
    );
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ZoneId zoneId;

    @Autowired
    public InsuranceMobileCarePlanService(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            ObjectMapper objectMapper
    ) {
        this(jdbc, objectMapper, Clock.systemUTC(), DEFAULT_ZONE);
    }

    InsuranceMobileCarePlanService(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock, ZoneId zoneId) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.zoneId = zoneId;
    }

    @Transactional
    public List<InsuranceMobileCarePlanResponse.Plan> current(String userId) {
        String normalizedUserId = required(userId, "userId", 64);
        LocalDateTime now = now();
        List<PlanRow> plans = jdbc.query("""
                SELECT plan.tenant_id, tenant.name organization_name, plan.id plan_id,
                       plan.subject_ref, revision.id revision_id, revision.revision_no,
                       revision.title, revision.summary, revision.effective_from, revision.effective_to
                FROM rehealth_care_plan plan
                JOIN rehealth_care_plan_revision revision
                  ON revision.tenant_id=plan.tenant_id AND revision.plan_id=plan.id
                 AND revision.status='published' AND revision.effective_from<=?
                 AND (revision.effective_to IS NULL OR revision.effective_to>?)
                JOIN rehealth_insurance_subject subject
                  ON subject.tenant_id=plan.tenant_id AND subject.subject_ref=plan.subject_ref
                 AND subject.rehealth_user_id=? AND subject.enrollment_status='active'
                 AND subject.consent_status='granted'
                JOIN sys_tenant tenant
                  ON tenant.id=plan.tenant_id AND tenant.status=1 AND tenant.del_flag=0
                WHERE plan.owner_type='insurance' AND plan.status='active'
                  AND plan.rehealth_user_id=?
                ORDER BY revision.effective_from DESC, plan.updated_at DESC
                """, (rs, rowNum) -> new PlanRow(
                rs.getInt("tenant_id"), rs.getString("organization_name"), rs.getString("plan_id"),
                rs.getString("subject_ref"), rs.getString("revision_id"), rs.getInt("revision_no"),
                rs.getString("title"), rs.getString("summary"), local(rs.getTimestamp("effective_from")),
                local(rs.getTimestamp("effective_to"))
        ), now, now, normalizedUserId, normalizedUserId);

        List<InsuranceMobileCarePlanResponse.Plan> response = new ArrayList<>();
        for (PlanRow plan : plans) {
            expandRollingWindow(plan, now);
            List<ItemRow> items = items(plan.tenantId(), plan.planId(), plan.revisionId());
            response.add(new InsuranceMobileCarePlanResponse.Plan(
                    plan.tenantId(), plan.organizationName(), plan.planId(), plan.revisionId(), plan.revisionNo(),
                    plan.title(), plan.summary(), plan.effectiveFrom(), plan.effectiveTo(),
                    adherence(plan, now), items.stream().map(item -> responseItem(plan, item, now)).toList()
            ));
        }
        return response;
    }

    @Transactional
    public Map<String, Object> feedback(
            String userId,
            String occurrenceId,
            InsuranceMobilePlanRequest.OccurrenceFeedback request
    ) {
        String normalizedUserId = required(userId, "userId", 64);
        String normalizedOccurrenceId = required(occurrenceId, "occurrenceId", 64);
        if (request == null) throw InsuranceApiException.badRequest("feedback request is required");
        OccurrenceOwner occurrence = jdbc.query("""
                SELECT occurrence.tenant_id, occurrence.id occurrence_id, occurrence.plan_id,
                       occurrence.revision_id, occurrence.plan_item_id, occurrence.logical_item_id,
                       occurrence.subject_ref, occurrence.status, item.allow_not_applicable
                FROM rehealth_care_plan_occurrence occurrence
                JOIN rehealth_care_plan plan
                  ON plan.tenant_id=occurrence.tenant_id AND plan.id=occurrence.plan_id
                 AND plan.owner_type='insurance' AND plan.status='active'
                JOIN rehealth_care_plan_item item
                  ON item.tenant_id=occurrence.tenant_id AND item.id=occurrence.plan_item_id
                JOIN rehealth_insurance_subject subject
                  ON subject.tenant_id=occurrence.tenant_id AND subject.subject_ref=occurrence.subject_ref
                 AND subject.rehealth_user_id=? AND subject.enrollment_status='active'
                 AND subject.consent_status='granted'
                JOIN sys_tenant tenant
                  ON tenant.id=occurrence.tenant_id AND tenant.status=1 AND tenant.del_flag=0
                WHERE occurrence.id=?
                """, (rs, rowNum) -> new OccurrenceOwner(
                rs.getInt("tenant_id"), rs.getString("occurrence_id"), rs.getString("plan_id"),
                rs.getString("revision_id"), rs.getString("plan_item_id"), rs.getString("logical_item_id"),
                rs.getString("subject_ref"), rs.getString("status"), rs.getBoolean("allow_not_applicable")
        ), normalizedUserId, normalizedOccurrenceId).stream().findFirst().orElseThrow(
                () -> InsuranceApiException.notFound("care plan occurrence was not found for the current user")
        );
        if (!"scheduled".equals(occurrence.status())) {
            throw InsuranceApiException.badRequest("cancelled care plan occurrence cannot be scored");
        }

        String feedbackType = required(request.feedbackType(), "feedbackType", 32).toLowerCase(Locale.ROOT);
        if (!FEEDBACK_TYPES.contains(feedbackType)) {
            throw InsuranceApiException.badRequest("feedbackType is unsupported");
        }
        if ("not_applicable".equals(feedbackType) && !occurrence.allowNotApplicable()) {
            throw InsuranceApiException.badRequest("this care plan item does not allow not_applicable feedback");
        }
        String verificationType = optional(request.verificationType(), "self_report", 32).toLowerCase(Locale.ROOT);
        if (!VERIFICATION_TYPES.contains(verificationType)) {
            throw InsuranceApiException.badRequest("verificationType is unsupported");
        }
        String sourceRecordId = required(request.sourceRecordId(), "sourceRecordId", 128);
        List<ExecutionReplay> replay = jdbc.query("""
                SELECT id, occurrence_id, feedback_type, score_value
                FROM rehealth_care_plan_execution
                WHERE tenant_id=? AND source_system='rehealth_app' AND source_record_id=?
                LIMIT 1
                """, (rs, rowNum) -> new ExecutionReplay(
                rs.getString("id"), rs.getString("occurrence_id"),
                rs.getString("feedback_type"), rs.getBigDecimal("score_value")
        ), occurrence.tenantId(), sourceRecordId);
        if (!replay.isEmpty()) {
            ExecutionReplay existing = replay.get(0);
            if (!occurrence.occurrenceId().equals(existing.occurrenceId())) {
                throw InsuranceApiException.conflict("sourceRecordId is already bound to another care plan occurrence");
            }
            return feedbackResponse(existing.id(), existing.feedbackType(), existing.scoreValue(), true);
        }

        LocalDateTime current = now();
        LocalDateTime occurredAt = request.occurredAt() == null ? current : request.occurredAt();
        if (occurredAt.isAfter(current.plusMinutes(5)) || occurredAt.isBefore(current.minusDays(35))) {
            throw InsuranceApiException.badRequest("occurredAt is outside the allowed feedback window");
        }
        BigDecimal score = score(feedbackType);
        String executionId = uuid();
        jdbc.update("""
                INSERT INTO rehealth_care_plan_execution (
                  id, tenant_id, occurrence_id, plan_id, revision_id, plan_item_id,
                  logical_item_id, subject_ref, feedback_type, score_value, verification_type,
                  note, occurred_at, source_system, source_record_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'rehealth_app', ?, ?)
                """, executionId, occurrence.tenantId(), occurrence.occurrenceId(), occurrence.planId(),
                occurrence.revisionId(), occurrence.planItemId(), occurrence.logicalItemId(), occurrence.subjectRef(),
                feedbackType, score, verificationType, optional(request.note(), null, 1000), occurredAt,
                sourceRecordId, current);
        return feedbackResponse(executionId, feedbackType, score, false);
    }

    private void expandRollingWindow(PlanRow plan, LocalDateTime now) {
        LocalDate startDate = now.toLocalDate().minusDays(27);
        LocalDateTime windowStart = startDate.atStartOfDay();
        LocalDateTime windowEnd = now.toLocalDate().plusDays(1).atStartOfDay();
        List<RevisionWindow> revisions = jdbc.query("""
                SELECT id, effective_from, effective_to
                FROM rehealth_care_plan_revision
                WHERE tenant_id=? AND plan_id=? AND status='published'
                  AND effective_from<? AND (effective_to IS NULL OR effective_to>?)
                ORDER BY revision_no
                """, (rs, rowNum) -> new RevisionWindow(
                rs.getString("id"), local(rs.getTimestamp("effective_from")), local(rs.getTimestamp("effective_to"))
        ), plan.tenantId(), plan.planId(), windowEnd, windowStart);
        for (RevisionWindow revision : revisions) {
            for (ItemRow item : items(plan.tenantId(), plan.planId(), revision.id())) {
                for (LocalDate date = startDate; !date.isAfter(now.toLocalDate()); date = date.plusDays(1)) {
                    ScheduledTime scheduled = scheduled(item.scheduleJson(), date);
                    if (!scheduled.supported() || scheduled.value() == null) continue;
                    LocalDateTime scheduledAt = scheduled.value();
                    if (revision.effectiveFrom() != null && scheduledAt.isBefore(revision.effectiveFrom())) continue;
                    if (revision.effectiveTo() != null && !scheduledAt.isBefore(revision.effectiveTo())) continue;
                    LocalDateTime dueAt = date.atTime(23, 59, 59, 999_000_000);
                    jdbc.update("""
                            INSERT IGNORE INTO rehealth_care_plan_occurrence (
                              id, tenant_id, plan_id, revision_id, plan_item_id, logical_item_id,
                              subject_ref, scheduled_at, due_at, status, exclusion_reason, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'scheduled', NULL, ?, ?)
                            """, occurrenceId(item.id(), scheduledAt), plan.tenantId(), plan.planId(), revision.id(),
                            item.id(), item.logicalItemId(), plan.subjectRef(), scheduledAt, dueAt, now, now);
                }
            }
        }
    }

    private InsuranceMobileCarePlanResponse.Item responseItem(PlanRow plan, ItemRow item, LocalDateTime now) {
        ScheduledTime schedule = scheduled(item.scheduleJson(), now.toLocalDate());
        InsuranceMobileCarePlanResponse.Occurrence occurrence = null;
        if (schedule.supported() && schedule.value() != null) {
            String id = occurrenceId(item.id(), schedule.value());
            occurrence = jdbc.query("""
                    SELECT occurrence.id, occurrence.scheduled_at, occurrence.due_at
                    FROM rehealth_care_plan_occurrence occurrence
                    WHERE occurrence.tenant_id=? AND occurrence.plan_id=? AND occurrence.revision_id=?
                      AND occurrence.plan_item_id=? AND occurrence.id=? AND occurrence.status='scheduled'
                    """, (rs, rowNum) -> {
                ExecutionValue execution = latestExecution(plan.tenantId(), rs.getString("id"));
                return new InsuranceMobileCarePlanResponse.Occurrence(
                        rs.getString("id"), local(rs.getTimestamp("scheduled_at")), local(rs.getTimestamp("due_at")),
                        execution == null ? null : execution.feedbackType(), execution == null ? null : execution.scoreValue()
                );
            }, plan.tenantId(), plan.planId(), plan.revisionId(), item.id(), id).stream().findFirst().orElse(null);
        }
        return new InsuranceMobileCarePlanResponse.Item(
                item.id(), item.logicalItemId(), item.category(), item.title(), item.instructions(), item.scoringWeight(),
                item.allowNotApplicable(), schedule.type(), schedule.supported(), occurrence
        );
    }

    private InsuranceMobileCarePlanResponse.Adherence adherence(PlanRow plan, LocalDateTime now) {
        LocalDateTime start = now.toLocalDate().minusDays(27).atStartOfDay();
        LocalDateTime end = now.toLocalDate().plusDays(1).atStartOfDay();
        List<OccurrenceValue> occurrences = jdbc.query("""
                SELECT occurrence.id, occurrence.due_at, item.scoring_weight
                FROM rehealth_care_plan_occurrence occurrence
                JOIN rehealth_care_plan_item item
                  ON item.tenant_id=occurrence.tenant_id AND item.id=occurrence.plan_item_id
                WHERE occurrence.tenant_id=? AND occurrence.plan_id=? AND occurrence.status='scheduled'
                  AND occurrence.scheduled_at>=? AND occurrence.scheduled_at<?
                ORDER BY occurrence.scheduled_at
                """, (rs, rowNum) -> new OccurrenceValue(
                rs.getString("id"), local(rs.getTimestamp("due_at")), rs.getBigDecimal("scoring_weight")
        ), plan.tenantId(), plan.planId(), start, end);

        BigDecimal numerator = BigDecimal.ZERO;
        BigDecimal denominator = BigDecimal.ZERO;
        int expectedCount = 0;
        int scoredCount = 0;
        int excludedCount = 0;
        for (OccurrenceValue occurrence : occurrences) {
            ExecutionValue execution = latestExecution(plan.tenantId(), occurrence.id());
            if (occurrence.dueAt().isAfter(now) && execution == null) continue;
            if (execution != null && "not_applicable".equals(execution.feedbackType())) {
                excludedCount++;
                continue;
            }
            BigDecimal weight = occurrence.weight() == null ? BigDecimal.ONE : occurrence.weight();
            denominator = denominator.add(weight);
            expectedCount++;
            if (execution != null) {
                scoredCount++;
                numerator = numerator.add(weight.multiply(execution.scoreValue() == null ? BigDecimal.ZERO : execution.scoreValue()));
            }
        }
        BigDecimal percent = denominator.signum() == 0 ? null
                : numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 1, RoundingMode.HALF_UP);
        return new InsuranceMobileCarePlanResponse.Adherence(
                28, percent, expectedCount, scoredCount, excludedCount, CALCULATION_VERSION
        );
    }

    private ExecutionValue latestExecution(int tenantId, String occurrenceId) {
        return jdbc.query("""
                SELECT feedback_type, score_value
                FROM rehealth_care_plan_execution
                WHERE tenant_id=? AND occurrence_id=?
                ORDER BY occurred_at DESC, created_at DESC, id DESC
                LIMIT 1
                """, (rs, rowNum) -> new ExecutionValue(rs.getString("feedback_type"), rs.getBigDecimal("score_value")),
                tenantId, occurrenceId).stream().findFirst().orElse(null);
    }

    private List<ItemRow> items(int tenantId, String planId, String revisionId) {
        return jdbc.query("""
                SELECT id, logical_item_id, category, title, instructions, schedule_json,
                       scoring_weight, allow_not_applicable, display_order
                FROM rehealth_care_plan_item
                WHERE tenant_id=? AND plan_id=? AND revision_id=?
                ORDER BY display_order
                """, (rs, rowNum) -> new ItemRow(
                rs.getString("id"), rs.getString("logical_item_id"), rs.getString("category"),
                rs.getString("title"), rs.getString("instructions"), rs.getString("schedule_json"),
                rs.getBigDecimal("scoring_weight"), rs.getBoolean("allow_not_applicable")
        ), tenantId, planId, revisionId);
    }

    private ScheduledTime scheduled(String json, LocalDate date) {
        if (json == null || json.isBlank()) return new ScheduledTime(null, false, null);
        try {
            JsonNode node = objectMapper.readTree(json);
            String type = text(node, "type");
            if (type == null) type = text(node, "frequency");
            if (type == null) return new ScheduledTime(null, false, null);
            type = type.toLowerCase(Locale.ROOT);
            LocalTime time = parseTime(text(node, "time"));
            boolean applies = switch (type) {
                case "daily" -> true;
                case "weekly" -> weeklyDays(node).contains(date.getDayOfWeek().getValue());
                case "once" -> date.toString().equals(text(node, "date"));
                default -> false;
            };
            boolean supported = Set.of("daily", "weekly", "once").contains(type);
            return new ScheduledTime(type, supported, supported && applies ? date.atTime(time) : null);
        } catch (Exception ignored) {
            return new ScheduledTime(null, false, null);
        }
    }

    private static Set<Integer> weeklyDays(JsonNode node) {
        JsonNode days = node.get("days");
        if (days == null || !days.isArray()) return Set.of();
        Set<Integer> result = new LinkedHashSet<>();
        for (JsonNode day : days) {
            if (day.canConvertToInt() && day.asInt() >= 1 && day.asInt() <= 7) result.add(day.asInt());
        }
        return result;
    }

    private static LocalTime parseTime(String value) {
        if (value == null) return LocalTime.of(20, 0);
        try { return LocalTime.parse(value); }
        catch (DateTimeParseException ignored) { return LocalTime.of(20, 0); }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || !value.isTextual() || value.asText().isBlank() ? null : value.asText().trim();
    }

    private static BigDecimal score(String feedbackType) {
        return switch (feedbackType) {
            case "completed" -> BigDecimal.ONE;
            case "partially_completed" -> BigDecimal.valueOf(0.5);
            case "skipped" -> BigDecimal.ZERO;
            default -> null;
        };
    }

    private static Map<String, Object> feedbackResponse(
            String executionId, String feedbackType, BigDecimal score, boolean replay
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("executionId", executionId);
        response.put("status", "accepted");
        response.put("feedbackType", feedbackType);
        if (score != null) response.put("scoreValue", score);
        response.put("idempotentReplay", replay);
        response.put("calculationVersion", CALCULATION_VERSION);
        return response;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), zoneId);
    }

    private static String occurrenceId(String itemId, LocalDateTime scheduledAt) {
        try {
            String value = itemId + "|" + scheduledAt;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static LocalDateTime local(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static String required(String value, String field, int max) {
        String normalized = optional(value, null, max);
        if (normalized == null) throw InsuranceApiException.badRequest(field + " is required");
        return normalized;
    }

    private static String optional(String value, String fallback, int max) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim();
        if (normalized.length() > max) throw InsuranceApiException.badRequest("value exceeds maximum length " + max);
        return normalized;
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private record PlanRow(
            int tenantId, String organizationName, String planId, String subjectRef, String revisionId,
            int revisionNo, String title, String summary, LocalDateTime effectiveFrom, LocalDateTime effectiveTo
    ) {
    }

    private record RevisionWindow(String id, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
    }

    private record ItemRow(
            String id, String logicalItemId, String category, String title, String instructions,
            String scheduleJson, BigDecimal scoringWeight, boolean allowNotApplicable
    ) {
    }

    private record OccurrenceValue(String id, LocalDateTime dueAt, BigDecimal weight) {
    }

    private record ExecutionValue(String feedbackType, BigDecimal scoreValue) {
    }

    private record ExecutionReplay(String id, String occurrenceId, String feedbackType, BigDecimal scoreValue) {
    }

    private record OccurrenceOwner(
            int tenantId, String occurrenceId, String planId, String revisionId, String planItemId,
            String logicalItemId, String subjectRef, String status, boolean allowNotApplicable
    ) {
    }

    private record ScheduledTime(String type, boolean supported, LocalDateTime value) {
    }
}
