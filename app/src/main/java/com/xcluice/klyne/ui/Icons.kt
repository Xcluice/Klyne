package com.xcluice.klyne.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SearchIcon(tint: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = this.size.minDimension * 0.11f, cap = StrokeCap.Round)
        val r = this.size.minDimension * 0.31f
        val center = Offset(this.size.width * 0.44f, this.size.height * 0.44f)
        drawCircle(color = tint, radius = r, center = center, style = stroke)
        drawLine(
            color = tint,
            start = Offset(center.x + r * 0.72f, center.y + r * 0.72f),
            end = Offset(this.size.width * 0.86f, this.size.height * 0.86f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SettingsIcon(tint: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val sw = this.size.minDimension * 0.1f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = tint, radius = this.size.minDimension * 0.18f, center = c, style = Stroke(sw))
        drawCircle(color = tint, radius = this.size.minDimension * 0.36f, center = c, style = Stroke(sw))
        for (i in 0 until 6) {
            val angle = Math.toRadians(i * 60.0)
            val x1 = c.x + (kotlin.math.cos(angle) * this.size.minDimension * 0.36f).toFloat()
            val y1 = c.y + (kotlin.math.sin(angle) * this.size.minDimension * 0.36f).toFloat()
            val x2 = c.x + (kotlin.math.cos(angle) * this.size.minDimension * 0.46f).toFloat()
            val y2 = c.y + (kotlin.math.sin(angle) * this.size.minDimension * 0.46f).toFloat()
            drawLine(tint, Offset(x1, y1), Offset(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun CloseIcon(tint: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val sw = this.size.minDimension * 0.11f
        val p = this.size.minDimension * 0.24f
        drawLine(tint, Offset(p, p), Offset(this.size.width - p, this.size.height - p), sw, StrokeCap.Round)
        drawLine(tint, Offset(this.size.width - p, p), Offset(p, this.size.height - p), sw, StrokeCap.Round)
    }
}

@Composable
fun BackIcon(tint: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val sw = this.size.minDimension * 0.11f
        val path = Path().apply {
            moveTo(this@Canvas.size.width * 0.62f, this@Canvas.size.height * 0.22f)
            lineTo(this@Canvas.size.width * 0.32f, this@Canvas.size.height * 0.50f)
            lineTo(this@Canvas.size.width * 0.62f, this@Canvas.size.height * 0.78f)
        }
        drawPath(path, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun PlusIcon(tint: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val sw = this.size.minDimension * 0.11f
        val c = this.size.minDimension / 2f
        val m = this.size.minDimension * 0.22f
        drawLine(tint, Offset(c, m), Offset(c, this.size.height - m), sw, StrokeCap.Round)
        drawLine(tint, Offset(m, c), Offset(this.size.width - m, c), sw, StrokeCap.Round)
    }
}

@Composable
fun KlyneLogoMark(tint: Color, accentTint: Color, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.16f),
            size = Size(w * 0.16f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
        )
        val upper = Path().apply {
            moveTo(w * 0.62f, h * 0.16f)
            lineTo(w * 0.82f, h * 0.16f)
            lineTo(w * 0.50f, h * 0.50f)
            lineTo(w * 0.34f, h * 0.50f)
            close()
        }
        drawPath(upper, tint)
        val lower = Path().apply {
            moveTo(w * 0.46f, h * 0.47f)
            lineTo(w * 0.60f, h * 0.47f)
            lineTo(w * 0.84f, h * 0.84f)
            lineTo(w * 0.66f, h * 0.84f)
            close()
        }
        drawPath(lower, accentTint)
    }
}
