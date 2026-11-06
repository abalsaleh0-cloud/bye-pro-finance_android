package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.i18n.WebI18n
import com.example.finance.model.AppLanguage
import com.example.ui.theme.ByeProTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun selectedDocumentName(context: android.content.Context, uri: Uri): String {
    var name = uri.lastPathSegment?.substringAfterLast('/') ?: ""
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) name = cursor.getString(index) ?: name
        }
    }
    return name
}

@Composable
fun AiModelsSettingsScreen(
    lang: AppLanguage,
    localModelReady: Boolean,
    localModelLoading: Boolean,
    localModelProgress: Float,
    localModelStatus: String,
    onImportAndLoadModel: (Uri) -> Unit,
    onTestLocalModel: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectionError by remember { mutableStateOf<String?>(null) }
    val modelPreferences = context.getSharedPreferences("bye_pro_finance_ai_models", 0)
    val visionModelReady = remember(localModelStatus) {
        val path = modelPreferences.getString("vision_model_path", null)
        modelPreferences.getBoolean("vision_model_available", false) &&
            !path.isNullOrBlank() && java.io.File(path).let { it.isFile && it.canRead() }
    }
    val textModelAvailable = remember(localModelStatus) {
        val path = modelPreferences.getString("active_model_path", null)
        !path.isNullOrBlank() && java.io.File(path).let { it.isFile && it.canRead() }
    }

    suspend fun acceptModel(uri: Uri, forVision: Boolean) {
        val name = withContext(Dispatchers.IO) { selectedDocumentName(context, uri) }
        val isVisionModel = name.endsWith(".litertlm", ignoreCase = true) &&
            name.contains("gemma-3n", ignoreCase = true)
        val isTextModel = name.endsWith(".gguf", ignoreCase = true) ||
            (name.endsWith(".litertlm", ignoreCase = true) && !isVisionModel)

        require(if (forVision) isVisionModel else isTextModel) {
            if (forVision) {
                WebI18n.tr(
                    "اختر نموذج الصور Gemma 3n بصيغة .litertlm.",
                    "Choose the Gemma 3n vision model in .litertlm format.",
                    lang
                )
            } else {
                WebI18n.tr(
                    "اختر نموذج محادثة بصيغة .gguf أو حزمة LiteRT-LM غير المخصصة للصور.",
                    "Choose a text model in .gguf format or a non-vision LiteRT-LM package.",
                    lang
                )
            }
        }
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        onImportAndLoadModel(uri)
    }

    val textModelPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching { acceptModel(uri, forVision = false) }
                .onFailure {
                    selectionError = it.message
                        ?: WebI18n.tr("تعذر اختيار النموذج.", "Could not select model.", lang)
                }
        }
    }
    val visionModelPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching { acceptModel(uri, forVision = true) }
                .onFailure {
                    selectionError = it.message
                        ?: WebI18n.tr("تعذر اختيار النموذج.", "Could not select model.", lang)
                }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            WebI18n.tr("إعدادات النماذج", "Model settings", lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ByeProTheme.colors.textPrimary
        )
        Text(
            WebI18n.tr(
                "اختر نموذجاً للمحادثة ونموذجاً للصور. يتم حفظ كل نموذج وتشغيله محلياً وبشكل مستقل.",
                "Choose one model for chat and one for images. Each model is stored and used locally and independently.",
                lang
            ),
            style = MaterialTheme.typography.bodySmall,
            color = ByeProTheme.colors.textSecondary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        WebI18n.tr("نموذج المحادثة والنصوص", "Chat & text model", lang),
                        fontWeight = FontWeight.SemiBold,
                        color = ByeProTheme.colors.textPrimary
                    )
                }
                Text(
                    if (localModelReady) WebI18n.tr("يوجد نموذج محادثة محمّل.", "A chat model is loaded.", lang)
                    else WebI18n.tr("لم يتم تحميل نموذج المحادثة.", "No chat model is loaded.", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (localModelReady) ByeProTheme.colors.goldPrimary else ByeProTheme.colors.textSecondary
                )
                OutlinedButton(
                    onClick = { textModelPicker.launch(arrayOf("*/*")) },
                    enabled = !localModelLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(WebI18n.tr("اختيار نموذج المحادثة من الهاتف", "Choose chat model from phone", lang))
                }
                OutlinedButton(
                    onClick = { onTestLocalModel(false) },
                    enabled = !localModelLoading && textModelAvailable,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(WebI18n.tr("اختبار تشغيل نموذج المحادثة", "Test chat model", lang))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ByeProTheme.colors.cardMain)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = ByeProTheme.colors.goldPrimary)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        WebI18n.tr("نموذج تحليل الصور", "Image analysis model", lang),
                        fontWeight = FontWeight.SemiBold,
                        color = ByeProTheme.colors.textPrimary
                    )
                }
                Text(
                    if (visionModelReady) WebI18n.tr("نموذج الصور محفوظ وجاهز عند طلب تحليل صورة.", "The vision model is saved and ready when image analysis is requested.", lang)
                    else WebI18n.tr("لم يتم تحميل نموذج الصور.", "No vision model is loaded.", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (visionModelReady) ByeProTheme.colors.goldPrimary else ByeProTheme.colors.textSecondary
                )
                OutlinedButton(
                    onClick = { visionModelPicker.launch(arrayOf("*/*")) },
                    enabled = !localModelLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(WebI18n.tr("اختيار نموذج الصور من الهاتف", "Choose vision model from phone", lang))
                }
                OutlinedButton(
                    onClick = { onTestLocalModel(true) },
                    enabled = !localModelLoading && visionModelReady,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(WebI18n.tr("اختبار تحليل الصور", "Test image analysis", lang))
                }
            }
        }

        if (localModelLoading) {
            LinearProgressIndicator(
                progress = { localModelProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (localModelStatus.isNotBlank()) {
            Text(
                localModelStatus,
                style = MaterialTheme.typography.bodySmall,
                color = ByeProTheme.colors.textSecondary
            )
        }
        selectionError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
