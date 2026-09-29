package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CivicTealPrimaryDark,
    onPrimary = Color(0xFF00372E),
    primaryContainer = CivicTealDark,
    onPrimaryContainer = CivicTealContainer,
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF0D47A1),
    background = CivicBackgroundDark,
    surface = CivicSurfaceDark,
    surfaceVariant = CivicSurfaceVariantDark,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9),
    outline = Color(0xFF64748B),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = CivicTealPrimary,
    onPrimary = CivicOnTeal,
    primaryContainer = CivicTealLight,
    onPrimaryContainer = CivicTealDark,
    secondary = CivicBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = CivicBlueContainer,
    tertiary = CivicAmberAccent,
    background = CivicNeutralBackground,
    surface = CivicNeutralSurface,
    surfaceVariant = CivicNeutralSurfaceVariant,
    onBackground = CivicTextPrimary,
    onSurface = CivicTextPrimary,
    outline = CivicBorder,
    error = CivicError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our civic branding by default for high visual identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
