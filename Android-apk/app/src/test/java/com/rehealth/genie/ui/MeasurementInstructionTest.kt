package com.rehealth.genie.ui

import com.rehealth.genie.ring.RingMetricType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MeasurementInstructionTest {
    @Test
    fun ecgInstructionsRequireElectrodeContactAndKeepMedicalBoundary() {
        val instruction = assertNotNull(measurementInstructionFor(RingMetricType.ECG))

        assertContains(instruction.steps.joinToString(), "金属电极片")
        assertContains(instruction.healthNotice, "单导联 ECG")
        assertContains(instruction.healthNotice, "不能替代医疗诊断")
    }

    @Test
    fun bodyCompositionInstructionsRequireCompleteMeasurementCircuit() {
        val instruction = assertNotNull(
            measurementInstructionFor(RingMetricType.BODY_COMPOSITION),
        )

        assertContains(instruction.steps.joinToString(), "金属电极片")
        assertContains(instruction.steps.joinToString(), "测量回路")
        assertContains(instruction.healthNotice, "设备算法估算")
    }

    @Test
    fun metricsWithoutElectrodeInstructionsAreNotIntercepted() {
        assertNull(measurementInstructionFor(RingMetricType.HEART_RATE))
        assertNull(measurementInstructionFor(RingMetricType.BLOOD_OXYGEN))
    }
}
