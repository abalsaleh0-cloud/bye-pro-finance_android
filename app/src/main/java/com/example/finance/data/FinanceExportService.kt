package com.example.finance.data

import com.example.finance.model.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Portable local export contract for transaction data. */
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

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
