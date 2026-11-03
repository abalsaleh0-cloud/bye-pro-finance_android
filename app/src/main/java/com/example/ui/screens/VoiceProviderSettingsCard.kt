package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.finance.ai.SecureVoiceSettings
import com.example.ui.theme.ByeProTheme
import com.example.finance.model.AppLanguage
import com.example.finance.i18n.WebI18n

@Composable
fun VoiceProviderSettingsCard(lang: AppLanguage) {
    val context = LocalContext.current
    val settings = remember(context) { SecureVoiceSettings(context.applicationContext) }
    var apiKey by remember { mutableStateOf("") }
    var voiceId by remember { mutableStateOf(settings.readVoiceId()) }
    var status by remember {
        mutableStateOf(
            if (settings.hasApiKey()) WebI18n.tr("تم حفظ مفتاح الصوت السحابي بشكل مشفر على هذا الجهاز.", "Cloud voice key is encrypted and saved on this device.", lang)
            else WebI18n.tr("لم تتم إضافة مفتاح صوت سحابي بعد. سيستخدم التطبيق الصوت المحلي عند عدم الاتصال.", "No cloud voice key added. Local device speech will be used offline.", lang)
        )
    }

    Card(colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(WebI18n.tr("الصوت البشري الطبيعي", "Natural human voice", lang), style = MaterialTheme.typography.titleMedium, color = ByeProTheme.colors.textPrimary)
            Text(
                WebI18n.tr("للحصول على صوت أكثر واقعية، يمكن استخدام ElevenLabs عند توفر الإنترنت. أدخل مفتاح API الخاص بك وVoice ID. المفتاح يُشفّر باستخدام Android Keystore ولا يُحفظ كنص عادي.", "For more natural speech, connect ElevenLabs when online. Enter your API key and Voice ID. The key is encrypted with Android Keystore and never stored as plain text.", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ElevenLabs API Key") },
                placeholder = { Text(if (settings.hasApiKey()) WebI18n.tr("مفتاح محفوظ؛ أدخل قيمة جديدة للاستبدال", "Key saved; enter a new value to replace it", lang) else WebI18n.tr("أدخل مفتاح API", "Enter API key", lang)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            OutlinedTextField(
                value = voiceId,
                onValueChange = { voiceId = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Voice ID") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (apiKey.isNotBlank()) settings.saveApiKey(apiKey)
                        settings.saveVoiceId(voiceId)
                        apiKey = ""
                        status = if (settings.hasApiKey()) {
                            WebI18n.tr("تم حفظ إعدادات الصوت. سيُستخدم الصوت السحابي أولاً مع الرجوع للصوت المحلي عند تعذر الاتصال.", "Voice settings saved. Cloud speech will be used first, with local speech as fallback.", lang)
                        } else {
                            WebI18n.tr("تم حفظ Voice ID. أضف مفتاح API لتفعيل الصوت السحابي.", "Voice ID saved. Add an API key to enable cloud speech.", lang)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(WebI18n.tr("حفظ الإعدادات", "Save settings", lang)) }
                OutlinedButton(
                    onClick = {
                        settings.saveApiKey("")
                        apiKey = ""
                        status = WebI18n.tr("تم حذف مفتاح الصوت السحابي. سيعود المساعد إلى الصوت المحلي.", "Cloud voice key deleted. The assistant will use local speech.", lang)
                    }
                ) { Text(WebI18n.tr("حذف المفتاح", "Delete key", lang)) }
            }
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
