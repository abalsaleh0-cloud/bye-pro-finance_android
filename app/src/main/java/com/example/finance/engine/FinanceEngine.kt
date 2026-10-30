package com.example.finance.engine

import com.example.finance.model.AccountEntity
import com.example.finance.model.AssetEntity
import com.example.finance.model.BudgetEntity
import com.example.finance.model.DebtEntity
import com.example.finance.model.TransactionEntity
import com.example.finance.model.TransactionType
import com.example.finance.model.TransactionStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object FinanceEngine {

    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)

    fun toBigDecimal(amount: Double): BigDecimal =
        BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_EVEN)

    fun toBigDecimal(amountStr: String): BigDecimal =
        try {
            BigDecimal(amountStr).setScale(2, RoundingMode.HALF_EVEN)
        } catch (_: Exception) {
            ZERO
        }

    // Pegged & average commercial exchange rates relative to 1.0 SAR
    val exchangeRatesToSar: Map<String, BigDecimal> = mapOf(
        "SAR" to BigDecimal("1.0000"),
        "USD" to BigDecimal("3.7500"),
        "EUR" to BigDecimal("4.0500"),
        "GBP" to BigDecimal("4.8500"),
        "AED" to BigDecimal("1.0210"),
        "KWD" to BigDecimal("12.2500"),
        "BHD" to BigDecimal("9.9500"),
        "OMR" to BigDecimal("9.7400"),
        "QAR" to BigDecimal("1.0300"),
        "EGP" to BigDecimal("0.0760")
    )

    fun convertCurrency(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String
    ): BigDecimal {
        val from = fromCurrency.trim().uppercase()
        val to = toCurrency.trim().uppercase()
        if (from == to) {
            return amount.setScale(2, RoundingMode.HALF_EVEN)
        }
        val fromRate = exchangeRatesToSar[from] ?: BigDecimal("1.0000")
        val toRate = exchangeRatesToSar[to] ?: BigDecimal("1.0000")

        val amountInSar = amount.multiply(fromRate)
        return amountInSar.divide(toRate, 2, RoundingMode.HALF_EVEN)
    }

    fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Double {
        return convertCurrency(toBigDecimal(amount), fromCurrency, toCurrency).toDouble()
    }

    fun calculateTotalIncome(transactions: List<TransactionEntity>): BigDecimal {
        var total = BigDecimal.ZERO
        for (tx in transactions) {
            if (tx.status == TransactionStatus.COMPLETED && (tx.type == TransactionType.INCOME || tx.type == TransactionType.REFUND)) {
                total = total.add(BigDecimal.valueOf(tx.amount))
            }
        }
        return total.setScale(2, RoundingMode.HALF_EVEN)
    }

    fun calculateTotalExpenses(transactions: List<TransactionEntity>): BigDecimal {
        var total = BigDecimal.ZERO
        for (tx in transactions) {
            if (tx.status == TransactionStatus.COMPLETED && tx.type == TransactionType.EXPENSE) {
                total = total.add(BigDecimal.valueOf(tx.amount))
            }
        }
        return total.setScale(2, RoundingMode.HALF_EVEN)
    }

    fun calculateNetCashFlow(income: BigDecimal, expenses: BigDecimal): BigDecimal {
        return income.subtract(expenses).setScale(2, RoundingMode.HALF_EVEN)
    }

    fun calculateNetCashFlow(transactions: List<TransactionEntity>): BigDecimal {
        val income = calculateTotalIncome(transactions)
        val expenses = calculateTotalExpenses(transactions)
        return calculateNetCashFlow(income, expenses)
    }

    /**
     * Calculates the new balance of an account starting from initial balance
     * and applying transactions sequentially.
     */
    fun calculateAccountBalance(
        startingBalance: BigDecimal,
        transactions: List<TransactionEntity>,
        accountId: Long
    ): BigDecimal {
        var current = startingBalance
        for (tx in transactions) {
            if (tx.status != TransactionStatus.COMPLETED) continue
            when (tx.type) {
                TransactionType.INCOME -> {
                    if (tx.accountId == accountId) {
                        current = current.add(BigDecimal.valueOf(tx.amount))
                    }
                }
                TransactionType.EXPENSE -> {
                    if (tx.accountId == accountId) {
                        current = current.subtract(BigDecimal.valueOf(tx.amount))
                    }
                }
                TransactionType.TRANSFER -> {
                    if (tx.accountId == accountId) {
                        // Outgoing transfer
                        current = current.subtract(BigDecimal.valueOf(tx.amount))
                    } else if (tx.targetAccountId == accountId) {
                        // Incoming transfer
                        current = current.add(BigDecimal.valueOf(tx.amount))
                    }
                }
                TransactionType.REFUND -> {
                    if (tx.accountId == accountId) {
                        current = current.add(BigDecimal.valueOf(tx.amount))
                    }
                }
                TransactionType.ADJUSTMENT -> {
                    if (tx.accountId == accountId) {
                        current = current.add(BigDecimal.valueOf(tx.amount))
                    }
                }
            }
        }
        return current.setScale(2, RoundingMode.HALF_EVEN)
    }

    /**
     * Net Worth = Total Assets + Total Liquid Account Balances - Total Remaining Debts/Liabilities
     */
    fun calculateNetWorth(
        accounts: List<AccountEntity>,
        assets: List<AssetEntity>,
        debts: List<DebtEntity>,
        baseCurrency: String = "SAR"
    ): BigDecimal {
        var liquidCash = BigDecimal.ZERO
        for (acc in accounts) {
            if (!acc.isArchived) {
                val inBase = convertCurrency(toBigDecimal(acc.balance), acc.currency, baseCurrency)
                liquidCash = liquidCash.add(inBase)
            }
        }

        var assetTotal = BigDecimal.ZERO
        for (asset in assets) {
            val inBase = convertCurrency(toBigDecimal(asset.value), asset.currency, baseCurrency)
            assetTotal = assetTotal.add(inBase)
        }

        var debtTotal = BigDecimal.ZERO
        for (debt in debts) {
            val inBase = convertCurrency(toBigDecimal(debt.remaining), debt.currency, baseCurrency)
            debtTotal = debtTotal.add(inBase)
        }

        val totalAssets = liquidCash.add(assetTotal)
        return totalAssets.subtract(debtTotal).setScale(2, RoundingMode.HALF_EVEN)
    }

    data class BudgetStatus(
        val limit: BigDecimal,
        val spent: BigDecimal,
        val remaining: BigDecimal,
        val percentage: Float,
        val isOverBudget: Boolean,
        val alertThreshold: Int // 0, 50, 75, 90, 100
    )

    fun calculateBudgetStatus(
        budget: BudgetEntity,
        transactions: List<TransactionEntity>
    ): BudgetStatus {
        val limit = BigDecimal.valueOf(budget.limitAmount).setScale(2, RoundingMode.HALF_EVEN)
        var spent = BigDecimal.ZERO

        for (tx in transactions) {
            if (tx.status == TransactionStatus.COMPLETED &&
                tx.type == TransactionType.EXPENSE &&
                tx.amount.isFinite() &&
                tx.currency.equals(budget.currency, ignoreCase = true) &&
                (tx.category.equals(budget.category, ignoreCase = true) || budget.category == "All") &&
                tx.date >= budget.startDate && tx.date <= budget.endDate
            ) {
                spent = spent.add(BigDecimal.valueOf(tx.amount))
            }
        }
        spent = spent.setScale(2, RoundingMode.HALF_EVEN)
        val remaining = limit.subtract(spent).setScale(2, RoundingMode.HALF_EVEN)

        val percentage = if (limit.compareTo(BigDecimal.ZERO) > 0) {
            spent.divide(limit, 4, RoundingMode.HALF_EVEN).toFloat() * 100f
        } else 0f

        val threshold = when {
            percentage >= 100f -> 100
            percentage >= 90f -> 90
            percentage >= 75f -> 75
            percentage >= 50f -> 50
            else -> 0
        }

        return BudgetStatus(
            limit = limit,
            spent = spent,
            remaining = remaining,
            percentage = percentage,
            isOverBudget = remaining < BigDecimal.ZERO,
            alertThreshold = threshold
        )
    }

    data class SavingsProgress(
        val target: BigDecimal,
        val current: BigDecimal,
        val remaining: BigDecimal,
        val percentage: Float,
        val monthsRemaining: Int,
        val requiredMonthlySaving: BigDecimal
    )

    fun calculateSavingsProgress(
        targetAmount: Double,
        currentAmount: Double,
        deadlineTimestamp: Long
    ): SavingsProgress {
        val target = BigDecimal.valueOf(targetAmount).setScale(2, RoundingMode.HALF_EVEN)
        val current = BigDecimal.valueOf(currentAmount).setScale(2, RoundingMode.HALF_EVEN)
        val remaining = target.subtract(current).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN)

        val percentage = if (target > BigDecimal.ZERO) {
            current.divide(target, 4, RoundingMode.HALF_EVEN).toFloat() * 100f
        } else 0f

        val now = System.currentTimeMillis()
        val millisLeft = (deadlineTimestamp - now).coerceAtLeast(0L)
        val daysLeft = millisLeft / (1000L * 60 * 60 * 24)
        val monthsRemaining = (daysLeft / 30).coerceAtLeast(1).toInt()

        val requiredMonthly = if (remaining > BigDecimal.ZERO && monthsRemaining > 0) {
            remaining.divide(BigDecimal.valueOf(monthsRemaining.toLong()), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        return SavingsProgress(
            target = target,
            current = current,
            remaining = remaining,
            percentage = percentage.coerceIn(0f, 100f),
            monthsRemaining = monthsRemaining,
            requiredMonthlySaving = requiredMonthly
        )
    }

    /**
     * Estimated payoff months for debt with interest
     */
    fun calculateDebtPayoffMonths(
        remainingPrincipal: Double,
        annualInterestRatePercent: Double,
        monthlyPayment: Double
    ): Int {
        if (monthlyPayment <= 0 || remainingPrincipal <= 0) return 0
        val r = (annualInterestRatePercent / 100.0) / 12.0
        if (r <= 0.0) {
            return Math.ceil(remainingPrincipal / monthlyPayment).toInt()
        }
        val interestFirstMonth = remainingPrincipal * r
        if (monthlyPayment <= interestFirstMonth) {
            return 999 // payment doesn't cover interest
        }
        val months = -Math.log(1 - (remainingPrincipal * r / monthlyPayment)) / Math.log(1 + r)
        return Math.ceil(months).toInt()
    }

    fun formatMoney(amount: Double, currency: String = "SAR"): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "${format.format(amount)} $currency"
    }

    fun formatMoney(amount: BigDecimal, currency: String = "SAR"): String {
        return formatMoney(amount.toDouble(), currency)
    }

    /**
     * Financial Health Score (0 - 100) based on:
     * - Savings rate
     * - Debt to asset ratio
     * - Budget adherence
     * - Emergency fund coverage
     */
    fun calculateFinancialHealthScore(
        totalIncome: BigDecimal,
        totalExpenses: BigDecimal,
        netWorth: BigDecimal,
        totalDebts: BigDecimal,
        budgetStatuses: List<BudgetStatus>
    ): Int {
        var score = 50

        // 1. Net Cash flow (up to +20 or -20)
        val netFlow = totalIncome.subtract(totalExpenses)
        if (totalIncome > BigDecimal.ZERO) {
            val savingsRate = netFlow.divide(totalIncome, 2, RoundingMode.HALF_EVEN).toFloat()
            score += when {
                savingsRate >= 0.30f -> 20
                savingsRate >= 0.15f -> 15
                savingsRate >= 0.05f -> 8
                savingsRate < 0f -> -15
                else -> 0
            }
        }

        // 2. Debt ratio
        if (totalDebts > BigDecimal.ZERO && netWorth > BigDecimal.ZERO) {
            val debtRatio = totalDebts.divide(netWorth, 2, RoundingMode.HALF_EVEN).toFloat()
            if (debtRatio < 0.2f) score += 15
            else if (debtRatio > 0.6f) score -= 15
        } else if (totalDebts == BigDecimal.ZERO) {
            score += 15
        }

        // 3. Budgets check
        val overBudgets = budgetStatuses.count { it.isOverBudget }
        if (budgetStatuses.isNotEmpty()) {
            if (overBudgets == 0) score += 15
            else score -= (overBudgets * 10)
        }

        return score.coerceIn(10, 100)
    }
}
