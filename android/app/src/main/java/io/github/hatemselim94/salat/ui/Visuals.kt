package io.github.hatemselim94.salat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.hatemselim94.salat.core.Prayer

/**
 * Visual language shared by the home screen: time-of-day colours, drawn glyphs,
 * the day-progress line. Everything here is vector drawing — no assets, no libraries.
 */

/** Period of the day, used to colour the next-prayer card. */
enum class Period { NIGHT, DAWN, MORNING, DAY, AFTERNOON, DUSK }

fun periodFor(now: Long, fajr: Long, sunrise: Long, dhuhr: Long, asr: Long, maghrib: Long, isha: Long): Period = when {
    now < fajr -> Period.NIGHT
    now < sunrise -> Period.DAWN
    now < dhuhr -> Period.MORNING
    now < asr -> Period.DAY
    now < maghrib -> Period.AFTERNOON
    now < isha -> Period.DUSK
    else -> Period.NIGHT
}

/** Gradient (start, end) and foreground for a period, light and dark variants. Soft, low-saturation. */
data class PeriodColors(val start: Color, val end: Color, val fg: Color, val fgMuted: Color)

fun periodColors(p: Period, dark: Boolean): PeriodColors = if (!dark) when (p) {
    Period.NIGHT -> PeriodColors(Color(0xFF243B4A), Color(0xFF1B2A36), Color(0xFFF2EFE6), Color(0xFFB9C4CC))
    Period.DAWN -> PeriodColors(Color(0xFFF6D9B8), Color(0xFFE8C2A0), Color(0xFF2B2520), Color(0xFF6F5F52))
    Period.MORNING -> PeriodColors(Color(0xFFDCEBF3), Color(0xFFC9E0EE), Color(0xFF1D2A33), Color(0xFF5F7280))
    Period.DAY -> PeriodColors(Color(0xFFE4F0EC), Color(0xFFD2E6DF), Color(0xFF1D1F1E), Color(0xFF5E6E68))
    Period.AFTERNOON -> PeriodColors(Color(0xFFF3E6CF), Color(0xFFEBD7B4), Color(0xFF2B2520), Color(0xFF6F6150))
    Period.DUSK -> PeriodColors(Color(0xFFE9C3A6), Color(0xFFC89A8A), Color(0xFF2B2020), Color(0xFF6E5450))
} else when (p) {
    Period.NIGHT -> PeriodColors(Color(0xFF1B2A36), Color(0xFF121C24), Color(0xFFECEBE6), Color(0xFF9AA8B3))
    Period.DAWN -> PeriodColors(Color(0xFF4B3A2E), Color(0xFF34281F), Color(0xFFF2E6D8), Color(0xFFBBA894))
    Period.MORNING -> PeriodColors(Color(0xFF2A3E4C), Color(0xFF1E2E39), Color(0xFFE6EEF3), Color(0xFF9FB0BC))
    Period.DAY -> PeriodColors(Color(0xFF223730), Color(0xFF1A2A25), Color(0xFFECEBE6), Color(0xFF96A09B))
    Period.AFTERNOON -> PeriodColors(Color(0xFF4A3D2A), Color(0xFF342B1E), Color(0xFFF2E8D6), Color(0xFFBDAE94))
    Period.DUSK -> PeriodColors(Color(0xFF4C3030), Color(0xFF352222), Color(0xFFF2E2DA), Color(0xFFC0A49C))
}

/** Animated brush for the next-prayer card. */
@Composable
fun periodBrush(period: Period): Pair<Brush, PeriodColors> {
    val dark = isSystemInDarkTheme()
    val target = periodColors(period, dark)
    val start by animateColorAsState(target.start, tween(900), label = "start")
    val end by animateColorAsState(target.end, tween(900), label = "end")
    return Brush.linearGradient(listOf(start, end), start = Offset.Zero, end = Offset(900f, 500f)) to target
}

/** Small glyph per prayer: crescent, sun on horizon, high sun, low sun, setting sun, moon+star. */
@Composable
fun PrayerGlyph(prayer: Prayer, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawPrayerGlyph(prayer, color) }
}

/** Crescent as a single filled path (outer arc + inner arc), so it works over any background. */
private fun DrawScope.crescent(color: Color, c: Offset, r: Float) {
    val path = Path().apply {
        moveTo(c.x + r * 0.55f, c.y - r * 0.75f)
        arcTo(Rect(c.x - r, c.y - r, c.x + r, c.y + r), -53.7f, -252.6f, false)
        val ic = Offset(c.x + r * 0.45f, c.y); val ir = r * 0.757f
        arcTo(Rect(ic.x - ir, ic.y - ir, ic.x + ir, ic.y + ir), 82.4f, 195.2f, false)
        close()
    }
    drawPath(path, color)
}

