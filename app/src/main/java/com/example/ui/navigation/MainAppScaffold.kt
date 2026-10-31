package com.example.ui.navigation

import com.example.finance.i18n.TextContent

import com.example.ui.theme.ByeProTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartScreen
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.finance.i18n.LocalAppLanguage
import com.example.finance.i18n.Strings
import com.example.finance.i18n.appText
import com.example.finance.i18n.WebI18n
import com.example.finance.model.AppLanguage
import com.example.finance.ui.FinanceViewModel
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AddTransactionDialog
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtsBillsScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.ImportExportScreen
import com.example.ui.screens.HouseholdScreen
import com.example.ui.screens.NotificationsAdminScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SavingsAssetsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartScanScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.TransfersScreen
import kotlinx.coroutines.launch

data class HubMenuItem(
    val id: String,
    val titleKey: String,
    val icon: ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: FinanceViewModel
) {
    val lang by viewModel.appLanguage.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    val unreadCount = notifications.count { !it.isRead }
    var showHubBottomSheet by remember { mutableStateOf(false) }
    var showQuickAddTx by remember { mutableStateOf(false) }

    // Language changes text only; the app shell and all navigation remain RTL.
    val layoutDirection = WebI18n.layoutDirection()

    // BackHandler to safely return to dashboard
    BackHandler(enabled = currentScreen != "dashboard") {
        viewModel.setScreen("dashboard")
    }

    val hubMenuItems = listOf(
        HubMenuItem("transfers", "nav_transfers", Icons.Default.SwapHoriz, ByeProTheme.colors.goldPrimary),
        HubMenuItem("debts", "nav_debts", Icons.Default.MoneyOff, ByeProTheme.colors.financialRed),
        HubMenuItem("bills", "nav_bills", Icons.Default.ReceiptLong, ByeProTheme.colors.financialRed),
        HubMenuItem("savings", "nav_savings", Icons.Default.Savings, ByeProTheme.colors.goldPrimary),
        HubMenuItem("assets", "nav_assets", Icons.Default.AccountBalance, ByeProTheme.colors.goldPrimary),
        HubMenuItem("reports", "nav_reports", Icons.Default.TableChart, ByeProTheme.colors.financialGreen),
        HubMenuItem("household", "nav_household", Icons.Default.Group, ByeProTheme.colors.financialGreen),
        HubMenuItem("documents", "nav_documents", Icons.Default.ReceiptLong, ByeProTheme.colors.goldPrimary),
        HubMenuItem("smart_scan", "nav_smart_scan", Icons.Default.SmartScreen, ByeProTheme.colors.goldPrimary),
        HubMenuItem("import_export", "nav_import_export", Icons.Default.TableChart, ByeProTheme.colors.goldPrimary),
        HubMenuItem("ai_assistant", "nav_ai_assistant", Icons.Default.AutoAwesome, ByeProTheme.colors.goldPrimary),
        HubMenuItem("notifications", "nav_notifications", Icons.Default.Notifications, ByeProTheme.colors.goldPrimary),
        HubMenuItem("admin", "nav_admin", Icons.Default.AdminPanelSettings, ByeProTheme.colors.financialGreen),
        HubMenuItem("settings", "nav_settings", Icons.Default.Settings, MaterialTheme.colorScheme.onSurface)
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppLanguage provides lang
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, ByeProTheme.colors.goldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.bye_pro_finance_icon),
                                    contentDescription = "BYE PRO Logo",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = appText("app_title"),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = ByeProTheme.colors.goldPrimary
                            )
                        }
                    },
                    actions = {
                        // Quick Language Toggle
                        Surface(
                            onClick = { viewModel.toggleLanguage() },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .testTag("top_lang_toggle_btn")
                                .padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = ByeProTheme.colors.goldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (lang == AppLanguage.AR) "EN" else "عربي",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Notifications Badge Button
                        IconButton(
                            onClick = { viewModel.setScreen("notifications") },
                            modifier = Modifier.testTag("top_notifications_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(containerColor = ByeProTheme.colors.financialRed) {
                                            Text(TextContent.number(unreadCount, lang, 0), color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = Strings.get("nav_notifications", lang),
                                    tint = if (unreadCount > 0) ByeProTheme.colors.goldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "dashboard",
                        onClick = { viewModel.setScreen("dashboard") },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = Strings.get("nav_dashboard", lang)) },
                        label = { Text(appText("nav_dashboard"), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ByeProTheme.colors.goldPrimary,
                            selectedTextColor = ByeProTheme.colors.goldPrimary,
                            indicatorColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_dashboard_tab")
                    )

                    NavigationBarItem(
                        selected = currentScreen == "accounts",
                        onClick = { viewModel.setScreen("accounts") },
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = Strings.get("nav_accounts", lang)) },
                        label = { Text(appText("nav_accounts"), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ByeProTheme.colors.goldPrimary,
                            selectedTextColor = ByeProTheme.colors.goldPrimary,
                            indicatorColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_accounts_tab")
                    )

                    NavigationBarItem(
                        selected = currentScreen == "transactions",
                        onClick = { viewModel.setScreen("transactions") },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = Strings.get("nav_transactions", lang)) },
                        label = { Text(appText("nav_transactions"), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ByeProTheme.colors.goldPrimary,
                            selectedTextColor = ByeProTheme.colors.goldPrimary,
                            indicatorColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_transactions_tab")
                    )

                    NavigationBarItem(
                        selected = currentScreen == "budgets",
                        onClick = { viewModel.setScreen("budgets") },
                        icon = { Icon(Icons.Default.PieChart, contentDescription = Strings.get("nav_budgets", lang)) },
                        label = { Text(appText("nav_budgets"), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ByeProTheme.colors.goldPrimary,
                            selectedTextColor = ByeProTheme.colors.goldPrimary,
                            indicatorColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_budgets_tab")
                    )

                    NavigationBarItem(
                        selected = currentScreen !in listOf("dashboard", "accounts", "transactions", "budgets"),
                        onClick = { showHubBottomSheet = true },
                        icon = { Icon(Icons.Default.Menu, contentDescription = WebI18n.tr("القائمة", "Hub menu", lang)) },
                        label = { Text(WebI18n.tr("المزيد", "More", lang), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ByeProTheme.colors.goldPrimary,
                            selectedTextColor = ByeProTheme.colors.goldPrimary,
                            indicatorColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("nav_more_hub_tab")
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.setScreen(it) },
                        onOpenAddTransaction = { showQuickAddTx = true }
                    )
                    "accounts" -> AccountsScreen(viewModel = viewModel)
                    "transactions" -> TransactionsScreen(viewModel = viewModel)
                    "transfers" -> TransfersScreen(viewModel = viewModel)
                    "budgets" -> BudgetsScreen(viewModel = viewModel)
                    "debts" -> DebtsBillsScreen(viewModel = viewModel, initialTabIndex = 0)
                    "bills" -> DebtsBillsScreen(viewModel = viewModel, initialTabIndex = 1)
                    "savings" -> SavingsAssetsScreen(viewModel = viewModel, initialTabIndex = 0)
                    "assets" -> SavingsAssetsScreen(viewModel = viewModel, initialTabIndex = 1)
                    "documents" -> DocumentsScreen(viewModel = viewModel)
                    "import_export" -> ImportExportScreen(viewModel = viewModel)
                    "reports" -> ReportsScreen(viewModel = viewModel)
                    "household" -> HouseholdScreen(viewModel = viewModel)
                    "smart_scan" -> SmartScanScreen(viewModel = viewModel)
                    "ai_assistant" -> AiAssistantScreen(viewModel = viewModel)
                    "notifications" -> NotificationsAdminScreen(viewModel = viewModel, initialTabIndex = 0)
                    "admin" -> NotificationsAdminScreen(viewModel = viewModel, initialTabIndex = 1)
                    "settings" -> SettingsScreen(viewModel = viewModel)
                    else -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.setScreen(it) },
                        onOpenAddTransaction = { showQuickAddTx = true }
                    )
                }
            }
        }
    }

    // Hub Navigation Bottom Sheet
    if (showHubBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHubBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = WebI18n.tr("مركز العمليات المالية (Finance Hub)", "Financial Hub", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(hubMenuItems) { item ->
                        Surface(
                            onClick = {
                                viewModel.setScreen(item.id)
                                showHubBottomSheet = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .testTag("hub_${item.id}_item")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(item.accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = Strings.get(item.titleKey, lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Quick Add Transaction Dialog
    if (showQuickAddTx) {
        AddTransactionDialog(
            accounts = accounts,
            lang = lang,
            onDismiss = { showQuickAddTx = false },
            onConfirm = { tx ->
                viewModel.addTransaction(tx)
                showQuickAddTx = false
            }
        )
    }
}
