package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val F1DarkColorScheme = darkColorScheme(
    primary = F1Red,
    onPrimary = F1TextPrimary,
    primaryContainer = F1RedDark,
    onPrimaryContainer = F1TextPrimary,
    secondary = ElectricCyan,
    onSecondary = CarbonBackground,
    secondaryContainer = CarbonCardElevated,
    onSecondaryContainer = ElectricCyan,
    tertiary = FlagYellow,
    onTertiary = CarbonBackground,
    background = CarbonBackground,
    onBackground = F1TextPrimary,
    surface = CarbonSurface,
    onSurface = F1TextPrimary,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = F1TextSecondary,
    surfaceContainerLowest = CarbonBackground,
    surfaceContainerLow = CarbonSurface,
    surfaceContainer = CarbonCard,
    surfaceContainerHigh = CarbonCardElevated,
    surfaceContainerHighest = CarbonCardElevated,
    outline = CarbonDivider,
    outlineVariant = CarbonDivider
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep consistent racing dark identity for optimal telemetry readability
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = F1DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