fun DrawScope.drawPrayerGlyph(prayer: Prayer, color: Color) {
    val w = size.width; val h = size.height
    val c = Offset(w / 2, h / 2)
    val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
    when (prayer) {
        Prayer.FAJR -> { // thin crescent, low, with a faint horizon
            crescent(color, Offset(c.x - w * 0.04f, c.y - h * 0.04f), w * 0.3f)
            drawLine(color.copy(alpha = color.alpha * 0.6f), Offset(w * 0.15f, h * 0.88f), Offset(w * 0.85f, h * 0.88f), stroke.width, StrokeCap.Round)
        }
        Prayer.SUNRISE -> { // half sun on a horizon line
            drawLine(color, Offset(w * 0.1f, h * 0.68f), Offset(w * 0.9f, h * 0.68f), stroke.width, StrokeCap.Round)
            drawArc(color, 180f, 180f, useCenter = true, topLeft = Offset(c.x - w * 0.24f, h * 0.68f - w * 0.24f), size = Size(w * 0.48f, w * 0.48f))
            rays(color, Offset(c.x, h * 0.68f), w * 0.34f, w * 0.44f, stroke.width, onlyTop = true)
        }
        Prayer.DHUHR -> { // full sun with rays
            drawCircle(color, w * 0.2f, c)
            rays(color, c, w * 0.3f, w * 0.42f, stroke.width, onlyTop = false)
        }
        Prayer.ASR -> { // smaller sun, lower right, shorter rays
            val cc = Offset(c.x + w * 0.05f, c.y + h * 0.08f)
            drawCircle(color, w * 0.17f, cc)
            rays(color, cc, w * 0.25f, w * 0.34f, stroke.width, onlyTop = false)
        }
        Prayer.MAGHRIB -> { // sun sinking below the line
            drawLine(color, Offset(w * 0.1f, h * 0.62f), Offset(w * 0.9f, h * 0.62f), stroke.width, StrokeCap.Round)
            drawArc(color, 180f, 180f, useCenter = true, topLeft = Offset(c.x - w * 0.2f, h * 0.62f - w * 0.14f), size = Size(w * 0.4f, w * 0.4f))
            drawLine(color, Offset(c.x - w * 0.14f, h * 0.8f), Offset(c.x + w * 0.14f, h * 0.8f), stroke.width, StrokeCap.Round)
        }
        Prayer.ISHA -> { // moon and a star
            crescent(color, Offset(c.x - w * 0.08f, c.y + h * 0.06f), w * 0.3f)
            drawCircle(color, w * 0.05f, Offset(c.x + w * 0.3f, c.y - h * 0.3f))
        }
    }
}

private fun DrawScope.rays(color: Color, c: Offset, r1: Float, r2: Float, width: Float, onlyTop: Boolean) {
    val angles = if (onlyTop) listOf(200.0, 240.0, 270.0, 300.0, 340.0) else listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)
    for (a in angles) {
        val rad = Math.toRadians(a)
        val dx = Math.cos(rad).toFloat(); val dy = Math.sin(rad).toFloat()
        drawLine(color, Offset(c.x + dx * r1, c.y + dy * r1), Offset(c.x + dx * r2, c.y + dy * r2), width, StrokeCap.Round)
    }
}

/**
 * Day-progress line: five dots for the obligatory prayers, filled up to the current one,
 * with a marker for "now" between them. Spans from today's Fajr to tomorrow's Fajr.
 */
@Composable
fun DayProgress(now: Long, times: List<Long>, nextFajr: Long, track: Color, fill: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val y = size.height / 2
        val pad = 6.dp.toPx()
        val x0 = pad; val x1 = size.width - pad
        val start = times.first(); val end = nextFajr
        fun xOf(t: Long): Float = x0 + ((t - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f) * (x1 - x0)
        drawLine(track, Offset(x0, y), Offset(x1, y), 3.dp.toPx(), StrokeCap.Round)
        val nowX = xOf(now)
        if (now > start) drawLine(fill, Offset(x0, y), Offset(nowX, y), 3.dp.toPx(), StrokeCap.Round)
        for (t in times) {
            val x = xOf(t)
            drawCircle(if (t <= now) fill else track, 4.dp.toPx(), Offset(x, y))
        }
        if (now in start..end) {
            drawCircle(fill, 6.dp.toPx(), Offset(nowX, y))
            drawCircle(Color.White, 2.5f.dp.toPx(), Offset(nowX, y))
        }
    }
}

/** Icons for the bottom row: calendar grid, compass, gear — drawn, ~20 lines each. */
enum class NavIcon { CALENDAR, COMPASS, GEAR }

@Composable
fun NavGlyph(icon: NavIcon, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val s = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        when (icon) {
            NavIcon.CALENDAR -> {
                val r = Rect(w * 0.12f, h * 0.18f, w * 0.88f, h * 0.9f)
                drawRoundRect(color, r.topLeft, r.size, CornerRadius(w * 0.1f), style = s)
                drawLine(color, Offset(r.left, h * 0.38f), Offset(r.right, h * 0.38f), s.width)
                for (i in 0..2) for (j in 0..1) drawCircle(color, w * 0.045f, Offset(r.left + w * 0.2f + i * w * 0.18f, h * 0.55f + j * h * 0.18f))
                drawLine(color, Offset(w * 0.32f, h * 0.08f), Offset(w * 0.32f, h * 0.26f), s.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.68f, h * 0.08f), Offset(w * 0.68f, h * 0.26f), s.width, StrokeCap.Round)
            }
            NavIcon.COMPASS -> {
                drawCircle(color, w * 0.4f, Offset(w / 2, h / 2), style = s)
                val p = Path().apply {
                    moveTo(w * 0.62f, h * 0.38f); lineTo(w * 0.44f, h * 0.44f); lineTo(w * 0.38f, h * 0.62f); lineTo(w * 0.56f, h * 0.56f); close()
                }
                drawPath(p, color)
            }
            NavIcon.GEAR -> {
                drawCircle(color, w * 0.14f, Offset(w / 2, h / 2), style = s)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val dx = Math.cos(a).toFloat(); val dy = Math.sin(a).toFloat()
                    drawLine(color, Offset(w / 2 + dx * w * 0.26f, h / 2 + dy * h * 0.26f), Offset(w / 2 + dx * w * 0.4f, h / 2 + dy * h * 0.4f), s.width, StrokeCap.Round)
                }
                drawCircle(color, w * 0.26f, Offset(w / 2, h / 2), style = Stroke(width = w * 0.08f))
            }
        }
    }
}
