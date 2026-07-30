package com.rehealth.genie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted

internal enum class SafeMarkdownBlockType {
    Paragraph,
    Heading,
    Bullet,
    Quote,
    Code,
}

internal data class SafeMarkdownBlock(
    val type: SafeMarkdownBlockType,
    val content: AnnotatedString,
)

/**
 * A deliberately small Markdown subset for server-provided health guidance.
 *
 * It never executes HTML, loads remote images, or turns links into automatic navigation.
 */
internal fun parseSafeMarkdown(markdown: String): List<SafeMarkdownBlock> {
    val normalized = markdown
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .filter { it == '\n' || it == '\t' || !it.isISOControl() }
        .take(MAX_MARKDOWN_CHARS)
    val blocks = mutableListOf<SafeMarkdownBlock>()
    val paragraph = mutableListOf<String>()
    val code = mutableListOf<String>()
    var inCodeFence = false

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += SafeMarkdownBlock(
                SafeMarkdownBlockType.Paragraph,
                parseSafeInlineMarkdown(paragraph.joinToString("\n")),
            )
            paragraph.clear()
        }
    }

    normalized.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        if (line.trimStart().startsWith("```")) {
            flushParagraph()
            if (inCodeFence) {
                blocks += SafeMarkdownBlock(
                    SafeMarkdownBlockType.Code,
                    AnnotatedString(code.joinToString("\n")),
                )
                code.clear()
            }
            inCodeFence = !inCodeFence
            return@forEach
        }
        if (inCodeFence) {
            code += line
            return@forEach
        }
        if (line.isBlank()) {
            flushParagraph()
            return@forEach
        }

        val trimmed = line.trimStart()
        val heading = HEADING.matchEntire(trimmed)
        val bullet = BULLET.matchEntire(trimmed)
        val numbered = NUMBERED.matchEntire(trimmed)
        when {
            heading != null -> {
                flushParagraph()
                blocks += SafeMarkdownBlock(
                    SafeMarkdownBlockType.Heading,
                    parseSafeInlineMarkdown(heading.groupValues[2]),
                )
            }
            bullet != null -> {
                flushParagraph()
                blocks += SafeMarkdownBlock(
                    SafeMarkdownBlockType.Bullet,
                    buildAnnotatedString {
                        append("• ")
                        append(parseSafeInlineMarkdown(bullet.groupValues[1]))
                    },
                )
            }
            numbered != null -> {
                flushParagraph()
                blocks += SafeMarkdownBlock(
                    SafeMarkdownBlockType.Bullet,
                    buildAnnotatedString {
                        append("${numbered.groupValues[1]}. ")
                        append(parseSafeInlineMarkdown(numbered.groupValues[2]))
                    },
                )
            }
            trimmed.startsWith("> ") -> {
                flushParagraph()
                blocks += SafeMarkdownBlock(
                    SafeMarkdownBlockType.Quote,
                    parseSafeInlineMarkdown(trimmed.removePrefix("> ")),
                )
            }
            else -> paragraph += line
        }
    }
    flushParagraph()
    if (code.isNotEmpty()) {
        blocks += SafeMarkdownBlock(SafeMarkdownBlockType.Code, AnnotatedString(code.joinToString("\n")))
    }
    return blocks.ifEmpty {
        listOf(SafeMarkdownBlock(SafeMarkdownBlockType.Paragraph, AnnotatedString("")))
    }
}

private fun parseSafeInlineMarkdown(source: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    while (cursor < source.length) {
        val token = INLINE_TOKEN.find(source, cursor)
        if (token == null || token.range.first > cursor) {
            val end = token?.range?.first ?: source.length
            append(source.substring(cursor, end))
            cursor = end
            continue
        }
        val value = token.value
        when {
            value.startsWith("![") -> {
                val alt = token.groups["imageAlt"]?.value.orEmpty().ifBlank { "图片" }
                withStyle(SpanStyle(color = Muted, fontStyle = FontStyle.Italic)) {
                    append("图片：$alt")
                }
            }
            value.startsWith("[") -> {
                val label = token.groups["linkLabel"]?.value.orEmpty()
                withStyle(SpanStyle(color = Mint, textDecoration = TextDecoration.Underline)) {
                    append(label)
                }
            }
            value.startsWith("**") || value.startsWith("__") -> {
                val text = token.groups["boldA"]?.value ?: token.groups["boldB"]?.value.orEmpty()
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text) }
            }
            value.startsWith("`") -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = MintSoft,
                    ),
                ) {
                    append(token.groups["code"]?.value.orEmpty())
                }
            }
            else -> {
                val text = token.groups["italicA"]?.value ?: token.groups["italicB"]?.value.orEmpty()
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text) }
            }
        }
        cursor = token.range.last + 1
    }
}

@Composable
internal fun SafeMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        parseSafeMarkdown(markdown).forEach { block ->
            val blockModifier = when (block.type) {
                SafeMarkdownBlockType.Quote -> Modifier.fillMaxWidth().background(MintSoft)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                SafeMarkdownBlockType.Code -> Modifier.fillMaxWidth()
                    .background(Color(0xFFF3F5F4), RoundedCornerShape(8.dp))
                    .padding(9.dp)
                else -> Modifier.fillMaxWidth()
            }
            Text(
                text = block.content,
                color = if (block.type == SafeMarkdownBlockType.Quote) Muted else color,
                fontSize = if (block.type == SafeMarkdownBlockType.Heading) 15.sp else fontSize,
                lineHeight = if (block.type == SafeMarkdownBlockType.Heading) 22.sp else 19.sp,
                fontWeight = if (block.type == SafeMarkdownBlockType.Heading) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                fontFamily = if (block.type == SafeMarkdownBlockType.Code) {
                    FontFamily.Monospace
                } else {
                    FontFamily.Default
                },
                modifier = blockModifier,
            )
        }
    }
}

private const val MAX_MARKDOWN_CHARS = 12_000
private val HEADING = Regex("^(#{1,3})\\s+(.+)$")
private val BULLET = Regex("^[-+*]\\s+(.+)$")
private val NUMBERED = Regex("^(\\d{1,3})[.)]\\s+(.+)$")
private val INLINE_TOKEN = Regex(
    """!\[(?<imageAlt>[^\]]*)]\([^)]+\)|\[(?<linkLabel>[^\]]+)]\([^)]+\)|\*\*(?<boldA>[^*]+)\*\*|__(?<boldB>[^_]+)__|`(?<code>[^`\n]+)`|\*(?<italicA>[^*\n]+)\*|_(?<italicB>[^_\n]+)_""",
)
