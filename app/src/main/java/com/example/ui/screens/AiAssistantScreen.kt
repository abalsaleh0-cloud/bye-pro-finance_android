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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finance.engine.FinanceEngine
import com.example.finance.i18n.Strings
import com.example.finance.ui.ChatMessage
import com.example.finance.model.AppLanguage
import com.example.finance.ui.FinanceViewModel
import com.example.ui.screens.VoiceInputButton
import com.example.ui.screens.SpeakTextButton

@Composable
fun AiAssistantScreen(
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val localModelReady by viewModel.localModelReady.collectAsState()
    val localModelLoading by viewModel.localModelLoading.collectAsState()
    val localModelProgress by viewModel.localModelProgress.collectAsState()
    val localModelStatus by viewModel.localModelStatus.collectAsState()
    val pendingAiAction by viewModel.pendingAiAction.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    var autoSpeakReplies by remember {
        mutableStateOf(
            context.getSharedPreferences("bye_pro_ai_settings", 0)
                .getBoolean("auto_speak_replies", false)
        )
    }
    var cloudGeneralEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("bye_pro_finance_ai_models", 0)
                .getBoolean("cloud_general_enabled", true)
        )
    }
    var inputText by remember { mutableStateOf("") }
    var speakNextAiResponse by remember { mutableStateOf(false) }
    var autoSpeakTimestamp by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    LaunchedEffect(messages.lastOrNull()?.timestamp, isAiLoading) {
        val lastMessage = messages.lastOrNull()
        if (speakNextAiResponse && !isAiLoading && lastMessage?.sender == "AI") {
            autoSpeakTimestamp = lastMessage.timestamp
            speakNextAiResponse = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ByeProTheme.colors.bgMain)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
                .border(1.dp, ByeProTheme.colors.goldPrimary.copy(alpha = 0.42f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.surfaceMain)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                        .background(ByeProTheme.colors.goldPrimary.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("AI BYE PRO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = ByeProTheme.colors.goldPrimary)
                    Text(WebI18n.tr("مركز الذكاء الاصطناعي • مساعدك العام", "INTELLIGENCE STUDIO • GENERAL AI", lang), style = MaterialTheme.typography.labelSmall, color = ByeProTheme.colors.textSecondary)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        if (localModelReady) WebI18n.tr("محلي نشط", "LOCAL READY", lang)
                        else if (cloudGeneralEnabled) WebI18n.tr("هجين", "HYBRID", lang)
                        else WebI18n.tr("سحابي متوقف", "CLOUD OFF", lang),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ByeProTheme.colors.goldPrimary
                    )
                    Text(if (localModelReady) "●" else "○", color = ByeProTheme.colors.goldPrimary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                label = { Text(WebI18n.tr("المحادثة", "Studio", lang), fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = ByeProTheme.colors.cardMain,
                    selectedContainerColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.18f),
                    labelColor = ByeProTheme.colors.textSecondary,
                    selectedLabelColor = ByeProTheme.colors.goldPrimary
                )
            )
            FilterChip(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                label = { Text(Strings.get("nav_ai_insights", lang), fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = ByeProTheme.colors.cardMain,
                    selectedContainerColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.18f),
                    labelColor = ByeProTheme.colors.textSecondary,
                    selectedLabelColor = ByeProTheme.colors.goldPrimary
                )
            )
            FilterChip(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                label = { Text(WebI18n.tr("النماذج والإعدادات", "Models & Settings", lang), fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = ByeProTheme.colors.cardMain,
                    selectedContainerColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.18f),
                    labelColor = ByeProTheme.colors.textSecondary,
                    selectedLabelColor = ByeProTheme.colors.goldPrimary
                )
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

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                                    .border(1.dp, ByeProTheme.colors.goldPrimary.copy(alpha = 0.38f), RoundedCornerShape(24.dp)),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.surfaceMain)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                                            .background(ByeProTheme.colors.goldPrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ByeProTheme.colors.goldPrimary, modifier = Modifier.size(30.dp))
                                    }
                                    Text("AI BYE PRO", style = MaterialTheme.typography.labelMedium, color = ByeProTheme.colors.goldPrimary, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        WebI18n.tr("مساحة تفكير وإنجاز، وليست مجرد محادثة", "A space to think and create — beyond chat", lang),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ByeProTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        WebI18n.tr("اسأل، حلّل، خطّط، اكتب، أو ابدأ مشروعاً جديداً. اختر أحد المسارات أدناه أو اكتب طلبك مباشرة.", "Ask, analyze, plan, write, or start a new project. Choose a prompt below or type your request.", lang),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ByeProTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                    items(messages) { msg ->
                        ChatBubbleItem(message = msg, lang = lang, autoSpeak = msg.timestamp == autoSpeakTimestamp)
                    }
                    if (isAiLoading) {
                        item {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ByeProTheme.colors.goldPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = WebI18n.tr("أفكر في إجابتك...", "Thinking through your request...", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ByeProTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Expanded chat composer: a blank, multi-line writing area with voice and send actions below.
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .border(1.dp, ByeProTheme.colors.goldPrimary.copy(alpha = 0.32f), RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.surfaceMain)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinanceTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(116.dp)
                                .testTag("ai_assistant_input"),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = false,
                            minLines = 3,
                            maxLines = 5
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            VoiceInputButton(
                                languageCode = if (lang == AppLanguage.AR) "ar" else "en",
                                enabled = !isAiLoading,
                                onRecognized = {
                                    inputText = ""
                                    speakNextAiResponse = autoSpeakReplies
                                    viewModel.askAiAssistant(it)
                                },
                                modifier = Modifier.testTag("ai_assistant_voice_input")
                            )
                            FinanceIconButton(
                                onClick = {
                                    if (inputText.isNotBlank() && !isAiLoading) {
                                        val q = inputText
                                        inputText = ""
                                        speakNextAiResponse = autoSpeakReplies
                                        viewModel.askAiAssistant(q)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ByeProTheme.colors.goldPrimary)
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
                }
            }
        } else if (selectedTabIndex == 1) {
            // Insights Tab
            val insights = viewModel.aiService.generateInsights(transactions, budgets, lang)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .border(1.dp, ByeProTheme.colors.goldPrimary.copy(alpha = 0.38f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.surfaceMain)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("AI INTELLIGENCE / INSIGHTS", style = MaterialTheme.typography.labelSmall, color = ByeProTheme.colors.goldPrimary, fontWeight = FontWeight.Bold)
                            Text(WebI18n.tr("مركز التحليلات الذكية", "Intelligence analytics center", lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = ByeProTheme.colors.textPrimary)
                            Text(WebI18n.tr("رؤى مبنية على بياناتك الفعلية لاكتشاف الأنماط والتنبيهات وفرص التحسين.", "Insights based on your actual data to reveal patterns, alerts and opportunities.", lang), style = MaterialTheme.typography.bodySmall, color = ByeProTheme.colors.textSecondary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(insights.size.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ByeProTheme.colors.goldPrimary)
                                Text(WebI18n.tr("إجمالي الرؤى", "Total insights", lang), style = MaterialTheme.typography.labelSmall, color = ByeProTheme.colors.textSecondary)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(insights.count { it.impactType == "POSITIVE" }.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ByeProTheme.colors.goldPrimary)
                                Text(WebI18n.tr("فرص إيجابية", "Positive signals", lang), style = MaterialTheme.typography.labelSmall, color = ByeProTheme.colors.textSecondary)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(insights.count { it.impactType == "WARNING" }.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(WebI18n.tr("تنبيهات", "Alerts", lang), style = MaterialTheme.typography.labelSmall, color = ByeProTheme.colors.textSecondary)
                            }
                        }
                    }
                    Text(
                        text = WebI18n.tr("الرؤى والتحليلات الاستباقية للإنفاق", "Proactive Spending Insights", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = WebI18n.tr("تحليلات دورية تستكشف الهدر وفرص التوفير ومخاطر الميزانيات بدقة رياضية.", "Periodic analysis of waste, savings opportunities, and budget risks.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = ByeProTheme.colors.textSecondary
                    )
                }

                items(insights) { insight ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                when (insight.impactType) {
                                    "POSITIVE" -> ByeProTheme.colors.goldPrimary.copy(alpha = 0.5f)
                                    "WARNING" -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    else -> ByeProTheme.colors.goldPrimary.copy(alpha = 0.5f)
                                },
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.surfaceMain),
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
                                        "POSITIVE" -> ByeProTheme.colors.goldPrimary
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
                                        "POSITIVE" -> ByeProTheme.colors.goldPrimary
                                        "WARNING" -> MaterialTheme.colorScheme.error
                                        else -> ByeProTheme.colors.goldPrimary
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = insight.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = ByeProTheme.colors.textPrimary
                            )
                            if (insight.estimatedSavings > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = WebI18n.tr("التوفير المتوقع: +", "Expected savings: +", lang) + TextContent.money(insight.estimatedSavings, "SAR", lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ByeProTheme.colors.goldPrimary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            AiModelsSettingsScreen(
                lang = lang,
                autoSpeakReplies = autoSpeakReplies,
                cloudGeneralEnabled = cloudGeneralEnabled,
                onCloudGeneralEnabledChange = { enabled ->
                    cloudGeneralEnabled = enabled
                    context.getSharedPreferences("bye_pro_finance_ai_models", 0)
                        .edit()
                        .putBoolean("cloud_general_enabled", enabled)
                        .apply()
                },
                localModelReady = localModelReady,
                localModelLoading = localModelLoading,
                localModelProgress = localModelProgress,
                localModelStatus = localModelStatus,
                onLoadModelFile = viewModel::loadLocalModelFile,
                onImportAndLoadModel = viewModel::importAndLoadLocalModel,
                onAutoSpeakRepliesChange = { enabled ->
                    autoSpeakReplies = enabled
                    context.getSharedPreferences("bye_pro_ai_settings", 0)
                        .edit()
                        .putBoolean("auto_speak_replies", enabled)
                        .apply()
                }
            )
        }
    }

    pendingAiAction?.let { action ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelPendingAiAction(action.id) },
            title = { Text(action.title) },
            text = { Text(action.description) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmPendingAiAction(action.id) }) {
                    Text(WebI18n.tr("تأكيد التنفيذ", "Confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelPendingAiAction(action.id) }) {
                    Text(WebI18n.tr("إلغاء", "Cancel", lang))
                }
            }
        )
    }
}

@Composable
fun ChatBubbleItem(message: ChatMessage, lang: AppLanguage, autoSpeak: Boolean = false) {
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
                containerColor = if (isUser) ByeProTheme.colors.goldPrimary else ByeProTheme.colors.surfaceMain
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(
                    1.dp,
                    if (isUser) ByeProTheme.colors.goldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) WebI18n.tr("أنت", "You", lang) else "AI Bye Pro",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else ByeProTheme.colors.goldPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else ByeProTheme.colors.textPrimary
                )
                if (!isUser) {
                    SpeakTextButton(
                        text = message.text,
                        languageCode = if (lang == AppLanguage.AR) "ar" else "en",
                        autoSpeak = autoSpeak
                    )
                }
            }
        }
    }
}
