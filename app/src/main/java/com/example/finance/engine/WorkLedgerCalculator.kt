package com.example.finance.engine
import com.example.finance.model.WorkRecordEntity
import com.example.finance.model.WorkRecordType
import com.example.finance.model.WorkSourceEntity
import com.example.finance.model.WorkSourceType
import java.math.BigDecimal
import java.math.RoundingMode
object WorkLedgerCalculator {
    private fun sum(rows: List<WorkRecordEntity>, types: Set<WorkRecordType>): BigDecimal = rows.asSequence().filter { it.type in types && it.amount.isFinite() }.fold(BigDecimal.ZERO) { a, r -> a.add(BigDecimal.valueOf(r.amount)) }
    fun salaryDue(source: WorkSourceEntity, records: List<WorkRecordEntity>, period: String): BigDecimal {
        val rows = records.filter { it.sourceId == source.id && it.period == period && it.currency.equals(source.currency, true) }
        return BigDecimal.valueOf(source.monthlySalary).add(sum(rows, setOf(WorkRecordType.SALARY_ALLOWANCE, WorkRecordType.SALARY_BONUS, WorkRecordType.SALARY_OVERTIME))).subtract(sum(rows, setOf(WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_DEDUCTION, WorkRecordType.SALARY_PAYMENT))).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN)
    }
    fun freelanceOutstanding(income: WorkRecordEntity, records: List<WorkRecordEntity>): BigDecimal {
        val paid = records.filter { it.type == WorkRecordType.FREELANCE_PAYMENT && it.relatedRecordId == income.id }.fold(BigDecimal.ZERO) { a, r -> a.add(BigDecimal.valueOf(r.amount)) }
        return BigDecimal.valueOf(income.amount).subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN)
    }
    fun cashBalance(records: List<WorkRecordEntity>, currency: String, scope: WorkSourceType? = null): BigDecimal {
        val rows = records.filter { it.currency.equals(currency, true) && (scope == null || it.scope == scope) }
        val incoming = sum(rows, setOf(WorkRecordType.WORK_INCOME, WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_PAYMENT, WorkRecordType.FREELANCE_PAYMENT, WorkRecordType.WORK_TRANSFER_IN))
        val outgoing = sum(rows, setOf(WorkRecordType.FREELANCE_EXPENSE, WorkRecordType.WORK_EXPENSE, WorkRecordType.WORK_TRANSFER_OUT))
        return incoming.subtract(outgoing).setScale(2, RoundingMode.HALF_EVEN)
    }
    fun isCashMovement(type: WorkRecordType) = type in setOf(WorkRecordType.WORK_INCOME, WorkRecordType.SALARY_ADVANCE, WorkRecordType.SALARY_PAYMENT, WorkRecordType.FREELANCE_PAYMENT, WorkRecordType.FREELANCE_EXPENSE, WorkRecordType.WORK_EXPENSE, WorkRecordType.WORK_TRANSFER_IN, WorkRecordType.WORK_TRANSFER_OUT)
}
