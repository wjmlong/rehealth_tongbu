package com.rehealth.genie.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileEditInputTest {
    @Test
    fun `normalizes supported gender values`() {
        assertEquals("male", normalizeProfileGender("男"))
        assertEquals("male", normalizeProfileGender(" M "))
        assertEquals("female", normalizeProfileGender("女"))
        assertEquals("female", normalizeProfileGender("female"))
        assertNull(normalizeProfileGender("unknown"))
    }

    @Test
    fun `accepts complete HBand-ready profile`() {
        val result = validateProfileEditInput(" 测试用户 ", "female", "35", "165.5", "55.2")

        assertTrue(result.isSuccess)
        assertEquals(
            ValidatedProfileEditInput("测试用户", "female", 35, 165.5, 55.2),
            result.getOrThrow(),
        )
    }

    @Test
    fun `rejects missing gender and out-of-range age`() {
        val missingGender = validateProfileEditInput("测试用户", null, "35", "165", "55")
        val invalidAge = validateProfileEditInput("测试用户", "male", "121", "190", "50")

        assertEquals("请选择性别", missingGender.exceptionOrNull()?.message)
        assertEquals("请输入 1–120 岁的有效年龄", invalidAge.exceptionOrNull()?.message)
    }
}
