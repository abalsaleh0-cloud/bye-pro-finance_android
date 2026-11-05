package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Static branded orb artwork. Animation is intentionally kept out of the global theme. */
@Composable
fun ByeOrb(modifier: Modifier = Modifier, size: Dp = 184.dp) {
    val palette = ByeProDesignTokens.Colors
    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width * 0.5f, this.size.height * 0.5f)
        val radius = this.size.minDimension * 0.5f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.orbIvory,
                    palette.orbPink,
                    palette.orbViolet,
                    palette.orbBlue,
                    palette.orbWarm
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.orbIvory.copy(alpha = 0.30f), androidx.compose.ui.graphics.Color.Transparent),
                center = Offset(center.x - radius * 0.28f, center.y - radius * 0.32f),
                radius = radius * 0.55f
            ),
            radius = radius,
            center = center
        )
    }
}
