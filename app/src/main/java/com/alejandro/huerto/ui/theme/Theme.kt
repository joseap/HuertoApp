package com.alejandro.huerto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    secondary = OrangeAccent,
    background = GrayBackground,
    surface = GrayBackground,
    onPrimary = ColorSchemeDefaults.onPrimary,
    onSecondary = ColorSchemeDefaults.onSecondary
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimary,
    secondary = OrangeAccent
)

@Composable
fun HuertoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private object ColorSchemeDefaults {
    val onPrimary = androidx.compose.ui.graphics.Color.White
    val onSecondary = androidx.compose.ui.graphics.Color.White
}
