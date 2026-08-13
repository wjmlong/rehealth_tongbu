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
}
