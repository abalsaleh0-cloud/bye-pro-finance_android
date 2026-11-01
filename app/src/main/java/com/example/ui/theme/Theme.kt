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
    val bgMain: Color, val surfaceMain: Color, val cardMain: Color, val cardSecondary: Color,
    val elementInner: Color, val hoverInteractive: Color, val borderBase: Color, val borderProminent: Color,
    val textPrimary: Color, val textSecondary: Color, val textMuted: Color, val textNeutral: Color,
    val goldPrimary: Color, val goldFeatured: Color, val goldMuted: Color, val goldDark: Color,
    val financialGreen: Color, val financialRed: Color, val statusSuccess: Color, val statusError: Color,
    val fieldBg: Color, val fieldText: Color, val fieldBorder: Color, val fieldFocus: Color, val isDark: Boolean
)

val ByeProDarkTokens = ByeProColorTokens(
    DarkBgMain, DarkSurfaceMain, DarkCardMain, DarkCardSecondary, DarkCardSecondary, DarkHoverInteractive,
    DarkBorderBase, DarkBorderProminent, DarkTextPrimary, DarkTextSecondary, DarkTextMuted, DarkTextNeutral,
    GoldPrimary, GoldFeatured, GoldMuted, GoldDark, FinancialGreen, FinancialRed,
    StatusSuccessDark, StatusErrorDark, DarkFieldBg, DarkFieldText, DarkFieldBorder, DarkFieldFocus, true
)

val ByeProLightTokens = ByeProColorTokens(
    LightBgMain, LightSurfaceMain, LightSurfaceMain, LightCardSecondary, LightCardSecondary, LightHoverInteractive,
    LightBorderBase, LightBorderProminent, LightTextPrimary, LightTextSecondary, LightTextMuted, LightTextMuted,
    LightGoldPrimary, LightGoldFeatured, LightGoldFeatured, LightGoldPrimary, LightStatusSuccess, LightStatusError,
    LightStatusSuccess, LightStatusError, LightFieldBg, LightFieldText, LightFieldBorder, LightFieldFocus, false
)

val LocalByeProColors = staticCompositionLocalOf { ByeProDarkTokens }

object ByeProTheme {
    val colors: ByeProColorTokens
        @Composable get() = LocalByeProColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = DarkBgMain,
    primaryContainer = GoldDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = FinancialGreen,
    onSecondary = DarkBgMain,
    secondaryContainer = StatusSuccessDark,
    onSecondaryContainer = DarkTextSecondary,
    tertiary = GoldFeatured,
    background = DarkBgMain,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceMain,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardMain,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderBase,
    outlineVariant = DarkBorderProminent,
    error = StatusErrorDark,
    onError = DarkBgMain,
    errorContainer = StatusErrorDark,
    onErrorContainer = DarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightGoldPrimary,
    onPrimary = LightSurfaceMain,
    primaryContainer = LightCardSecondary,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightStatusSuccess,
    onSecondary = LightSurfaceMain,
    secondaryContainer = LightCardSecondary,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightGoldFeatured,
    background = LightBgMain,
    onBackground = LightTextPrimary,
    surface = LightSurfaceMain,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceSecondary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderBase,
    outlineVariant = LightBorderProminent,
    error = LightStatusError,
    onError = LightSurfaceMain,
    errorContainer = LightStatusError.copy(alpha = 0.10f),
    onErrorContainer = LightStatusError
)

@Composable
fun MyApplicationTheme(darkTheme: Boolean = true, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tokens = if (darkTheme) ByeProDarkTokens else ByeProLightTokens
    CompositionLocalProvider(LocalByeProColors provides tokens) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
