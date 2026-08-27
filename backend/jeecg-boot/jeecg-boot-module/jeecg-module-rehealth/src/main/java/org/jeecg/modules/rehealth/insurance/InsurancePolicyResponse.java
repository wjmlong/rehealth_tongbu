package org.jeecg.modules.rehealth.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Responses for the insurance-side basic policy library: the tenant policy
 * list (pure policy information + linked-user count), the dispatchable
 * subject candidates, and the policy-to-user link result.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧基础保单库】响应结构-----------
public final class InsurancePolicyResponse {
    private InsurancePolicyResponse() {
    }

    public record PolicyPage(long total, List<Item> items) {
    }

    public record Item(
            String policyNo,
            String productName,
            String policyType,
            String defaultPlanId,
            String planName,
            String status,
            LocalDate effectiveOn,
            LocalDate expiresOn,
            BigDecimal coverageAmount,
            BigDecimal premiumAmount,
            long linkCount
    ) {
    }

    public record DispatchableSubject(
            String enrollmentId,
            String subjectRef,
            String userName,
            String employeeName
    ) {
    }

    public record LinkRequest(
            String policyNo,
            String phone,
            String enrollmentId
    ) {
    }

    public record LinkResult(
            String policyNo,
            String subjectRef,
            String userName,
            LocalDateTime linkedAt
    ) {
    }
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧基础保单库】响应结构-----------
