package org.jeecg.modules.rehealth.insurance;

/**
 * Candidate policy for the App's zero-input plan binding: the current user's
 * active policies across their insurance enrollments, with a masked number
 * for display and the insurer-assigned default plan id.
 */
public record InsuranceMobileBindablePolicy(
        Integer tenantId,
        String policyNo,
        String policyNoMasked,
        String productName,
        String defaultPlanId,
        boolean hasPlan
) {
}
