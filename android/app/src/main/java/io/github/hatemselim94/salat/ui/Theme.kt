package io.github.hatemselim94.salat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Same palette as the Chrome extension and the Qt app.
object Palette {
    val LightBg = Color(0xFFFBFAF7)
    val LightFg = Color(0xFF1D1F1E)
    val LightMuted = Color(0xFF6B7470)
    val LightLine = Color(0xFFE6E3DC)
    val LightCard = Color(0xFFFFFFFF)
    val LightAccent = Color(0xFF2F6F5E)
    val LightAccentSoft = Color(0xFFE4F0EC)

    val DarkBg = Color(0xFF161A19)
    val DarkFg = Color(0xFFECEBE6)
    val DarkMuted = Color(0xFF96A09B)
    val DarkLine = Color(0xFF2A302E)
    val DarkCard = Color(0xFF1E2422)
    val DarkAccent = Color(0xFF5FB49C)
    val DarkAccentSoft = Color(0xFF223730)
}

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Palette.LightAccent,
    onPrimary = Color.White,
    primaryContainer = Palette.LightAccentSoft,
    onPrimaryContainer = Palette.LightFg,
    background = Palette.LightBg,
    onBackground = Palette.LightFg,
    surface = Palette.LightCard,
    onSurface = Palette.LightFg,
    surfaceVariant = Palette.LightCard,
    onSurfaceVariant = Palette.LightMuted,
    outline = Palette.LightLine,
    outlineVariant = Palette.LightLine,
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Palette.DarkAccent,
    onPrimary = Color(0xFF0F1513),
    primaryContainer = Palette.DarkAccentSoft,
    onPrimaryContainer = Palette.DarkFg,
    background = Palette.DarkBg,
    onBackground = Palette.DarkFg,
    surface = Palette.DarkCard,
    onSurface = Palette.DarkFg,
    surfaceVariant = Palette.DarkCard,
    onSurfaceVariant = Palette.DarkMuted,
    outline = Palette.DarkLine,
    outlineVariant = Palette.DarkLine,
)

@Composable
fun SalatTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme, content = content)
}
