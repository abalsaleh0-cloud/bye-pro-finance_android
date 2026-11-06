package com.example.ui.screens

import com.example.ui.theme.ByeProTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.model.TransactionType
import com.example.finance.ui.FinanceViewModel
import com.example.ui.components.InteractiveCashFlowChart
import java.math.BigDecimal

@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val debts by viewModel.debts.collectAsState()

    var exportModalData by remember { mutableStateOf<String?>(null) }
    var exportTitle by remember { mutableStateOf("") }

    val totalIncome = FinanceEngine.calculateTotalIncome(transactions)
    val totalExpenses = FinanceEngine.calculateTotalExpenses(transactions)
    val netCashFlow = FinanceEngine.calculateNetCashFlow(totalIncome, totalExpenses)
    val netWorth = FinanceEngine.calculateNetWorth(accounts, assets, debts)

    // Category breakdown for expenses
    val categoryTotals = mutableMapOf<String, Double>()
    for (tx in transactions) {
        if (tx.type == TransactionType.EXPENSE) {
            categoryTotals[tx.category] = (categoryTotals[tx.category] ?: 0.0) + tx.amount
        }
    }
    val totalExpenseDouble = totalExpenses.toDouble().coerceAtLeast(1.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Strings.get("reports_title", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "تقارير تحليلية شاملة للتدفقات النقدية ومسارات الإنفاق مع إمكانية التصدير الفوري.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Export Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        exportTitle = "تقرير المعاملات المالي (CSV Export)"
                        exportModalData = viewModel.generateCsvExport()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_csv_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.financialGreen, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(Strings.get("export_csv", lang), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        exportTitle = "التقرير المالي التنفيذي (JSON Export)"
                        exportModalData = viewModel.generateJsonExport()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_json_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ByeProTheme.colors.goldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(Strings.get("export_json", lang), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Summary Cards
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الملخص المالي الشامل",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("إجمالي الدخل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FinanceEngine.formatMoney(totalIncome, "SAR"), color = ByeProTheme.colors.financialGreen, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("إجمالي المصروفات", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FinanceEngine.formatMoney(totalExpenses, "SAR"), color = ByeProTheme.colors.financialRed, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("صافي التدفق", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FinanceEngine.formatMoney(netCashFlow, "SAR"), color = ByeProTheme.colors.goldPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Cash flow chart
        item {
            InteractiveCashFlowChart(
                income = totalIncome.toDouble(),
                expense = totalExpenses.toDouble(),
                currency = "SAR"
            )
        }

        // Spending by category breakdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = Strings.get("spending_by_category", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (categoryTotals.isEmpty()) {
                        Text(
                            text = "لا توجد مصروفات مسجلة لتحليلها.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categoryTotals.forEach { (cat, amount) ->
                            val pct = (amount / totalExpenseDouble).toFloat()
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$cat (${(pct * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = FinanceEngine.formatMoney(amount, "SAR"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ByeProTheme.colors.goldPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = ByeProTheme.colors.goldPrimary,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Export preview dialog
    if (exportModalData != null) {
        AlertDialog(
            onDismissRequest = { exportModalData = null },
            title = { Text(exportTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "البيانات جاهزة للتصدير والحفظ في قاعدة بيانات خارجية أو ملف جدولي:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Text(
                            text = exportModalData!!,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { exportModalData = null }) {
                    Text(Strings.get("confirm", lang))
                }
            }
        )
    }
}
