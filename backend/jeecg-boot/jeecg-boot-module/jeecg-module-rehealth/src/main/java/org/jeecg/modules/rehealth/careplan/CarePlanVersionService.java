package org.jeecg.modules.rehealth.careplan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class CarePlanVersionService {
    private static final Set<String> OWNER_TYPES = Set.of("insurance", "medical", "personal");
    private static final Set<String> INSURANCE_CATEGORIES = Set.of(
            "exercise", "nutrition", "sleep", "lifestyle", "follow_up",
            "reminder", "incentive", "education", "monitoring"
    );

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public CarePlanVersionService(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            ObjectMapper objectMapper
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<CarePlanVersionResponse.Plan> list(int tenantId, String ownerType, String subjectRef) {
        String normalizedOwner = ownerType(ownerType);
        List<String> ids = jdbc.query("""
                SELECT id FROM rehealth_care_plan
                WHERE tenant_id=? AND owner_type=? AND subject_ref=?
                ORDER BY updated_at DESC
                """, (rs, rowNum) -> rs.getString(1), tenantId, normalizedOwner,
                required(subjectRef, "subjectRef", 64));
        return ids.stream().map(id -> get(tenantId, normalizedOwner, id)).toList();
    }

    public CarePlanVersionResponse.Plan get(int tenantId, String ownerType, String planId) {
        PlanRow plan = requirePlan(tenantId, ownerType(ownerType), planId);
        List<CarePlanVersionResponse.Revision> revisions = jdbc.query("""
                SELECT id, revision_no, status, title, summary, change_reason, content_hash,
                       effective_from, effective_to, published_at
                FROM rehealth_care_plan_revision
                WHERE tenant_id=? AND plan_id=?
                ORDER BY revision_no DESC
                """, (rs, rowNum) -> revision(rs, tenantId, plan.id()), tenantId, plan.id());
        return new CarePlanVersionResponse.Plan(
                plan.id(), plan.subjectRef(), plan.ownerType(), plan.status(), plan.lockVersion(),
                plan.currentRevisionId(), plan.draftRevisionId(), revisions, format(plan.updatedAt())
        );
    }

    public String subjectRef(int tenantId, String ownerType, String planId) {
        return requirePlan(tenantId, ownerType(ownerType), planId).subjectRef();
    }

    @Transactional
    public CarePlanVersionResponse.Plan createDraft(
            int tenantId,
            String ownerType,
            String ownerOrgRef,
            String subjectRef,
            String rehealthUserId,
            String actorUserId,
            CarePlanVersionRequest.CreateDraft request
    ) {
        if (request == null) throw CarePlanVersionException.badRequest("care plan request is required");
        String normalizedOwner = ownerType(ownerType);
        String planId = uuid();
        String revisionId = uuid();
        String title = required(request.title(), "title", 255);
        String summary = optional(request.summary(), 2000);
        String reason = optional(request.changeReason(), 1000);
        List<ItemValue> items = normalizeItems(normalizedOwner, null, request.items());
        String hash = contentHash(title, summary, reason, items);
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO rehealth_care_plan (
                  id, tenant_id, owner_type, owner_org_ref, subject_ref, rehealth_user_id,
                  source_plan_id, status, current_revision_id, draft_revision_id, lock_version,
                  created_by, created_at, updated_by, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, NULL, 'draft', NULL, ?, 0, ?, ?, ?, ?)
                """, planId, tenantId, normalizedOwner, required(ownerOrgRef, "ownerOrgRef", 64),
                required(subjectRef, "subjectRef", 64), required(rehealthUserId, "rehealthUserId", 64),
                revisionId, required(actorUserId, "actorUserId", 64), now, actorUserId, now);
        insertRevision(tenantId, planId, revisionId, 1, title, summary, reason, hash, actorUserId, now);
        replaceItems(tenantId, planId, revisionId, items, now);
        audit(tenantId, normalizedOwner, actorUserId, "create_draft", planId, revisionId,
                null, hash, reason, now);
        return get(tenantId, normalizedOwner, planId);
    }

    @Transactional
    public CarePlanVersionResponse.Plan updateDraft(
            int tenantId,
            String ownerType,
            String planId,
            String actorUserId,
            CarePlanVersionRequest.UpdateDraft request
    ) {
        if (request == null) throw CarePlanVersionException.badRequest("care plan request is required");
        String normalizedOwner = ownerType(ownerType);
        PlanRow plan = requirePlan(tenantId, normalizedOwner, planId);
        long expected = expected(request.expectedLockVersion());
        if (plan.draftRevisionId() == null) {
            throw CarePlanVersionException.conflict("published plans must be cloned to a new draft revision before editing");
        }
        RevisionRow draft = requireRevision(tenantId, plan.id(), plan.draftRevisionId());
        if (!"draft".equals(draft.status())) {
            throw CarePlanVersionException.conflict("only a draft revision can be edited");
        }
        String title = required(request.title(), "title", 255);
        String summary = optional(request.summary(), 2000);
        String reason = optional(request.changeReason(), 1000);
        List<ItemValue> items = normalizeItems(normalizedOwner, plan.id(), request.items());
        String beforeHash = draft.contentHash();
        String afterHash = contentHash(title, summary, reason, items);
        LocalDateTime now = LocalDateTime.now();
        acquireLock(plan, tenantId, normalizedOwner, expected, actorUserId, now);
        jdbc.update("""
                UPDATE rehealth_care_plan_revision
                SET title=?, summary=?, change_reason=?, content_hash=?, updated_by=?, updated_at=?
                WHERE tenant_id=? AND plan_id=? AND id=? AND status='draft'
                """, title, summary, reason, afterHash, actorUserId, now,
                tenantId, plan.id(), draft.id());
        replaceItems(tenantId, plan.id(), draft.id(), items, now);
        audit(tenantId, normalizedOwner, actorUserId, "update_draft", plan.id(), draft.id(),
                beforeHash, afterHash, reason, now);
        return get(tenantId, normalizedOwner, plan.id());
    }

    @Transactional
    public CarePlanVersionResponse.Plan cloneRevision(
            int tenantId,
            String ownerType,
            String planId,
            String actorUserId,
            CarePlanVersionRequest.CreateRevision request
    ) {
        if (request == null) throw CarePlanVersionException.badRequest("revision request is required");
        String normalizedOwner = ownerType(ownerType);
        PlanRow plan = requirePlan(tenantId, normalizedOwner, planId);
        long expected = expected(request.expectedLockVersion());
        if (!"active".equals(plan.status()) || plan.currentRevisionId() == null) {
            throw CarePlanVersionException.conflict("only an active published plan can create a new revision");
        }
        if (plan.draftRevisionId() != null) {
            throw CarePlanVersionException.conflict("the plan already has a mutable draft revision");
        }
        RevisionRow current = requireRevision(tenantId, plan.id(), plan.currentRevisionId());
        List<ItemValue> items = itemValues(tenantId, current.id());
        int nextRevision = jdbc.queryForObject(
                "SELECT COALESCE(MAX(revision_no), 0) + 1 FROM rehealth_care_plan_revision WHERE tenant_id=? AND plan_id=?",
                Integer.class, tenantId, plan.id());
        String revisionId = uuid();
        String reason = optional(request.changeReason(), 1000);
        String hash = contentHash(current.title(), current.summary(), reason, items);
        LocalDateTime now = LocalDateTime.now();
        acquireLock(plan, tenantId, normalizedOwner, expected, actorUserId, now);
        insertRevision(tenantId, plan.id(), revisionId, nextRevision, current.title(), current.summary(),
                reason, hash, actorUserId, now);
        replaceItems(tenantId, plan.id(), revisionId, items, now);
        jdbc.update("""
                UPDATE rehealth_care_plan SET draft_revision_id=?
                WHERE tenant_id=? AND owner_type=? AND id=?
                """, revisionId, tenantId, normalizedOwner, plan.id());
        audit(tenantId, normalizedOwner, actorUserId, "clone_revision", plan.id(), revisionId,
                current.contentHash(), hash, reason, now);
        return get(tenantId, normalizedOwner, plan.id());
    }

    @Transactional
    public CarePlanVersionResponse.Plan publish(
            int tenantId,
            String ownerType,
            String planId,
            String actorUserId,
            CarePlanVersionRequest.Publish request
    ) {
        if (request == null) throw CarePlanVersionException.badRequest("publish request is required");
        String normalizedOwner = ownerType(ownerType);
        PlanRow plan = requirePlan(tenantId, normalizedOwner, planId);
        long expected = expected(request.expectedLockVersion());
        if (plan.draftRevisionId() == null) {
            throw CarePlanVersionException.conflict("the plan has no draft revision to publish");
        }
        RevisionRow draft = requireRevision(tenantId, plan.id(), plan.draftRevisionId());
        if (!"draft".equals(draft.status())) {
            throw CarePlanVersionException.conflict("only a draft revision can be published");
        }
        if (itemValues(tenantId, draft.id()).isEmpty()) {
            throw CarePlanVersionException.badRequest("a published care plan must contain at least one item");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveAt = request.effectiveAt() == null ? now : request.effectiveAt();
        if (effectiveAt.isBefore(now.minusSeconds(1))) {
            throw CarePlanVersionException.badRequest("effective_at cannot be in the past");
        }
        RevisionRow previous = plan.currentRevisionId() == null
                ? null : requireRevision(tenantId, plan.id(), plan.currentRevisionId());
        if (previous != null && previous.effectiveFrom() != null && previous.effectiveFrom().isAfter(now)) {
            throw CarePlanVersionException.conflict("a future published revision is already scheduled");
        }
        acquireLock(plan, tenantId, normalizedOwner, expected, actorUserId, now);
        if (previous != null) {
            jdbc.update("""
                    UPDATE rehealth_care_plan_revision SET effective_to=?, updated_by=?, updated_at=?
                    WHERE tenant_id=? AND plan_id=? AND id=? AND status='published'
                    """, effectiveAt, actorUserId, now, tenantId, plan.id(), previous.id());
            jdbc.update("""
                    UPDATE rehealth_care_plan_occurrence
                    SET status='cancelled', exclusion_reason='superseded_by_revision', updated_at=?
                    WHERE tenant_id=? AND plan_id=? AND revision_id=? AND status='scheduled'
                      AND scheduled_at>=?
                    """, now, tenantId, plan.id(), previous.id(), effectiveAt);
        }
        jdbc.update("""
                UPDATE rehealth_care_plan_revision
                SET status='published', effective_from=?, effective_to=NULL,
                    published_by=?, published_at=?, updated_by=?, updated_at=?
                WHERE tenant_id=? AND plan_id=? AND id=? AND status='draft'
                """, effectiveAt, actorUserId, now, actorUserId, now,
                tenantId, plan.id(), draft.id());
        jdbc.update("""
                UPDATE rehealth_care_plan
                SET status='active', current_revision_id=?, draft_revision_id=NULL
                WHERE tenant_id=? AND owner_type=? AND id=?
                """, draft.id(), tenantId, normalizedOwner, plan.id());
        audit(tenantId, normalizedOwner, actorUserId, "publish", plan.id(), draft.id(),
                previous == null ? null : previous.contentHash(), draft.contentHash(), draft.changeReason(), now);
        return get(tenantId, normalizedOwner, plan.id());
    }

    @Transactional
    public CarePlanVersionResponse.Plan withdraw(
            int tenantId,
            String ownerType,
            String planId,
            String actorUserId,
            CarePlanVersionRequest.Withdraw request
    ) {
        if (request == null) throw CarePlanVersionException.badRequest("withdraw request is required");
        String normalizedOwner = ownerType(ownerType);
        PlanRow plan = requirePlan(tenantId, normalizedOwner, planId);
        long expected = expected(request.expectedLockVersion());
        String reason = required(request.reason(), "reason", 1000);
        if (plan.draftRevisionId() != null) {
            throw CarePlanVersionException.conflict("discard the draft revision before withdrawing the published plan");
        }
        if (!"active".equals(plan.status()) || plan.currentRevisionId() == null) {
            throw CarePlanVersionException.conflict("only an active plan can be withdrawn");
        }
        RevisionRow current = requireRevision(tenantId, plan.id(), plan.currentRevisionId());
        LocalDateTime now = LocalDateTime.now();
        acquireLock(plan, tenantId, normalizedOwner, expected, actorUserId, now);
        if (current.effectiveFrom() != null && current.effectiveFrom().isAfter(now)) {
            RevisionRow previous = currentEffectiveRevision(tenantId, plan.id(), current.id(), now);
            jdbc.update("""
                    UPDATE rehealth_care_plan_revision
                    SET status='withdrawn', effective_to=effective_from, withdrawn_by=?, withdrawn_at=?,
                        updated_by=?, updated_at=?
                    WHERE tenant_id=? AND plan_id=? AND id=? AND status='published'
                    """, actorUserId, now, actorUserId, now, tenantId, plan.id(), current.id());
            if (previous == null) {
                jdbc.update("""
                        UPDATE rehealth_care_plan SET status='withdrawn', current_revision_id=NULL
                        WHERE tenant_id=? AND owner_type=? AND id=?
                        """, tenantId, normalizedOwner, plan.id());
            } else {
                jdbc.update("""
                        UPDATE rehealth_care_plan_revision SET effective_to=NULL, updated_by=?, updated_at=?
                        WHERE tenant_id=? AND plan_id=? AND id=?
                        """, actorUserId, now, tenantId, plan.id(), previous.id());
                jdbc.update("""
                        UPDATE rehealth_care_plan SET current_revision_id=?
                        WHERE tenant_id=? AND owner_type=? AND id=?
                        """, previous.id(), tenantId, normalizedOwner, plan.id());
            }
        } else {
            jdbc.update("""
                    UPDATE rehealth_care_plan_revision
                    SET status='withdrawn', effective_to=?, withdrawn_by=?, withdrawn_at=?,
                        updated_by=?, updated_at=?
                    WHERE tenant_id=? AND plan_id=? AND id=? AND status='published'
                    """, now, actorUserId, now, actorUserId, now, tenantId, plan.id(), current.id());
            jdbc.update("""
                    UPDATE rehealth_care_plan SET status='withdrawn'
                    WHERE tenant_id=? AND owner_type=? AND id=?
                    """, tenantId, normalizedOwner, plan.id());
        }
        jdbc.update("""
                UPDATE rehealth_care_plan_occurrence
                SET status='cancelled', exclusion_reason='revision_withdrawn', updated_at=?
                WHERE tenant_id=? AND plan_id=? AND revision_id=? AND status='scheduled'
                  AND scheduled_at>=?
                """, now, tenantId, plan.id(), current.id(), now);
        audit(tenantId, normalizedOwner, actorUserId, "withdraw", plan.id(), current.id(),
                current.contentHash(), null, reason, now);
        return get(tenantId, normalizedOwner, plan.id());
    }

    @Transactional
    public CarePlanVersionResponse.Plan discardDraft(
            int tenantId,
            String ownerType,
            String planId,
            String actorUserId,
            CarePlanVersionRequest.DiscardDraft request
    ) {
        if (request == null) throw CarePlanVersionException.badRequest("discard draft request is required");
        String normalizedOwner = ownerType(ownerType);
        PlanRow plan = requirePlan(tenantId, normalizedOwner, planId);
        long expected = expected(request.expectedLockVersion());
        String reason = required(request.reason(), "reason", 1000);
        if (plan.draftRevisionId() == null) {
            throw CarePlanVersionException.conflict("the plan has no draft revision to discard");
        }
        RevisionRow draft = requireRevision(tenantId, plan.id(), plan.draftRevisionId());
        if (!"draft".equals(draft.status())) {
            throw CarePlanVersionException.conflict("only a draft revision can be discarded");
        }
        LocalDateTime now = LocalDateTime.now();
        acquireLock(plan, tenantId, normalizedOwner, expected, actorUserId, now);
        jdbc.update("""
                UPDATE rehealth_care_plan_revision
                SET status='withdrawn', withdrawn_by=?, withdrawn_at=?, updated_by=?, updated_at=?
                WHERE tenant_id=? AND plan_id=? AND id=? AND status='draft'
                """, actorUserId, now, actorUserId, now, tenantId, plan.id(), draft.id());
        jdbc.update("""
                UPDATE rehealth_care_plan
                SET draft_revision_id=NULL, status=?
                WHERE tenant_id=? AND owner_type=? AND id=?
                """, plan.currentRevisionId() == null ? "withdrawn" : "active",
                tenantId, normalizedOwner, plan.id());
        audit(tenantId, normalizedOwner, actorUserId, "discard_draft", plan.id(), draft.id(),
                draft.contentHash(), null, reason, now);
        return get(tenantId, normalizedOwner, plan.id());
    }

    private PlanRow requirePlan(int tenantId, String ownerType, String planId) {
        return jdbc.query("""
                SELECT id, owner_type, subject_ref, status, current_revision_id, draft_revision_id,
                       lock_version, updated_at
                FROM rehealth_care_plan
                WHERE tenant_id=? AND owner_type=? AND id=?
                LIMIT 1
                """, (rs, rowNum) -> new PlanRow(
                        rs.getString("id"), rs.getString("owner_type"), rs.getString("subject_ref"),
                        rs.getString("status"), rs.getString("current_revision_id"),
                        rs.getString("draft_revision_id"), rs.getLong("lock_version"),
                        rs.getTimestamp("updated_at")
                ), tenantId, ownerType, required(planId, "planId", 64)).stream().findFirst()
                .orElseThrow(() -> CarePlanVersionException.notFound("care plan was not found"));
    }

    private RevisionRow requireRevision(int tenantId, String planId, String revisionId) {
        return jdbc.query("""
                SELECT id, revision_no, status, title, summary, change_reason, content_hash,
                       effective_from, effective_to
                FROM rehealth_care_plan_revision
                WHERE tenant_id=? AND plan_id=? AND id=?
                LIMIT 1
                """, (rs, rowNum) -> new RevisionRow(
                        rs.getString("id"), rs.getInt("revision_no"), rs.getString("status"),
                        rs.getString("title"), rs.getString("summary"), rs.getString("change_reason"),
                        rs.getString("content_hash"), local(rs.getTimestamp("effective_from")),
                        local(rs.getTimestamp("effective_to"))
                ), tenantId, planId, revisionId).stream().findFirst()
                .orElseThrow(() -> CarePlanVersionException.notFound("care plan revision was not found"));
    }

    private RevisionRow currentEffectiveRevision(int tenantId, String planId, String excludedId, LocalDateTime now) {
        return jdbc.query("""
                SELECT id, revision_no, status, title, summary, change_reason, content_hash,
                       effective_from, effective_to
                FROM rehealth_care_plan_revision
                WHERE tenant_id=? AND plan_id=? AND id<>? AND status='published'
                  AND effective_from<=? AND (effective_to IS NULL OR effective_to>?)
                ORDER BY revision_no DESC LIMIT 1
                """, (rs, rowNum) -> new RevisionRow(
                        rs.getString("id"), rs.getInt("revision_no"), rs.getString("status"),
                        rs.getString("title"), rs.getString("summary"), rs.getString("change_reason"),
                        rs.getString("content_hash"), local(rs.getTimestamp("effective_from")),
                        local(rs.getTimestamp("effective_to"))
                ), tenantId, planId, excludedId, now, now).stream().findFirst().orElse(null);
    }

    private CarePlanVersionResponse.Revision revision(ResultSet rs, int tenantId, String planId) throws SQLException {
        String revisionId = rs.getString("id");
        return new CarePlanVersionResponse.Revision(
                revisionId, rs.getInt("revision_no"), rs.getString("status"), rs.getString("title"),
                rs.getString("summary"), rs.getString("change_reason"), rs.getString("content_hash"),
                format(rs.getTimestamp("effective_from")), format(rs.getTimestamp("effective_to")),
                format(rs.getTimestamp("published_at")), responseItems(tenantId, planId, revisionId)
        );
    }

    private List<CarePlanVersionResponse.Item> responseItems(int tenantId, String planId, String revisionId) {
        return jdbc.query("""
                SELECT id, logical_item_id, category, title, instructions, schedule_json,
                       scoring_weight, allow_not_applicable, display_order
                FROM rehealth_care_plan_item
                WHERE tenant_id=? AND plan_id=? AND revision_id=?
                ORDER BY display_order
                """, (rs, rowNum) -> new CarePlanVersionResponse.Item(
                        rs.getString("id"), rs.getString("logical_item_id"), rs.getString("category"),
                        rs.getString("title"), rs.getString("instructions"), jsonNode(rs.getString("schedule_json")),
                        rs.getBigDecimal("scoring_weight"), rs.getBoolean("allow_not_applicable"),
                        rs.getInt("display_order")
                ), tenantId, planId, revisionId);
    }

    private List<ItemValue> itemValues(int tenantId, String revisionId) {
        return jdbc.query("""
                SELECT logical_item_id, category, title, instructions, schedule_json,
                       scoring_weight, allow_not_applicable, display_order
                FROM rehealth_care_plan_item
                WHERE tenant_id=? AND revision_id=?
                ORDER BY display_order
                """, (rs, rowNum) -> new ItemValue(
                        rs.getString("logical_item_id"), rs.getString("category"), rs.getString("title"),
                        rs.getString("instructions"), rs.getString("schedule_json"),
                        rs.getBigDecimal("scoring_weight"), rs.getBoolean("allow_not_applicable"),
                        rs.getInt("display_order")
                ), tenantId, revisionId);
    }

    private List<ItemValue> normalizeItems(
            String ownerType, String planId, List<CarePlanVersionRequest.Item> requested
    ) {
        if (requested == null) throw CarePlanVersionException.badRequest("items are required");
        if (requested.size() > 100) throw CarePlanVersionException.badRequest("items must not exceed 100");
        Set<String> existingLogicalIds = planId == null ? Set.of() : new HashSet<>(jdbc.query(
                "SELECT DISTINCT logical_item_id FROM rehealth_care_plan_item WHERE plan_id=?",
                (rs, rowNum) -> rs.getString(1), planId));
        Set<String> submittedLogicalIds = new HashSet<>();
        List<ItemValue> result = new ArrayList<>(requested.size());
        for (int index = 0; index < requested.size(); index++) {
            CarePlanVersionRequest.Item item = requested.get(index);
            if (item == null) throw CarePlanVersionException.badRequest("items must not contain null entries");
            String category = required(item.category(), "item.category", 32).toLowerCase();
            if ("insurance".equals(ownerType) && !INSURANCE_CATEGORIES.contains(category)) {
                throw CarePlanVersionException.badRequest(
                        "insurance plans support lifestyle, reminder, education, monitoring, and follow-up items only"
                );
            }
            String logicalId = optional(item.logicalItemId(), 64);
            if (logicalId != null && (planId == null || !existingLogicalIds.contains(logicalId))) {
                throw CarePlanVersionException.badRequest("logical_item_id does not belong to this care plan");
            }
            if (logicalId == null) logicalId = uuid();
            if (!submittedLogicalIds.add(logicalId)) {
                throw CarePlanVersionException.badRequest("logical_item_id must be unique within a revision");
            }
            BigDecimal weight = item.scoringWeight() == null ? BigDecimal.ONE : item.scoringWeight();
            if (weight.signum() <= 0 || weight.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw CarePlanVersionException.badRequest("scoring_weight must be greater than 0 and at most 100");
            }
            String scheduleJson = scheduleJson(item.schedule());
            result.add(new ItemValue(
                    logicalId, category, required(item.title(), "item.title", 255),
                    optional(item.instructions(), 4000), scheduleJson, weight,
                    item.allowNotApplicable() == null || item.allowNotApplicable(), index + 1
            ));
        }
        return result;
    }

    private void insertRevision(
            int tenantId, String planId, String revisionId, int revisionNo,
            String title, String summary, String reason, String hash, String actorUserId, LocalDateTime now
    ) {
        jdbc.update("""
                INSERT INTO rehealth_care_plan_revision (
                  id, tenant_id, plan_id, revision_no, status, title, summary, change_reason,
                  content_hash, effective_from, effective_to, published_by, published_at,
                  withdrawn_by, withdrawn_at, created_by, created_at, updated_by, updated_at
                ) VALUES (?, ?, ?, ?, 'draft', ?, ?, ?, ?, NULL, NULL, NULL, NULL,
                          NULL, NULL, ?, ?, ?, ?)
                """, revisionId, tenantId, planId, revisionNo, title, summary, reason, hash,
                actorUserId, now, actorUserId, now);
    }

    private void replaceItems(
            int tenantId, String planId, String revisionId, List<ItemValue> items, LocalDateTime now
    ) {
        jdbc.update("DELETE FROM rehealth_care_plan_item WHERE tenant_id=? AND plan_id=? AND revision_id=?",
                tenantId, planId, revisionId);
        for (ItemValue item : items) {
            jdbc.update("""
                    INSERT INTO rehealth_care_plan_item (
                      id, tenant_id, plan_id, revision_id, logical_item_id, category, title,
                      instructions, schedule_json, scoring_weight, allow_not_applicable,
                      display_order, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, uuid(), tenantId, planId, revisionId, item.logicalItemId(), item.category(),
                    item.title(), item.instructions(), item.scheduleJson(), item.scoringWeight(),
                    item.allowNotApplicable(), item.displayOrder(), now);
        }
    }

    private void acquireLock(
            PlanRow plan,
            int tenantId,
            String ownerType,
            long expected,
            String actorUserId,
            LocalDateTime now
    ) {
        if (plan.lockVersion() != expected) {
            throw CarePlanVersionException.conflict("care plan was changed by another editor; reload before retrying");
        }
        int updated = jdbc.update("""
                UPDATE rehealth_care_plan
                SET lock_version=lock_version+1, updated_by=?, updated_at=?
                WHERE tenant_id=? AND owner_type=? AND id=? AND lock_version=?
                """, required(actorUserId, "actorUserId", 64), now,
                tenantId, ownerType, plan.id(), expected);
        if (updated != 1) {
            throw CarePlanVersionException.conflict("care plan was changed by another editor; reload before retrying");
        }
    }

    private void audit(
            int tenantId, String ownerType, String actorUserId, String action,
            String planId, String revisionId, String beforeHash, String afterHash,
            String reason, LocalDateTime now
    ) {
        jdbc.update("""
                INSERT INTO rehealth_care_plan_audit_event (
                  id, tenant_id, owner_type, actor_user_id, action, plan_id, revision_id,
                  before_hash, after_hash, reason, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, uuid(), tenantId, ownerType, actorUserId, action, planId, revisionId,
                beforeHash, afterHash, reason, now);
    }

    private String contentHash(String title, String summary, String reason, List<ItemValue> items) {
        StringBuilder canonical = new StringBuilder();
        canonical.append(title).append('\n').append(nullToEmpty(summary)).append('\n')
                .append(nullToEmpty(reason)).append('\n');
        items.stream().sorted(Comparator.comparingInt(ItemValue::displayOrder)).forEach(item -> canonical
                .append(item.logicalItemId()).append('|').append(item.category()).append('|')
                .append(item.title()).append('|').append(nullToEmpty(item.instructions())).append('|')
                .append(nullToEmpty(item.scheduleJson())).append('|').append(item.scoringWeight().stripTrailingZeros())
                .append('|').append(item.allowNotApplicable()).append('|').append(item.displayOrder()).append('\n'));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String scheduleJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) return null;
        try {
            String json = objectMapper.writer().with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(value);
            if (json.length() > 4000) throw CarePlanVersionException.badRequest("item.schedule is too large");
            return json;
        } catch (JsonProcessingException e) {
            throw CarePlanVersionException.badRequest("item.schedule is invalid");
        }
    }

    private JsonNode jsonNode(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("stored care plan schedule JSON is invalid", e);
        }
    }

    private static String ownerType(String value) {
        String normalized = required(value, "ownerType", 32).toLowerCase();
        if (!OWNER_TYPES.contains(normalized)) {
            throw CarePlanVersionException.badRequest("ownerType is unsupported");
        }
        return normalized;
    }

    private static long expected(Long value) {
        if (value == null || value < 0) {
            throw CarePlanVersionException.badRequest("expected_lock_version is required and must not be negative");
        }
        return value;
    }

    private static String required(String value, String field, int max) {
        String normalized = optional(value, max);
        if (normalized == null) throw CarePlanVersionException.badRequest(field + " is required");
        return normalized;
    }

    private static String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > max) {
            throw CarePlanVersionException.badRequest("value exceeds maximum length " + max);
        }
        return normalized;
    }

    private static LocalDateTime local(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static String format(Timestamp value) {
        return value == null ? null : value.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private record PlanRow(
            String id,
            String ownerType,
            String subjectRef,
            String status,
            String currentRevisionId,
            String draftRevisionId,
            long lockVersion,
            Timestamp updatedAt
    ) {
    }

    private record RevisionRow(
            String id,
            int revisionNo,
            String status,
            String title,
            String summary,
            String changeReason,
            String contentHash,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo
    ) {
    }

    private record ItemValue(
            String logicalItemId,
            String category,
            String title,
            String instructions,
            String scheduleJson,
            BigDecimal scoringWeight,
            boolean allowNotApplicable,
            int displayOrder
    ) {
    }
}
