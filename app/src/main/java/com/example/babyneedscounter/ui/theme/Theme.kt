package com.example.babyneedscounter.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = AppBackground,
    primaryContainer = Color(0xFF2E2620),
    onPrimaryContainer = AccentOrangeSecondary,

    secondary = AccentOrangeSecondary,
    onSecondary = AppBackground,
    secondaryContainer = Color(0xFF2F2A26),
    onSecondaryContainer = AccentOrangeSecondary,

    tertiary = CategorySleep,
    onTertiary = AppBackground,
    tertiaryContainer = Color(0xFF1A2C2A),
    onTertiaryContainer = CategorySleep,

    background = AppBackground,
    onBackground = TextPrimary,

    surface = AppSurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = AppSurface,
    onSurfaceVariant = TextSecondary,

    outline = TextDisabled,
    outlineVariant = Color(0xFF3D3D44),

    error = ErrorRed,
    onError = AppBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0CC),
    onPrimaryContainer = Color(0xFF3D2100),

    secondary = AccentOrangeSecondary,
    onSecondary = Color(0xFF3D2100),
    secondaryContainer = Color(0xFFFFE8D6),
    onSecondaryContainer = Color(0xFF3D2100),

    tertiary = CategorySleep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5F5F0),
    onTertiaryContainer = Color(0xFF004D45),

    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF1A1A1F),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1F),
    surfaceVariant = Color(0xFFEBEBEF),
    onSurfaceVariant = Color(0xFF5C5C64),

    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun BabyNeedsCounterTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
