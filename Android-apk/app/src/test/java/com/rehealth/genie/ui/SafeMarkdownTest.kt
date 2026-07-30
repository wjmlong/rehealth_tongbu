package com.rehealth.genie.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeMarkdownTest {
    @Test
    fun `parses common health guidance markdown blocks`() {
        val blocks = parseSafeMarkdown(
            """
            ## 今日建议

            - **饭后**散步 20 分钟
            1. 记录 `血压`
            > 严重不适请及时就医
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                SafeMarkdownBlockType.Heading,
                SafeMarkdownBlockType.Bullet,
                SafeMarkdownBlockType.Bullet,
                SafeMarkdownBlockType.Quote,
            ),
            blocks.map { it.type },
        )
        assertEquals("• 饭后散步 20 分钟", blocks[1].content.text)
        assertEquals("1. 记录 血压", blocks[2].content.text)
    }

    @Test
    fun `does not expose remote image or link targets`() {
        val blocks = parseSafeMarkdown(
            "查看 [健康说明](https://example.invalid/page) ![检查图](https://example.invalid/a.png)",
        )
        val text = blocks.single().content.text

        assertTrue(text.contains("健康说明"))
        assertTrue(text.contains("图片：检查图"))
        assertFalse(text.contains("https://"))
    }

    @Test
    fun `raw html remains inert text`() {
        val text = parseSafeMarkdown("<script>alert('x')</script>").single().content.text

        assertEquals("<script>alert('x')</script>", text)
    }
}
