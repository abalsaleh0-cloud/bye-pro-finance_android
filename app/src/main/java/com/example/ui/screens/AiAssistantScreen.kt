package com.example.ui.screens

import com.example.finance.i18n.TextContent

import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField
import com.example.ui.components.FinanceIconButton
import com.example.ui.components.FinanceFloatingActionButton

import com.example.finance.i18n.WebI18n
import com.example.ui.theme.ByeProTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.ui.ChatMessage
import com.example.finance.model.AppLanguage
import com.example.finance.ui.FinanceViewModel

@Composable
fun AiAssistantScreen(
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickChips = if (lang == com.example.finance.model.AppLanguage.AR) listOf(
        WebI18n.tr("كم أنفقت هذا الشهر؟", "How much have I spent this month?", lang),
        WebI18n.tr("ما أعلى تصنيف أنفقت عليه؟", "What is my highest spending category?", lang),
        WebI18n.tr("هل أنا في خطر تجاوز الميزانيات؟", "Am I at risk of exceeding my budgets?", lang),
        WebI18n.tr("كيف أستطيع توفير المزيد من المال؟", "How can I save more money?", lang)
    ) else listOf(
        "What is my spending this month?",
        "What is my highest expense category?",
        "Am I exceeding my budgets?",
        "How can I save more money?"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = ByeProTheme.colors.financialGreen
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text(Strings.get("nav_ai_assistant", lang), fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text(Strings.get("nav_ai_insights", lang), fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTabIndex == 0) {
            // Chat Tab
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FinanceOutlinedButton(onClick = { viewModel.clearAiChat() }, enabled = !isAiLoading) {
                        Text(WebI18n.tr("مسح المحادثة", "Clear chat", lang), style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Safety Disclaimer banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(1.dp, ByeProTheme.colors.goldPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ByeProTheme.colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = Strings.get("ai_disclaimer", lang),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubbleItem(message = msg, lang = lang)
                    }
                    if (isAiLoading) {
                        item {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ByeProTheme.colors.financialGreen,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = WebI18n.tr("جاري التحليل المالي الذكي...", "Analyzing financial data...", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Quick Prompt Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickChips.forEach { chip ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.askAiAssistant(chip) },
                            label = { Text(chip, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }

                // Input box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FinanceTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(Strings.get("ask_placeholder", lang), fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_assistant_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    FinanceIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val q = inputText
                                inputText = ""
                                viewModel.askAiAssistant(q)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ByeProTheme.colors.financialGreen)
                            .testTag("ai_assistant_send_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = WebI18n.tr("إرسال", "Send", lang),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        } else {
            // Insights Tab
            val insights = viewModel.aiService.generateInsights(transactions, budgets, lang)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = WebI18n.tr("الرؤى والتحليلات الاستباقية للإنفاق", "Proactive Spending Insights", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = WebI18n.tr("تحليلات دورية تستكشف الهدر وفرص التوفير ومخاطر الميزانيات بدقة رياضية.", "Periodic analysis of waste, savings opportunities, and budget risks.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(insights) { insight ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                when (insight.impactType) {
                                    "POSITIVE" -> ByeProTheme.colors.financialGreen.copy(alpha = 0.5f)
                                    "WARNING" -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    else -> ByeProTheme.colors.goldPrimary.copy(alpha = 0.5f)
                                },
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = insight.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (insight.impactType) {
                                        "POSITIVE" -> ByeProTheme.colors.financialGreen
                                        "WARNING" -> MaterialTheme.colorScheme.error
                                        else -> ByeProTheme.colors.goldPrimary
                                    }
                                )
                                Icon(
                                    imageVector = when (insight.impactType) {
                                        "POSITIVE" -> Icons.Default.TrendingUp
                                        "WARNING" -> Icons.Default.Warning
                                        else -> Icons.Default.AutoAwesome
                                    },
                                    contentDescription = null,
                                    tint = when (insight.impactType) {
                                        "POSITIVE" -> ByeProTheme.colors.financialGreen
                                        "WARNING" -> MaterialTheme.colorScheme.error
                                        else -> ByeProTheme.colors.goldPrimary
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = insight.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (insight.estimatedSavings > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = WebI18n.tr("التوفير المتوقع: +", "Expected savings: +", lang) + TextContent.money(insight.estimatedSavings, "SAR", lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ByeProTheme.colors.financialGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(message: ChatMessage, lang: AppLanguage) {
    val isUser = message.sender == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) ByeProTheme.colors.financialGreen else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(
                    1.dp,
                    if (isUser) ByeProTheme.colors.financialGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) WebI18n.tr("أنت", "You", lang) else "BYE PRO Assistant",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else ByeProTheme.colors.goldPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
