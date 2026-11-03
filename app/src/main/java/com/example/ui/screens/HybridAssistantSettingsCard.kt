package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ByeProTheme

@Composable
fun HybridAssistantSettingsCard(
    cloudGeneralEnabled: Boolean,
    onCloudGeneralEnabledChange: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("وضع الذكاء الاصطناعي الهجين", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("السماح بالمساعد السحابي للأسئلة العامة")
                    Text(
                        "عند التفعيل، يستخدم التطبيق النموذج المحلي أولاً، ثم يحاول الخدمة السحابية عند عدم توفره أو فشله. يُرسل نص السؤال العام فقط؛ لا تُرسل الحسابات أو المعاملات أو الأرصدة المالية إلى هذا المسار.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = cloudGeneralEnabled, onCheckedChange = onCloudGeneralEnabledChange, colors = SwitchDefaults.colors(checkedThumbColor = ByeProTheme.colors.goldPrimary, checkedTrackColor = ByeProTheme.colors.goldPrimary.copy(alpha = 0.28f)))
            }
            Text(
                "يتطلب الاتصال بالإنترنت وجلسة تسجيل دخول صالحة. عند إيقاف الخيار أو تعذر الخدمة، يعود التطبيق إلى التحليل المحلي الحتمي.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
