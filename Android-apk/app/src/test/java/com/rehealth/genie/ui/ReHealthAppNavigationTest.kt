package com.rehealth.genie.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReHealthAppNavigationTest {
    @Test
    fun `new account enters interview after authentication`() {
        assertEquals(AppStage.InterviewSession, stageAfterAuthentication(onboardingComplete = false))
    }

    @Test
    fun `completed onboarding enters main after authentication`() {
        assertEquals(AppStage.Main, stageAfterAuthentication(onboardingComplete = true))
    }
}
