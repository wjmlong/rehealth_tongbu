package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

/** Privacy-safe insurance-plan DTOs. Raw health measurements never cross this boundary. */
@JsonClass(generateAdapter = true)
data class InsurancePlanBindRequestDto(
    val tenantId: String? = null,
    val policyNo: String? = null,
    val planId: String? = null,
    val consentVersion: String = "v1.0",
    val consentType: String = "insurance_health_program",
    val evidenceRef: String? = null,
    val evidenceHash: String? = null,
    val sourceRecordId: String,
    val metadata: Map<String, String> = emptyMap(),
)

/** 零输入绑定的候选保单（服务端按当前用户聚合，保单号已脱敏，含计划名称）。 */
@JsonClass(generateAdapter = true)
data class InsuranceMobileBindablePolicyDto(
    val tenantId: Int,
    val policyNo: String,
    val policyNoMasked: String,
    val productName: String? = null,
    val defaultPlanId: String? = null,
    val planName: String? = null,
    val hasPlan: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class InsurancePlanBindingDto(
    val tenantId: Int,
    val bindingId: String,
    val subjectRef: String,
    val policyId: String,
    val policyNo: String,
    val planId: String,
    val planName: String? = null,
    val consentId: String,
    val consentVersion: String,
    val status: String,
    val boundAt: String,
)

/**
 * 当前 App 用户的服务专员信息（保险侧服务关系）。
 * 无服务关系时服务端返回 null，客户端展示“暂无专属服务专员”。
 */
@JsonClass(generateAdapter = true)
data class InsuranceServiceContactDto(
    val tenantId: Int,
    val projectName: String? = null,
    val employeeName: String? = null,
    val roleType: String? = null,
    val startTime: String? = null,
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

@JsonClass(generateAdapter = true)
data class InstitutionCarePlanDto(
    val tenantId: Int,
    val organizationName: String? = null,
    val planId: String,
    val revisionId: String,
    val revisionNo: Int,
    val title: String,
    val summary: String? = null,
    val effectiveFrom: String,
    val effectiveTo: String? = null,
    val adherence28d: InstitutionCarePlanAdherenceDto,
    val items: List<InstitutionCarePlanItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class InstitutionCarePlanAdherenceDto(
    val windowDays: Int = 28,
    val scorePercent: Double? = null,
    val expectedCount: Int = 0,
    val scoredCount: Int = 0,
    val excludedCount: Int = 0,
    val calculationVersion: String,
)

@JsonClass(generateAdapter = true)
data class InstitutionCarePlanItemDto(
    val itemId: String,
    val logicalItemId: String,
    val category: String,
    val title: String,
    val instructions: String? = null,
    val scoringWeight: Double = 1.0,
    val allowNotApplicable: Boolean = true,
    val scheduleType: String? = null,
    val scheduleSupported: Boolean = false,
    val todayOccurrence: InstitutionCarePlanOccurrenceDto? = null,
)

@JsonClass(generateAdapter = true)
data class InstitutionCarePlanOccurrenceDto(
    val occurrenceId: String,
    val scheduledAt: String,
    val dueAt: String,
    val feedbackType: String? = null,
    val scoreValue: Double? = null,
)

@JsonClass(generateAdapter = true)
data class InstitutionCarePlanFeedbackRequestDto(
    val feedbackType: String,
    val occurredAt: String,
    val sourceRecordId: String,
    val verificationType: String = "self_report",
    val note: String? = null,
)
