package org.jeecg.modules.rehealth.insurance;

import java.time.LocalDateTime;

/**
 * Responses for the insurance-side scan association: employee QR management,
 * the APP scan preview (masked employee info) and the confirm result.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧扫码关联】响应结构-----------
public final class InsuranceScanLinkResponse {
    private InsuranceScanLinkResponse() {
    }

    public record QrCode(
            String code,
            String status,
            LocalDateTime expiresAt,
            long scanCount,
            String payload
    ) {
    }

    public record ScanPreview(
            String sessionId,
            LocalDateTime expiresAt,
            Employee employee,
            Contact existingContact
    ) {
        public record Employee(
                String name,
                String orgName,
                String departmentName,
                String avatarInitial
        ) {
        }

        public record Contact(
                String tenantId,
                String employeeName,
                String roleType
        ) {
        }
    }

    public record ConfirmRequest(
            boolean replaceExisting
    ) {
    }

    public record ConfirmResult(
            boolean created,
            boolean alreadyServed,
            String employeeName
    ) {
    }
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧扫码关联】响应结构-----------
