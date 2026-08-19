package com.rehealth.genie.ui

import kotlin.test.Test
import kotlin.test.assertContains

class DataDeviceNoticePresentationTest {
    @Test
    fun `ring notice centralizes device estimate and medical disclaimer`() {
        val notice = dataDeviceNoticeText(cloudMode = false)

        assertContains(notice, "设备估算")
        assertContains(notice, "仅供健康参考")
        assertContains(notice, "不能替代医疗诊断")
    }

    @Test
    fun `cloud notice keeps cloud provenance and medical disclaimer`() {
        val notice = dataDeviceNoticeText(cloudMode = true)

        assertContains(notice, "云米云端手表历史数据")
        assertContains(notice, "不能替代医疗诊断")
    }
}
