package com.rehealth.genie.features

import com.rehealth.genie.network.dto.RhiV2FeatureFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RhiV2DraftMapperTest {
    @Test
    fun `maps only semantically compatible v1 fields`() {
        val request = RhiV2DraftMapper.fromCvdV1(completeV1(), requestId = "rhi-draft")
        val vector = request.featureVector

        assertEquals(52, vector.age)
        assertEquals("male", vector.biologicalSex)
        assertEquals(27.4, vector.bmi)
        assertEquals(5.1, vector.glycemiaValue)
        assertEquals("fasting_glucose_mmol_l", request.glycemiaMetric)
        assertEquals(0, vector.nicotineExposure)
        assertNull(vector.prematureCvdFamilyHistory)
        assertEquals(
            "MISSING",
            request.featureQuality.getValue("premature_cvd_family_history").status,
        )
        assertEquals("rhi-draft", request.requestId)
    }

    @Test
    fun `does not relabel single blood pressure or exercise frequency`() {
        val request = RhiV2DraftMapper.fromCvdV1(completeV1())

        assertNull(request.featureVector.sbp7dMean)
        assertNull(request.featureVector.dbp7dMean)
        assertNull(request.featureVector.steps7dMean)
        assertNull(request.featureVector.mvpaMinutes7d)
        assertEquals("MISSING", request.featureQuality.getValue("sbp_7d_mean").status)
        assertEquals("MISSING", request.featureQuality.getValue("steps_7d_mean").status)
    }

    @Test
    fun `creates explicit quality for all 32 fields`() {
        val request = RhiV2DraftMapper.fromCvdV1(completeV1())

        assertEquals(32, RhiV2FeatureFields.ALL.size)
        assertEquals(RhiV2FeatureFields.ALL.toSet(), request.featureQuality.keys)
        assertTrue(request.featureQuality.values.none { it.reason.isBlank() })
    }

    private fun completeV1(): CvdFeatureVector {
        val quality = CvdFeatureFields.ALL.associateWith {
            FeatureQuality.valid(FeatureSource.USER_REPORTED, reason = "test")
        }
        return CvdFeatureVector(
            age = 52,
            gender = 1,
            bmi = 27.4,
            sbp = 136.0,
            dbp = 86.0,
            fastingGlucose = 5.1,
            totalCholesterol = 6.2,
            ldl = 3.0,
            hdl = 1.2,
            triglycerides = 2.0,
            exerciseDays = 3,
            smoking = 0,
            drinking = 0,
            diabetesHistory = 0,
            hypertensionHistory = 1,
            familyHistory = 1,
            featureQuality = quality,
        )
    }
}
