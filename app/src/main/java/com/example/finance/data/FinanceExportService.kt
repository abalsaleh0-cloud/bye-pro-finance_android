package com.example.finance.data

import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Portable local import/export contract for transaction data. */
object FinanceExportService {
    fun transactionsToCsv(transactions: List<TransactionEntity>): String = buildString {
        appendLine("id,accountId,type,amount,currency,category,date,description,merchant,tags,notes,attachmentUri,isRecurring,status,targetAccountId")
        transactions.forEach { tx ->
            appendLine(listOf(
                tx.id, tx.accountId, tx.type.name,
                "%.2f".format(Locale.US, tx.amount),
                csv(tx.currency), csv(tx.category), tx.date,
                csv(tx.description), csv(tx.merchant), csv(tx.tags), csv(tx.notes),
                csv(tx.attachmentUri ?: ""), tx.isRecurring, tx.status.name,
                tx.targetAccountId ?: ""
            ).joinToString(","))
        }
    }

    fun transactionsToJson(transactions: List<TransactionEntity>): String {
        val root = JSONObject().put("version", 1).put("entity", "transactions")
        val rows = JSONArray()
        transactions.forEach { tx ->
            rows.put(JSONObject().apply {
                put("id", tx.id)
                put("accountId", tx.accountId)
                put("type", tx.type.name)
                put("amount", tx.amount)
                put("currency", tx.currency)
                put("category", tx.category)
                put("date", tx.date)
                put("description", tx.description)
                put("merchant", tx.merchant)
                put("tags", tx.tags)
                put("notes", tx.notes)
                put("attachmentUri", tx.attachmentUri ?: JSONObject.NULL)
                put("isRecurring", tx.isRecurring)
                put("status", tx.status.name)
                put("targetAccountId", tx.targetAccountId ?: JSONObject.NULL)
            })
        }
        root.put("rows", rows)
        return root.toString(2)
    }

    fun transactionsFromCsv(csvText: String): List<TransactionEntity> {
        val rows = parseCsv(csvText)
        require(rows.isNotEmpty()) { "CSV file is empty" }
        val header = rows.first().map { it.trim() }
        val required = listOf("accountId", "type", "amount", "currency", "category", "date")
        require(required.all { it in header }) { "CSV header is missing required transaction columns" }
        val index = header.withIndex().associate { it.value to it.index }
        fun value(row: List<String>, key: String): String = row.getOrNull(index[key] ?: -1).orEmpty()
        return rows.drop(1).filter { row -> row.any { it.isNotBlank() } }.map { row ->
            val type = runCatching { TransactionType.valueOf(value(row, "type").trim().uppercase(Locale.ROOT)) }
                .getOrElse { throw IllegalArgumentException("Invalid transaction type in CSV") }
            val status = value(row, "status").takeIf { it.isNotBlank() }?.let {
                runCatching { TransactionStatus.valueOf(it.trim().uppercase(Locale.ROOT)) }.getOrElse { throw IllegalArgumentException("Invalid transaction status in CSV") }
            } ?: TransactionStatus.COMPLETED
            val amount = value(row, "amount").toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid transaction amount in CSV")
            require(amount.isFinite() && amount > 0.0) { "Transaction amount must be finite and positive" }
            TransactionEntity(
                accountId = value(row, "accountId").toLongOrNull() ?: throw IllegalArgumentException("Invalid accountId in CSV"),
                type = type,
                amount = amount,
                currency = value(row, "currency").ifBlank { "LYD" }.uppercase(Locale.ROOT),
                category = value(row, "category").ifBlank { "Other" },
                date = value(row, "date").toLongOrNull() ?: throw IllegalArgumentException("Invalid date in CSV"),
                description = value(row, "description"),
                merchant = value(row, "merchant"),
                tags = value(row, "tags"),
                notes = value(row, "notes"),
                attachmentUri = value(row, "attachmentUri").takeIf { it.isNotBlank() },
                isRecurring = value(row, "isRecurring").toBooleanStrictOrNull() ?: false,
                status = status,
                targetAccountId = value(row, "targetAccountId").toLongOrNull()
            )
        }
    }

    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '"' && quoted && i + 1 < text.length && text[i + 1] == '"' -> { cell.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> { row.add(cell.toString()); cell.setLength(0) }
                (ch == '\n' || ch == '\r') && !quoted -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(cell.toString()); cell.setLength(0)
                    rows.add(row); row = mutableListOf()
                }
                else -> cell.append(ch)
            }
            i++
        }
        require(!quoted) { "CSV contains an unterminated quoted field" }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows
    }

    fun transactionsFromJson(json: String): List<TransactionEntity> {
        val root = JSONObject(json)
        require(root.optInt("version", -1) == 1) { "Unsupported export version" }
        require(root.optString("entity") == "transactions") { "Unsupported entity" }
        val rows = root.optJSONArray("rows") ?: JSONArray()
        return buildList(rows.length()) {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val type = runCatching { TransactionType.valueOf(row.optString("type")) }
                    .getOrElse { throw IllegalArgumentException("Invalid transaction type") }
                val status = runCatching { TransactionStatus.valueOf(row.optString("status")) }
                    .getOrDefault(TransactionStatus.COMPLETED)
                add(TransactionEntity(
                    id = 0L,
                    accountId = row.optLong("accountId", 0L),
                    type = type,
                    amount = row.optDouble("amount", 0.0),
                    currency = row.optString("currency", "LYD"),
                    category = row.optString("category", "Other"),
                    date = row.optLong("date", System.currentTimeMillis()),
                    description = row.optString("description", ""),
                    merchant = row.optString("merchant", ""),
                    tags = row.optString("tags", ""),
                    notes = row.optString("notes", ""),
                    attachmentUri = if (row.isNull("attachmentUri")) null else row.optString("attachmentUri", ""),
                    isRecurring = row.optBoolean("isRecurring", false),
                    status = status,
                    targetAccountId = if (row.isNull("targetAccountId")) null else row.optLong("targetAccountId")
                ))
            }
        }
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
