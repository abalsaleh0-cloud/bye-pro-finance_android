package com.example.ui.screens

import androidx.compose.material.icons.filled.*

import com.example.finance.i18n.TextContent

import com.example.ui.components.FinanceAlertDialog
import com.example.ui.components.FinanceButton
import com.example.ui.components.FinanceOutlinedButton
import com.example.ui.components.FinanceTextField
import com.example.ui.components.FinanceIconButton
import com.example.ui.components.FinanceFloatingActionButton

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finance.i18n.Strings
import com.example.finance.model.AuditLogEntity
import com.example.finance.model.NotificationEntity
import com.example.finance.model.UserRole
import com.example.finance.ui.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsAdminScreen(
    viewModel: FinanceViewModel,
    initialTabIndex: Int = 0
) {
    val lang by viewModel.appLanguage.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var selectedTabIndex by remember(initialTabIndex) { mutableIntStateOf(initialTabIndex.coerceIn(0, 1)) }
    var notificationFilter by remember { mutableIntStateOf(0) }
    val filteredNotifications = when (notificationFilter) { 1 -> notifications.filter { !it.isRead }; 2 -> notifications.filter { it.isRead }; else -> notifications }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = ByeProTheme.colors.financialGreen
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text(Strings.get("nav_notifications", lang), fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text(Strings.get("nav_admin", lang), fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTabIndex == 0) {
            // Notifications List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = WebI18n.tr("تنبيهات النظام والمواعيد المالية", "System alerts and financial due dates", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (notifications.any { !it.isRead }) FinanceOutlinedButton(onClick = { viewModel.markAllNotificationsRead() }, shape = RoundedCornerShape(8.dp)) {
                                Text(WebI18n.tr("قراءة الكل", "Read all", lang), style = MaterialTheme.typography.labelSmall)
                            }
                            if (notifications.isNotEmpty()) FinanceOutlinedButton(onClick = { viewModel.clearNotifications() }, shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(WebI18n.tr("مسح الكل", "Clear all", lang), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(WebI18n.tr("الكل", "All", lang), WebI18n.tr("غير مقروء", "Unread", lang), WebI18n.tr("مقروء", "Read", lang)).forEachIndexed { index, label ->
                            FinanceOutlinedButton(onClick = { notificationFilter = index }, shape = RoundedCornerShape(8.dp)) {
                                Text((if (notificationFilter == index) "✓ " else "") + label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Text(WebI18n.tr("غير المقروء: ", "Unread: ", lang) + notifications.count { !it.isRead }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (filteredNotifications.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = WebI18n.tr("لا توجد تنبيهات جديدة في الوقت الحالي.", "There are no new alerts right now.", lang),
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredNotifications, key = { it.id }) { notif ->
                        NotificationCardItem(
                            notification = notif,
                            lang = lang,
                            onRead = { viewModel.markNotificationRead(notif.id) },
                            onDelete = { viewModel.deleteNotification(notif.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        } else {
            // Admin & Audit Trail
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = Strings.get("admin_title", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = WebI18n.tr("مراجعة السجلات المحلية وحالة تشغيل التطبيق. إدارة الصلاحيات المركزية غير متاحة في الوضع المحلي.", "Review local records and app runtime status. Centralized role management is unavailable in local mode.", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Local profile role is informational only; it is not server-enforced RBAC.
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, ByeProTheme.colors.goldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = WebI18n.tr("الدور المسجل محلياً (غير موثّق خادمياً): ", "Locally stored role (not server-verified): ", lang) + (currentUser?.role?.name ?: "USER"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = WebI18n.tr(
                                    "هذه نسخة محلية تعمل بقاعدة Room على الجهاز. إدارة الأدوار الحقيقية وسجل التدقيق المركزي يتطلبان مصادقة وخادماً متصلاً؛ لذلك لا يمكن منح أو سحب صلاحيات من هذه الشاشة.",
                                    "This is a local Room-based build. Real role management and centralized audit logging require authenticated server access; roles cannot be granted or revoked here."
                                , lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // System Health & Storage Status
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = ByeProTheme.colors.financialGreen)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(WebI18n.tr("تخزين البيانات", "Data storage", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(WebI18n.tr("Room SQLite محلي", "Local Room SQLite", lang), fontWeight = FontWeight.Bold, color = ByeProTheme.colors.financialGreen)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(WebI18n.tr("السجلات المحلية", "Local records", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(WebI18n.tr("${TextContent.number(transactions.size + accounts.size + auditLogs.size, lang, 0)} سجل", "${TextContent.number(transactions.size + accounts.size + auditLogs.size, lang, 0)} records", lang), fontWeight = FontWeight.Bold, color = ByeProTheme.colors.goldPrimary)
                            }
                        }
                    }
                }

                // Audit Logs Header
                item {
                    Text(
                        text = WebI18n.tr("سجل التدقيق المحلي", "Local audit log", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (auditLogs.isEmpty()) {
                    item {
                        Text(
                            text = WebI18n.tr("لا توجد سجلات تدقيق بعد.", "No audit records yet.", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(auditLogs.take(20), key = { it.id }) { log ->
                        AuditLogRowItem(log = log)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(
    notification: NotificationEntity,
    lang: com.example.finance.model.AppLanguage,
    onRead: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (!notification.isRead) ByeProTheme.colors.financialGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (!notification.isRead) ByeProTheme.colors.financialGreen else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                if (!notification.isRead) {
                    FinanceIconButton(onClick = onRead) {
                        Icon(Icons.Default.Check, contentDescription = WebI18n.tr("تحديد كمقروء", "Mark as read", lang), tint = ByeProTheme.colors.financialGreen)
                    }
                }
                FinanceIconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = WebI18n.tr("حذف التنبيه", "Delete notification", lang), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AuditLogRowItem(log: AuditLogEntity) {
    val dateStr = try {
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    } catch (_: Exception) {
        ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "[${log.action}] ${log.resource}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ByeProTheme.colors.financialGreen
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
