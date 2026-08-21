package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = TextDark,
    primaryContainer = JarvisDarkSurfaceVariant,
    onPrimaryContainer = JarvisCyanBright,
    secondary = JarvisStarkGold,
    onSecondary = TextDark,
    secondaryContainer = JarvisDarkSurface,
    onSecondaryContainer = JarvisStarkGold,
    tertiary = JarvisElectricBlue,
    onTertiary = Color.White,
    background = JarvisDarkBg,
    onBackground = TextPrimary,
    surface = JarvisDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = JarvisDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = JarvisBorderGlow,
    error = JarvisRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
