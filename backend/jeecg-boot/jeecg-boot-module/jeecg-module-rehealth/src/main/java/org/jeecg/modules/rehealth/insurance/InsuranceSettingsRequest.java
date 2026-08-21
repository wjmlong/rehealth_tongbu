package org.jeecg.modules.rehealth.insurance;

public final class InsuranceSettingsRequest {
    private InsuranceSettingsRequest() {
    }

    public record Organization(
            String name,
            String licenseNo,
            String insuranceType,
            String complianceEmail,
            String regulatoryEmail,
            Integer dataRetentionYears,
            Boolean maskSensitiveData,
            Boolean accessLogEnabled
    ) {
    }

    public record MemberStatus(String status) {
    }

    public record MemberDepartment(String departmentId) {
    }

    public record MemberRole(String roleCode) {
    }

    public record MemberInvitation(String phone, String departmentId, String roleCode) {
        public MemberInvitation(String phone, String departmentId) {
            this(phone, departmentId, null);
        }
    }

    public record MemberCreation(
            String username,
            String realName,
            String phone,
            String email,
            String departmentId,
            String roleCode
    ) {
    }
}
