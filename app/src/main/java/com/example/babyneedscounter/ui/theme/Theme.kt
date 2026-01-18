package com.example.babyneedscounter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BabyBlue,
    onPrimary = DarkBackground,
    primaryContainer = BabyBlueLight,
    onPrimaryContainer = DarkBackground,
    
    secondary = SoftPink,
    onSecondary = DarkBackground,
    secondaryContainer = SoftPink,
    onSecondaryContainer = DarkBackground,
    
    tertiary = MintGreen,
    onTertiary = DarkBackground,
    
    background = DarkBackground,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    error = ErrorRed,
    onError = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = BabyBlue,
    secondary = SoftPink,
    tertiary = MintGreen
)

@Composable
fun BabyNeedsCounterTheme(
    darkTheme: Boolean = true,
    // Disable dynamic color for consistent professional look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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
        content = content
    )
}