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
            baselineItems = listOf(HealthInterviewBaselineItemDto("睡眠", "平均 7 小时")),
            focusAreas = listOf("规律作息"),
            generatedAt = 2L,
        )

        val rows = healthArchiveRows(profile, interview).toMap()

        assertEquals("女", rows["性别"])
        assertEquals("待补全", rows["家族史"])
        assertEquals("有", rows["高血压史"])
        assertEquals("平均 7 小时", rows["健康问答 · 睡眠"])
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
}
