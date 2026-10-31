package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ByeProColorTokens(
    val bgMain: Color,
    val surfaceMain: Color,
    val cardMain: Color,
    val cardSecondary: Color,
    val elementInner: Color,
    val hoverInteractive: Color,
    val borderBase: Color,
    val borderProminent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textNeutral: Color,
    val goldPrimary: Color,
    val goldFeatured: Color,
    val primaryButtonBg: Color,
    val goldMuted: Color,
    val goldDark: Color,
    val financialGreen: Color,
    val financialRed: Color,
    val statusSuccess: Color,
    val statusError: Color,
    val fieldBg: Color,
    val fieldText: Color,
    val fieldBorder: Color,
    val fieldFocus: Color,
    val quietButtonBg: Color,
    val quietButtonBorder: Color,
    val quietButtonText: Color,
    val dialogBorder: Color,
    val isDark: Boolean
)

val ByeProDarkTokens = ByeProColorTokens(
    bgMain = DarkBgMain,
    surfaceMain = DarkSurfaceMain,
    cardMain = DarkCardMain,
    cardSecondary = DarkCardSecondary,
    elementInner = DarkCardSecondary,
    hoverInteractive = DarkHoverInteractive,
    borderBase = DarkBorderBase,
    borderProminent = DarkBorderProminent,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    textNeutral = DarkTextNeutral,
    goldPrimary = GoldPrimary,
    goldFeatured = GoldFeatured,
    primaryButtonBg = GoldPrimary,
    goldMuted = GoldMuted,
    goldDark = GoldDark,
    financialGreen = FinancialGreen,
    financialRed = FinancialRed,
    statusSuccess = StatusSuccessDark,
    statusError = StatusErrorDark,
    fieldBg = DarkFieldBg,
    fieldText = DarkFieldText,
    fieldBorder = DarkFieldBorder,
    fieldFocus = DarkFieldFocus,
    quietButtonBg = GoldDark,
    quietButtonBorder = GoldMuted,
    quietButtonText = GoldPrimary,
    dialogBorder = Color(0xFF40372A),
    isDark = true
)

val ByeProLightTokens = ByeProColorTokens(
    bgMain = LightBgMain,
    surfaceMain = LightSurfaceMain,
    cardMain = LightSurfaceMain,
    cardSecondary = LightCardSecondary,
    elementInner = LightCardSecondary,
    hoverInteractive = LightHoverInteractive,
    borderBase = LightBorderBase,
    borderProminent = LightBorderProminent,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    textNeutral = LightTextMuted,
    goldPrimary = LightGoldPrimary,
    goldFeatured = LightGoldFeatured,
    primaryButtonBg = GoldPrimary,
    goldMuted = LightGoldFeatured,
    goldDark = LightGoldPrimary,
    financialGreen = LightStatusSuccess,
    financialRed = LightStatusError,
    statusSuccess = LightStatusSuccess,
    statusError = LightStatusError,
    fieldBg = LightFieldBg,
    fieldText = LightFieldText,
    fieldBorder = LightFieldBorder,
    fieldFocus = LightFieldFocus,
    quietButtonBg = Color(0xFFF7F2E7),
    quietButtonBorder = Color(0xFFB99B5E),
    quietButtonText = Color(0xFF6C5420),
    dialogBorder = Color(0xFFD7D8DD),
    isDark = false
)

val LocalByeProColors = staticCompositionLocalOf {
    ByeProDarkTokens
}

object ByeProTheme {
    val colors: ByeProColorTokens
        @Composable
        get() = LocalByeProColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF17130B),
    primaryContainer = GoldDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = FinancialGreen,
    onSecondary = Color(0xFF101114),
    secondaryContainer = StatusSuccessDark,
    onSecondaryContainer = DarkTextSecondary,
    tertiary = GoldPrimary,
    background = DarkBgMain,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceMain,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardMain,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderBase,
    outlineVariant = DarkBorderProminent,
    error = FinancialRed,
    onError = Color(0xFF101115),
    errorContainer = Color(0xFF7E3035),
    onErrorContainer = DarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightGoldPrimary,
    onPrimary = Color(0xFF17130B),
    primaryContainer = LightCardSecondary,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightStatusSuccess,
    onSecondary = Color.White,
    secondaryContainer = LightCardSecondary,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightGoldFeatured,
    background = LightBgMain,
    onBackground = LightTextPrimary,
    surface = LightSurfaceMain,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceSecondary,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorderBase,
    outlineVariant = LightBorderProminent,
    error = LightStatusError,
    onError = Color.White,
    errorContainer = Color(0xFFF9E7E9),
    onErrorContainer = LightStatusError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tokens = if (darkTheme) ByeProDarkTokens else ByeProLightTokens

    CompositionLocalProvider(LocalByeProColors provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
