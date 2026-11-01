package com.example.finance.i18n

import android.content.Context
import com.example.finance.model.AppLanguage
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Android implementation of the web app's i18n contract.
 *
 * Web contract:
 * - locale values are exactly "ar" and "en"
 * - storage key is exactly "bye-pro-finance.locale"
 * - Arabic is the default/fallback locale
 * - locale changes translated text only
 * - visual/layout direction remains RTL for both locales
 *
 * Android uses SharedPreferences as the native equivalent of web localStorage.
 */
object WebI18n {
    const val LOCALE_KEY = "bye-pro-finance.locale"
    const val THEME_KEY = "bye-pro-finance.theme"
    const val AR = "ar"
    const val EN = "en"
    const val DARK = "dark"
    const val LIGHT = "light"

    fun locale(language: AppLanguage): String = if (language == AppLanguage.EN) EN else AR

    fun language(locale: String?): AppLanguage =
        if (locale == EN) AppLanguage.EN else AppLanguage.AR

    fun read(context: Context): AppLanguage = language(
        context.getSharedPreferences("bye_pro_finance_preferences", Context.MODE_PRIVATE)
            .getString(LOCALE_KEY, AR)
    )

    fun write(context: Context, language: AppLanguage) {
        context.getSharedPreferences("bye_pro_finance_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString(LOCALE_KEY, locale(language))
            .apply()
    }

    fun theme(isDark: Boolean): String = if (isDark) DARK else LIGHT

    fun isDarkTheme(value: String?): Boolean = value != LIGHT

    /**
     * Uses the exact web localStorage key/value contract. Migrates the former
     * Android-only boolean once, preserving the user's existing theme choice.
     */
    fun readDarkMode(context: Context): Boolean {
        val preferences = context.getSharedPreferences("bye_pro_finance_preferences", Context.MODE_PRIVATE)
        val storedTheme = preferences.getString(THEME_KEY, null)
        if (storedTheme == DARK || storedTheme == LIGHT) return isDarkTheme(storedTheme)

        val legacyDarkMode = preferences.getBoolean("dark_mode", true)
        preferences.edit()
            .putString(THEME_KEY, theme(legacyDarkMode))
            .remove("dark_mode")
            .apply()
        return legacyDarkMode
    }

    fun writeDarkMode(context: Context, dark: Boolean) {
        context.getSharedPreferences("bye_pro_finance_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString(THEME_KEY, theme(dark))
            .remove("dark_mode")
            .apply()
    }

    /** Web-equivalent inline translation helper: Arabic source + English translation. */
    fun tr(arabic: String, english: String, language: AppLanguage): String =
        if (language == AppLanguage.EN) english else arabic

    /** Fixed Arabic visual direction; English must never mirror the Compose layout. */
    fun layoutDirection(): androidx.compose.ui.unit.LayoutDirection =
        androidx.compose.ui.unit.LayoutDirection.Rtl

    fun number(value: Number, language: AppLanguage): String =
        TextContent.number(value, language)

    fun money(value: Number, currency: String, language: AppLanguage): String =
        TextContent.money(value, currency, language)

    fun date(value: Date, language: AppLanguage): String =
        DateFormat.getDateInstance(
            DateFormat.MEDIUM,
            if (language == AppLanguage.EN) Locale.ENGLISH else Locale("ar")
        ).format(value)
}
