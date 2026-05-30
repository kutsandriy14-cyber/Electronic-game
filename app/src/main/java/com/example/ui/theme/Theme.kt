package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val VibrantColorScheme = darkColorScheme(
    primary = VibrantPrimary,
    onPrimary = VibrantOnPrimary,
    primaryContainer = VibrantPrimaryContainer,
    background = VibrantBackground,
    onBackground = VibrantOnBackground,
    surface = VibrantSurface,
    onSurface = VibrantOnBackground,
    surfaceVariant = VibrantSurface,
    onSurfaceVariant = VibrantOnSurfaceVariant,
    outline = VibrantOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for vibrant palette
    dynamicColor: Boolean = false, // Force custom vibrant palette
    content: @Composable () -> Unit,
) {
    val colorScheme = VibrantColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
