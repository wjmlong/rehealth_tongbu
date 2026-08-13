package org.jeecg.modules.rehealth.insurance;

import java.util.List;

public final class InsuranceSettingsResponse {
    private InsuranceSettingsResponse() {
    }

    public record Organization(
            String id,
            int tenantId,
            String name,
            String licenseNo,
            String insuranceType,
            String complianceEmail,
            String regulatoryEmail,
            int dataRetentionYears,
            boolean maskSensitiveData,
            boolean accessLogEnabled,
            int version
    ) {
    }

    public record Department(
            String id,
            String name,
            String parentId,
            int memberCount
    ) {
    }

    public record Member(
            String id,
            String username,
            String realName,
            String email,
            String phone,
            String status,
            String departmentId,
            String departments,
            String roleCode,
            String roles,
            int assignmentCount
    ) {
    }

    public record MemberInvitation(
            String userId,
            String status,
            String message
    ) {
    }

    public record Assignment(
            String subjectRef,
            String managerUserId,
            String managerName,
            String departmentId,
            String departmentName,
            String status
    ) {
    }

    public record AssignmentRequest(
            String managerUserId,
            String departmentId,
            String status
    ) {
    }

    public record Snapshot(
            Organization organization,
            List<Department> departments,
            List<Member> members,
            List<Assignment> assignments
    ) {
    }
}
