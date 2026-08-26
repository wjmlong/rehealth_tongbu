package org.jeecg.modules.rehealth.insurance;

import java.time.LocalDate;
import java.util.List;

/**
 * Responses for the insurance-side policy dispatch interface: the tenant
 * policy list (scoped to the current staff's responsibility range) and the
 * dispatchable subject candidates for the import dialog.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧保单派发】官网保单列表与可派发被保人响应-----------
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
            String insuredSubjectRef,
            String insuredUserName,
            String status,
            LocalDate effectiveOn
    ) {
    }

    public record DispatchableSubject(
            String enrollmentId,
            String subjectRef,
            String userName,
            String employeeName
    ) {
    }
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧保单派发】官网保单列表与可派发被保人响应-----------
