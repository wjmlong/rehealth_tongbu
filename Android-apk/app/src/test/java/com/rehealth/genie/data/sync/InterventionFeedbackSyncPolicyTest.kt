package com.rehealth.genie.data.sync

import com.rehealth.genie.ui.institutionFeedbackSyncMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class InterventionFeedbackSyncPolicyTest {
    private val feedback = InterventionFeedbackEntity(
        id = "feedback-1",
        ownerUserId = "user-1",
        interventionId = "plan-1",
        occurrenceId = "occurrence-1",
        status = "completed",
        checkedAt = 1L,
        createdAt = 1L,
    )

    @Test
    fun `transient failure enters bounded retry state`() {
        val retried = feedback.nextFeedbackRetry("network", now = 1_000L)

        assertEquals("retry", retried.uploadStatus)
        assertEquals(1, retried.uploadAttempts)
        assertEquals(31_000L, retried.nextRetryAt)
    }

    @Test
    fun `retry exhaustion becomes terminal instead of syncing forever`() {
        val exhausted = feedback.copy(uploadAttempts = MAX_FEEDBACK_UPLOAD_ATTEMPTS - 1)
            .nextFeedbackRetry("network", now = 1_000L)

        assertEquals("dead_letter", exhausted.uploadStatus)
        assertEquals(MAX_FEEDBACK_UPLOAD_ATTEMPTS, exhausted.uploadAttempts)
    }

    @Test
    fun `presentation distinguishes synced retrying and failed feedback`() {
        assertEquals(
            "保险机构：完成状态已记录，已同步",
            institutionFeedbackSyncMessage("保险机构", "completed", "done"),
        )
        assertEquals(
            "保险机构：完成状态已记录，网络恢复后将自动重试",
            institutionFeedbackSyncMessage("保险机构", "completed", "retry"),
        )
        assertEquals(
            "保险机构：反馈已保存在本机，但同步失败，请检查登录状态后重试",
            institutionFeedbackSyncMessage("保险机构", "completed", "dead_letter"),
        )
    }
}
