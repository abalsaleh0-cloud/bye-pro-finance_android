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

@Composable
fun VoiceProviderSettingsCard() {
    val context = LocalContext.current
    val settings = remember(context) { SecureVoiceSettings(context.applicationContext) }
    var apiKey by remember { mutableStateOf("") }
    var voiceId by remember { mutableStateOf(settings.readVoiceId()) }
    var status by remember {
        mutableStateOf(
            if (settings.hasApiKey()) "تم حفظ مفتاح الصوت السحابي بشكل مشفر على هذا الجهاز."
            else "لم تتم إضافة مفتاح صوت سحابي بعد. سيستخدم التطبيق الصوت المحلي عند عدم الاتصال."
        )
    }

    Card(colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("الصوت البشري الطبيعي", style = MaterialTheme.typography.titleMedium)
            Text(
                "للحصول على صوت أكثر واقعية، يمكن استخدام ElevenLabs عند توفر الإنترنت. أدخل مفتاح API الخاص بك وVoice ID. المفتاح يُشفّر باستخدام Android Keystore ولا يُحفظ كنص عادي.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ElevenLabs API Key") },
                placeholder = { Text(if (settings.hasApiKey()) "مفتاح محفوظ؛ أدخل قيمة جديدة للاستبدال" else "أدخل مفتاح API") },
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
                            "تم حفظ إعدادات الصوت. سيُستخدم الصوت السحابي أولاً مع الرجوع للصوت المحلي عند تعذر الاتصال."
                        } else {
                            "تم حفظ Voice ID. أضف مفتاح API لتفعيل الصوت السحابي."
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("حفظ الإعدادات") }
                OutlinedButton(
                    onClick = {
                        settings.saveApiKey("")
                        apiKey = ""
                        status = "تم حذف مفتاح الصوت السحابي. سيعود المساعد إلى الصوت المحلي."
                    }
                ) { Text("حذف المفتاح") }
            }
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
