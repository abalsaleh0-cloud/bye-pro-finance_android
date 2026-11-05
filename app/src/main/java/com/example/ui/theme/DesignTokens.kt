package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Android mirror of the canonical BYE PRO FINANCE web design tokens.
 *
 * Source of truth:
 * - packages/ui/src/tokens.css
 * - apps/web/src/unified-ui.css (theme and form-control overrides)
 *
 * Keep this file aligned with those web tokens. Android-only rendering values
 * (dp/sp) are converted from the web rem/px values at the 1:1 CSS pixel scale.
 */
object ByeProDesignTokens {
    object Colors {
        val background = Color(0xFF2A0F2E)
        val surface = Color(0xFF170918)
        val surfaceRaised = Color(0xFF4A1A45)
        val border = Color(0x66F6ECF5)
        val text = Color(0xFFF6ECF5)
        val muted = Color(0xFFCDB8CC)
        val accent = Color(0xFFF58BD6)
        val success = Color(0xFFA8EFC0)
        val danger = Color(0xFFFFB3BD)
    }

    object LightColors {
        val background = Color(0xFFFBF5FA)
        val surface = Color(0xFFF6E3F3)
        val surfaceRaised = Color(0xFFFFFFFF)
        val border = Color(0x336B4A69)
        val text = Color(0xFF1F0B22)
        val muted = Color(0xFF6B4A69)
        val accent = Color(0xFF9C1F86)
        val primaryAction = Color(0xFF9C1F86)
        val success = Color(0xFF15803D)
        val danger = Color(0xFFBE123C)
    }

    object Fields {
        val darkBackground = Color(0xFF170918)
        val darkText = Color(0xFFF6ECF5)
        val darkBorder = Color(0x66F6ECF5)
        val lightBackground = Color(0xFFFBF5FA)
        val lightText = Color(0xFF1F0B22)
        val lightBorder = Color(0x336B4A69)
        val focus = Colors.accent
        val darkGradientTop = Color(0xFF4A1A45)
        val darkGradientBottom = Color(0xFF170918)
        val orbPink = Color(0xFFF06BC8)
        val orbViolet = Color(0xFF7A3CFF)
        val orbBlue = Color(0xFF2A6BFF)
        val orbWarm = Color(0xFFFF9A4D)
        val cyan = Color(0xFF4FD6D0)
        val gold = Color(0xFFFADA53)
        val lavender = Color(0xFFB79BFF)
        val blue = Color(0xFF7FB2FF)
        val mint = Color(0xFF7FE0C0)
        val amber = Color(0xFFFFB86B)
        val income = Color(0xFF0F766E)
        val expense = Color(0xFFBE123C)
        val warning = Color(0xFFB45309)
        val info = Color(0xFF0E7490)
        val savings = Color(0xFF6D4AD0)
    }

    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
        val xxxl = 32.dp
    }

    object Radius {
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 28.dp
    }

    object Control {
        val height = 52.dp
        val contentMaxWidth = 1440.dp
        val focusRingWidth = 3.dp
    }

    object Breakpoints {
        val xs = 360.dp
        val sm = 480.dp
        val md = 640.dp
        val lg = 768.dp
        val xl = 1024.dp
        val xxl = 1280.dp
        val xxxl = 1536.dp
    }

    object Typography {
        val body = 16.sp
        val bodyLine = 24.sp
        val label = 14.sp
        val title = 22.sp
        val display = 30.sp
    }

    object Shadow {
        const val smallAlpha = 0.14f
        const val mediumAlpha = 0.20f
        const val focusRingAlpha = 0.32f
    }
}
