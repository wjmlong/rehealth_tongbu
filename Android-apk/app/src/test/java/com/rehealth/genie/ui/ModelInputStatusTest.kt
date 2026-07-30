package com.rehealth.genie.ui

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelInputStatusTest {
    @Test
    fun `user model inputs exclude temperature`() {
        val types = modelInputsFromRingState(RingUiState()).map { it.type }

        assertFalse(RingMetricType.TEMPERATURE in types)
        assertTrue(RingMetricType.HEART_RATE in types)
        assertTrue(RingMetricType.STEPS in types)
    }
}
