package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Tajawal family bundled locally under the SIL Open Font License. */
val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold)
)

/** Typography mapped to the shared BYE PRO FINANCE design token scale. */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = TajawalFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = ByeProDesignTokens.Typography.body,
        lineHeight = ByeProDesignTokens.Typography.bodyLine,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = TajawalFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = ByeProDesignTokens.Typography.label,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = TajawalFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = ByeProDesignTokens.Typography.title,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = ByeProDesignTokens.Typography.display,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
)
