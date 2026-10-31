package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Android mirror of apps/web/src/advanced-finance.css and the shared Web control contract. */
object ByeProDesignTokens {
    object Colors {
        val background = Color(0xFF0B0A0C)
        val surface = Color(0xFF111015)
        val surfaceRaised = Color(0xFF1C1721)
        val surfaceHover = Color(0xFF211C25)
        val border = Color(0xFF2A2530)
        val borderStrong = Color(0xFF3A3341)
        val text = Color(0xFFEDE8DE)
        val textMain = Color(0xFFEDE8DE)
        val textSecondary = Color(0xFFD8D3CA)
        val muted = Color(0xFF948C7E)
        val accent = Color(0xFFD6B56D)
        val success = Color(0xFF67D6A1)
        val danger = Color(0xFFFF8585)
    }
    object LightColors {
        val background = Color(0xFFF5F3EF)
        val surface = Color(0xFFFFFFFF)
        val surfaceSecondary = Color(0xFFFBFAF7)
        val surfaceRaised = Color(0xFFF1EEE8)
        val surfaceHover = Color(0xFFE9E5DD)
        val border = Color(0xFFD9D4CA)
        val borderStrong = Color(0xFFC8C1B5)
        val text = Color(0xFF202020)
        val textSecondary = Color(0xFF303030)
        val muted = Color(0xFF6B665D)
        val mutedSecondary = Color(0xFF68645C)
        val accent = Color(0xFFD6B56D)
        val success = Color(0xFF16794A)
        val danger = Color(0xFFA43E45)
    }
    object Fields {
        val darkBackground = Color(0xFF101115)
        val darkText = Color(0xFFF7F4EC)
        val darkPlaceholder = Color(0xFFA5A7B0)
        val darkBorder = Color(0xFF363740)
        val lightBackground = Color(0xFFFFFFFF)
        val lightText = Color(0xFF202020)
        val lightPlaceholder = Color(0xFF8A857C)
        val lightBorder = Color(0xFFC8C1B5)
        val darkFocus = Color(0xFFD6B56D)
        val lightFocus = Color(0xFFA67C12)
        val height = 43.dp
        val radius = 9.dp
    }
    object Buttons {
        val darkSecondaryBackground = Color(0xFF211E18)
        val darkSecondaryBorder = Color(0xFF655638)
        val darkSecondaryText = Color(0xFFD6B56D)
        val lightSecondaryBackground = Color(0xFFF7F2E7)
        val lightSecondaryBorder = Color(0xFFB99B5E)
        val lightSecondaryText = Color(0xFF6C5420)
        val darkQuietBackground = Color(0xFF211E18)
        val darkQuietBorder = Color(0xFF655638)
        val darkQuietText = Color(0xFFD6B56D)
        val lightQuietBackground = Color(0xFFF7F2E7)
        val lightQuietBorder = Color(0xFFB99B5E)
        val lightQuietText = Color(0xFF6C5420)
    }
    object Dialog {
        val darkBorder = Color(0xFF40372A)
        val lightBorder = Color(0xFFD7D8DD)
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
