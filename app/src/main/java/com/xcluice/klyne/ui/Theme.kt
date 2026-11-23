package com.xcluice.klyne.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.xcluice.klyne.core.SurfaceMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

@Immutable
data class KlynePalette(
    val scrimBackground: Color,
    val drawerBackground: Color,
    val cardSurface: Color,
    val dockSurface: Color,
    val dockBorder: Color,
    val pillSurface: Color,
    val borderSubtle: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val homeTextShadow: Color,
    val isDark: Boolean,
    val useGlassBlur: Boolean
)

@Composable
fun rememberKlynePalette(
    mode: SurfaceMode,
    darkTheme: Boolean,
    wallpaperSeed: Color
): KlynePalette {
    // Desaturate wallpaper seed to keep strictly balanced monochrome-friendly tint (never neon)
    val mutedSeed = lerp(
        wallpaperSeed,
        if (darkTheme) Color(0xFF7D838E) else Color(0xFF5A606B),
        0.62f
    )

    val targetScrim: Color
    val targetDrawer: Color
    val targetCard: Color
    val targetDock: Color
    val targetDockBorder: Color
    val targetPill: Color
    val targetBorder: Color
    val targetOnSurface: Color
    val targetOnMuted: Color
    val targetAccent: Color
    val targetOnAccent: Color

    when (mode) {
        SurfaceMode.FROSTED_GLASS -> {
            if (darkTheme) {
                targetScrim = Color(0x520B0C0E)
                targetDrawer = Color(0xCC111216)
                targetCard = Color(0xBF1A1C21)
                targetDock = Color(0x99181A20)
                targetDockBorder = Color(0x33FFFFFF)
                targetPill = Color(0xA61D1F25)
                targetBorder = Color(0x26FFFFFF)
                targetOnSurface = Color(0xFFF4F5F7)
                targetOnMuted = Color(0xFFA7ABB5)
                targetAccent = Color(0xFFE4E6EC)
                targetOnAccent = Color(0xFF101114)
            } else {
                targetScrim = Color(0x2EFFFFFF)
                targetDrawer = Color(0xD9F4F5F7)
                targetCard = Color(0xCCF7F8FA)
                targetDock = Color(0xB3F4F5F8)
                targetDockBorder = Color(0x66FFFFFF)
                targetPill = Color(0xCCF2F3F6)
                targetBorder = Color(0x22000000)
                targetOnSurface = Color(0xFF141518)
                targetOnMuted = Color(0xFF5B5F68)
                targetAccent = Color(0xFF1E2025)
                targetOnAccent = Color(0xFFF6F7F9)
            }
        }
        SurfaceMode.OLED_MATTE -> {
            if (darkTheme) {
                targetScrim = Color(0xE6000000)
                targetDrawer = Color(0xFF000000)
                targetCard = Color(0xFF121214)
                targetDock = Color(0xF20D0D0F)
                targetDockBorder = Color(0x24FFFFFF)
                targetPill = Color(0xFF161619)
                targetBorder = Color(0x1FFFFFFF)
                targetOnSurface = Color(0xFFF5F5F5)
                targetOnMuted = Color(0xFF9698A0)
                targetAccent = Color(0xFFECEEF2)
                targetOnAccent = Color(0xFF09090B)
            } else {
                targetScrim = Color(0xEBFAF9F6)
                targetDrawer = Color(0xFFF7F6F2)
                targetCard = Color(0xFFFFFFFF)
                targetDock = Color(0xF5FFFFFF)
                targetDockBorder = Color(0x1A000000)
                targetPill = Color(0xFFECEBE6)
                targetBorder = Color(0x1A000000)
                targetOnSurface = Color(0xFF121212)
                targetOnMuted = Color(0xFF606268)
                targetAccent = Color(0xFF18191C)
                targetOnAccent = Color(0xFFFAF9F6)
            }
        }
        SurfaceMode.MUTED_ADAPTIVE -> {
            if (darkTheme) {
                val base = lerp(Color(0xFF121316), mutedSeed, 0.22f)
                targetScrim = base.copy(alpha = 0.62f)
                targetDrawer = base.copy(alpha = 0.94f)
                targetCard = lerp(Color(0xFF1C1E22), mutedSeed, 0.26f)
                targetDock = lerp(Color(0xFF191B20), mutedSeed, 0.28f).copy(alpha = 0.86f)
                targetDockBorder = lerp(Color.White, mutedSeed, 0.4f).copy(alpha = 0.22f)
                targetPill = lerp(Color(0xFF21242A), mutedSeed, 0.28f).copy(alpha = 0.88f)
                targetBorder = Color(0x26FFFFFF)
                targetOnSurface = Color(0xFFF2F3F5)
                targetOnMuted = Color(0xFFAAB0BC)
                targetAccent = lerp(Color(0xFFDCE0E8), mutedSeed, 0.35f)
                targetOnAccent = Color(0xFF111317)
            } else {
                val base = lerp(Color(0xFFF3F4F6), mutedSeed, 0.14f)
                targetScrim = base.copy(alpha = 0.48f)
                targetDrawer = base.copy(alpha = 0.95f)
                targetCard = lerp(Color(0xFFFAFBFD), mutedSeed, 0.12f)
                targetDock = lerp(Color(0xFFF4F5F8), mutedSeed, 0.18f).copy(alpha = 0.88f)
                targetDockBorder = lerp(Color.Black, mutedSeed, 0.4f).copy(alpha = 0.12f)
                targetPill = lerp(Color(0xFFEBEDF1), mutedSeed, 0.18f)
                targetBorder = Color(0x1C000000)
                targetOnSurface = Color(0xFF15171C)
                targetOnMuted = Color(0xFF595F6C)
                targetAccent = lerp(Color(0xFF252830), mutedSeed, 0.35f)
                targetOnAccent = Color(0xFFF7F8FA)
            }
        }
    }

    val spec = tween<Color>(280)
    val scrim by animateColorAsState(targetScrim, spec, label = "scrim")
    val drawer by animateColorAsState(targetDrawer, spec, label = "drawer")
    val card by animateColorAsState(targetCard, spec, label = "card")
    val dock by animateColorAsState(targetDock, spec, label = "dock")
    val dockBorder by animateColorAsState(targetDockBorder, spec, label = "dockBorder")
    val pill by animateColorAsState(targetPill, spec, label = "pill")
    val border by animateColorAsState(targetBorder, spec, label = "border")
    val onSurface by animateColorAsState(targetOnSurface, spec, label = "onSurface")
    val onMuted by animateColorAsState(targetOnMuted, spec, label = "onMuted")
    val accent by animateColorAsState(targetAccent, spec, label = "accent")
    val onAccent by animateColorAsState(targetOnAccent, spec, label = "onAccent")

    return KlynePalette(
        scrimBackground = scrim,
        drawerBackground = drawer,
        cardSurface = card,
        dockSurface = dock,
        dockBorder = dockBorder,
        pillSurface = pill,
        borderSubtle = border,
        onSurface = onSurface,
        onSurfaceMuted = onMuted,
        accent = accent,
        onAccent = onAccent,
        homeTextShadow = if (darkTheme) Color(0x88000000) else Color(0x33000000),
        isDark = darkTheme,
        useGlassBlur = mode == SurfaceMode.FROSTED_GLASS
    )
}

/**
 * Authentic One UI 9 continuous superellipse squircle shape.
 */
object OneUiSquircleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val a = size.width / 2f
        val b = size.height / 2f
        val cx = a
        val cy = b
        val n = 4.2 // One UI superellipse curvature
        val exp = 2.0 / n
        val steps = 64
        for (i in 0..steps) {
            val theta = (2.0 * PI * i) / steps
            val ct = cos(theta)
            val st = sin(theta)
            val x = cx + (a * sign(ct) * abs(ct).pow(exp)).toFloat()
            val y = cy + (b * sign(st) * abs(st).pow(exp)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}
