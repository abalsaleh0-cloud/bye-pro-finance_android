package com.example.finance.engine

import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionStatus
import com.example.finance.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/** Android counterpart of packages/finance-engine/src/index.ts on Web. */
object LedgerSummary {
    data class CurrencyTotals(
        val income: BigDecimal = BigDecimal.ZERO.setScale(2),
        val expense: BigDecimal = BigDecimal.ZERO.setScale(2),
        val adjustment: BigDecimal = BigDecimal.ZERO.setScale(2),
        val net: BigDecimal = BigDecimal.ZERO.setScale(2),
    )

    fun summarizeByCurrency(lines: List<TransactionEntity>): Map<String, CurrencyTotals> {
        val totals = linkedMapOf<String, CurrencyTotals>()
        lines.forEach { line ->
            if (line.type == TransactionType.TRANSFER ||
                line.status == TransactionStatus.CANCELLED ||
                !line.amount.isFinite()
            ) return@forEach

            val currency = line.currency.trim().ifEmpty { "UNKNOWN" }.uppercase()
            val current = totals[currency] ?: CurrencyTotals()
            val amount = BigDecimal.valueOf(line.amount).setScale(2, RoundingMode.HALF_EVEN)
            val next = when (line.type) {
                TransactionType.INCOME, TransactionType.REFUND -> current.copy(income = current.income.add(amount))
                TransactionType.EXPENSE -> current.copy(expense = current.expense.add(amount))
                TransactionType.ADJUSTMENT -> current.copy(adjustment = current.adjustment.add(amount))
                TransactionType.TRANSFER -> current
            }
            totals[currency] = next.copy(
                net = next.income.subtract(next.expense).add(next.adjustment).setScale(2, RoundingMode.HALF_EVEN)
            )
        }
        return totals
    }
}
