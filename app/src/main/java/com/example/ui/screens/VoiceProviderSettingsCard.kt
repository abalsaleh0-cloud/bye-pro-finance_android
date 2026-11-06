package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.finance.ai.ElevenLabsConnectionResult
import com.example.finance.ai.ElevenLabsSpeechProvider
import com.example.finance.ai.SecureVoiceSettings
import com.example.ui.theme.ByeProTheme
import com.example.finance.model.AppLanguage
import com.example.finance.i18n.WebI18n
import kotlinx.coroutines.launch

@Composable
fun VoiceProviderSettingsCard(lang: AppLanguage) {
    val context = LocalContext.current
    val settings = remember(context) { SecureVoiceSettings(context.applicationContext) }
    val provider = remember(context) { ElevenLabsSpeechProvider(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var apiKey by remember { mutableStateOf("") }
    var voiceId by remember { mutableStateOf(settings.readVoiceId()) }
    var isTesting by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf(
            if (settings.hasApiKey()) WebI18n.tr("المفتاح محفوظ، لكن لم يتم التحقق من الاتصال بعد.", "API key saved; connection has not been verified yet.", lang)
            else WebI18n.tr("لم تتم إضافة مفتاح صوت سحابي بعد. سيستخدم التطبيق الصوت المحلي.", "No cloud voice key added. Local device speech will be used.", lang)
        )
    }

    fun persistSettings(): Boolean = try {
        if (apiKey.isNotBlank()) settings.saveApiKey(apiKey)
        settings.saveVoiceId(voiceId)
        apiKey = ""
        true
    } catch (error: Exception) {
        status = WebI18n.tr(
            "تعذر حفظ إعدادات الصوت بأمان. تحقق من إعدادات أمان الجهاز ثم أعد المحاولة.",
            "Could not securely save voice settings. Check device security settings and try again.",
            lang
        )
        false
    }

    fun resultMessage(result: ElevenLabsConnectionResult): String = when (result) {
        ElevenLabsConnectionResult.CONNECTED ->
            WebI18n.tr("متصل بنجاح — مفتاح ElevenLabs صالح.", "Connected — ElevenLabs API key is valid.", lang)
        ElevenLabsConnectionResult.MISSING_KEY ->
            WebI18n.tr("أدخل مفتاح API واحفظه أولاً.", "Enter and save an API key first.", lang)
        ElevenLabsConnectionResult.INVALID_KEY ->
            WebI18n.tr("فشل التحقق: مفتاح API غير صالح (401).", "Verification failed: invalid API key (401).", lang)
        ElevenLabsConnectionResult.FORBIDDEN ->
            WebI18n.tr("المفتاح وصل إلى ElevenLabs لكن صلاحية هذا الطلب مرفوضة (403). راجع صلاحيات User: Read أو قيود IP للمفتاح.", "ElevenLabs rejected this endpoint (403). Check User: Read scope and the key's IP allowlist.", lang)
        ElevenLabsConnectionResult.BAD_REQUEST ->
            WebI18n.tr("رفضت ElevenLabs صيغة الطلب (400). راجع إعدادات المفتاح ثم أعد المحاولة.", "ElevenLabs rejected the request (400). Review the API key settings and retry.", lang)
        ElevenLabsConnectionResult.NOT_FOUND ->
            WebI18n.tr("لم يتم العثور على مسار اختبار الاتصال (404). تحقق من الشبكة أو حدّث التطبيق.", "The connection-test endpoint was not found (404). Check the network or update the app.", lang)
        ElevenLabsConnectionResult.INVALID_REQUEST ->
            WebI18n.tr("رفضت ElevenLabs الطلب (422). تحقق من إعدادات المفتاح ثم أعد المحاولة.", "ElevenLabs could not process the request (422). Review the key settings and retry.", lang)
        ElevenLabsConnectionResult.OTHER_HTTP_RESPONSE ->
            WebI18n.tr("أعادت ElevenLabs استجابة HTTP غير متوقعة. تحقق من صلاحيات المفتاح وحاول لاحقاً.", "ElevenLabs returned an unexpected HTTP response. Check key permissions and retry.", lang)
        ElevenLabsConnectionResult.TIMEOUT ->
            WebI18n.tr("انتهت مهلة الاتصال بـ ElevenLabs. جرّب شبكة إنترنت أخرى.", "The connection to ElevenLabs timed out. Try another network.", lang)
        ElevenLabsConnectionResult.TLS_ERROR ->
            WebI18n.tr("تعذّر إنشاء اتصال TLS آمن مع ElevenLabs. تحقق من تاريخ ووقت الهاتف وجرّب شبكة أخرى.", "A secure TLS connection to ElevenLabs failed. Check the phone date/time and try another network.", lang)
        ElevenLabsConnectionResult.DNS_ERROR ->
            WebI18n.tr("تعذّر العثور على خادم ElevenLabs عبر DNS. تحقق من الشبكة أو إعداد DNS الخاص.", "DNS could not resolve ElevenLabs. Check the network or private DNS settings.", lang)
        ElevenLabsConnectionResult.RATE_LIMITED ->
            WebI18n.tr("تم تقييد الطلبات مؤقتاً (429). حاول لاحقاً.", "Requests are temporarily rate-limited (429). Try again later.", lang)
        ElevenLabsConnectionResult.SERVER_ERROR ->
            WebI18n.tr("خدمة ElevenLabs تواجه مشكلة مؤقتة. حاول لاحقاً.", "ElevenLabs is temporarily unavailable. Try again later.", lang)
        ElevenLabsConnectionResult.NETWORK_ERROR ->
            WebI18n.tr("تعذر الاتصال بالإنترنت أو الوصول إلى ElevenLabs.", "Could not reach ElevenLabs. Check your internet connection.", lang)
        ElevenLabsConnectionResult.REQUEST_FAILED ->
            WebI18n.tr("حدث خطأ غير متوقع أثناء اختبار الاتصال. لم يتم عرض المفتاح أو تسجيله. أعد المحاولة، وإذا تكرر الخطأ أرسل نص هذه الرسالة فقط.", "An unexpected error occurred during the connection test. The key was not displayed or logged. Retry; if it persists, share only this message.", lang)
    }

    Card(colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(WebI18n.tr("الصوت البشري الطبيعي", "Natural human voice", lang), style = MaterialTheme.typography.titleMedium, color = ByeProTheme.colors.textPrimary)
            Text(
                WebI18n.tr("يمكن استخدام ElevenLabs عند توفر الإنترنت. أدخل مفتاح API وVoice ID. المفتاح يُشفّر باستخدام Android Keystore ولا يُحفظ كنص عادي.", "Use ElevenLabs when online. Enter your API key and Voice ID. The key is encrypted with Android Keystore and never stored as plain text.", lang),
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
                        if (persistSettings()) {
                            status = if (settings.hasApiKey()) {
                                WebI18n.tr("تم حفظ المفتاح مشفّراً. اضغط اختبار الاتصال للتحقق من صلاحيته.", "API key saved securely. Tap Test connection to verify it.", lang)
                            } else {
                                WebI18n.tr("تم حفظ Voice ID. أضف مفتاح API لتفعيل الصوت السحابي.", "Voice ID saved. Add an API key to enable cloud speech.", lang)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(WebI18n.tr("حفظ الإعدادات", "Save settings", lang)) }
                OutlinedButton(
                    enabled = !isTesting,
                    onClick = {
                        if (persistSettings()) {
                            isTesting = true
                            status = WebI18n.tr("جارٍ التحقق من مفتاح ElevenLabs…", "Verifying ElevenLabs API key…", lang)
                            scope.launch {
                                try {
                                    val result = runCatching { provider.testConnection() }
                                        .getOrElse { ElevenLabsConnectionResult.REQUEST_FAILED }
                                    status = resultMessage(result)
                                } finally {
                                    isTesting = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTesting) CircularProgressIndicator(strokeWidth = 2.dp)
                    else Text(WebI18n.tr("اختبار الاتصال", "Test connection", lang))
                }
            }
            OutlinedButton(
                enabled = !isTesting,
                onClick = {
                    runCatching { settings.saveApiKey("") }
                        .onSuccess {
                            apiKey = ""
                            status = WebI18n.tr("تم حذف مفتاح الصوت السحابي. سيعود المساعد إلى الصوت المحلي.", "Cloud voice key deleted. The assistant will use local speech.", lang)
                        }
                        .onFailure {
                            status = WebI18n.tr("تعذر حذف المفتاح من التخزين الآمن.", "Could not delete the key from secure storage.", lang)
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(WebI18n.tr("حذف المفتاح", "Delete key", lang)) }
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
