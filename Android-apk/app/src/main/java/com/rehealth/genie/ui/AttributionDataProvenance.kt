package com.rehealth.genie.ui

import com.rehealth.genie.network.PatientMvpPayload
import com.rehealth.genie.network.PatientProfilePayload

internal object AttributionDataProvenance {
    fun trustedProfile(patientMvp: PatientMvpPayload?): PatientProfilePayload? {
        val profile = patientMvp?.profile ?: return null
        if (patientMvp.risk?.mode.equals("local_heuristic", ignoreCase = true)) return null
        if (profile.patientId.equals("local-computed", ignoreCase = true)) return null
        return profile
    }
}
