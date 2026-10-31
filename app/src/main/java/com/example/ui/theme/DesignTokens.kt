package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Android mirror of packages/ui/src/tokens.css and apps/web/src/unified-ui.css. */
object ByeProDesignTokens {
    object Colors {
        val background = Color(0xFF101114)
        val surface = Color(0xFF181A1F)
        val surfaceRaised = Color(0xFF1E1F24)
        val border = Color(0xFF2B2D34)
        val text = Color(0xFFF1F0EB)
        val muted = Color(0xFFA5A7B0)
        val accent = Color(0xFFD6B56D)
        val success = Color(0xFF16794A)
        val danger = Color(0xFFB42332)
    }
    object LightColors {
        val background = Color(0xFFF4F5F7)
        val surface = Color(0xFFFFFFFF)
        val surfaceRaised = Color(0xFFF0F1F4)
        val border = Color(0xFFD9DCE3)
        val text = Color(0xFF20232A)
        val muted = Color(0xFF626875)
        val accent = Color(0xFF8A671D)
        val success = Color(0xFF67D6A1)
        val danger = Color(0xFFFF8585)
    }
    object Fields {
        val darkBackground = Color(0xFF101115)
        val darkText = Color(0xFFF7F4EC)
        val darkPlaceholder = Color(0xFFA5A7B0)
        val darkBorder = Color(0xFF363740)
        val lightBackground = Color(0xFFFFFFFF)
        val lightText = Color(0xFF20232A)
        val lightPlaceholder = Color(0xFF626875)
        val lightBorder = Color(0xFFCBD0D9)
        val darkFocus = Color(0xFFD6B56D)
        val lightFocus = Color(0xFF8A671D)
        val height = 44.dp
        val radius = 10.dp
    }
    object Buttons {
        val darkPrimaryBackground = Color(0xFFD6B56D)
        val darkPrimaryBorder = Color(0xFFE0C17E)
        val darkPrimaryText = Color(0xFF19160F)
        val lightPrimaryBackground = Color(0xFFD6B56D)
        val lightPrimaryBorder = Color(0xFFB99B5E)
        val lightPrimaryText = Color(0xFF17130B)
        val darkSecondaryBackground = Color(0xFF211E18)
        val darkSecondaryBorder = Color(0xFF655638)
        val darkSecondaryText = Color(0xFFD6B56D)
        val lightSecondaryBackground = Color(0xFFF7F2E7)
        val lightSecondaryBorder = Color(0xFFB99B5E)
        val lightSecondaryText = Color(0xFF6C5420)
        val darkQuietBackground = Color.Transparent
        val darkQuietBorder = Color.Transparent
        val darkQuietText = Color(0xFFF1F0EB)
        val lightQuietBackground = Color.Transparent
        val lightQuietBorder = Color.Transparent
        val lightQuietText = Color(0xFF20232A)
    }
    object Dialog {
        val darkBorder = Color(0xFF40372A)
        val lightBorder = Color(0xFFD9DCE3)
        val radius = 16.dp
    }
    object Spacing {
        val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp
        val xl = 20.dp; val xxl = 24.dp; val xxxl = 32.dp
    }
    object Radius {
        val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 22.dp
    }
    object Control {
        val height = 44.dp
        val contentMaxWidth = 1440.dp
        val focusRingWidth = 2.dp
    }
    object Breakpoints {
        val xs = 360.dp; val sm = 480.dp; val md = 640.dp; val lg = 768.dp
        val xl = 1024.dp; val xxl = 1280.dp; val xxxl = 1536.dp
    }
    object Typography {
        val body = 16.sp; val bodyLine = 24.sp; val label = 14.sp
        val title = 22.sp; val display = 30.sp
    }
    object Shadow {
        const val smallAlpha = 0.14f
        const val mediumAlpha = 0.20f
        const val focusRingAlpha = 0.32f
    }
}
