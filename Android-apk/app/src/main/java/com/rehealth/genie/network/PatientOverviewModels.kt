package com.rehealth.genie.network

data class PatientMvpPayload(
    val profile: PatientProfilePayload?,
    val risk: PatientRiskPayload?,
    val interventionPlan: List<PatientInterventionPayload>?,
    val recentCheckins: List<PatientCheckInPayload>?,
    val updatedAt: Long?,
)

data class PatientProfilePayload(
    val patientId: String?,
    val name: String?,
    val gender: String?,
    val age: Int?,
    val heightCm: Double?,
    val weightKg: Double?,
    val bmi: Double?,
    val diagnoses: List<String>?,
    val medications: List<String>?,
    val allergies: List<String>?,
    val familyHistory: Boolean?,
    val smoking: Boolean?,
    val drinking: Boolean?,
    val diabetesHistory: Boolean?,
    val hypertensionHistory: Boolean?,
    val updatedAt: Long?,
)

data class PatientRiskPayload(
    val mode: String?,
    val modelVersion: String?,
    val riskScore: Double?,
    val riskLevel: String?,
    val summary: String?,
    val generatedAt: String?,
)

data class PatientInterventionPayload(
    val id: String?,
    val title: String?,
    val goal: String?,
    val action: String?,
    val duration: String?,
    val reason: String?,
    val status: String?,
)

data class PatientCheckInPayload(
    val checkInId: String?,
    val itemId: String?,
    val status: String?,
    val mood: String?,
    val note: String?,
    val checkedAt: Long?,
)
