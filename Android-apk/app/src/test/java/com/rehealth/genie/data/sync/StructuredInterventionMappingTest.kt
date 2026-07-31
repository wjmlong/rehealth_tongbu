package com.rehealth.genie.data.sync

import com.rehealth.genie.network.dto.InterventionActionDto
import com.rehealth.genie.network.dto.InterventionPlanDto
import org.junit.Assert.assertEquals
import org.junit.Test

class StructuredInterventionMappingTest {
    @Test
    fun `maps structured plan items to numbered attribution actions`() {
        val plan = InterventionPlanDto(
            plan_id = "plan-1",
            items = listOf(
                InterventionActionDto(
                    id = "action-2",
                    category = "sleep",
                    title = "睡眠",
                    action = "今晚固定时间准备入睡",
                    rationale = "最近睡眠时长下降",
                    target = "保持规律作息",
                    timing = "睡前1小时",
                    priority = 2,
                ),
                InterventionActionDto(
                    id = "action-1",
                    category = "exercise",
                    title = "运动",
                    action = "晚餐后步行15分钟",
                    rationale = "今天活动时长较少",
                    target = "增加15分钟活动",
                    timing = "晚餐后",
                    priority = 1,
                ),
            ),
        )

        val actions = plan.toPatientInterventionPayloads()

        assertEquals(2, actions.size)
        assertEquals("运动", actions[0].title)
        assertEquals("晚餐后", actions[0].duration)
        assertEquals("plan-1", actions[0].id)
        assertEquals("睡眠", actions[1].title)
    }
}
