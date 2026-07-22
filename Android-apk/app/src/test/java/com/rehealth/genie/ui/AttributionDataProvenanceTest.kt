package com.rehealth.genie.ui

import com.rehealth.genie.network.PatientMvpPayload
import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.network.PatientRiskPayload
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class AttributionDataProvenanceTest {
    @Test
    fun `rejects local heuristic profile with default false answers`() {
        val profile = profile(patientId = "local-computed")
        val patientMvp = patientMvp(profile = profile, mode = "local_heuristic")

        val result = AttributionDataProvenance.trustedProfile(patientMvp)

        assertNull(result)
    }

    @Test
    fun `rejects local computed profile when risk metadata is missing`() {
        val profile = profile(patientId = "local-computed")
        val patientMvp = patientMvp(profile = profile, mode = null)

        val result = AttributionDataProvenance.trustedProfile(patientMvp)

        assertNull(result)
    }

    @Test
    fun `retains real remote profile with explicit false answers`() {
        val profile = profile(patientId = "patient-42")
        val patientMvp = patientMvp(profile = profile, mode = "remote")

        val result = AttributionDataProvenance.trustedProfile(patientMvp)

        assertSame(profile, result)
    }

    private fun profile(patientId: String) = PatientProfilePayload(
        patientId = patientId,
        name = null,
        gender = null,
        age = null,
        heightCm = null,
        weightKg = null,
        bmi = null,
        diagnoses = emptyList(),
        medications = emptyList(),
        allergies = emptyList(),
        familyHistory = false,
        smoking = false,
        drinking = false,
        diabetesHistory = false,
        hypertensionHistory = false,
        updatedAt = null,
    )

    private fun patientMvp(profile: PatientProfilePayload, mode: String?) = PatientMvpPayload(
        profile = profile,
        risk = mode?.let {
            PatientRiskPayload(
                mode = it,
                modelVersion = null,
                riskScore = null,
                riskLevel = null,
                summary = null,
                generatedAt = null,
            )
        },
        interventionPlan = emptyList(),
        recentCheckins = emptyList(),
        updatedAt = null,
    )
}
