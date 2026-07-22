package com.rehealth.genie.interview

import kotlin.test.Test
import kotlin.test.assertEquals

class HealthInterviewSyncPayloadTest {
    @Test
    fun `maps interview answers and baseline to cloud payload`() {
        val question = InterviewQuestion(
            id = "profile",
            topic = InterviewTopic.PROFILE,
            text = "question shown in UI",
            helper = "helper shown in UI",
        )
        val baseline = HealthBaseline(
            items = listOf(BaselineItem("基本资料", "32 岁")),
            focusAreas = listOf("睡眠节律"),
            generatedAt = 1_726_000_000_000L,
        )

        val payload = healthInterviewSyncPayload(
            answers = listOf(InterviewAnswer(question, "32 岁")),
            baseline = baseline,
        )

        assertEquals("profile", payload.answers.single().questionId)
        assertEquals("PROFILE", payload.answers.single().topic)
        assertEquals("32 岁", payload.answers.single().content)
        assertEquals("基本资料", payload.baselineItems.single().label)
        assertEquals(1_726_000_000_000L, payload.generatedAt)
    }
}
