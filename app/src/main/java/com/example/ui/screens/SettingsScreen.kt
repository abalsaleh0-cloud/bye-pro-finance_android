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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    val timeZone by viewModel.timeZone.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()
    val numberFormat by viewModel.numberFormat.collectAsState()
    val emailNotifications by viewModel.emailNotifications.collectAsState()
    val transactionAlerts by viewModel.transactionAlerts.collectAsState()
    val billReminders by viewModel.billReminders.collectAsState()
    val budgetAlerts by viewModel.budgetAlerts.collectAsState()
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var profileName by remember(currentUser?.id, currentUser?.name) { mutableStateOf(currentUser?.name.orEmpty()) }

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
                    Spacer(modifier = Modifier.height(10.dp))
                    FinanceTextField(
                        value = profileName,
                        onValueChange = { profileName = it.take(100) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                        label = { Text(WebI18n.tr("الاسم الشخصي", "Display name", lang)) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FinanceButton(
                        onClick = { currentUser?.let { user -> viewModel.updateLocalProfile(user.copy(name = profileName.trim())) } },
                        enabled = profileName.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().testTag("save_profile_name_btn")
                    ) { Text(WebI18n.tr("حفظ الاسم محلياً", "Save name locally", lang)) }
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
                        FinanceOutlinedButton(
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

        // Personal regional formats — persisted on device, separate from application-wide configuration.
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(WebI18n.tr("التنسيقات والمنطقة الزمنية", "Formats and time zone", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SettingsChoiceRow(WebI18n.tr("المنطقة الزمنية", "Time zone", lang), timeZone, listOf("Africa/Tripoli", "Asia/Damascus", "Asia/Amman", "Europe/Istanbul", "Europe/London", "UTC"), viewModel::setTimeZone)
                    SettingsChoiceRow(WebI18n.tr("تنسيق التاريخ", "Date format", lang), dateFormat, listOf("YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY"), viewModel::setDateFormat)
                    SettingsChoiceRow(WebI18n.tr("تنسيق الأرقام", "Number format", lang), numberFormat, listOf("1,234.56", "1.234,56", "1 234,56"), viewModel::setNumberFormat)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(WebI18n.tr("تفضيلات التنبيهات", "Notification preferences", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SettingsSwitchRow(WebI18n.tr("إشعارات البريد", "Email notifications", lang), emailNotifications, viewModel::setEmailNotifications)
                    SettingsSwitchRow(WebI18n.tr("تنبيهات المعاملات", "Transaction alerts", lang), transactionAlerts, viewModel::setTransactionAlerts)
                    SettingsSwitchRow(WebI18n.tr("تذكيرات الفواتير", "Bill reminders", lang), billReminders, viewModel::setBillReminders)
                    SettingsSwitchRow(WebI18n.tr("تنبيهات الميزانيات", "Budget alerts", lang), budgetAlerts, viewModel::setBudgetAlerts)
                    Text(WebI18n.tr("هذه التفضيلات تُحفظ على هذا الجهاز حالياً، ولا ترسل إشعارات بريدية أو تربط الحساب بالخادم.", "These preferences are stored on this device; they do not send email or connect to a server.", lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Application information is intentionally opened on demand, matching the web "About" area.
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(WebI18n.tr("حول التطبيق", "About the app", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        WebI18n.tr("عرض بيانات التطبيق وإصدار Android والتقنيات المستخدمة.", "View application details, Android version, and technologies.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FinanceOutlinedButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("about_application_button")
                    ) { Text(WebI18n.tr("حول التطبيق", "About application", lang)) }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(WebI18n.tr("بيانات التطبيق", "Application information", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("BYE PRO FINANCE")
                    Text(WebI18n.tr("الإصدار: ", "Version: ", lang) + com.example.BuildConfig.VERSION_NAME)
                    Text(WebI18n.tr("المنصة: Android", "Platform: Android", lang))
                    Text(WebI18n.tr("محرك الحسابات: BigDecimal", "Finance engine: BigDecimal", lang))
                    Text(WebI18n.tr("قاعدة البيانات: Room SQLite", "Database: Room SQLite", lang))
                    Text(WebI18n.tr("الواجهة: Kotlin + Jetpack Compose", "UI: Kotlin + Jetpack Compose", lang))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(WebI18n.tr("إغلاق", "Close", lang))
                }
            }
        )
    }
}

@Composable
private fun SettingsChoiceRow(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            FinanceOutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(option); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
