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
        appendLine("id,accountId,type,amount,currency,category,date,description,merchant,status,targetAccountId")
        transactions.forEach { tx ->
            appendLine(listOf(
                tx.id, tx.accountId, tx.type.name,
                "%.2f".format(Locale.US, tx.amount),
                csv(tx.currency), csv(tx.category), tx.date,
                csv(tx.description), csv(tx.merchant), tx.status.name,
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
                put("status", tx.status.name)
                put("targetAccountId", tx.targetAccountId ?: JSONObject.NULL)
            })
        }
        root.put("rows", rows)
        return root.toString(2)
    }

    fun transactionsFromJson(json: String): List<TransactionEntity> {
        val root = JSONObject(json)
        require(root.optInt("version", -1) == 1) { "Unsupported export version" }
        require(root.optString("entity") == "transactions") { "Unsupported entity" }
        val rows = root.getJSONArray("rows")
        return buildList(rows.length()) {
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                add(TransactionEntity(
                    id = 0L,
                    accountId = row.getLong("accountId"),
                    type = TransactionType.valueOf(row.getString("type")),
                    amount = row.getDouble("amount"),
                    currency = row.optString("currency", "SAR"),
                    category = row.optString("category", "Other"),
                    date = row.optLong("date", System.currentTimeMillis()),
                    description = row.optString("description", ""),
                    merchant = row.optString("merchant", ""),
                    status = TransactionStatus.valueOf(row.optString("status", TransactionStatus.COMPLETED.name)),
                    targetAccountId = if (row.isNull("targetAccountId")) null else row.optLong("targetAccountId")
                ))
            }
        }
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
