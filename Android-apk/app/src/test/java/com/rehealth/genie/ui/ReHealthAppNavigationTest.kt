package com.rehealth.genie.ui

import com.rehealth.genie.data.sync.QueueState
import com.rehealth.genie.network.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReHealthAppNavigationTest {
    @Test
    fun `new account enters interview after authentication`() {
        assertEquals(AppStage.InterviewSession, stageAfterAuthentication(requiresOnboarding = true))
    }

    @Test
    fun `completed onboarding enters main after authentication`() {
        assertEquals(AppStage.Main, stageAfterAuthentication(requiresOnboarding = false))
    }

    @Test
    fun `unauthorized paused main session returns to login`() {
        assertTrue(
            shouldReturnToLogin(
                stage = AppStage.Main,
                queueState = QueueState.Paused,
                authState = AuthState.Unauthorized,
            ),
        )
        assertFalse(
            shouldReturnToLogin(
                stage = AppStage.Login,
                queueState = QueueState.Paused,
                authState = AuthState.Unauthorized,
            ),
        )
    }
}
