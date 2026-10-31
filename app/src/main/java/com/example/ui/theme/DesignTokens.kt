package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Android mirror of packages/ui/src/tokens.css and the Web global control contract. */
object ByeProDesignTokens {
    object Colors {
        val background = Color(0xFF0B0A0C)
        val surface = Color(0xFF181A1F)
        val surfaceRaised = Color(0xFF1E1F24)
        val border = Color(0xFF2A2530)
        val text = Color(0xFFEDE8DE)
        val muted = Color(0xFF948C7E)
        val accent = Color(0xFFD6B56D)
        val success = Color(0xFF67D6A1)
        val danger = Color(0xFFFF8585)
    }
    object LightColors {
        val background = Color(0xFFF5F3EF)
        val surface = Color(0xFFFFFFFF)
        val surfaceRaised = Color(0xFFF1EEE8)
        val border = Color(0xFFD9D4CA)
        val text = Color(0xFF202020)
        val muted = Color(0xFF6B665D)
        val accent = Color(0xFFD6B56D)
        val success = Color(0xFF16794A)
        val danger = Color(0xFFB42332)
    }
    object Fields {
        val darkBackground = Color(0xFF101115)
        val darkText = Color(0xFFF7F4EC)
        val darkBorder = Color(0xFF363740)
        val lightBackground = Color(0xFFFFFFFF)
        val lightText = Color(0xFF202020)
        val lightBorder = Color(0xFFC8C1B5)
        val focus = Color(0xFFA67C12)
        val height = 43.dp
        val radius = 9.dp
    }
    object Buttons {
        val darkSecondaryBackground = Color(0xFF1E1F24)
        val darkSecondaryBorder = Color(0xFF2A2530)
        val darkSecondaryText = Color(0xFFEDE8DE)
        val lightSecondaryBackground = Color(0xFFF1EEE8)
        val lightSecondaryBorder = Color(0xFFD9D4CA)
        val lightSecondaryText = Color(0xFF202020)
        val darkQuietBackground = Color.Transparent
        val darkQuietBorder = Color.Transparent
        val darkQuietText = Color(0xFFEDE8DE)
        val lightQuietBackground = Color.Transparent
        val lightQuietBorder = Color.Transparent
        val lightQuietText = Color(0xFF202020)
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
