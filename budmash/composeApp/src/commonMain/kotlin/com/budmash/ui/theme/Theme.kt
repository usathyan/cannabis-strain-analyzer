package com.budmash.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = Teal80,
    tertiary = GreenGrey80,
    surface = SurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = Teal40,
    tertiary = GreenGrey40,
    surface = SurfaceLight
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
