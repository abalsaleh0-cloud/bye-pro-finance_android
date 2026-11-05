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
    val fieldBg: Color, val fieldText: Color, val fieldBorder: Color, val fieldFocus: Color, val isDark: Boolean,
    val cyan: Color, val lavender: Color, val blue: Color, val mint: Color, val amber: Color,
    val coral: Color, val income: Color, val expense: Color, val warning: Color, val info: Color, val savings: Color
)

val ByeProDarkTokens = ByeProColorTokens(
    bgMain = DarkBgMain,
    surfaceMain = DarkSurfaceMain,
    cardMain = DarkCardMain,
    cardSecondary = DarkCardSecondary,
    elementInner = DarkElementInner,
    hoverInteractive = DarkHoverInteractive,
    borderBase = DarkBorderBase,
    borderProminent = DarkBorderProminent,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    textNeutral = DarkTextNeutral,
    goldPrimary = ByeProDesignTokens.Colors.accent,
    goldFeatured = ByeProDesignTokens.Colors.accent,
    goldMuted = ByeProDesignTokens.Colors.gold,
    goldDark = ByeProDesignTokens.Colors.gold,
    financialGreen = ByeProDesignTokens.Colors.mint,
    financialRed = ByeProDesignTokens.Colors.coral,
    statusSuccess = ByeProDesignTokens.Colors.success,
    statusError = ByeProDesignTokens.Colors.coral,
    fieldBg = DarkFieldBg,
    fieldText = DarkFieldText,
    fieldBorder = DarkFieldBorder,
    fieldFocus = ByeProDesignTokens.Colors.accent,
    isDark = true,
    cyan = ByeProDesignTokens.Colors.cyan,
    lavender = ByeProDesignTokens.Colors.lavender,
    blue = ByeProDesignTokens.Colors.blue,
    mint = ByeProDesignTokens.Colors.mint,
    amber = ByeProDesignTokens.Colors.amber,
    coral = ByeProDesignTokens.Colors.coral,
    income = ByeProDesignTokens.Colors.income,
    expense = ByeProDesignTokens.Colors.expense,
    warning = ByeProDesignTokens.Colors.warning,
    info = ByeProDesignTokens.Colors.info,
    savings = ByeProDesignTokens.Colors.savings
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
    goldPrimary = ByeProDesignTokens.LightColors.accent,
    goldFeatured = ByeProDesignTokens.LightColors.accent,
    goldMuted = ByeProDesignTokens.Colors.gold,
    goldDark = ByeProDesignTokens.Colors.gold,
    financialGreen = ByeProDesignTokens.LightColors.income,
    financialRed = ByeProDesignTokens.LightColors.expense,
    statusSuccess = ByeProDesignTokens.LightColors.success,
    statusError = ByeProDesignTokens.LightColors.expense,
    fieldBg = LightFieldBg,
    fieldText = LightFieldText,
    fieldBorder = LightFieldBorder,
    fieldFocus = ByeProDesignTokens.LightColors.accent,
    isDark = false,
    cyan = ByeProDesignTokens.Colors.cyan,
    lavender = ByeProDesignTokens.Colors.lavender,
    blue = ByeProDesignTokens.Colors.blue,
    mint = ByeProDesignTokens.Colors.mint,
    amber = ByeProDesignTokens.Colors.amber,
    coral = ByeProDesignTokens.Colors.coral,
    income = ByeProDesignTokens.LightColors.income,
    expense = ByeProDesignTokens.LightColors.expense,
    warning = ByeProDesignTokens.LightColors.warning,
    info = ByeProDesignTokens.LightColors.info,
    savings = ByeProDesignTokens.LightColors.savings
)

val LocalByeProColors = staticCompositionLocalOf { ByeProDarkTokens }

object ByeProTheme {
    val colors: ByeProColorTokens
        @Composable get() = LocalByeProColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = ByeProDesignTokens.Colors.accent,
    onPrimary = ByeProDesignTokens.Colors.onAccent,
    primaryContainer = ByeProDesignTokens.Colors.surfaceRaised,
    onPrimaryContainer = ByeProDesignTokens.Colors.text,
    secondary = ByeProDesignTokens.Colors.cyan,
    onSecondary = ByeProDesignTokens.Colors.surface,
    secondaryContainer = ByeProDesignTokens.Colors.mint,
    onSecondaryContainer = ByeProDesignTokens.Colors.background,
    tertiary = ByeProDesignTokens.Colors.lavender,
    onTertiary = ByeProDesignTokens.Colors.background,
    background = ByeProDesignTokens.Colors.background,
    onBackground = ByeProDesignTokens.Colors.text,
    surface = ByeProDesignTokens.Colors.surface,
    onSurface = ByeProDesignTokens.Colors.text,
    surfaceVariant = ByeProDesignTokens.Colors.surfaceRaised,
    onSurfaceVariant = ByeProDesignTokens.Colors.muted,
    outline = ByeProDesignTokens.Colors.border,
    outlineVariant = ByeProDesignTokens.Colors.border,
    error = ByeProDesignTokens.Colors.coral,
    onError = ByeProDesignTokens.Colors.background,
    errorContainer = ByeProDesignTokens.Colors.coral,
    onErrorContainer = ByeProDesignTokens.Colors.background
)

private val LightColorScheme = lightColorScheme(
    primary = ByeProDesignTokens.LightColors.accent,
    onPrimary = ByeProDesignTokens.LightColors.onAccent,
    primaryContainer = ByeProDesignTokens.LightColors.surface,
    onPrimaryContainer = ByeProDesignTokens.LightColors.text,
    secondary = ByeProDesignTokens.LightColors.income,
    onSecondary = Color.White,
    secondaryContainer = ByeProDesignTokens.LightColors.surface,
    onSecondaryContainer = ByeProDesignTokens.LightColors.text,
    tertiary = ByeProDesignTokens.LightColors.savings,
    onTertiary = Color.White,
    background = ByeProDesignTokens.LightColors.background,
    onBackground = ByeProDesignTokens.LightColors.text,
    surface = ByeProDesignTokens.LightColors.surface,
    onSurface = ByeProDesignTokens.LightColors.text,
    surfaceVariant = ByeProDesignTokens.LightColors.surfaceRaised,
    onSurfaceVariant = ByeProDesignTokens.LightColors.muted,
    outline = ByeProDesignTokens.LightColors.border,
    outlineVariant = ByeProDesignTokens.LightColors.border,
    error = ByeProDesignTokens.LightColors.expense,
    onError = Color.White,
    errorContainer = ByeProDesignTokens.LightColors.expense.copy(alpha = 0.12f),
    onErrorContainer = ByeProDesignTokens.LightColors.expense
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Dynamic system colors are intentionally disabled: the product palette is canonical.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tokens = if (darkTheme) ByeProDarkTokens else ByeProLightTokens
    CompositionLocalProvider(LocalByeProColors provides tokens) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
