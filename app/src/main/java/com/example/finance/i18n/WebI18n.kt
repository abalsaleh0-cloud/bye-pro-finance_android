package com.example.finance.i18n

import android.content.Context
import com.example.finance.model.AppLanguage
import java.text.DateFormat
import java.text.NumberFormat
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
    const val AR = "ar"
    const val EN = "en"

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

    /** Web-equivalent inline translation helper: Arabic source + English translation. */
    fun tr(arabic: String, english: String, language: AppLanguage): String =
        if (language == AppLanguage.EN) english else arabic

    /** Fixed Arabic visual direction; English must never mirror the Compose layout. */
    fun layoutDirection(): androidx.compose.ui.unit.LayoutDirection =
        androidx.compose.ui.unit.LayoutDirection.Rtl

    fun number(value: Number, language: AppLanguage): String =
        NumberFormat.getNumberInstance(if (language == AppLanguage.EN) Locale.ENGLISH else Locale("ar"))
            .format(value)

    fun money(value: Number, currency: String, language: AppLanguage): String =
        runCatching {
            NumberFormat.getCurrencyInstance(if (language == AppLanguage.EN) Locale.ENGLISH else Locale("ar"))
                .apply { currency = java.util.Currency.getInstance(currency) }
                .format(value)
        }.getOrElse { "${number(value, language)} $currency" }

    fun date(value: Date, language: AppLanguage): String =
        DateFormat.getDateInstance(
            DateFormat.MEDIUM,
            if (language == AppLanguage.EN) Locale.ENGLISH else Locale("ar")
        ).format(value)
}
