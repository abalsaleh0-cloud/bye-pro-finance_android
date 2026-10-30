package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// BYE PRO FINANCE - Official Brand Color System (المنظومة اللونية الرسمية)
// ============================================================================

// --- Dark Mode Palette (الوضع الداكن المرجعي) ---
val DarkBgMain = Color(0xFF101114)           // خلفية التطبيق (#0B0A0C)
val DarkSurfaceMain = Color(0xFF181A1F)      // السطح الرئيسي (#111015)
val DarkCardMain = Color(0xFF1E1F24)         // البطاقات (#151119)
val DarkCardSecondary = Color(0xFF1E1F24)    // البطاقات الثانوية (#1C1721)
val DarkHoverInteractive = Color(0xFF1E1F24) // Hover / التحويم (#211C25)
val DarkBorderBase = Color(0xFF2B2D34)       // الحدود (#2A2530)
val DarkBorderProminent = Color(0xFF3A3341)  // الحدود القوية (#3A3341)
val DarkTextPrimary = Color(0xFFF1F0EB)      // النص الرئيسي (#EDE8DE)
val DarkTextSecondary = Color(0xFFA5A7B0)    // النص الثانوي (#D8D3CA)
val DarkTextMuted = Color(0xFFA5A7B0)        // النص الخافت (#948C7E)
val DarkTextNeutral = Color(0xFFA5A7B0)      // النص المحايد (#A5A7B0)

// --- Official Gold Accents (اللون الذهبي الرسمي) ---
val GoldPrimary = Color(0xFFD6B56D)          // الذهبي الأساسي (#D6B56D)
val GoldFeatured = Color(0xFFD4A62A)         // الذهبي المميز (#D4A62A)
val GoldMuted = Color(0xFFA67C12)            // الذهبي المكتوم (#A67C12)
val GoldDark = Color(0xFF8A6D1C)             // الذهبي الداكن (#8A6D1C)

// --- Semantic & Financial Status (الألوان المالية والدلالية) ---
val FinancialGreen = Color(0xFF67D6A1)       // الأخضر للدخل والنجاح المالي (#67D6A1)
val FinancialRed = Color(0xFFFF8585)         // الأحمر للمصروفات (#FF8585)
val StatusSuccessDark = Color(0xFF1F7A5C)    // النجاح الداكن (#1F7A5C)
val StatusErrorDark = Color(0xFFB42332)      // الأخطاء الداكن (#B42332)
val PositiveDarkGreen = StatusSuccessDark
val WarningDarkRed = StatusErrorDark

// --- Light Mode Palette (الوضع الفاتح المرجعي) ---
val LightBgMain = Color(0xFFF4F5F7)          // خلفية التطبيق (#F5F3EF)
val LightSurfaceMain = Color(0xFFFFFFFF)     // السطح الرئيسي (#FFFFFF)
val LightSurfaceSecondary = Color(0xFFF0F1F4)// السطح الثانوي (#FBFAF7)
val LightCardSecondary = Color(0xFFF0F1F4)   // البطاقات الثانوية (#F1EEE8)
val LightHoverInteractive = Color(0xFFF2F3F5)// Hover / التحويم (#E9E5DD)
val LightBorderBase = Color(0xFFD9DCE3)      // الحدود (#D9D4CA)
val LightBorderProminent = Color(0xFFCBD0D9) // الحدود القوية (#C8C1B5)
val LightTextPrimary = Color(0xFF20232A)     // النص الرئيسي (#202020)
val LightTextSecondary = Color(0xFF626875)   // النص الثانوي (#303030)
val LightTextMuted = Color(0xFF626875)       // النص الخافت (#6B665D)
val LightGoldPrimary = Color(0xFF8A671D)     // الذهبي الأساسي الفاتح (#8A671D)
val LightGoldFeatured = Color(0xFFA67C12)    // الذهبي المميز الفاتح (#A67C12)
val LightStatusSuccess = Color(0xFF16794A)   // النجاح الفاتح (#16794A)
val LightStatusError = Color(0xFFB42332)     // الأخطاء الفاتحة (#B42332)

// --- Field Color Tokens (ألوان الحقول الموحدة) ---
val DarkFieldBg = Color(0xFF101115)          // Dark Field Background (#101115)
val DarkFieldText = Color(0xFFF7F4EC)        // Dark Field Text (#F7F4EC)
val DarkFieldBorder = Color(0xFF363740)      // Dark Field Border (#363740)
val DarkFieldFocus = GoldPrimary             // Dark Field Focus (#D6B56D)

val LightFieldBg = Color(0xFFFFFFFF)         // Light Field Background (#FFFFFF)
val LightFieldText = Color(0xFF20232A)       // Light Field Text (#202020)
val LightFieldBorder = Color(0xFFCBD0D9)     // Light Field Border (#C8C9CF)
val LightFieldFocus = LightGoldFeatured      // Light Field Focus (#A67C12)

// --- Logo Spec Colors (شعار BYE PRO FINANCE) ---
val LogoBgDark = Color(0xFF101114)           // خلفية الشعار الداكنة (#101114)
val LogoGoldBorder = GoldFeatured            // إطار ودائرة الشعار الذهبية (#D4A62A)
val LogoIvoryIcon = Color(0xFFF2EEE5)         // الرمز المركزي العاجي (#F2EEE5)

// --- System & Compatibility Aliases ---
val DarkElementInner = DarkCardSecondary
val EmeraldMint = FinancialGreen
val EmeraldMintDark = PositiveDarkGreen
val ExecutiveGold = GoldPrimary
val ExecutiveCyan = GoldPrimary
val DangerRed = FinancialRed
val SuccessGreen = FinancialGreen
val WarningAmber = GoldPrimary
val InfoBlue = GoldPrimary
val DarkNavyBg = DarkBgMain
val MidnightSlate = DarkSurfaceMain
val NavyCardSurface = DarkCardMain
val CardBorder = DarkBorderBase
val TextPrimaryDark = DarkTextPrimary
val TextSecondaryDark = DarkTextSecondary
val TextMuted = DarkTextMuted

