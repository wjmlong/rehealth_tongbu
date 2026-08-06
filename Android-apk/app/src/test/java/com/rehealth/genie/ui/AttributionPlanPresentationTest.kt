package com.rehealth.genie.ui

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
}
