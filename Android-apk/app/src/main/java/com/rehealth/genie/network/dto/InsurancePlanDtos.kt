package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

/** Privacy-safe insurance-plan DTOs. Raw health measurements never cross this boundary. */
@JsonClass(generateAdapter = true)
data class InsurancePlanBindRequestDto(
    val tenantId: String,
    val policyNo: String,
    val planId: String,
    val consentVersion: String,
    val consentType: String = "insurance_health_program",
    val evidenceRef: String? = null,
    val evidenceHash: String? = null,
    val sourceRecordId: String,
    val metadata: Map<String, String> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class InsurancePlanBindingDto(
    val tenantId: Int,
    val bindingId: String,
    val subjectRef: String,
    val policyId: String,
    val policyNo: String,
    val planId: String,
    val consentId: String,
    val consentVersion: String,
    val status: String,
    val boundAt: String,
)

@JsonClass(generateAdapter = true)
data class InsurancePlanFeedbackRequestDto(
    val feedbackType: String,
    val occurredAt: String,
    val completionRate: Double? = null,
    val adherenceScore: Double? = null,
    val sourceRecordId: String,
    val interventionId: String? = null,
    val planItemId: String,
    val expectedCount: Double = 1.0,
    val completedCount: Double? = null,
    val verificationType: String = "self_report",
    val outcomeSummary: Map<String, String> = emptyMap(),
)
