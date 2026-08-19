package com.rehealth.genie.ui

import com.rehealth.genie.network.dto.InstitutionCarePlanItemDto
import com.rehealth.genie.network.dto.InstitutionCarePlanOccurrenceDto
import kotlin.test.Test
import kotlin.test.assertEquals

class AttributionPlanPresentationTest {
    @Test
    fun `uses reference copy for the sixteen input plan`() {
        assertEquals("围绕 16 项健康输入安排下一步行动", interventionPlanSubtitle(16))
    }

    @Test
    fun `reports plan expansion state and matching action`() {
        assertEquals("待生成", interventionPlanStateLabel(hasPlan = false, expanded = false))
        assertEquals("已展开", interventionPlanStateLabel(hasPlan = true, expanded = true))
        assertEquals("已收起", interventionPlanStateLabel(hasPlan = true, expanded = false))
        assertEquals("收起干预计划", interventionPlanToggleLabel(expanded = true))
        assertEquals("展开干预计划", interventionPlanToggleLabel(expanded = false))
    }

    @Test
    fun `keeps action and timing in one reference style paragraph`() {
        assertEquals(
            "晚餐后步行 15 分钟；每日晚餐后",
            interventionPlanActionText("晚餐后步行 15 分钟", "每日晚餐后"),
        )
        assertEquals(
            "23:00 前入睡",
            interventionPlanActionText("23:00 前入睡", "23:00 前"),
        )
    }

    @Test
    fun `formats institution adherence and the four scoring states`() {
        assertEquals("28 日依从性 75.0%", institutionAdherenceText(75.0))
        assertEquals("28 日依从性积累中", institutionAdherenceText(null))
        assertEquals("今日任务 · 已完成（100 分）", institutionTaskStatusLabel(item("completed")))
        assertEquals("今日任务 · 部分完成（50 分）", institutionTaskStatusLabel(item("partially_completed")))
        assertEquals("今日任务 · 稍后完成（0 分）", institutionTaskStatusLabel(item("skipped")))
        assertEquals("今日任务 · 不适用（不计入依从性）", institutionTaskStatusLabel(item("not_applicable")))
    }

    private fun item(feedbackType: String?) = InstitutionCarePlanItemDto(
        itemId = "item-1",
        logicalItemId = "logical-1",
        category = "exercise",
        title = "晚间步行",
        scheduleSupported = true,
        todayOccurrence = InstitutionCarePlanOccurrenceDto(
            occurrenceId = "occurrence-1",
            scheduledAt = "2026-08-19T19:00:00",
            dueAt = "2026-08-19T23:59:59",
            feedbackType = feedbackType,
        ),
    )
}
