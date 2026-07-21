package com.rehealth.genie.features

import com.rehealth.genie.network.dto.FeatureQualityDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertIs

class CvdFeatureVectorDtoMapperTest {

    @Test
    fun mapsAllSixteenFieldsWithSnakeCaseKeys() {
        val vector = aCompleteVector()

        val dto = CvdFeatureVectorDtoMapper.toFeatureEvaluateRequest(vector).featureVector

        assertEquals(52, dto.age)
        assertEquals(1, dto.gender)
        assertEquals(27.4, dto.bmi!!)
        assertEquals(136.0, dto.sbp!!)
        assertEquals(86.0, dto.dbp!!)
        assertEquals(3, dto.exercise_days)
        assertEquals(0, dto.smoking)
        assertEquals(0, dto.drinking)
        assertEquals(0, dto.diabetes_history)
        assertEquals(1, dto.hypertension_history)
        assertEquals(1, dto.family_history)
        // snake_case only on the lab fields
        assertEquals(5.1, dto.fasting_glucose)
        assertEquals(6.2, dto.total_cholesterol)
        assertEquals(3.0, dto.ldl)
        assertEquals(1.2, dto.hdl)
        assertEquals(2.0, dto.triglycerides)
    }

    @Test
    fun preservesFeatureQualityEntriesForEveryField() {
        val vector = aCompleteVector()

        val dto = CvdFeatureVectorDtoMapper.toFeatureEvaluateRequest(vector).featureVector

        // Exactly the 16 canonical fields, all present as featureQuality keys.
        assertEquals(CvdFeatureFields.ALL.toSet(), dto.featureQuality.keys)
    }

    @Test
    fun qualityDtosKeepStatusSourceObservedAtAndReason() {
        val observedAt = 1_783_540_800_000L
        val vector = aCompleteVector(
            qualityOverride = mapOf(
                CvdFeatureFields.AGE to FeatureQuality.valid(
                    source = FeatureSource.USER_REPORTED,
                    observedAt = observedAt,
                    reason = "provided by interview",
                ),
            ),
        )

        val ageQuality: FeatureQualityDto =
            CvdFeatureVectorDtoMapper.toFeatureEvaluateRequest(vector).featureVector
                .featureQuality.getValue(CvdFeatureFields.AGE)

        assertEquals("VALID", ageQuality.status)
        assertEquals("USER_REPORTED", ageQuality.source)
        assertEquals(observedAt, ageQuality.observedAt)
        assertEquals("provided by interview", ageQuality.reason)
    }

    @Test
    fun missingFieldsAreNullInDtoButKeepMissingQuality() {
        val vector = CvdFeatureVector(
            age = null,
            gender = null,
            bmi = null,
            sbp = null,
            dbp = null,
            fastingGlucose = null,
            totalCholesterol = null,
            ldl = null,
            hdl = null,
            triglycerides = null,
            exerciseDays = null,
            smoking = null,
            drinking = null,
            diabetesHistory = null,
            hypertensionHistory = null,
            familyHistory = null,
            featureQuality = CvdFeatureFields.ALL.associateWith {
                FeatureQuality.missing("not provided")
            },
        )

        val dto = CvdFeatureVectorDtoMapper.toFeatureEvaluateRequest(vector).featureVector

        assertNull(dto.age)
        assertNull(dto.fasting_glucose)
        assertNull(dto.sbp)
        assertTrue(dto.featureQuality.values.all { it.status == "MISSING" })
    }

    @Test
    fun throwsWhenQualityCoverageIsIncomplete() {
        val incompleteQuality = CvdFeatureFields.ALL.drop(1)
            .associateWith { FeatureQuality.missing("not provided") }
        val vector = aCompleteVector().copy(featureQuality = incompleteQuality)

        val error = assertFailsWith<IllegalStateException> {
            CvdFeatureVectorDtoMapper.toFeatureEvaluateRequest(vector)
        }
        assertTrue(error.message!!.contains(CvdFeatureFields.AGE))
    }

    @Test
    fun requestIdIsAlwaysPopulatedWhenCallerDoesNotProvideOne() {
        val request = CvdFeatureVectorDtoMapper.toFeatureEvaluateRequest(aCompleteVector())
        assertNotNull(request.requestId)
        assertTrue(request.requestId!!.isNotBlank())
    }

    @Test
    fun callerSuppliedRequestIdIsRespected() {
        val request = CvdFeatureVectorDtoMapper.toFeatureEvaluateRequest(
            aCompleteVector(),
            requestId = "caller-id-123",
        )
        assertEquals("caller-id-123", request.requestId)
    }

    private fun aCompleteVector(
        qualityOverride: Map<String, FeatureQuality> = emptyMap(),
    ): CvdFeatureVector {
        val base = CvdFeatureFields.ALL.associateWith {
            FeatureQuality.valid(FeatureSource.USER_REPORTED, reason = "test")
        }
        val finalQuality = base + qualityOverride
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
            featureQuality = finalQuality,
        )
    }
}
