package com.example.babyneedscounter.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Dark-first palette: warm orange as **accent only**; surfaces stay neutral dark.
 * Use [AccentOrange] for CTAs, primary controls, feeding category, and thin highlights — not large backgrounds.
 * Text: [TextPrimary] / [TextSecondary] / [TextDisabled]; on filled orange buttons use dark [onPrimary] from the theme.
 */
object AppColors {
    // --- Surfaces ---
    val Background = Color(0xFF0E0E11)
    /** Cards, main elevated panels */
    val SurfaceCard = Color(0xFF1A1A1F)
    /** Secondary surfaces (chips, rows, subtle elevation) */
    val Surface = Color(0xFF222229)

    // --- Accent (warm orange; not for large backgrounds) ---
    val AccentOrange = Color(0xFFFF8A3D)
    val AccentOrangeSecondary = Color(0xFFFFB26B)
    val AccentGradientStart = Color(0xFFFF7A2F)
    val AccentGradientEnd = Color(0xFFFFB56B)

    // --- Text ---
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B0B5)
    val TextDisabled = Color(0xFF6E6E73)

    // --- Categories (meaning + small tints; avoid big blocks) ---
    /** Feeding — same as brand accent */
    val CategoryFeeding = AccentOrange
    /** Sleep — soft teal */
    val CategorySleep = Color(0xFF5EC4B8)
    /** Poop — muted brown (category only) */
    val CategoryPoop = Color(0xFF9A8472)
    /** Pee — soft blue */
    val CategoryPee = Color(0xFF7AB0E0)

    // --- Status ---
    val Success = Color(0xFF6FD490)
    val Warning = Color(0xFFFFAB47)
    val Error = Color(0xFFFF6B6B)
}

// Top-level aliases for concise call sites (`CategorySleep`, `TextSecondary`, …)
val AppBackground = AppColors.Background
val AppSurfaceCard = AppColors.SurfaceCard
val AppSurface = AppColors.Surface

val AccentOrange = AppColors.AccentOrange
val AccentOrangeSecondary = AppColors.AccentOrangeSecondary
val AccentGradientStart = AppColors.AccentGradientStart
val AccentGradientEnd = AppColors.AccentGradientEnd

val TextPrimary = AppColors.TextPrimary
val TextSecondary = AppColors.TextSecondary
val TextDisabled = AppColors.TextDisabled

val CategoryFeeding = AppColors.CategoryFeeding
val CategorySleep = AppColors.CategorySleep
val CategoryPoop = AppColors.CategoryPoop
val CategoryPee = AppColors.CategoryPee

val SuccessGreen = AppColors.Success
val WarningAmber = AppColors.Warning
val ErrorRed = AppColors.Error
