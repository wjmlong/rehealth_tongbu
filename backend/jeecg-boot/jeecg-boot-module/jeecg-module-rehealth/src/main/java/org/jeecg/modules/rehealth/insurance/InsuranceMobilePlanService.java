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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceMobilePlanService {
    private static final String ADHERENCE_CALCULATION_VERSION = "insurance-adherence-event-v1";
    private static final Set<String> FEEDBACK_TYPES = Set.of(
            "completed", "partially_completed", "skipped", "not_applicable"
    );
    private static final Set<String> VERIFICATION_TYPES = Set.of(
            "self_report", "device_verified", "staff_confirmed"
    );
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
        //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】零输入绑定：自动选租户/保单/计划-----------
        int tenantId = resolveBindTenant(userId, request.tenantId());
        requireActiveTenant(tenantId);
        InsuranceSubjectEntity subject = subjectMapper.selectOne(new LambdaQueryWrapper<InsuranceSubjectEntity>()
                .eq(InsuranceSubjectEntity::getTenantId, tenantId)
                .eq(InsuranceSubjectEntity::getRehealthUserId, userId)
                .eq(InsuranceSubjectEntity::getEnrollmentStatus, "active")
                .last("LIMIT 1"));
        if (subject == null || !"active".equals(subject.getEnrollmentStatus())) {
            throw InsuranceApiException.forbidden("current user is not enrolled in the requested insurance tenant");
        }
        InsurancePolicyEntity policy = resolveBindPolicy(tenantId, subject.getSubjectRef(), request.policyNo());
        String planId = request.planId() == null || request.planId().isBlank()
                ? policy.getDefaultPlanId()
                : request.planId().trim();
        if (planId == null || planId.isBlank()) {
            throw InsuranceApiException.badRequest("该保单未配置健康计划，请联系保险机构");
        }
        //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】零输入绑定：自动选租户/保单/计划-----------
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
                .eq(InsurancePlanBindingEntity::getPlanId, planId)
                .last("LIMIT 1"));
        if (binding == null) {
            binding = new InsurancePlanBindingEntity();
            binding.setId(uuid());
            binding.setTenantId(tenantId);
            binding.setSubjectRef(subject.getSubjectRef());
            binding.setPolicyId(policy.getId());
            binding.setPlanId(planId);
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

    //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】零输入绑定：自动选租户/保单/计划-----------
    /** Zero-input binding helpers: resolve tenant, policy and plan automatically. */
    private int resolveBindTenant(String userId, String tenantValue) {
        if (tenantValue != null && !tenantValue.isBlank()) {
            return positiveTenant(tenantValue);
        }
        List<InsuranceSubjectEntity> subjects = subjectMapper.selectList(
                new LambdaQueryWrapper<InsuranceSubjectEntity>()
                        .eq(InsuranceSubjectEntity::getRehealthUserId, userId)
                        .eq(InsuranceSubjectEntity::getEnrollmentStatus, "active"));
        List<Integer> tenants = subjects.stream()
                .map(InsuranceSubjectEntity::getTenantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (tenants.isEmpty()) {
            throw InsuranceApiException.forbidden("当前用户没有有效的保险参保关系");
        }
        if (tenants.size() > 1) {
            throw InsuranceApiException.badRequest("当前用户关联多个保险机构，请先选择机构");
        }
        return tenants.get(0);
    }

    private InsurancePolicyEntity resolveBindPolicy(int tenantId, String subjectRef, String policyNo) {
        LambdaQueryWrapper<InsurancePolicyEntity> query = new LambdaQueryWrapper<InsurancePolicyEntity>()
                .eq(InsurancePolicyEntity::getTenantId, tenantId)
                .eq(InsurancePolicyEntity::getInsuredSubjectRef, subjectRef)
                .eq(InsurancePolicyEntity::getStatus, "active");
        if (policyNo != null && !policyNo.isBlank()) {
            query.eq(InsurancePolicyEntity::getPolicyNo, policyNo.trim()).last("LIMIT 1");
            InsurancePolicyEntity policy = policyMapper.selectOne(query);
            if (policy == null) {
                throw InsuranceApiException.notFound("active policy was not found for the current user");
            }
            return policy;
        }
        List<InsurancePolicyEntity> candidates = policyMapper.selectList(
                query.orderByDesc(InsurancePolicyEntity::getEffectiveOn).last("LIMIT 10"));
        if (candidates.isEmpty()) {
            throw InsuranceApiException.notFound("当前用户没有有效保单，请联系保险机构");
        }
        if (candidates.size() > 1) {
            throw InsuranceApiException.badRequest("存在多张有效保单，请先选择保单");
        }
        return candidates.get(0);
    }

    public List<InsuranceMobileBindablePolicy> bindablePolicies(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        List<InsuranceMobileBindablePolicy> result = new ArrayList<>();
        List<InsuranceSubjectEntity> subjects = subjectMapper.selectList(
                new LambdaQueryWrapper<InsuranceSubjectEntity>()
                        .eq(InsuranceSubjectEntity::getRehealthUserId, userId)
                        .eq(InsuranceSubjectEntity::getEnrollmentStatus, "active"));
        for (InsuranceSubjectEntity subject : subjects) {
            if (subject.getTenantId() == null || subjectMapper.countActiveTenant(subject.getTenantId()) < 1) {
                continue;
            }
            List<InsurancePolicyEntity> policies = policyMapper.selectList(
                    new LambdaQueryWrapper<InsurancePolicyEntity>()
                            .eq(InsurancePolicyEntity::getTenantId, subject.getTenantId())
                            .eq(InsurancePolicyEntity::getInsuredSubjectRef, subject.getSubjectRef())
                            .eq(InsurancePolicyEntity::getStatus, "active")
                            .orderByDesc(InsurancePolicyEntity::getEffectiveOn));
            for (InsurancePolicyEntity policy : policies) {
                String policyNo = policy.getPolicyNo() == null ? "" : policy.getPolicyNo();
                String masked = policyNo.length() > 4 ? "尾号 " + policyNo.substring(policyNo.length() - 4) : policyNo;
                result.add(new InsuranceMobileBindablePolicy(
                        subject.getTenantId(),
                        policyNo,
                        masked,
                        policy.getProductName(),
                        policy.getDefaultPlanId(),
                        policy.getDefaultPlanId() != null && !policy.getDefaultPlanId().isBlank()));
            }
        }
        return result;
    }
    //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】零输入绑定：自动选租户/保单/计划-----------

    public InsuranceMobilePlanResponse current(String userId, String tenantValue) {
        int tenantId = positiveTenant(tenantValue);
        requireActiveTenant(tenantId);
        InsuranceSubjectEntity subject = subjectMapper.selectOne(new LambdaQueryWrapper<InsuranceSubjectEntity>()
                .eq(InsuranceSubjectEntity::getTenantId, tenantId)
                .eq(InsuranceSubjectEntity::getRehealthUserId, userId)
                .eq(InsuranceSubjectEntity::getEnrollmentStatus, "active")
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
        if (policy == null || !"active".equals(policy.getStatus())
                || consent == null || !"granted".equals(consent.getStatus())) {
            throw InsuranceApiException.notFound("tenant-scoped policy or consent was not found");
        }
        return response(binding, policy, consent);
    }

    public List<InsuranceMobilePlanResponse> active(String userId) {
        if (userId == null || userId.isBlank()) return List.of();
        List<InsuranceMobilePlanResponse> result = new ArrayList<>();
        List<InsuranceSubjectEntity> subjects = subjectMapper.selectList(
                new LambdaQueryWrapper<InsuranceSubjectEntity>()
                        .eq(InsuranceSubjectEntity::getRehealthUserId, userId)
                        .eq(InsuranceSubjectEntity::getEnrollmentStatus, "active")
        );
        for (InsuranceSubjectEntity subject : subjects) {
            if (subject.getTenantId() == null || subjectMapper.countActiveTenant(subject.getTenantId()) < 1) continue;
            List<InsurancePlanBindingEntity> bindings = bindingMapper.selectList(
                    new LambdaQueryWrapper<InsurancePlanBindingEntity>()
                            .eq(InsurancePlanBindingEntity::getTenantId, subject.getTenantId())
                            .eq(InsurancePlanBindingEntity::getSubjectRef, subject.getSubjectRef())
                            .eq(InsurancePlanBindingEntity::getStatus, "active")
                            .orderByDesc(InsurancePlanBindingEntity::getUpdatedAt)
            );
            for (InsurancePlanBindingEntity binding : bindings) {
                InsurancePolicyEntity policy = policyMapper.selectOne(
                        new LambdaQueryWrapper<InsurancePolicyEntity>()
                                .eq(InsurancePolicyEntity::getTenantId, subject.getTenantId())
                                .eq(InsurancePolicyEntity::getId, binding.getPolicyId())
                                .eq(InsurancePolicyEntity::getStatus, "active")
                                .last("LIMIT 1")
                );
                InsuranceConsentRecordEntity consent = consentMapper.selectOne(
                        new LambdaQueryWrapper<InsuranceConsentRecordEntity>()
                                .eq(InsuranceConsentRecordEntity::getTenantId, subject.getTenantId())
                                .eq(InsuranceConsentRecordEntity::getId, binding.getConsentId())
                                .eq(InsuranceConsentRecordEntity::getStatus, "granted")
                                .last("LIMIT 1")
                );
                if (policy != null && consent != null) result.add(response(binding, policy, consent));
            }
        }
        return result;
    }

    @Transactional
    public Map<String, Object> feedback(
            String userId, String bindingId, InsuranceMobilePlanRequest.Feedback request
    ) {
        InsurancePlanBindingEntity binding = bindingMapper.selectById(required(bindingId, "bindingId", 64));
        if (binding == null || !"active".equals(binding.getStatus())) {
            throw InsuranceApiException.notFound("active insurance plan binding was not found");
        }
        requireActiveTenant(binding.getTenantId());
        InsuranceSubjectEntity subject = subjectMapper.selectOne(new LambdaQueryWrapper<InsuranceSubjectEntity>()
                .eq(InsuranceSubjectEntity::getTenantId, binding.getTenantId())
                .eq(InsuranceSubjectEntity::getSubjectRef, binding.getSubjectRef())
                .eq(InsuranceSubjectEntity::getRehealthUserId, userId)
                .eq(InsuranceSubjectEntity::getEnrollmentStatus, "active")
                .last("LIMIT 1"));
        if (subject == null) {
            throw InsuranceApiException.forbidden("binding does not belong to the current user");
        }
        InsurancePolicyEntity policy = policyMapper.selectOne(new LambdaQueryWrapper<InsurancePolicyEntity>()
                .eq(InsurancePolicyEntity::getTenantId, binding.getTenantId())
                .eq(InsurancePolicyEntity::getId, binding.getPolicyId())
                .eq(InsurancePolicyEntity::getStatus, "active")
                .last("LIMIT 1"));
        InsuranceConsentRecordEntity consent = consentMapper.selectOne(
                new LambdaQueryWrapper<InsuranceConsentRecordEntity>()
                        .eq(InsuranceConsentRecordEntity::getTenantId, binding.getTenantId())
                        .eq(InsuranceConsentRecordEntity::getId, binding.getConsentId())
                        .eq(InsuranceConsentRecordEntity::getStatus, "granted")
                        .last("LIMIT 1"));
        if (policy == null || consent == null) {
            throw InsuranceApiException.forbidden("insurance policy or consent is no longer active");
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
        String feedbackType = required(request.feedbackType(), "feedbackType", 64).toLowerCase();
        if (!FEEDBACK_TYPES.contains(feedbackType)) {
            throw InsuranceApiException.badRequest("feedbackType is unsupported");
        }
        String verificationType = optional(request.verificationType(), "self_report", 32).toLowerCase();
        if (!VERIFICATION_TYPES.contains(verificationType)) {
            throw InsuranceApiException.badRequest("verificationType is unsupported");
        }
        BigDecimal expectedCount = "not_applicable".equals(feedbackType)
                ? null : count(request.expectedCount(), BigDecimal.ONE, "expectedCount");
        BigDecimal completedCount = "not_applicable".equals(feedbackType)
                ? null : completed(request, feedbackType, expectedCount);
        BigDecimal completionRate = expectedCount == null ? null
                : completedCount.divide(expectedCount, 6, RoundingMode.HALF_UP);
        InsuranceInterventionFeedbackEntity entity = new InsuranceInterventionFeedbackEntity();
        entity.setId(uuid());
        entity.setTenantId(binding.getTenantId());
        entity.setBindingId(binding.getId());
        entity.setSubjectRef(binding.getSubjectRef());
        entity.setInterventionId(trim(request.interventionId(), 64));
        entity.setPlanItemId(required(request.planItemId(), "planItemId", 128));
        entity.setFeedbackType(feedbackType);
        entity.setOccurredAt(request.occurredAt() == null ? LocalDateTime.now() : request.occurredAt());
        entity.setCompletionRate(completionRate);
        entity.setAdherenceScore(completionRate);
        entity.setExpectedCount(expectedCount);
        entity.setCompletedCount(completedCount);
        entity.setVerificationType(verificationType);
        entity.setCalculationVersion(ADHERENCE_CALCULATION_VERSION);
        entity.setOutcomeSummaryJson(json(request.outcomeSummary()));
        entity.setSourceSystem("rehealth_app");
        entity.setSourceRecordId(sourceRecordId);
        entity.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(entity);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("feedbackId", entity.getId());
        response.put("status", "accepted");
        response.put("idempotentReplay", false);
        response.put("calculationVersion", ADHERENCE_CALCULATION_VERSION);
        if (expectedCount != null) response.put("expectedCount", expectedCount);
        if (completedCount != null) response.put("completedCount", completedCount);
        if (completionRate != null) response.put("adherenceScore", completionRate);
        return response;
    }

    private InsuranceMobilePlanResponse response(
            InsurancePlanBindingEntity binding, InsurancePolicyEntity policy, InsuranceConsentRecordEntity consent
    ) {
        return new InsuranceMobilePlanResponse(binding.getTenantId(), binding.getId(), binding.getSubjectRef(), binding.getPolicyId(),
                policy == null ? null : policy.getPolicyNo(), binding.getPlanId(), binding.getConsentId(),
                consent == null ? null : consent.getConsentVersion(), binding.getStatus(), binding.getBoundAt());
    }

    private void requireActiveTenant(int tenantId) {
        if (subjectMapper.countActiveTenant(tenantId) < 1) {
            throw InsuranceApiException.forbidden("requested insurance tenant is not active");
        }
    }

    private static BigDecimal count(BigDecimal value, BigDecimal fallback, String field) {
        BigDecimal normalized = value == null ? fallback : value;
        if (normalized == null || normalized.signum() <= 0 || normalized.compareTo(BigDecimal.valueOf(366)) > 0) {
            throw InsuranceApiException.badRequest(field + " must be greater than 0 and at most 366");
        }
        return normalized;
    }

    private static BigDecimal completed(
            InsuranceMobilePlanRequest.Feedback request, String feedbackType, BigDecimal expectedCount
    ) {
        BigDecimal value = request.completedCount();
        if (value == null) {
            value = switch (feedbackType) {
                case "completed" -> expectedCount;
                case "partially_completed" -> expectedCount.multiply(BigDecimal.valueOf(0.5));
                default -> BigDecimal.ZERO;
            };
        }
        if (value.signum() < 0 || value.compareTo(expectedCount) > 0) {
            throw InsuranceApiException.badRequest("completedCount must be between 0 and expectedCount");
        }
        return value;
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
