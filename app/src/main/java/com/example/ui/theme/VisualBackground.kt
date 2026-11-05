package com.example.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/** Canonical BYE PRO FINANCE background: three-stop gradient plus restrained radial glow. */
fun Modifier.byeVisualBackground(isDark: Boolean): Modifier = drawBehind {
    val palette = ByeProDesignTokens
    if (isDark) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.Fields.darkGradientTop,
                    palette.Fields.darkGradientMiddle,
                    palette.Fields.darkGradientBottom
                ),
                startY = 0f,
                endY = size.height
            )
        )
        val center = Offset(size.width * 0.52f, size.height * 0.08f)
        val radius = size.width * 0.72f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.Colors.orbIvory.copy(alpha = 0.20f),
                    palette.Colors.orbPink.copy(alpha = 0.13f),
                    palette.Colors.orbViolet.copy(alpha = 0.07f),
                    androidx.compose.ui.graphics.Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.LightColors.surface,
                    palette.LightColors.background,
                    palette.LightColors.background
                ),
                startY = 0f,
                endY = size.height
            )
        )
    }
}
