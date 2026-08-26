package org.jeecg.modules.rehealth.insurance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceClaimEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceImportBatchEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsurancePolicyEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceSubjectEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceClaimMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceImportBatchMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePolicyMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceSubjectMapper;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceImportService {
    private static final int MAX_BATCH_SIZE = 2_000;

    private final InsuranceSubjectMapper subjectMapper;
    private final InsurancePolicyMapper policyMapper;
    private final InsuranceClaimMapper claimMapper;
    private final InsuranceImportBatchMapper importBatchMapper;
    private final ObjectMapper objectMapper;

    public InsuranceImportService(
            InsuranceSubjectMapper subjectMapper,
            InsurancePolicyMapper policyMapper,
            InsuranceClaimMapper claimMapper,
            InsuranceImportBatchMapper importBatchMapper,
            ObjectMapper objectMapper
    ) {
        this.subjectMapper = subjectMapper;
        this.policyMapper = policyMapper;
        this.claimMapper = claimMapper;
        this.importBatchMapper = importBatchMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InsuranceImportResponse.BatchResult importSubjects(
            int tenantId,
            String actorUserId,
            InsuranceImportRequest.SubjectBatch request
    ) {
        BatchContext batch = begin(tenantId, actorUserId, "subject", request.sourceSystem(),
                request.idempotencyKey(), request.records());
        if (batch.replay()) {
            return replay(batch.entity());
        }
        List<InsuranceImportResponse.RecordResult> results = new ArrayList<>();
        int rowNumber = 0;
        for (InsuranceImportRequest.SubjectRow row : request.records()) {
            rowNumber++;
            String userId = required(row.rehealthUserId(), "records[" + rowNumber + "].rehealthUserId", 64);
            if (subjectMapper.countActiveMember(tenantId, userId) < 1) {
                throw InsuranceApiException.badRequest("records[" + rowNumber + "] is not an active member of the tenant");
            }
            String subjectRef = sha256(tenantId + ":" + userId);
            InsuranceSubjectEntity entity = subjectMapper.selectOne(new LambdaQueryWrapper<InsuranceSubjectEntity>()
                    .eq(InsuranceSubjectEntity::getTenantId, tenantId)
                    .eq(InsuranceSubjectEntity::getRehealthUserId, userId)
                    .last("LIMIT 1"));
            LocalDateTime now = LocalDateTime.now();
            boolean created = entity == null;
            if (created) {
                entity = new InsuranceSubjectEntity();
                entity.setId(uuid());
                entity.setTenantId(tenantId);
                entity.setSubjectRef(subjectRef);
                entity.setRehealthUserId(userId);
                entity.setCreatedAt(now);
            }
            if (row.externalSubjectRef() != null && !row.externalSubjectRef().isBlank()) {
                entity.setExternalSubjectRefHash(sha256(row.externalSubjectRef().trim()));
            }
            entity.setEnrollmentStatus(optional(row.enrollmentStatus(), "active", 32));
            entity.setConsentStatus(optional(row.consentStatus(), "pending", 32));
            entity.setConsentVersion(trim(row.consentVersion(), 64));
            entity.setConsentedAt(row.consentedAt());
            entity.setSourceSystem(batch.entity().getSourceSystem());
            entity.setSourceRecordId(trim(row.sourceRecordId(), 128));
            entity.setMetadataJson(json(row.metadata()));
            entity.setUpdatedAt(now);
            if (created) {
                subjectMapper.insert(entity);
            } else {
                subjectMapper.updateById(entity);
            }
            results.add(new InsuranceImportResponse.RecordResult(
                    rowNumber, entity.getId(), userId, subjectRef, created ? "created" : "updated"));
        }
        return complete(batch.entity(), results);
    }

    @Transactional
    public InsuranceImportResponse.BatchResult importPolicies(
            int tenantId,
            String actorUserId,
            InsuranceImportRequest.PolicyBatch request
    ) {
        BatchContext batch = begin(tenantId, actorUserId, "policy", request.sourceSystem(),
                request.idempotencyKey(), request.records());
        if (batch.replay()) {
            return replay(batch.entity());
        }
        List<InsuranceImportResponse.RecordResult> results = new ArrayList<>();
        int rowNumber = 0;
        for (InsuranceImportRequest.PolicyRow row : request.records()) {
            rowNumber++;
            String policyNo = required(row.policyNo(), "records[" + rowNumber + "].policyNo", 128);
            String subjectRef = subjectRef(tenantId, row.insuredSubjectRef(), rowNumber);
            InsurancePolicyEntity entity = policyMapper.selectOne(new LambdaQueryWrapper<InsurancePolicyEntity>()
                    .eq(InsurancePolicyEntity::getTenantId, tenantId)
                    .eq(InsurancePolicyEntity::getPolicyNo, policyNo)
                    .last("LIMIT 1"));
            LocalDateTime now = LocalDateTime.now();
            boolean created = entity == null;
            if (created) {
                entity = new InsurancePolicyEntity();
                entity.setId(uuid());
                entity.setTenantId(tenantId);
                entity.setPolicyNo(policyNo);
                entity.setCreatedAt(now);
            }
            entity.setProductCode(trim(row.productCode(), 64));
            entity.setProductName(trim(row.productName(), 255));
            entity.setPolicyType(required(row.policyType(), "records[" + rowNumber + "].policyType", 64));
            //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】保单导入指定默认健康计划-----------
            entity.setDefaultPlanId(trim(row.defaultPlanId(), 128));
            //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】保单导入指定默认健康计划-----------
            entity.setPolicyholderSubjectRef(row.policyholderSubjectRef() == null || row.policyholderSubjectRef().isBlank()
                    ? null : normalizedHash(row.policyholderSubjectRef(), "policyholderSubjectRef"));
            entity.setInsuredSubjectRef(subjectRef);
            entity.setCoverageAmount(amount(row.coverageAmount(), "coverageAmount"));
            entity.setPremiumAmount(amount(row.premiumAmount(), "premiumAmount"));
            entity.setDeductibleAmount(amount(row.deductibleAmount(), "deductibleAmount"));
            entity.setWaitingPeriodDays(nonNegative(row.waitingPeriodDays(), "waitingPeriodDays"));
            entity.setEffectiveOn(row.effectiveOn());
            entity.setExpiresOn(row.expiresOn());
            if (row.effectiveOn() != null && row.expiresOn() != null && row.expiresOn().isBefore(row.effectiveOn())) {
                throw InsuranceApiException.badRequest("records[" + rowNumber + "].expiresOn must not be before effectiveOn");
            }
            entity.setStatus(optional(row.status(), "active", 32).toLowerCase(Locale.ROOT));
            entity.setSourceSystem(batch.entity().getSourceSystem());
            entity.setSourceRecordId(trim(row.sourceRecordId(), 128));
            entity.setMetadataJson(json(row.metadata()));
            entity.setUpdatedAt(now);
            if (created) {
                policyMapper.insert(entity);
            } else {
                policyMapper.updateById(entity);
            }
            results.add(new InsuranceImportResponse.RecordResult(
                    rowNumber, entity.getId(), policyNo, subjectRef, created ? "created" : "updated"));
        }
        return complete(batch.entity(), results);
    }

    @Transactional
    public InsuranceImportResponse.BatchResult importClaims(
            int tenantId,
            String actorUserId,
            InsuranceImportRequest.ClaimBatch request
    ) {
        BatchContext batch = begin(tenantId, actorUserId, "claim", request.sourceSystem(),
                request.idempotencyKey(), request.records());
        if (batch.replay()) {
            return replay(batch.entity());
        }
        List<InsuranceImportResponse.RecordResult> results = new ArrayList<>();
        int rowNumber = 0;
        for (InsuranceImportRequest.ClaimRow row : request.records()) {
            rowNumber++;
            String claimNo = required(row.claimNo(), "records[" + rowNumber + "].claimNo", 128);
            String policyNo = required(row.policyNo(), "records[" + rowNumber + "].policyNo", 128);
            InsurancePolicyEntity policy = policyMapper.selectOne(new LambdaQueryWrapper<InsurancePolicyEntity>()
                    .eq(InsurancePolicyEntity::getTenantId, tenantId)
                    .eq(InsurancePolicyEntity::getPolicyNo, policyNo)
                    .last("LIMIT 1"));
            if (policy == null) {
                throw InsuranceApiException.badRequest("records[" + rowNumber + "].policyNo does not exist in the tenant");
            }
            String subjectRef = normalizedHash(row.subjectRef(), "records[" + rowNumber + "].subjectRef");
            if (!policy.getInsuredSubjectRef().equals(subjectRef)) {
                throw InsuranceApiException.badRequest("records[" + rowNumber + "] subjectRef does not match the policy insured");
            }
            InsuranceClaimEntity entity = claimMapper.selectOne(new LambdaQueryWrapper<InsuranceClaimEntity>()
                    .eq(InsuranceClaimEntity::getTenantId, tenantId)
                    .eq(InsuranceClaimEntity::getClaimNo, claimNo)
                    .last("LIMIT 1"));
            LocalDateTime now = LocalDateTime.now();
            boolean created = entity == null;
            if (created) {
                entity = new InsuranceClaimEntity();
                entity.setId(uuid());
                entity.setTenantId(tenantId);
                entity.setClaimNo(claimNo);
                entity.setCreatedAt(now);
            }
            entity.setPolicyId(policy.getId());
            entity.setSubjectRef(subjectRef);
            entity.setClaimType(required(row.claimType(), "records[" + rowNumber + "].claimType", 64));
            entity.setEventOn(row.eventOn());
            entity.setSubmittedAt(row.submittedAt());
            entity.setDecidedAt(row.decidedAt());
            entity.setStatus(optional(row.status(), "submitted", 32).toLowerCase(Locale.ROOT));
            entity.setBilledAmount(amount(row.billedAmount(), "billedAmount"));
            entity.setApprovedAmount(amount(row.approvedAmount(), "approvedAmount"));
            entity.setPaidAmount(amount(row.paidAmount(), "paidAmount"));
            entity.setCurrency(optional(row.currency(), "CNY", 3).toUpperCase(Locale.ROOT));
            entity.setCoverageCode(trim(row.coverageCode(), 64));
            entity.setOutcomeCode(trim(row.outcomeCode(), 64));
            entity.setSourceSystem(batch.entity().getSourceSystem());
            entity.setSourceRecordId(trim(row.sourceRecordId(), 128));
            entity.setMetadataJson(json(row.metadata()));
            entity.setUpdatedAt(now);
            if (created) {
                claimMapper.insert(entity);
            } else {
                claimMapper.updateById(entity);
            }
            results.add(new InsuranceImportResponse.RecordResult(
                    rowNumber, entity.getId(), claimNo, subjectRef, created ? "created" : "updated"));
        }
        return complete(batch.entity(), results);
    }

    private BatchContext begin(
            int tenantId,
            String actorUserId,
            String importType,
            String sourceSystem,
            String idempotencyKey,
            List<?> records
    ) {
        sourceSystem = required(sourceSystem, "sourceSystem", 64);
        idempotencyKey = required(idempotencyKey, "idempotencyKey", 128);
        if (records == null || records.isEmpty() || records.size() > MAX_BATCH_SIZE) {
            throw InsuranceApiException.badRequest("records must contain between 1 and " + MAX_BATCH_SIZE + " rows");
        }
        String contentHash = sha256(json(records));
        InsuranceImportBatchEntity existing = importBatchMapper.selectOne(
                new LambdaQueryWrapper<InsuranceImportBatchEntity>()
                        .eq(InsuranceImportBatchEntity::getTenantId, tenantId)
                        .eq(InsuranceImportBatchEntity::getImportType, importType)
                        .eq(InsuranceImportBatchEntity::getIdempotencyKey, idempotencyKey)
                        .last("LIMIT 1"));
        if (existing != null) {
            if (!existing.getContentHash().equals(contentHash)) {
                throw InsuranceApiException.conflict("idempotencyKey was already used with different content");
            }
            return new BatchContext(existing, true);
        }
        InsuranceImportBatchEntity batch = new InsuranceImportBatchEntity();
        batch.setId(uuid());
        batch.setTenantId(tenantId);
        batch.setImportType(importType);
        batch.setSourceSystem(sourceSystem);
        batch.setIdempotencyKey(idempotencyKey);
        batch.setContentHash(contentHash);
        batch.setStatus("processing");
        batch.setTotalCount(records.size());
        batch.setSuccessCount(0);
        batch.setFailureCount(0);
        batch.setCreatedBy(actorUserId);
        batch.setCreatedAt(LocalDateTime.now());
        importBatchMapper.insert(batch);
        return new BatchContext(batch, false);
    }

    private InsuranceImportResponse.BatchResult complete(
            InsuranceImportBatchEntity batch,
            List<InsuranceImportResponse.RecordResult> records
    ) {
        LocalDateTime completedAt = LocalDateTime.now();
        batch.setStatus("completed");
        batch.setSuccessCount(records.size());
        batch.setFailureCount(0);
        batch.setCompletedAt(completedAt);
        importBatchMapper.updateById(batch);
        return new InsuranceImportResponse.BatchResult(
                batch.getId(), batch.getImportType(), batch.getStatus(), batch.getTotalCount(),
                batch.getSuccessCount(), batch.getFailureCount(), false, completedAt, List.copyOf(records));
    }

    private InsuranceImportResponse.BatchResult replay(InsuranceImportBatchEntity batch) {
        return new InsuranceImportResponse.BatchResult(
                batch.getId(), batch.getImportType(), batch.getStatus(), batch.getTotalCount(),
                batch.getSuccessCount(), batch.getFailureCount(), true, batch.getCompletedAt(), List.of());
    }

    private String subjectRef(int tenantId, String value, int rowNumber) {
        String normalized = normalizedHash(value, "records[" + rowNumber + "].insuredSubjectRef");
        Long count = subjectMapper.selectCount(new LambdaQueryWrapper<InsuranceSubjectEntity>()
                .eq(InsuranceSubjectEntity::getTenantId, tenantId)
                .eq(InsuranceSubjectEntity::getSubjectRef, normalized));
        if (count == null || count < 1) {
            throw InsuranceApiException.badRequest("records[" + rowNumber + "].insuredSubjectRef does not exist in the tenant");
        }
        return normalized;
    }

    private String normalizedHash(String value, String field) {
        String normalized = required(value, field, 64).toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw InsuranceApiException.badRequest(field + " must be a 64-character SHA-256 subject reference");
        }
        return normalized;
    }

    private static String required(String value, String field, int maxLength) {
        String normalized = trim(value, maxLength);
        if (normalized == null || normalized.isBlank()) {
            throw InsuranceApiException.badRequest(field + " is required");
        }
        return normalized;
    }

    private static String optional(String value, String fallback, int maxLength) {
        String normalized = trim(value, maxLength);
        return normalized == null || normalized.isBlank() ? fallback : normalized;
    }

    private static String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw InsuranceApiException.badRequest("value exceeds maximum length " + maxLength);
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static BigDecimal amount(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw InsuranceApiException.badRequest(field + " must not be negative");
        }
        return value;
    }

    private static Integer nonNegative(Integer value, String field) {
        if (value != null && value < 0) {
            throw InsuranceApiException.badRequest(field + " must not be negative");
        }
        return value;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw InsuranceApiException.badRequest("request contains unsupported JSON content");
        }
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

    private record BatchContext(InsuranceImportBatchEntity entity, boolean replay) {
    }
}
