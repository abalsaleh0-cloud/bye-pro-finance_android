package com.example.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** BYE PRO FINANCE design tokens aligned with packages/ui/src/tokens.css on Web. */
object ByeProDesignTokens {
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
    }
}
