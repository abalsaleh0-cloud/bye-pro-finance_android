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
        val background = Color(0xFF101114)
        val surface = Color(0xFF181A1F)
        val surfaceRaised = Color(0xFF1E1F24)
        val border = Color(0xFF2B2D34)
        val text = Color(0xFFF1F0EB)
        val muted = Color(0xFFA5A7B0)
        val accent = Color(0xFFD6B56D)
        val success = Color(0xFF67D6A1)
        val danger = Color(0xFFFF8585)
    }

    object LightColors {
        val background = Color(0xFFF4F5F7)
        val surface = Color(0xFFFFFFFF)
        val surfaceRaised = Color(0xFFF0F1F4)
        val border = Color(0xFFD9DCE3)
        val text = Color(0xFF20232A)
        val muted = Color(0xFF626875)
        // The web primary action remains the same brand gold in both themes.
        val accent = Color(0xFFD6B56D)
        val success = Color(0xFF67D6A1)
        val danger = Color(0xFFB42332)
    }

    object Fields {
        val darkBackground = Color(0xFF101115)
        val darkText = Color(0xFFF7F4EC)
        val darkBorder = Color(0xFF363740)
        val lightBackground = Color(0xFFFFFFFF)
        val lightText = Color(0xFF20232A)
        val lightBorder = Color(0xFFCBD0D9)
        val focus = Colors.accent
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
        val xl = 22.dp
    }

    object Control {
        val height = 44.dp
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
