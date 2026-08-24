package com.rehealth.genie.ui

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
    fun `expired authorized main session returns to login`() {
        assertTrue(
            shouldReturnToLogin(
                stage = AppStage.Main,
                authState = AuthState.Unauthorized,
                sessionExpired = true,
            ),
        )
        assertFalse(
            shouldReturnToLogin(
                stage = AppStage.Login,
                authState = AuthState.Unauthorized,
                sessionExpired = true,
            ),
        )
        assertFalse(
            // Anonymous guest browsing Main is not an expired session.
            shouldReturnToLogin(
                stage = AppStage.Main,
                authState = AuthState.Unauthorized,
                sessionExpired = false,
            ),
        )
    }
}
