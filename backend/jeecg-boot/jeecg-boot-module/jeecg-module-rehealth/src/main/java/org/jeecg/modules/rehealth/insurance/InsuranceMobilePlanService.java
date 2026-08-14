package org.jeecg.modules.rehealth.insurance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceConsentRecordEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceInterventionFeedbackEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsurancePlanBindingEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsurancePolicyEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceSubjectEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceConsentRecordMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceInterventionFeedbackMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePlanBindingMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePolicyMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceSubjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceMobilePlanService {
    private final InsuranceSubjectMapper subjectMapper;
    private final InsurancePolicyMapper policyMapper;
    private final InsuranceConsentRecordMapper consentMapper;
    private final InsurancePlanBindingMapper bindingMapper;
    private final InsuranceInterventionFeedbackMapper feedbackMapper;
    private final ObjectMapper objectMapper;

    public InsuranceMobilePlanService(
            InsuranceSubjectMapper subjectMapper,
            InsurancePolicyMapper policyMapper,
            InsuranceConsentRecordMapper consentMapper,
            InsurancePlanBindingMapper bindingMapper,
            InsuranceInterventionFeedbackMapper feedbackMapper,
            ObjectMapper objectMapper
    ) {
        this.subjectMapper = subjectMapper;
        this.policyMapper = policyMapper;
        this.consentMapper = consentMapper;
        this.bindingMapper = bindingMapper;
        this.feedbackMapper = feedbackMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InsuranceMobilePlanResponse bind(String userId, InsuranceMobilePlanRequest.Bind request) {
        int tenantId = positiveTenant(request.tenantId());
        requireActiveTenantMember(tenantId, userId);
        InsuranceSubjectEntity subject = subjectMapper.selectOne(new LambdaQueryWrapper<InsuranceSubjectEntity>()
                .eq(InsuranceSubjectEntity::getTenantId, tenantId)
                .eq(InsuranceSubjectEntity::getRehealthUserId, userId)
                .last("LIMIT 1"));
        if (subject == null || !"active".equals(subject.getEnrollmentStatus())) {
            throw InsuranceApiException.forbidden("current user is not enrolled in the requested insurance tenant");
        }
        InsurancePolicyEntity policy = policyMapper.selectOne(new LambdaQueryWrapper<InsurancePolicyEntity>()
                .eq(InsurancePolicyEntity::getTenantId, tenantId)
                .eq(InsurancePolicyEntity::getPolicyNo, required(request.policyNo(), "policyNo", 128))
                .eq(InsurancePolicyEntity::getInsuredSubjectRef, subject.getSubjectRef())
                .eq(InsurancePolicyEntity::getStatus, "active")
                .last("LIMIT 1"));
        if (policy == null) {
            throw InsuranceApiException.notFound("active policy was not found for the current user");
        }
        String consentType = optional(request.consentType(), "insurance_program", 64);
        String consentVersion = required(request.consentVersion(), "consentVersion", 64);
        LocalDateTime now = LocalDateTime.now();
        InsuranceConsentRecordEntity consent = consentMapper.selectOne(
                new LambdaQueryWrapper<InsuranceConsentRecordEntity>()
                        .eq(InsuranceConsentRecordEntity::getTenantId, tenantId)
                        .eq(InsuranceConsentRecordEntity::getSubjectRef, subject.getSubjectRef())
                        .eq(InsuranceConsentRecordEntity::getConsentType, consentType)
                        .eq(InsuranceConsentRecordEntity::getConsentVersion, consentVersion)
                        .last("LIMIT 1"));
        if (consent == null) {
            consent = new InsuranceConsentRecordEntity();
            consent.setId(uuid());
            consent.setTenantId(tenantId);
            consent.setSubjectRef(subject.getSubjectRef());
            consent.setConsentType(consentType);
            consent.setConsentVersion(consentVersion);
            consent.setCreatedAt(now);
        }
        consent.setStatus("granted");
        consent.setGrantedAt(now);
        consent.setRevokedAt(null);
        consent.setEvidenceRef(trim(request.evidenceRef(), 128));
        consent.setEvidenceHash(trim(request.evidenceHash(), 64));
        consent.setSourceSystem("rehealth_app");
        consent.setSourceRecordId(trim(request.sourceRecordId(), 128));
        consent.setMetadataJson(json(request.metadata()));
        consent.setUpdatedAt(now);
        if (consentMapper.selectById(consent.getId()) == null) consentMapper.insert(consent); else consentMapper.updateById(consent);

        InsurancePlanBindingEntity binding = bindingMapper.selectOne(new LambdaQueryWrapper<InsurancePlanBindingEntity>()
                .eq(InsurancePlanBindingEntity::getTenantId, tenantId)
                .eq(InsurancePlanBindingEntity::getSubjectRef, subject.getSubjectRef())
                .eq(InsurancePlanBindingEntity::getPolicyId, policy.getId())
                .eq(InsurancePlanBindingEntity::getPlanId, required(request.planId(), "planId", 128))
                .last("LIMIT 1"));
        if (binding == null) {
            binding = new InsurancePlanBindingEntity();
            binding.setId(uuid());
            binding.setTenantId(tenantId);
            binding.setSubjectRef(subject.getSubjectRef());
            binding.setPolicyId(policy.getId());
            binding.setPlanId(request.planId().trim());
            binding.setCreatedAt(now);
        }
        binding.setConsentId(consent.getId());
        binding.setStatus("active");
        binding.setBoundAt(binding.getBoundAt() == null ? now : binding.getBoundAt());
        binding.setUnboundAt(null);
        binding.setSourceSystem("rehealth_app");
        binding.setSourceRecordId(trim(request.sourceRecordId(), 128));
        binding.setMetadataJson(json(request.metadata()));
        binding.setUpdatedAt(now);
        if (bindingMapper.selectById(binding.getId()) == null) bindingMapper.insert(binding); else bindingMapper.updateById(binding);

        subject.setConsentStatus("granted");
        subject.setConsentVersion(consentVersion);
        subject.setConsentedAt(now);
        subject.setUpdatedAt(now);
        subjectMapper.updateById(subject);
        return response(binding, policy, consent);
    }

    public InsuranceMobilePlanResponse current(String userId, String tenantValue) {
        int tenantId = positiveTenant(tenantValue);
        requireActiveTenantMember(tenantId, userId);
        InsuranceSubjectEntity subject = subjectMapper.selectOne(new LambdaQueryWrapper<InsuranceSubjectEntity>()
                .eq(InsuranceSubjectEntity::getTenantId, tenantId)
                .eq(InsuranceSubjectEntity::getRehealthUserId, userId)
                .last("LIMIT 1"));
        if (subject == null) {
            throw InsuranceApiException.notFound("insurance enrollment was not found");
        }
        InsurancePlanBindingEntity binding = bindingMapper.selectOne(new LambdaQueryWrapper<InsurancePlanBindingEntity>()
                .eq(InsurancePlanBindingEntity::getTenantId, tenantId)
                .eq(InsurancePlanBindingEntity::getSubjectRef, subject.getSubjectRef())
                .eq(InsurancePlanBindingEntity::getStatus, "active")
                .orderByDesc(InsurancePlanBindingEntity::getUpdatedAt)
                .last("LIMIT 1"));
        if (binding == null) {
            throw InsuranceApiException.notFound("active insurance plan binding was not found");
        }
        InsurancePolicyEntity policy = policyMapper.selectOne(new LambdaQueryWrapper<InsurancePolicyEntity>()
                .eq(InsurancePolicyEntity::getTenantId, tenantId)
                .eq(InsurancePolicyEntity::getId, binding.getPolicyId())
                .last("LIMIT 1"));
        InsuranceConsentRecordEntity consent = consentMapper.selectOne(
                new LambdaQueryWrapper<InsuranceConsentRecordEntity>()
                        .eq(InsuranceConsentRecordEntity::getTenantId, tenantId)
                        .eq(InsuranceConsentRecordEntity::getId, binding.getConsentId())
                        .last("LIMIT 1"));
        if (policy == null || consent == null) {
            throw InsuranceApiException.notFound("tenant-scoped policy or consent was not found");
        }
        return response(binding, policy, consent);
    }

    @Transactional
    public Map<String, Object> feedback(
            String userId, String bindingId, InsuranceMobilePlanRequest.Feedback request
    ) {
        InsurancePlanBindingEntity binding = bindingMapper.selectById(required(bindingId, "bindingId", 64));
        if (binding == null || !"active".equals(binding.getStatus())) {
            throw InsuranceApiException.notFound("active insurance plan binding was not found");
        }
        requireActiveTenantMember(binding.getTenantId(), userId);
        InsuranceSubjectEntity subject = subjectMapper.selectOne(new LambdaQueryWrapper<InsuranceSubjectEntity>()
                .eq(InsuranceSubjectEntity::getTenantId, binding.getTenantId())
                .eq(InsuranceSubjectEntity::getSubjectRef, binding.getSubjectRef())
                .eq(InsuranceSubjectEntity::getRehealthUserId, userId)
                .last("LIMIT 1"));
        if (subject == null) {
            throw InsuranceApiException.forbidden("binding does not belong to the current user");
        }
        String sourceRecordId = required(request.sourceRecordId(), "sourceRecordId", 128);
        InsuranceInterventionFeedbackEntity existing = feedbackMapper.selectOne(
                new LambdaQueryWrapper<InsuranceInterventionFeedbackEntity>()
                        .eq(InsuranceInterventionFeedbackEntity::getTenantId, binding.getTenantId())
                        .eq(InsuranceInterventionFeedbackEntity::getSourceSystem, "rehealth_app")
                        .eq(InsuranceInterventionFeedbackEntity::getSourceRecordId, sourceRecordId)
                        .last("LIMIT 1"));
        if (existing != null) {
            return Map.of("feedbackId", existing.getId(), "status", "accepted", "idempotentReplay", true);
        }
        checkScore(request.completionRate(), "completionRate");
        checkScore(request.adherenceScore(), "adherenceScore");
        InsuranceInterventionFeedbackEntity entity = new InsuranceInterventionFeedbackEntity();
        entity.setId(uuid());
        entity.setTenantId(binding.getTenantId());
        entity.setBindingId(binding.getId());
        entity.setSubjectRef(binding.getSubjectRef());
        entity.setInterventionId(trim(request.interventionId(), 64));
        entity.setFeedbackType(required(request.feedbackType(), "feedbackType", 64));
        entity.setOccurredAt(request.occurredAt() == null ? LocalDateTime.now() : request.occurredAt());
        entity.setCompletionRate(request.completionRate());
        entity.setAdherenceScore(request.adherenceScore());
        entity.setOutcomeSummaryJson(json(request.outcomeSummary()));
        entity.setSourceSystem("rehealth_app");
        entity.setSourceRecordId(sourceRecordId);
        entity.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(entity);
        return Map.of("feedbackId", entity.getId(), "status", "accepted", "idempotentReplay", false);
    }

    private InsuranceMobilePlanResponse response(
            InsurancePlanBindingEntity binding, InsurancePolicyEntity policy, InsuranceConsentRecordEntity consent
    ) {
        return new InsuranceMobilePlanResponse(binding.getId(), binding.getSubjectRef(), binding.getPolicyId(),
                policy == null ? null : policy.getPolicyNo(), binding.getPlanId(), binding.getConsentId(),
                consent == null ? null : consent.getConsentVersion(), binding.getStatus(), binding.getBoundAt());
    }

    private void requireActiveTenantMember(int tenantId, String userId) {
        if (userId == null || userId.isBlank() || subjectMapper.countActiveMember(tenantId, userId) < 1) {
            throw InsuranceApiException.forbidden("current user is not an active member of the requested insurance tenant");
        }
    }

    private static int positiveTenant(String value) {
        try {
            int tenant = Integer.parseInt(required(value, "tenantId", 16));
            if (tenant <= 0) throw new NumberFormatException();
            return tenant;
        } catch (NumberFormatException e) {
            throw InsuranceApiException.badRequest("tenantId must be a positive integer");
        }
    }

    private static void checkScore(BigDecimal value, String field) {
        if (value != null && (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0)) {
            throw InsuranceApiException.badRequest(field + " must be between 0 and 1");
        }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value == null ? Map.of() : value); }
        catch (JsonProcessingException e) { throw InsuranceApiException.badRequest("metadata is invalid"); }
    }

    private static String required(String value, String field, int max) {
        String normalized = trim(value, max);
        if (normalized == null) throw InsuranceApiException.badRequest(field + " is required");
        return normalized;
    }

    private static String optional(String value, String fallback, int max) {
        String normalized = trim(value, max);
        return normalized == null ? fallback : normalized;
    }

    private static String trim(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > max) throw InsuranceApiException.badRequest("value exceeds maximum length " + max);
        return normalized;
    }

    private static String uuid() { return UUID.randomUUID().toString().replace("-", ""); }
}
