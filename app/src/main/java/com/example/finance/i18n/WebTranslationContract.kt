package com.example.finance.i18n

import androidx.compose.ui.unit.LayoutDirection
import com.example.finance.model.AppLanguage

/** Cross-platform translation contract matching the BYE PRO FINANCE Web locale rules. */
object WebTranslationContract {
    const val LOCALE_KEY = "bye-pro-finance.locale"
    const val AR = "ar"
    const val EN = "en"

    val stableWebKeys = setOf("dashboard", "accounts", "transactions", "settings")

    fun text(key: String, language: AppLanguage): String = when (key) {
        "dashboard" -> Strings.get("dashboard", language)
        "accounts" -> Strings.get("nav_accounts", language)
        "transactions" -> Strings.get("nav_transactions", language)
        "settings" -> Strings.get("nav_settings", language)
        else -> Strings.get(key, language)
    }

    fun layoutDirection(): LayoutDirection = LayoutDirection.Rtl
    fun locale(language: AppLanguage): String = if (language == AppLanguage.EN) EN else AR
    fun language(locale: String?): AppLanguage = if (locale == EN) AppLanguage.EN else AppLanguage.AR
}
