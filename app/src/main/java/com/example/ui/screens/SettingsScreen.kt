package com.example.ui.screens

import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField

import com.example.finance.i18n.WebI18n
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.i18n.Strings
import com.example.finance.model.AppLanguage
import com.example.finance.ui.FinanceViewModel

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val primaryCurrency by viewModel.primaryCurrency.collectAsState()
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Strings.get("settings_title", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = WebI18n.tr("تخصيص تفضيلات اللغة، العملة، والحماية الشخصية.", "Customize language, currency, and security preferences.", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Profile Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ByeProTheme.colors.financialGreen)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = WebI18n.tr("الملف الشخصي والحساب", "Profile and account", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentUser?.name ?: WebI18n.tr("ملف محلي", "Local profile", lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentUser?.email ?: WebI18n.tr("لا يوجد حساب مرتبط", "No linked account", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Language Switcher
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = Strings.get("language", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FinanceButton(
                            onClick = { viewModel.setLanguage(AppLanguage.AR) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lang == AppLanguage.AR) ByeProTheme.colors.financialGreen else MaterialTheme.colorScheme.background,
                                contentColor = if (lang == AppLanguage.AR) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_lang_ar_btn")
                        ) {
                            Text(Strings.get("arabic", lang), fontWeight = FontWeight.Bold)
                        }

                        FinanceButton(
                            onClick = { viewModel.setLanguage(AppLanguage.EN) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lang == AppLanguage.EN) ByeProTheme.colors.financialGreen else MaterialTheme.colorScheme.background,
                                contentColor = if (lang == AppLanguage.EN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_lang_en_btn")
                        ) {
                            Text(Strings.get("english", lang), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Primary currency — mirrors the web settings default and available options.
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
                        text = Strings.get("currency_pref", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        OutlinedButton(
                            onClick = { currencyMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("primary_currency_selector")
                        ) {
                            Text(primaryCurrency)
                        }
                        DropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false }
                        ) {
                            listOf("LYD", "USD", "EUR", "GBP", "SAR", "AED", "EGP").forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code) },
                                    onClick = {
                                        viewModel.setPrimaryCurrency(code)
                                        currencyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Appearance / Theme Selector
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
                        text = Strings.get("appearance_title", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Strings.get("appearance_hint", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Strings.get(if (isDark) "dark_mode" else "light_mode", lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isDark,
                            onCheckedChange = viewModel::setDarkMode,
                            modifier = Modifier.testTag("theme_mode_switch")
                        )
                    }
                }
            }
        }

        // App System Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "BYE PRO FINANCE Enterprise",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ByeProTheme.colors.financialGreen
                    )
                    Text(WebI18n.tr("الإصدار: 1.0.0 (Production Core)", "Version: 1.0.0 (Production Core)", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(WebI18n.tr("محرك الحسابات: BigDecimal Exact Precision Engine", "Finance engine: BigDecimal Exact Precision Engine", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(WebI18n.tr("الذكاء الاصطناعي: Google Gemini Flash Multimodal", "AI: Google Gemini Flash Multimodal", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(WebI18n.tr("قاعدة البيانات: Room SQLite + ACID Transactions", "Database: Room SQLite + ACID Transactions", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
