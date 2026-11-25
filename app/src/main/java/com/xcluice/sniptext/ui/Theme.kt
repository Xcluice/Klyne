package com.xcluice.sniptext.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SnipColors {
    val Bg = Color(0xFF111214)
    val Surface = Color(0xFF181A1F)
    val SurfaceElevated = Color(0xFF20232A)
    val Border = Color(0xFF2B2F38)
    val TextPrimary = Color(0xFFF4F5F7)
    val TextSecondary = Color(0xFF9AA0A6)
    val Accent = Color(0xFFCBD5E1)
    val AccentMuted = Color(0xFF64748B)
    val ActiveDot = Color(0xFFE2E8F0)
}

private val MonochromeScheme = darkColorScheme(
    primary = SnipColors.TextPrimary,
    onPrimary = SnipColors.Bg,
    secondary = SnipColors.Accent,
    onSecondary = SnipColors.Bg,
    background = SnipColors.Bg,
    onBackground = SnipColors.TextPrimary,
    surface = SnipColors.Surface,
    onSurface = SnipColors.TextPrimary,
    surfaceVariant = SnipColors.SurfaceElevated,
    onSurfaceVariant = SnipColors.TextSecondary,
    outline = SnipColors.Border
)

@Composable
fun SnipTextTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonochromeScheme,
        content = content
    )
}
