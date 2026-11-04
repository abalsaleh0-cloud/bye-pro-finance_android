package com.example.finance.ui

import java.util.Locale

/**
 * Conservative intent gate for routing write requests to local app tools.
 * A bare "سجل" is ambiguous: it can mean a ledger/history (read) or an
 * imperative "record" (write). Only classify it as a write with an explicit
 * recordable item; known history/list phrasing remains read-only.
 */
internal object AiWriteIntentClassifier {
    fun isWriteIntent(query: String): Boolean {
        val normalized = query.trim().lowercase(Locale.ROOT)
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
            .replace(Regex("[؟?!،,.؛:]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.isBlank()) return false

        val readPhrases = listOf(
            "اعرض سجل", "عرض سجل", "اظهر سجل", "أظهر سجل", "اريد سجل", "أريد سجل",
            "سجل المصروفات", "سجل الايرادات", "سجل الإيرادات", "سجل الديون",
            "سجل المعاملات", "سجل التحويلات", "سجل الفواتير", "سجل المدفوعات",
            "history", "show history", "list history", "transaction history",
            "expense history", "income history", "debt history"
        )
        if (readPhrases.any(normalized::contains)) return false

        val explicitRecordCommand = Regex(
            """(?:^|\\s)(?:سجل|سجّل)\\s+(?:مصروف|مصروفا|إيراد|ايراد|معاملة|دين|فاتورة|دفعة|تحويل)(?=\\s|$)"""
        ).containsMatchIn(normalized) || Regex(
            """(?:^|\\s)record\\s+(?:an?\\s+)?(?:expense|income|transaction|debt|bill|payment|transfer)(?=\\s|$)"""
        ).containsMatchIn(normalized)

        val explicitWriteTerms = listOf(
            "تسجيل", "اسجل", "إضافة", "اضافة", "أضيف", "اضيف", "اضاف",
            "أدخل", "ادخل", "إدخال", "ادخال", "أنشئ", "انشئ", "إنشاء", "انشاء",
            "عدّل", "عدل", "تعديل", "حدّث", "حدث", "تحديث", "احذف", "حذف", "مسح",
            "ادفع", "دفع", "سدد", "سدّد", "سداد", "تسديد", "حوّل", "حول", "تحويل",
            "افتح حساب", "فتح حساب", "create", "add", "register", "insert",
            "update", "edit", "delete", "remove", "pay", "settle", "transfer"
        ).any(normalized::contains)

        return explicitRecordCommand || explicitWriteTerms
    }
}
