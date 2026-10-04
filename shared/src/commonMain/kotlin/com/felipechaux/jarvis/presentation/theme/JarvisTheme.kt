package com.felipechaux.jarvis.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object JarvisColors {
    val Background = Color(0xFF020408)
    val BackgroundSecondary = Color(0xFF050D18)
    val BackgroundTertiary = Color(0xFF0A1628)
    val Surface = Color(0xFF060E1C)
    val SurfaceElevated = Color(0xFF0D1D35)
    val SurfaceGlass = Color(0xE6060E1C)

    val Accent = Color(0xFF00D4FF) // Arc Reactor Cyan
    val AccentHover = Color(0xFF33DDFF)
    val AccentGlow = Color(0x5900D4FF)
    val AccentSubtle = Color(0x1A00D4FF)

    val AccentAmber = Color(0xFFFF9F00)
    val AccentAmberSubtle = Color(0x1AFF9F00)
    val AccentPurple = Color(0xFF7B5FFF)

    val Success = Color(0xFF00FFB3)
    val Warning = Color(0xFFFF9F00)
    val Error = Color(0xFFFF4466)
    val ErrorSubtle = Color(0x26FF4466)

    val TextPrimary = Color(0xFFD6EEFF)
    val TextSecondary = Color(0xFF4D8FA8)
    val TextTertiary = Color(0xFF2A5970)

    val Border = Color(0x2600D4FF)
    val BorderSubtle = Color(0x1200D4FF)
    val BorderHighlight = Color(0x6600D4FF)

    val UserBubble = Color(0x1F00D4FF)
    val UserBubbleBorder = Color(0x3D00D4FF)
    val UserBubbleText = Color(0xFFCCEEFF)

    val CodeBackground = Color(0xFF030A14)
}

val LocalJarvisColors = staticCompositionLocalOf { JarvisColors }

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisColors.Accent,
    onPrimary = Color(0xFF020408),
    primaryContainer = JarvisColors.UserBubble,
    onPrimaryContainer = JarvisColors.UserBubbleText,

    secondary = JarvisColors.AccentAmber,
    onSecondary = Color(0xFF020408),
    secondaryContainer = JarvisColors.BackgroundTertiary,
    onSecondaryContainer = JarvisColors.TextPrimary,

    tertiary = JarvisColors.AccentPurple,
    onTertiary = Color.White,

    background = JarvisColors.Background,
    onBackground = JarvisColors.TextPrimary,

    surface = JarvisColors.Surface,
    onSurface = JarvisColors.TextPrimary,
    surfaceVariant = JarvisColors.BackgroundTertiary,
    onSurfaceVariant = JarvisColors.TextSecondary,

    outline = JarvisColors.Border,
    outlineVariant = JarvisColors.BorderSubtle,

    error = JarvisColors.Error,
    onError = Color.White,
)

/**
 * OpenJarvis global theme. Always dark HUD sci-fi aesthetic matching OpenJarvis desktop.
 */
@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalJarvisColors provides JarvisColors) {
        MaterialTheme(
            colorScheme = JarvisDarkColorScheme,
            content = content,
        )
    }
}
