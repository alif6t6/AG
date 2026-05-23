package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DevilRed,
    onPrimary = Color.White,
    primaryContainer = DevilRedDim,
    onPrimaryContainer = DevilRed,
    secondary = TextGrey,
    onSecondary = Color.White,
    tertiary = DarkSurface3,
    background = DarkBg,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = TextGrey,
    outline = BorderColor
)

private val LightColorScheme = lightColorScheme(
    primary = DevilRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD6D4),
    onPrimaryContainer = DevilRed,
    secondary = Color(0xFF555555),
    onSecondary = Color.Black,
    tertiary = Color(0xFFF0F0F0),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF0A0A0A),
    surface = Color.White,
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFFE5E5E5)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to Dark Mode requested of DEVIL GPT!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
