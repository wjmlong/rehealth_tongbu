package com.rehealth.genie.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF092A3E)
val Mint = Color(0xFF19A96B)
val MintSoft = Color(0xFFE8F8F1)
val Canvas = Color(0xFFF8FCFB)
val Line = Color(0xFFE7EFEC)
val Muted = Color(0xFF718087)

private val Colors = lightColorScheme(
    primary = Mint,
    onPrimary = Color.White,
    secondary = Color(0xFF67D3B2),
    background = Canvas,
    surface = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    outline = Line,
)

@Composable
fun ReHealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        content = content,
    )
}
