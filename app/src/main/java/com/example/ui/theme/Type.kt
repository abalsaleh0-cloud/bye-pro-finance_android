package com.example.ui.theme

import androidx.compose.material3.Typography as MaterialTypography
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

private fun TextStyle.withTajawal(weight: FontWeight = FontWeight.Normal): TextStyle =
    copy(fontFamily = TajawalFontFamily, fontWeight = weight)

/** Apply Tajawal consistently to every Material 3 text role. */
val Typography = MaterialTypography().run {
    copy(
        displayLarge = displayLarge.withTajawal(FontWeight.Bold),
        displayMedium = displayMedium.withTajawal(FontWeight.Bold),
        displaySmall = displaySmall.withTajawal(FontWeight.Bold).copy(
            fontSize = ByeProDesignTokens.Typography.display,
            lineHeight = 36.sp
        ),
        headlineLarge = headlineLarge.withTajawal(FontWeight.Bold),
        headlineMedium = headlineMedium.withTajawal(FontWeight.Bold),
        headlineSmall = headlineSmall.withTajawal(FontWeight.Bold),
        titleLarge = titleLarge.withTajawal(FontWeight.Bold).copy(
            fontSize = ByeProDesignTokens.Typography.title,
            lineHeight = 28.sp
        ),
        titleMedium = titleMedium.withTajawal(FontWeight.Medium),
        titleSmall = titleSmall.withTajawal(FontWeight.Medium),
        bodyLarge = bodyLarge.withTajawal(FontWeight.Normal).copy(
            fontSize = ByeProDesignTokens.Typography.body,
            lineHeight = ByeProDesignTokens.Typography.bodyLine,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = bodyMedium.withTajawal(FontWeight.Normal).copy(
            fontSize = ByeProDesignTokens.Typography.label,
            lineHeight = 20.sp
        ),
        bodySmall = bodySmall.withTajawal(FontWeight.Normal),
        labelLarge = labelLarge.withTajawal(FontWeight.Medium),
        labelMedium = labelMedium.withTajawal(FontWeight.Medium),
        labelSmall = labelSmall.withTajawal(FontWeight.Medium)
    )
}
