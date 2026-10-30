package com.example.finance.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.example.finance.model.AppLanguage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared text-content layer for the Android client.
 *
 * Keeps UI copy in the central Strings catalogue, supports named placeholders,
 * locale-aware plural categories, consistent financial formatting, and safe
 * mixed-direction values while the app's visual layout remains RTL.
 */
object TextContent {
    private val placeholder = Regex("\\{([A-Za-z][A-Za-z0-9_]*)}")

    fun text(
        key: String,
        language: AppLanguage,
        arguments: Map<String, Any?> = emptyMap()
    ): String {
        val template = Strings.get(key, language)
        if (template == key || arguments.isEmpty()) return template
        return placeholder.replace(template) { match ->
            val name = match.groupValues[1]
            arguments[name]?.toString()?.let(::isolate) ?: match.value
        }
    }

    fun plural(key: String, count: Int, language: AppLanguage): String {
        val category = pluralCategory(count, language)
        val specific = Strings.get("${key}_${category}", language)
        val fallback = Strings.get("${key}_other", language)
        val template = if (specific != "${key}_${category}") specific else fallback
        return if (template == "${key}_other") Strings.get(key, language)
        else textTemplate(template, mapOf("count" to number(count, language)))
    }

    fun pluralCategory(count: Int, language: AppLanguage): String {
        if (language == AppLanguage.EN) return if (count == 1) "one" else "other"
        return when {
            count == 0 -> "zero"
            count == 1 -> "one"
            count == 2 -> "two"
            count % 100 in 3..10 -> "few"
            count % 100 in 11..99 -> "many"
            else -> "other"
        }
    }

    fun number(value: Number, language: AppLanguage, maximumFractionDigits: Int = 2): String =
        NumberFormat.getNumberInstance(locale(language)).apply {
            this.maximumFractionDigits = maximumFractionDigits.coerceAtLeast(0)
        }.format(value)

    fun money(value: Number, currencyCode: String, language: AppLanguage): String =
        runCatching {
            NumberFormat.getCurrencyInstance(locale(language)).apply {
                currency = java.util.Currency.getInstance(currencyCode)
                maximumFractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
            }.format(value)
        }.getOrElse { "${number(value, language)} ${currencyCode.uppercase(Locale.ROOT)}" }

    fun percent(value: Number, language: AppLanguage, fractionDigits: Int = 1): String =
        NumberFormat.getPercentInstance(locale(language)).apply {
            maximumFractionDigits = fractionDigits.coerceAtLeast(0)
        }.format(value)

    fun date(value: Date, language: AppLanguage): String =
        SimpleDateFormat("d MMM yyyy", locale(language)).format(value)

    fun dateTime(value: Date, language: AppLanguage): String =
        SimpleDateFormat("d MMM yyyy, HH:mm", locale(language)).format(value)

    /** Unicode First Strong Isolate / Pop Directional Isolate for user-entered LTR/RTL values. */
    fun isolate(value: String): String = "\u2068$value\u2069"

    private fun textTemplate(template: String, arguments: Map<String, Any?>): String =
        placeholder.replace(template) { match ->
            arguments[match.groupValues[1]]?.toString()?.let(::isolate) ?: match.value
        }

    private fun locale(language: AppLanguage): Locale =
        if (language == AppLanguage.EN) Locale.ENGLISH else Locale("ar")
}

val LocalTextLanguage = compositionLocalOf { AppLanguage.AR }

@Composable
fun ProvideTextLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextLanguage provides language, content = content)
}

/** Compose-ready string lookup. Use this instead of adding new visible literals to screens. */
@Composable
fun appText(key: String, arguments: Map<String, Any?> = emptyMap()): String =
    TextContent.text(key, LocalTextLanguage.current, arguments)
