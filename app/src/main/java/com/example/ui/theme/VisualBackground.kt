package com.example.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Shared branded gradient and soft ambient orb used by the Phase 1 entry screens. */
fun Modifier.byeVisualBackground(isDark: Boolean): Modifier = drawBehind {
    val gradient = if (isDark) {
        listOf(Color(0xFF4A1A45), Color(0xFF2A0F2E), Color(0xFF170918))
    } else {
        listOf(Color(0xFFF6E3F3), Color(0xFFFBF5FA))
    }
    drawRect(Brush.verticalGradient(gradient, startY = 0f, endY = size.height))
    if (isDark) {
        val center = Offset(size.width * 0.52f, size.height * 0.08f)
        val radius = size.width * 0.72f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FFE0F5), Color(0x44F06BC8), Color(0x227A3CFF), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}
