package com.rehealth.genie.ui

import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.network.dto.HealthInterviewBaselineItemDto
import com.rehealth.genie.network.dto.HealthInterviewSubmitRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileHealthArchiveTest {
    @Test
    fun combinesPersistedProfileAndLatestInterviewForDisplay() {
        val profile = PatientProfilePayload(
            patientId = "patient-1",
            name = "测试用户",
            gender = "female",
            age = 35,
            heightCm = 165.0,
            weightKg = 55.0,
            bmi = 20.2,
            diagnoses = listOf("高血压"),
            medications = emptyList(),
            allergies = emptyList(),
            familyHistory = null,
            smoking = false,
            drinking = false,
            diabetesHistory = false,
            hypertensionHistory = true,
            updatedAt = 1L,
        )
        val interview = HealthInterviewSubmitRequestDto(
            answers = emptyList(),
            baselineItems = listOf(
                HealthInterviewBaselineItemDto("基本资料", "35 岁，165 cm，55 kg"),
                HealthInterviewBaselineItemDto("睡眠", "平均 7 小时"),
            ),
            focusAreas = listOf("规律作息"),
            generatedAt = 2L,
        )

        val rows = healthArchiveRows(profile, interview).toMap()

        assertEquals(false, rows.containsKey("性别"))
        assertEquals(false, rows.containsKey("基本资料"))
        assertEquals("待补全", rows["家族史"])
        assertEquals("有", rows["高血压史"])
        assertEquals("平均 7 小时", rows["睡眠"])
        assertEquals("规律作息", rows["关注方向"])
    }

    @Test
    fun distinguishesMissingHistoryFromNegativeHistory() {
        val rows = healthArchiveRows(profile = null, interview = null).toMap()

        assertEquals("待补全", rows["家族史"])
        assertEquals("待补全", rows["高血压史"])
        assertEquals("暂无健康问答记录", rows["关注方向"])
        assertTrue(rows.values.none { it == "无" })
    }

    @Test
    fun latestProfileOverridesStaleAttributionValuesAndKeepsClinicalFields() {
        val profile = PatientProfilePayload(
            patientId = "patient-1",
            name = "测试用户",
            gender = "female",
            age = 42,
            heightCm = 165.0,
            weightKg = 60.0,
            bmi = 22.0,
            diagnoses = emptyList(),
            medications = emptyList(),
            allergies = emptyList(),
            familyHistory = true,
            smoking = true,
            drinking = false,
            diabetesHistory = false,
            hypertensionHistory = true,
            updatedAt = 3L,
        )

        val values = mergedAttributionFactorValues(
            evaluatedValues = mapOf(
                "age" to "40 岁",
                "smoking" to "否",
                "fasting_glucose" to "5.20 mmol/L",
            ),
            profile = profile,
        )

        assertEquals("42 岁", values["age"])
        assertEquals("是", values["smoking"])
        assertEquals("有", values["family_history"])
        assertEquals("5.20 mmol/L", values["fasting_glucose"])
    }
}
