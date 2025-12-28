package com.budmash.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = SurfaceDark,
    secondary = Gold80,
    onSecondary = SurfaceDark,
    tertiary = Teal80,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFB0B0B0)
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    secondary = Gold40,
    onSecondary = Color.Black,
    tertiary = Teal40,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF505050)
)

@Composable
fun BudMashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
