package org.jeecg.modules.rehealth.insurance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Responses for the insurance-side policy dispatch interface: the tenant
 * policy list (scoped to the current staff's responsibility range), the
 * dispatchable subject candidates for the import dialog, and the two-step
 * assignment (assign an unassigned policy to an APP user by phone).
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧两步式保单派发】响应结构-----------
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
            LocalDate effectiveOn,
            LocalDateTime assignedAt
    ) {
    }

    public record DispatchableSubject(
            String enrollmentId,
            String subjectRef,
            String userName,
            String employeeName
    ) {
    }

    public record AssignRequest(
            String policyNo,
            String phone
    ) {
    }

    public record AssignResult(
            String policyNo,
            String insuredSubjectRef,
            String insuredUserName,
            LocalDateTime assignedAt
    ) {
    }
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧两步式保单派发】响应结构-----------
