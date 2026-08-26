package org.jeecg.modules.rehealth.insurance;

import java.util.List;

/**
 * Response payloads for the insurance user service assignment API.
 */
//update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增服务关系响应对象-----------
public final class InsuranceAssignmentResponse {
    private InsuranceAssignmentResponse() {
    }

    public record Assignment(
            String id,
            int tenantId,
            String enrollmentId,
            String projectId,
            String projectName,
            String subjectRef,
            String rehealthUserId,
            String userName,
            String employeeId,
            String employeeName,
            String roleType,
            String startTime,
            String endTime,
            String status,
            String startTimeSource,
            String changeReason
    ) {
    }

    public record Claimed(
            Assignment assignment,
            boolean created
    ) {
    }

    public record Page(
            long total,
            List<Assignment> records
    ) {
    }

    public record Enrollment(
            String id,
            String projectId,
            String projectName,
            String subjectRef,
            String userName,
            String enrollmentStatus,
            String ownerEmployeeId,
            String ownerEmployeeName
    ) {
    }

    public record EnrollmentPage(
            long total,
            List<Enrollment> records
    ) {
    }

    public record EnrollRecord(
            String phone,
            String enrollmentId,
            String status,
            String message
    ) {
    }

    public record EnrollResult(
            int requested,
            List<EnrollRecord> records
    ) {
    }

    public record TransferResult(
            int requested,
            int transferred,
            List<String> errors
    ) {
    }

    public record EndResult(
            Assignment assignment,
            String message
    ) {
    }

    public record ChangeLog(
            String id,
            String enrollmentId,
            String assignmentId,
            String changeType,
            String beforeJson,
            String afterJson,
            String reason,
            String operatorId,
            String createdAt
    ) {
    }

    public record History(
            List<Assignment> assignments,
            List<ChangeLog> logs
    ) {
    }

    public record MobileContact(
            int tenantId,
            String projectName,
            String employeeName,
            String roleType,
            String startTime
    ) {
    }
}
//update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增服务关系响应对象-----------
