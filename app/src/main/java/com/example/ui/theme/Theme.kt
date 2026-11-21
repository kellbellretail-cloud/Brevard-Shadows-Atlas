package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShadowAtlasColorScheme = darkColorScheme(
    primary = PhosphorCyan,
    onPrimary = Color(0xFF002026),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFFB2EBF2),
    secondary = SpectralEmerald,
    onSecondary = Color(0xFF002910),
    secondaryContainer = Color(0xFF0D5329),
    onSecondaryContainer = Color(0xFFB9F6CA),
    tertiary = GhostAmber,
    onTertiary = Color(0xFF332300),
    tertiaryContainer = Color(0xFF5E4200),
    onTertiaryContainer = Color(0xFFFFE082),
    background = MidnightObsidian,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = WarningCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Paranormal investigation atlas is immersive dark-mode first
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ShadowAtlasColorScheme,
        typography = Typography,
        content = content
    )
}
