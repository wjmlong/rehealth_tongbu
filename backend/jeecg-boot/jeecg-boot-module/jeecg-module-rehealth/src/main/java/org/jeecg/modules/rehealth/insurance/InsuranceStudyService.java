package org.jeecg.modules.rehealth.insurance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceAuditEventEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceRweReportEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceSettlementApprovalEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceSettlementPackageEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceStudyEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceStudyJobEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceStudyMemberEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceStudyResultEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceStudySnapshotEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceAuditEventMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceRweReportMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceSettlementApprovalMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceSettlementPackageMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceStudyJobMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceStudyMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceStudyMemberMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceStudyResultMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceStudySnapshotMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceStudyService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final InsuranceStudyMapper studyMapper;
    private final InsuranceStudySnapshotMapper snapshotMapper;
    private final InsuranceStudyMemberMapper memberMapper;
    private final InsuranceStudyJobMapper jobMapper;
    private final InsuranceStudyResultMapper resultMapper;
    private final InsuranceRweReportMapper reportMapper;
    private final InsuranceSettlementPackageMapper settlementMapper;
    private final InsuranceSettlementApprovalMapper approvalMapper;
    private final InsuranceAuditEventMapper auditMapper;
    private final ObjectMapper objectMapper;

    public InsuranceStudyService(
            InsuranceStudyMapper studyMapper,
            InsuranceStudySnapshotMapper snapshotMapper,
            InsuranceStudyMemberMapper memberMapper,
            InsuranceStudyJobMapper jobMapper,
            InsuranceStudyResultMapper resultMapper,
            InsuranceRweReportMapper reportMapper,
            InsuranceSettlementPackageMapper settlementMapper,
            InsuranceSettlementApprovalMapper approvalMapper,
            InsuranceAuditEventMapper auditMapper,
            ObjectMapper objectMapper
    ) {
        this.studyMapper = studyMapper;
        this.snapshotMapper = snapshotMapper;
        this.memberMapper = memberMapper;
        this.jobMapper = jobMapper;
        this.resultMapper = resultMapper;
        this.reportMapper = reportMapper;
        this.settlementMapper = settlementMapper;
        this.approvalMapper = approvalMapper;
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InsuranceStudyResponse.Study createStudy(
            int tenantId, String actorUserId, InsuranceStudyRequest.CreateStudy request
    ) {
        if (request.periodStart() == null || request.periodEnd() == null
                || request.periodEnd().isBefore(request.periodStart())) {
            throw InsuranceApiException.badRequest("periodStart and periodEnd must define a valid period");
        }
        String studyNo = required(request.studyNo(), "studyNo", 128);
        if (studyMapper.selectCount(new LambdaQueryWrapper<InsuranceStudyEntity>()
                .eq(InsuranceStudyEntity::getTenantId, tenantId)
                .eq(InsuranceStudyEntity::getStudyNo, studyNo)) > 0) {
            throw InsuranceApiException.conflict("studyNo already exists in the tenant");
        }
        LocalDateTime now = LocalDateTime.now();
        InsuranceStudyEntity entity = new InsuranceStudyEntity();
        entity.setId(uuid());
        entity.setTenantId(tenantId);
        entity.setStudyNo(studyNo);
        entity.setTitle(required(request.title(), "title", 255));
        entity.setPeriodStart(request.periodStart());
        entity.setPeriodEnd(request.periodEnd());
        entity.setPopulationRuleJson(json(request.populationRule()));
        entity.setInterventionRuleJson(json(request.interventionRule()));
        entity.setOutcomeRuleJson(json(request.outcomeRule()));
        entity.setMethodology("psm");
        entity.setStatus("draft");
        entity.setModelVersion(optional(request.modelVersion(), "psm-fastapi-v1", 128));
        entity.setCreatedBy(actorUserId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        studyMapper.insert(entity);
        audit(tenantId, actorUserId, "study.create", "study", entity.getId(), null, null, hash(entity), Map.of());
        return study(entity);
    }

    public List<InsuranceStudyResponse.Study> studies(int tenantId) {
        return studyMapper.selectList(new LambdaQueryWrapper<InsuranceStudyEntity>()
                        .eq(InsuranceStudyEntity::getTenantId, tenantId)
                        .orderByDesc(InsuranceStudyEntity::getUpdatedAt))
                .stream().map(this::study).toList();
    }

    @Transactional
    public InsuranceStudyResponse.Snapshot freezeSnapshot(int tenantId, String actorUserId, String studyId) {
        InsuranceStudyEntity study = requireStudy(tenantId, studyId);
        if (!Set.of("draft", "ready", "returned").contains(study.getStatus())) {
            throw InsuranceApiException.conflict("study cannot create a snapshot in its current status");
        }
        List<InsuranceStudyMemberMapper.SnapshotCandidate> candidates = memberMapper.selectSnapshotCandidates(
                tenantId, study.getPeriodStart(), study.getPeriodEnd());
        if (candidates.isEmpty()) {
            throw InsuranceApiException.conflict("no consented subjects with active policies are available");
        }
        long treated = candidates.stream().filter(candidate -> "treated".equals(candidate.cohortGroup())).count();
        long control = candidates.size() - treated;
        if (treated < 2 || control < 2) {
            throw InsuranceApiException.conflict("snapshot requires at least two treated and two control subjects");
        }
        Integer currentVersion = snapshotMapper.selectList(new LambdaQueryWrapper<InsuranceStudySnapshotEntity>()
                        .eq(InsuranceStudySnapshotEntity::getTenantId, tenantId)
                        .eq(InsuranceStudySnapshotEntity::getStudyId, studyId))
                .stream().map(InsuranceStudySnapshotEntity::getSnapshotVersion).max(Integer::compareTo).orElse(0);
        String source = json(candidates);
        String snapshotHash = sha256(source);
        LocalDateTime now = LocalDateTime.now();
        InsuranceStudySnapshotEntity snapshot = new InsuranceStudySnapshotEntity();
        snapshot.setId(uuid());
        snapshot.setTenantId(tenantId);
        snapshot.setStudyId(studyId);
        snapshot.setSnapshotVersion(currentVersion + 1);
        snapshot.setSnapshotHash(snapshotHash);
        snapshot.setSourceWatermark(now.toString());
        snapshot.setCohortTotal(candidates.size());
        snapshot.setTreatedTotal(Math.toIntExact(treated));
        snapshot.setControlTotal(Math.toIntExact(control));
        snapshot.setSourceSummaryJson(json(Map.of(
                "source", "software_db",
                "policyAndClaimTables", true,
                "rawHealthDataIncluded", false,
                "periodStart", study.getPeriodStart().toString(),
                "periodEnd", study.getPeriodEnd().toString())));
        snapshot.setImmutable(true);
        snapshot.setCreatedBy(actorUserId);
        snapshot.setCreatedAt(now);
        snapshotMapper.insert(snapshot);
        List<InsuranceStudyResponse.Member> members = new ArrayList<>();
        for (InsuranceStudyMemberMapper.SnapshotCandidate candidate : candidates) {
            InsuranceStudyMemberEntity member = new InsuranceStudyMemberEntity();
            member.setId(uuid());
            member.setTenantId(tenantId);
            member.setSnapshotId(snapshot.getId());
            member.setSubjectRef(candidate.subjectRef());
            member.setCohortGroup(candidate.cohortGroup());
            member.setBaselineRisk(candidate.baselineRisk());
            member.setOutcomeValue(candidate.outcomeValue());
            member.setInterventionStatus(candidate.interventionStatus());
            member.setCovariateJson(candidate.covariateJson());
            member.setSourceRowHash(sha256(json(candidate)));
            member.setCreatedAt(now);
            memberMapper.insert(member);
            members.add(member(member));
        }
        study.setStatus("ready");
        study.setUpdatedAt(now);
        studyMapper.updateById(study);
        audit(tenantId, actorUserId, "snapshot.freeze", "study_snapshot", snapshot.getId(), null,
                null, snapshotHash, Map.of("studyId", studyId, "version", snapshot.getSnapshotVersion()));
        return snapshot(snapshot, members);
    }

    public InsuranceStudyResponse.Snapshot snapshot(int tenantId, String snapshotId) {
        InsuranceStudySnapshotEntity snapshot = requireSnapshot(tenantId, snapshotId);
        List<InsuranceStudyResponse.Member> members = memberMapper.selectList(
                        new LambdaQueryWrapper<InsuranceStudyMemberEntity>()
                                .eq(InsuranceStudyMemberEntity::getTenantId, tenantId)
                                .eq(InsuranceStudyMemberEntity::getSnapshotId, snapshotId)
                                .orderByAsc(InsuranceStudyMemberEntity::getSubjectRef))
                .stream().map(this::member).toList();
        return snapshot(snapshot, members);
    }

    @Transactional
    public InsuranceStudyResponse.Job queueJob(
            int tenantId, String actorUserId, String studyId, InsuranceStudyRequest.QueueJob request
    ) {
        InsuranceStudyEntity study = requireStudy(tenantId, studyId);
        InsuranceStudySnapshotEntity snapshot = requireSnapshot(tenantId, required(request.snapshotId(), "snapshotId", 64));
        if (!studyId.equals(snapshot.getStudyId())) {
            throw InsuranceApiException.badRequest("snapshot does not belong to the study");
        }
        String requestId = required(request.requestId(), "requestId", 128);
        InsuranceStudyJobEntity existing = jobMapper.selectOne(new LambdaQueryWrapper<InsuranceStudyJobEntity>()
                .eq(InsuranceStudyJobEntity::getTenantId, tenantId)
                .eq(InsuranceStudyJobEntity::getStudyId, studyId)
                .eq(InsuranceStudyJobEntity::getRequestId, requestId)
                .last("LIMIT 1"));
        if (existing != null) {
            if (!existing.getSnapshotId().equals(snapshot.getId())) {
                throw InsuranceApiException.conflict("requestId was already used with another snapshot");
            }
            return job(existing);
        }
        LocalDateTime now = LocalDateTime.now();
        InsuranceStudyJobEntity entity = new InsuranceStudyJobEntity();
        entity.setId(uuid());
        entity.setTenantId(tenantId);
        entity.setStudyId(studyId);
        entity.setSnapshotId(snapshot.getId());
        entity.setJobType("psm");
        entity.setStatus("queued");
        entity.setRequestId(requestId);
        entity.setAttempt(0);
        entity.setCreatedBy(actorUserId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        jobMapper.insert(entity);
        study.setStatus("queued");
        study.setUpdatedAt(now);
        studyMapper.updateById(study);
        audit(tenantId, actorUserId, "psm.queue", "study_job", entity.getId(), requestId,
                null, hash(entity), Map.of("studyId", studyId, "snapshotId", snapshot.getId()));
        return job(entity);
    }

    public InsuranceStudyResponse.Job job(int tenantId, String jobId) {
        return job(requireJob(tenantId, jobId));
    }

    @Transactional
    public InsuranceStudyResponse.Result completeJob(
            int tenantId, String actorUserId, String jobId, InsuranceStudyRequest.CompleteJob request
    ) {
        InsuranceStudyJobEntity job = requireJob(tenantId, jobId);
        if (job.getResultId() != null && !job.getResultId().isBlank()) {
            return result(requireResult(tenantId, job.getResultId()));
        }
        if (!Set.of("queued", "running").contains(job.getStatus())) {
            throw InsuranceApiException.conflict("job cannot be completed in its current status");
        }
        String completionStatus = optional(request.status(), "calculated", 32).toLowerCase(Locale.ROOT);
        if (!Set.of("calculated", "failed").contains(completionStatus)) {
            throw InsuranceApiException.badRequest("status must be calculated or failed");
        }
        LocalDateTime now = LocalDateTime.now();
        if ("failed".equals(completionStatus)) {
            job.setStatus("failed");
            job.setErrorMessage("PSM worker reported a failed calculation");
            job.setAttempt(job.getAttempt() + 1);
            job.setStartedAt(job.getStartedAt() == null ? now : job.getStartedAt());
            job.setFinishedAt(now);
            job.setUpdatedAt(now);
            jobMapper.updateById(job);
            InsuranceStudyEntity study = requireStudy(tenantId, job.getStudyId());
            study.setStatus("returned");
            study.setUpdatedAt(now);
            studyMapper.updateById(study);
            audit(tenantId, actorUserId, "psm.failed", "study_job", jobId, job.getRequestId(),
                    null, hash(job), Map.of("safeError", "calculation_failed"));
            return new InsuranceStudyResponse.Result(null, job.getStudyId(), job.getSnapshotId(), 0,
                    "failed", null, null, null, null, Map.of(), Map.of(),
                    optional(request.modelVersion(), "psm-fastapi-v1", 128),
                    request.result() == null ? Map.of("safeError", "calculation_failed") : request.result(), now);
        }
        if (request.result() == null || request.attEstimate() == null || request.matchedPairs() == null
                || request.matchedPairs() < 1) {
            throw InsuranceApiException.badRequest("calculated jobs require result, attEstimate and matchedPairs");
        }
        int version = resultMapper.selectList(new LambdaQueryWrapper<InsuranceStudyResultEntity>()
                        .eq(InsuranceStudyResultEntity::getTenantId, tenantId)
                        .eq(InsuranceStudyResultEntity::getStudyId, job.getStudyId()))
                .stream().map(InsuranceStudyResultEntity::getResultVersion).max(Integer::compareTo).orElse(0) + 1;
        InsuranceStudyResultEntity entity = new InsuranceStudyResultEntity();
        entity.setId(uuid());
        entity.setTenantId(tenantId);
        entity.setStudyId(job.getStudyId());
        entity.setSnapshotId(job.getSnapshotId());
        entity.setResultVersion(version);
        entity.setStatus("calculated");
        entity.setAttEstimate(request.attEstimate());
        entity.setCiLower(request.ciLower());
        entity.setCiUpper(request.ciUpper());
        entity.setMatchedPairs(request.matchedPairs());
        entity.setBalanceJson(json(request.balance()));
        entity.setCostBasisJson(json(request.costBasis()));
        entity.setModelVersion(optional(request.modelVersion(), "psm-fastapi-v1", 128));
        entity.setResultJson(json(request.result()));
        entity.setCreatedBy(actorUserId);
        entity.setCreatedAt(now);
        resultMapper.insert(entity);
        job.setStatus("completed");
        job.setAttempt(job.getAttempt() + 1);
        job.setStartedAt(job.getStartedAt() == null ? now : job.getStartedAt());
        job.setFinishedAt(now);
        job.setResultId(entity.getId());
        job.setUpdatedAt(now);
        jobMapper.updateById(job);
        InsuranceStudyEntity study = requireStudy(tenantId, job.getStudyId());
        study.setStatus("calculated");
        study.setUpdatedAt(now);
        studyMapper.updateById(study);
        audit(tenantId, actorUserId, "psm.complete", "study_result", entity.getId(), job.getRequestId(),
                null, hash(entity), Map.of("jobId", jobId, "snapshotId", job.getSnapshotId()));
        return result(entity);
    }

    @Transactional
    public InsuranceStudyResponse.Result reviewResult(
            int tenantId, String actorUserId, String resultId, InsuranceStudyRequest.Review request
    ) {
        InsuranceStudyResultEntity entity = requireResult(tenantId, resultId);
        String action = action(request.action(), Set.of("approve", "return"));
        String before = hash(entity);
        entity.setStatus("approve".equals(action) ? "approved" : "returned");
        resultMapper.updateById(entity);
        InsuranceStudyEntity study = requireStudy(tenantId, entity.getStudyId());
        study.setStatus(entity.getStatus());
        study.setApprovedBy("approved".equals(entity.getStatus()) ? actorUserId : null);
        study.setApprovedAt("approved".equals(entity.getStatus()) ? LocalDateTime.now() : null);
        study.setUpdatedAt(LocalDateTime.now());
        studyMapper.updateById(study);
        audit(tenantId, actorUserId, "psm." + action, "study_result", resultId,
                required(request.requestId(), "requestId", 128), before, hash(entity),
                Map.of("comment", optional(request.comment(), "", 2000)));
        return result(entity);
    }

    @Transactional
    public InsuranceStudyResponse.Report createReport(
            int tenantId, String actorUserId, String studyId, InsuranceStudyRequest.CreateReport request
    ) {
        InsuranceStudyEntity study = requireStudy(tenantId, studyId);
        InsuranceStudyResultEntity approved = latestApprovedResult(tenantId, studyId);
        InsuranceStudySnapshotEntity snapshot = requireSnapshot(tenantId, approved.getSnapshotId());
        int version = reportMapper.selectList(new LambdaQueryWrapper<InsuranceRweReportEntity>()
                        .eq(InsuranceRweReportEntity::getTenantId, tenantId)
                        .eq(InsuranceRweReportEntity::getStudyId, studyId))
                .stream().map(InsuranceRweReportEntity::getReportVersion).max(Integer::compareTo).orElse(0) + 1;
        Map<String, Object> reportData = new LinkedHashMap<>();
        reportData.put("template", "docs/ReHealth_PSM_RWE_Report_Draft_V0.1.docx");
        reportData.put("templateVersion", "V0.1");
        reportData.put("regulatoryStatus", "internal_draft_not_formal_settlement");
        reportData.put("studyId", studyId);
        reportData.put("studyNo", study.getStudyNo());
        reportData.put("snapshotId", snapshot.getId());
        reportData.put("snapshotHash", snapshot.getSnapshotHash());
        reportData.put("resultId", approved.getId());
        reportData.put("result", map(approved.getResultJson()));
        reportData.put("content", request.report() == null ? Map.of() : request.report());
        String reportJson = json(reportData);
        LocalDateTime now = LocalDateTime.now();
        InsuranceRweReportEntity entity = new InsuranceRweReportEntity();
        entity.setId(uuid());
        entity.setTenantId(tenantId);
        entity.setReportNo(required(request.reportNo(), "reportNo", 128));
        entity.setStudyId(studyId);
        entity.setReportType("rwe");
        entity.setReportVersion(version);
        entity.setTitle(required(request.title(), "title", 255));
        entity.setPeriodStart(study.getPeriodStart());
        entity.setPeriodEnd(study.getPeriodEnd());
        entity.setStatus("draft");
        entity.setEvidenceHash(sha256(snapshot.getSnapshotHash() + hash(approved) + reportJson));
        entity.setReportJson(reportJson);
        entity.setCreatedBy(actorUserId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        reportMapper.insert(entity);
        audit(tenantId, actorUserId, "rwe.create", "rwe_report", entity.getId(), null,
                null, entity.getEvidenceHash(), Map.of("studyId", studyId, "version", version));
        return report(entity);
    }

    public List<InsuranceStudyResponse.Report> reports(int tenantId) {
        return reportMapper.selectList(new LambdaQueryWrapper<InsuranceRweReportEntity>()
                        .eq(InsuranceRweReportEntity::getTenantId, tenantId)
                        .orderByDesc(InsuranceRweReportEntity::getUpdatedAt))
                .stream().map(this::report).toList();
    }

    @Transactional
    public InsuranceStudyResponse.Report reviewReport(
            int tenantId, String actorUserId, String reportId, InsuranceStudyRequest.Review request
    ) {
        InsuranceRweReportEntity entity = requireReport(tenantId, reportId);
        String action = action(request.action(), Set.of("submit", "approve", "return"));
        String next = switch (action) {
            case "submit" -> {
                if (!Set.of("draft", "returned").contains(entity.getStatus())) {
                    throw InsuranceApiException.conflict("only draft or returned reports can be submitted");
                }
                yield "submitted";
            }
            case "approve" -> {
                if (!"submitted".equals(entity.getStatus())) {
                    throw InsuranceApiException.conflict("only submitted reports can be approved");
                }
                yield "approved";
            }
            default -> "returned";
        };
        String before = hash(entity);
        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(next);
        entity.setSubmittedAt("submitted".equals(next) ? now : entity.getSubmittedAt());
        entity.setApprovedBy("approved".equals(next) ? actorUserId : null);
        entity.setApprovedAt("approved".equals(next) ? now : null);
        entity.setUpdatedAt(now);
        reportMapper.updateById(entity);
        audit(tenantId, actorUserId, "rwe." + action, "rwe_report", reportId,
                required(request.requestId(), "requestId", 128), before, hash(entity),
                Map.of("comment", optional(request.comment(), "", 2000)));
        return report(entity);
    }

    @Transactional
    public InsuranceStudyResponse.Settlement createSettlement(
            int tenantId, String actorUserId, String studyId, InsuranceStudyRequest.CreateSettlement request
    ) {
        InsuranceStudyEntity study = requireStudy(tenantId, studyId);
        InsuranceRweReportEntity report = requireReport(tenantId, required(request.reportId(), "reportId", 64));
        if (!studyId.equals(report.getStudyId()) || !"approved".equals(report.getStatus())) {
            throw InsuranceApiException.conflict("an approved RWE report from the study is required");
        }
        InsuranceStudyResultEntity result = latestApprovedResult(tenantId, studyId);
        InsuranceStudySnapshotEntity snapshot = requireSnapshot(tenantId, result.getSnapshotId());
        int version = settlementMapper.selectList(new LambdaQueryWrapper<InsuranceSettlementPackageEntity>()
                        .eq(InsuranceSettlementPackageEntity::getTenantId, tenantId)
                        .eq(InsuranceSettlementPackageEntity::getStudyId, studyId))
                .stream().map(InsuranceSettlementPackageEntity::getPackageVersion).max(Integer::compareTo).orElse(0) + 1;
        Map<String, Object> manifest = request.evidenceManifest() == null
                ? Map.of("snapshotId", snapshot.getId(), "resultId", result.getId(), "reportId", report.getId())
                : request.evidenceManifest();
        Map<String, Object> packageData = request.packageData() == null ? Map.of() : request.packageData();
        BigDecimal estimatedSavings = nonNegative(request.estimatedSavings(), "estimatedSavings");
        String immutableContent = json(Map.of(
                "studyId", studyId,
                "snapshotHash", snapshot.getSnapshotHash(),
                "reportEvidenceHash", report.getEvidenceHash(),
                "estimatedSavings", estimatedSavings,
                "manifest", manifest,
                "package", packageData));
        LocalDateTime now = LocalDateTime.now();
        InsuranceSettlementPackageEntity entity = new InsuranceSettlementPackageEntity();
        entity.setId(uuid());
        entity.setTenantId(tenantId);
        entity.setPackageNo(required(request.packageNo(), "packageNo", 128));
        entity.setStudyId(studyId);
        entity.setReportId(report.getId());
        entity.setPackageVersion(version);
        entity.setStatus("draft");
        entity.setCurrency(optional(request.currency(), "CNY", 3).toUpperCase(Locale.ROOT));
        entity.setEstimatedSavings(estimatedSavings);
        entity.setSnapshotHash(snapshot.getSnapshotHash());
        entity.setEvidenceManifestJson(json(manifest));
        entity.setPackageJson(json(packageData));
        entity.setContentHash(sha256(immutableContent));
        entity.setCreatedBy(actorUserId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        settlementMapper.insert(entity);
        audit(tenantId, actorUserId, "settlement.create", "settlement_package", entity.getId(), null,
                null, entity.getContentHash(), Map.of("studyId", studyId, "version", version));
        return settlement(entity);
    }

    public List<InsuranceStudyResponse.Settlement> settlements(int tenantId) {
        return settlementMapper.selectList(new LambdaQueryWrapper<InsuranceSettlementPackageEntity>()
                        .eq(InsuranceSettlementPackageEntity::getTenantId, tenantId)
                        .orderByDesc(InsuranceSettlementPackageEntity::getUpdatedAt))
                .stream().map(this::settlement).toList();
    }

    @Transactional
    public InsuranceStudyResponse.Settlement settlementAction(
            int tenantId, String actorUserId, String packageId, InsuranceStudyRequest.SettlementAction request
    ) {
        InsuranceSettlementPackageEntity entity = requireSettlement(tenantId, packageId);
        String action = action(request.action(), Set.of("submit", "approve", "return", "recalculate"));
        String requestId = required(request.requestId(), "requestId", 128);
        InsuranceSettlementApprovalEntity existing = approvalMapper.selectOne(
                new LambdaQueryWrapper<InsuranceSettlementApprovalEntity>()
                        .eq(InsuranceSettlementApprovalEntity::getTenantId, tenantId)
                        .eq(InsuranceSettlementApprovalEntity::getPackageId, packageId)
                        .eq(InsuranceSettlementApprovalEntity::getRequestId, requestId)
                        .last("LIMIT 1"));
        if (existing != null) {
            return settlement(entity);
        }
        if ("recalculate".equals(action)) {
            return recalculate(tenantId, actorUserId, entity, request, requestId);
        }
        String next = switch (action) {
            case "submit" -> {
                if (!Set.of("draft", "returned").contains(entity.getStatus())) {
                    throw InsuranceApiException.conflict("only draft or returned packages can be submitted");
                }
                yield "submitted";
            }
            case "approve" -> {
                if (!"submitted".equals(entity.getStatus())) {
                    throw InsuranceApiException.conflict("only submitted packages can be approved");
                }
                yield "approved";
            }
            default -> "returned";
        };
        String before = hash(entity);
        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(next);
        entity.setApprovedAmount("approved".equals(next)
                ? nonNegative(request.approvedAmount(), "approvedAmount") : entity.getApprovedAmount());
        entity.setApprovedBy("approved".equals(next) ? actorUserId : null);
        entity.setApprovedAt("approved".equals(next) ? now : null);
        entity.setUpdatedAt(now);
        settlementMapper.updateById(entity);
        saveApproval(tenantId, packageId, action, request.comment(), actorUserId, requestId);
        audit(tenantId, actorUserId, "settlement." + action, "settlement_package", packageId,
                requestId, before, hash(entity), Map.of("comment", optional(request.comment(), "", 2000)));
        return settlement(entity);
    }

    private InsuranceStudyResponse.Settlement recalculate(
            int tenantId,
            String actorUserId,
            InsuranceSettlementPackageEntity source,
            InsuranceStudyRequest.SettlementAction request,
            String requestId
    ) {
        if ("approved".equals(source.getStatus())) {
            throw InsuranceApiException.conflict("approved settlement packages cannot be recalculated");
        }
        int nextVersion = settlementMapper.selectList(new LambdaQueryWrapper<InsuranceSettlementPackageEntity>()
                        .eq(InsuranceSettlementPackageEntity::getTenantId, tenantId)
                        .eq(InsuranceSettlementPackageEntity::getStudyId, source.getStudyId()))
                .stream().map(InsuranceSettlementPackageEntity::getPackageVersion).max(Integer::compareTo).orElse(0) + 1;
        LocalDateTime now = LocalDateTime.now();
        InsuranceSettlementPackageEntity next = new InsuranceSettlementPackageEntity();
        next.setId(uuid());
        next.setTenantId(tenantId);
        next.setPackageNo(source.getPackageNo() + "-R" + nextVersion);
        next.setStudyId(source.getStudyId());
        next.setReportId(source.getReportId());
        next.setPackageVersion(nextVersion);
        next.setStatus("draft");
        next.setCurrency(source.getCurrency());
        next.setEstimatedSavings(request.approvedAmount() == null
                ? source.getEstimatedSavings() : nonNegative(request.approvedAmount(), "approvedAmount"));
        next.setSnapshotHash(source.getSnapshotHash());
        next.setEvidenceManifestJson(source.getEvidenceManifestJson());
        next.setPackageJson(source.getPackageJson());
        next.setContentHash(sha256(source.getContentHash() + nextVersion + next.getEstimatedSavings()));
        next.setCreatedBy(actorUserId);
        next.setCreatedAt(now);
        next.setUpdatedAt(now);
        settlementMapper.insert(next);
        source.setStatus("superseded");
        source.setUpdatedAt(now);
        settlementMapper.updateById(source);
        saveApproval(tenantId, source.getId(), "recalculate", request.comment(), actorUserId, requestId);
        audit(tenantId, actorUserId, "settlement.recalculate", "settlement_package", next.getId(),
                requestId, source.getContentHash(), next.getContentHash(), Map.of("supersedes", source.getId()));
        return settlement(next);
    }

    private void saveApproval(
            int tenantId, String packageId, String action, String comment, String actorUserId, String requestId
    ) {
        InsuranceSettlementApprovalEntity approval = new InsuranceSettlementApprovalEntity();
        approval.setId(uuid());
        approval.setTenantId(tenantId);
        approval.setPackageId(packageId);
        approval.setAction(action);
        approval.setComment(optional(comment, "", 2000));
        approval.setActorUserId(actorUserId);
        approval.setRequestId(requestId);
        approval.setCreatedAt(LocalDateTime.now());
        approvalMapper.insert(approval);
    }

    private InsuranceStudyResultEntity latestApprovedResult(int tenantId, String studyId) {
        InsuranceStudyResultEntity entity = resultMapper.selectOne(new LambdaQueryWrapper<InsuranceStudyResultEntity>()
                .eq(InsuranceStudyResultEntity::getTenantId, tenantId)
                .eq(InsuranceStudyResultEntity::getStudyId, studyId)
                .eq(InsuranceStudyResultEntity::getStatus, "approved")
                .orderByDesc(InsuranceStudyResultEntity::getResultVersion)
                .last("LIMIT 1"));
        if (entity == null) {
            throw InsuranceApiException.conflict("an approved PSM result is required");
        }
        return entity;
    }

    private InsuranceStudyEntity requireStudy(int tenantId, String id) {
        InsuranceStudyEntity entity = studyMapper.selectById(required(id, "studyId", 64));
        if (entity == null || !Integer.valueOf(tenantId).equals(entity.getTenantId())) {
            throw InsuranceApiException.notFound("study was not found in the tenant");
        }
        return entity;
    }

    private InsuranceStudySnapshotEntity requireSnapshot(int tenantId, String id) {
        InsuranceStudySnapshotEntity entity = snapshotMapper.selectById(required(id, "snapshotId", 64));
        if (entity == null || !Integer.valueOf(tenantId).equals(entity.getTenantId())) {
            throw InsuranceApiException.notFound("snapshot was not found in the tenant");
        }
        return entity;
    }

    private InsuranceStudyJobEntity requireJob(int tenantId, String id) {
        InsuranceStudyJobEntity entity = jobMapper.selectById(required(id, "jobId", 64));
        if (entity == null || !Integer.valueOf(tenantId).equals(entity.getTenantId())) {
            throw InsuranceApiException.notFound("job was not found in the tenant");
        }
        return entity;
    }

    private InsuranceStudyResultEntity requireResult(int tenantId, String id) {
        InsuranceStudyResultEntity entity = resultMapper.selectById(required(id, "resultId", 64));
        if (entity == null || !Integer.valueOf(tenantId).equals(entity.getTenantId())) {
            throw InsuranceApiException.notFound("result was not found in the tenant");
        }
        return entity;
    }

    private InsuranceRweReportEntity requireReport(int tenantId, String id) {
        InsuranceRweReportEntity entity = reportMapper.selectById(required(id, "reportId", 64));
        if (entity == null || !Integer.valueOf(tenantId).equals(entity.getTenantId())) {
            throw InsuranceApiException.notFound("report was not found in the tenant");
        }
        return entity;
    }

    private InsuranceSettlementPackageEntity requireSettlement(int tenantId, String id) {
        InsuranceSettlementPackageEntity entity = settlementMapper.selectById(required(id, "packageId", 64));
        if (entity == null || !Integer.valueOf(tenantId).equals(entity.getTenantId())) {
            throw InsuranceApiException.notFound("settlement package was not found in the tenant");
        }
        return entity;
    }

    private InsuranceStudyResponse.Study study(InsuranceStudyEntity entity) {
        InsuranceStudySnapshotEntity snapshot = snapshotMapper.selectOne(
                new LambdaQueryWrapper<InsuranceStudySnapshotEntity>()
                        .eq(InsuranceStudySnapshotEntity::getTenantId, entity.getTenantId())
                        .eq(InsuranceStudySnapshotEntity::getStudyId, entity.getId())
                        .orderByDesc(InsuranceStudySnapshotEntity::getSnapshotVersion)
                        .last("LIMIT 1"));
        InsuranceStudyJobEntity job = jobMapper.selectOne(new LambdaQueryWrapper<InsuranceStudyJobEntity>()
                .eq(InsuranceStudyJobEntity::getTenantId, entity.getTenantId())
                .eq(InsuranceStudyJobEntity::getStudyId, entity.getId())
                .orderByDesc(InsuranceStudyJobEntity::getCreatedAt)
                .last("LIMIT 1"));
        InsuranceStudyResultEntity result = resultMapper.selectOne(
                new LambdaQueryWrapper<InsuranceStudyResultEntity>()
                        .eq(InsuranceStudyResultEntity::getTenantId, entity.getTenantId())
                        .eq(InsuranceStudyResultEntity::getStudyId, entity.getId())
                        .orderByDesc(InsuranceStudyResultEntity::getResultVersion)
                        .last("LIMIT 1"));
        return new InsuranceStudyResponse.Study(entity.getId(), entity.getStudyNo(), entity.getTitle(),
                entity.getPeriodStart(), entity.getPeriodEnd(), entity.getMethodology(), entity.getStatus(),
                entity.getModelVersion(), entity.getCreatedAt(), entity.getUpdatedAt(),
                snapshot == null ? null : snapshot.getId(), job == null ? null : job.getId(),
                job == null ? null : job.getStatus(), result == null ? null : result.getId(),
                result == null ? null : result.getStatus());
    }

    private InsuranceStudyResponse.Snapshot snapshot(
            InsuranceStudySnapshotEntity entity, List<InsuranceStudyResponse.Member> members
    ) {
        return new InsuranceStudyResponse.Snapshot(entity.getId(), entity.getStudyId(), entity.getSnapshotVersion(),
                entity.getSnapshotHash(), entity.getSourceWatermark(), entity.getCohortTotal(),
                entity.getTreatedTotal(), entity.getControlTotal(), Boolean.TRUE.equals(entity.getImmutable()),
                entity.getCreatedAt(), members);
    }

    private InsuranceStudyResponse.Member member(InsuranceStudyMemberEntity entity) {
        return new InsuranceStudyResponse.Member(entity.getSubjectRef(), entity.getCohortGroup(),
                entity.getBaselineRisk(), entity.getOutcomeValue(), entity.getInterventionStatus(),
                map(entity.getCovariateJson()));
    }

    private InsuranceStudyResponse.Job job(InsuranceStudyJobEntity entity) {
        return new InsuranceStudyResponse.Job(entity.getId(), entity.getStudyId(), entity.getSnapshotId(),
                entity.getStatus(), entity.getRequestId(), entity.getAttempt(), entity.getErrorMessage(),
                entity.getResultId(), entity.getCreatedAt(), entity.getStartedAt(), entity.getFinishedAt());
    }

    private InsuranceStudyResponse.Result result(InsuranceStudyResultEntity entity) {
        return new InsuranceStudyResponse.Result(entity.getId(), entity.getStudyId(), entity.getSnapshotId(),
                entity.getResultVersion(), entity.getStatus(), entity.getAttEstimate(), entity.getCiLower(),
                entity.getCiUpper(), entity.getMatchedPairs(), map(entity.getBalanceJson()),
                map(entity.getCostBasisJson()), entity.getModelVersion(), map(entity.getResultJson()),
                entity.getCreatedAt());
    }

    private InsuranceStudyResponse.Report report(InsuranceRweReportEntity entity) {
        return new InsuranceStudyResponse.Report(entity.getId(), entity.getReportNo(), entity.getStudyId(),
                entity.getReportVersion(), entity.getTitle(), entity.getStatus(), entity.getEvidenceHash(),
                map(entity.getReportJson()), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private InsuranceStudyResponse.Settlement settlement(InsuranceSettlementPackageEntity entity) {
        return new InsuranceStudyResponse.Settlement(entity.getId(), entity.getPackageNo(), entity.getStudyId(),
                entity.getReportId(), entity.getPackageVersion(), entity.getStatus(), entity.getCurrency(),
                entity.getEstimatedSavings(), entity.getApprovedAmount(), entity.getSnapshotHash(),
                entity.getContentHash(), map(entity.getEvidenceManifestJson()), map(entity.getPackageJson()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private void audit(
            int tenantId,
            String actorUserId,
            String action,
            String resourceType,
            String resourceId,
            String requestId,
            String beforeHash,
            String afterHash,
            Map<String, Object> metadata
    ) {
        InsuranceAuditEventEntity event = new InsuranceAuditEventEntity();
        event.setId(uuid());
        event.setTenantId(tenantId);
        event.setActorUserId(actorUserId);
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setRequestId(requestId);
        event.setBeforeHash(beforeHash);
        event.setAfterHash(afterHash);
        event.setMetadataJson(json(metadata));
        event.setCreatedAt(LocalDateTime.now());
        auditMapper.insert(event);
    }

    private String hash(Object value) {
        return sha256(json(value));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw InsuranceApiException.badRequest("content cannot be represented as JSON");
        }
    }

    private Map<String, Object> map(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of("parseStatus", "invalid_persisted_json");
        }
    }

    private static String action(String value, Set<String> allowed) {
        String normalized = required(value, "action", 32).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw InsuranceApiException.badRequest("unsupported action");
        }
        return normalized;
    }

    private static String required(String value, String field, int maxLength) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw InsuranceApiException.badRequest(field + " is required");
        }
        if (normalized.length() > maxLength) {
            throw InsuranceApiException.badRequest(field + " exceeds maximum length " + maxLength);
        }
        return normalized;
    }

    private static String optional(String value, String fallback, int maxLength) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return required(value, "value", maxLength);
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw InsuranceApiException.badRequest(field + " must not be negative");
        }
        return value;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
