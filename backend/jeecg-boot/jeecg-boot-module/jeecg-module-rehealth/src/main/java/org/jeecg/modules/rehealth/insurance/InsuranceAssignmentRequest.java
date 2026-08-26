package org.jeecg.modules.rehealth.insurance;

import java.util.List;

/**
 * Request payloads for the insurance user service assignment API.
 */
//update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增服务关系请求对象-----------
public final class InsuranceAssignmentRequest {
    private InsuranceAssignmentRequest() {
    }

    /** Claim an enrolled user as the current employee by phone or enrollment id. */
    public record Claim(String phone, String enrollmentId, String roleType) {
    }

    /** Transfer one or more enrollments from one employee to another. */
    public record Transfer(
            List<String> enrollmentIds,
            String fromEmployeeId,
            String toEmployeeId,
            String roleType,
            String reason
    ) {
    }

    /** End a single active assignment. */
    public record End(String reason) {
    }

    /** Enroll one or more registered APP users into the tenant by phone. */
    public record Enroll(List<String> phones, String projectId) {
    }

    /** Reserved contract for the phase-2 invite-code flow. */
    public record InviteCode(String phone, Integer expiresInMinutes) {
    }

    /** Reserved contract for the phase-2 employee QR scan flow. */
    public record Scan(String employeeCode, String tenantId) {
    }
}
//update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增服务关系请求对象-----------
