package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0x3322D3EE),
    onPrimaryContainer = CyanPrimary,
    secondary = TealLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0x330D9488),
    onSecondaryContainer = TealLight,
    tertiary = NeonPurple,
    onTertiary = Color.White,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassBorderCyan,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ScanProTheme(
    darkTheme: Boolean = true, // Premium futuristic dark theme by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
