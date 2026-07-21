package com.rehealth.genie.phm

import com.rehealth.genie.network.dto.AttributionEventItemDto
import com.rehealth.genie.network.dto.CvdFeatureVectorDto
import com.rehealth.genie.network.dto.FeatureQualityDto
import com.rehealth.genie.network.dto.InterventionGenerateResponseDto
import com.rehealth.genie.network.dto.RiskEvaluateResponseDto

// ---- App domain <-> backend DTO ----

fun CvdFeatureVector.toDto(): CvdFeatureVectorDto = CvdFeatureVectorDto(
    age = age,
    gender = gender,
    bmi = bmi,
    sbp = sbp,
    dbp = dbp,
    fasting_glucose = fastingGlucose,
    total_cholesterol = totalCholesterol,
    ldl = ldl,
    hdl = hdl,
    triglycerides = triglycerides,
    exercise_days = exerciseDays,
    smoking = smoking,
    drinking = drinking,
    diabetes_history = diabetesHistory,
    hypertension_history = hypertensionHistory,
    family_history = familyHistory,
    featureQuality = featureQuality
        .mapValues { (_, v) -> FeatureQualityDto(v.status ?: "", v.source ?: "", v.observedAt, v.reason ?: "") },
)

fun CvdFeatureVectorDto.toDomain(): CvdFeatureVector = CvdFeatureVector(
    age = age,
    gender = gender,
    bmi = bmi,
    sbp = sbp,
    dbp = dbp,
    fastingGlucose = fasting_glucose,
    totalCholesterol = total_cholesterol,
    ldl = ldl,
    hdl = hdl,
    triglycerides = triglycerides,
    exerciseDays = exercise_days,
    smoking = smoking,
    drinking = drinking,
    diabetesHistory = diabetes_history,
    hypertensionHistory = hypertension_history,
    familyHistory = family_history,
    featureQuality = featureQuality
        ?.mapValues { (_, v) -> FeatureQuality(v.status, v.source, v.observedAt, v.reason) }
        ?: emptyMap(),
)

fun RiskEvaluateResponseDto.toDomain(): RiskResult = RiskResult(
    riskScore = riskScore,
    riskLevel = riskLevel,
    featureContributions = featureContributions ?: emptyMap(),
    modelVersion = modelVersion,
    isMock = isMock,
    missingFields = missingFields ?: emptyList(),
    qualityWarnings = qualityWarnings ?: emptyList(),
    requestId = requestId,
    contributionMethod = contributionMethod,
    summary = summary,
)

fun InterventionGenerateResponseDto.toDomain(): InterventionPlan = InterventionPlan(
    planId = planId,
    generatedAt = generatedAt,
    priorityIntervention = priorityIntervention,
    rationale = rationale,
    expectedImpact = expectedImpact,
    contraindications = contraindications ?: emptyList(),
    confidence = confidence,
    modelVersion = modelVersion,
    isMock = isMock,
    medicalDisclaimer = medicalDisclaimer,
)

fun AttributionEvent.toDto(): AttributionEventItemDto =
    AttributionEventItemDto(date, riskScore, interventionId, adherence)
