package com.example.ui.screens

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.media.MediaPlayer
import android.widget.Toast
import java.io.File
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import java.util.Locale

@Composable
fun VoiceInputButton(
    languageCode: String,
    enabled: Boolean = true,
    onRecognized: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestOnRecognized by rememberUpdatedState(onRecognized)
    var listening by remember { mutableStateOf(false) }
    var startAfterPermission by remember { mutableStateOf(false) }
    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> startAfterPermission = granted }

    DisposableEffect(recognizer) {
        if (recognizer != null) {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { listening = true }
                override fun onBeginningOfSpeech() { listening = true }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { listening = false }
                override fun onError(error: Int) { listening = false }
                override fun onResults(results: Bundle?) {
                    listening = false
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim().orEmpty()
                    if (spoken.isNotBlank()) latestOnRecognized(spoken)
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
        }
    }

    LaunchedEffect(startAfterPermission) {
        if (startAfterPermission) {
            startAfterPermission = false
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (languageCode == "ar") "ar" else Locale.US.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            runCatching { recognizer?.startListening(intent) }.onFailure { listening = false }
        }
    }

    IconButton(
        enabled = enabled && recognizer != null,
        onClick = {
            if (listening) {
                recognizer?.stopListening()
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startAfterPermission = true
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = if (languageCode == "ar") "التحدث صوتياً" else "Speak",
            tint = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SpeakTextButton(
    text: String,
    languageCode: String,
    autoSpeak: Boolean = false,
    modifier: Modifier = Modifier,
    onSynthesizeSpeech: (suspend (String) -> Result<ByteArray>)? = null
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()
    val latestText by rememberUpdatedState(text)
    val latestSynthesis by rememberUpdatedState(onSynthesizeSpeech)

    DisposableEffect(languageCode) {
        val engine = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                val targetLocale = if (languageCode == "ar") Locale("ar") else Locale.US
                tts?.language = targetLocale
                val preferredVoice = tts?.voices
                    ?.filter { it.locale.language == targetLocale.language }
                    ?.maxByOrNull { it.quality }
                if (preferredVoice != null) tts?.voice = preferredVoice
                tts?.setSpeechRate(0.94f)
                tts?.setPitch(1.0f)
            }
        }
        tts = engine
        onDispose {
            player?.release()
            audioFile?.delete()
            engine.stop()
            engine.shutdown()
            tts = null
            player = null
            audioFile = null
            ready = false
        }
    }

    fun speakLocalFallback() {
        if (!ready) return
        val locale = if (languageCode == "ar") Locale("ar") else Locale.US
        tts?.language = locale
        tts?.setSpeechRate(0.94f)
        tts?.setPitch(1.0f)
        tts?.speak(latestText, TextToSpeech.QUEUE_FLUSH, null, "bye-pro-ai-response-fallback")
    }

    fun speak() {
        scope.launch {
            val synthesis = latestSynthesis
            if (synthesis == null) {
                speakLocalFallback()
                return@launch
            }
            val audioResult = runCatching { synthesis(latestText) }.getOrNull()
            val audio = audioResult?.getOrNull()
            if (audio == null || audio.isEmpty()) {
                Toast.makeText(
                    context,
                    if (languageCode == "ar") "تعذّر توليد الصوت الجديد. تحقق من الاتصال وحاول مجدداً." else "Could not generate the selected voice. Check the connection and try again.",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            val file = File(context.cacheDir, "bye-pro-speech-${UUID.randomUUID()}.mp3")
            runCatching {
                file.writeBytes(audio)
                player?.release()
                audioFile?.delete()
                val nextPlayer = MediaPlayer()
                nextPlayer.setDataSource(file.absolutePath)
                nextPlayer.setOnPreparedListener { it.start() }
                nextPlayer.setOnCompletionListener { completed ->
                    completed.release()
                    file.delete()
                    if (player === completed) player = null
                    if (audioFile == file) audioFile = null
                }
                nextPlayer.setOnErrorListener { failed, _, _ ->
                    failed.release()
                    file.delete()
                    if (player === failed) player = null
                    if (audioFile == file) audioFile = null
                    Toast.makeText(
                        context,
                        if (languageCode == "ar") "تعذّر تشغيل الصوت المولّد." else "Could not play the generated audio.",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                player = nextPlayer
                audioFile = file
                nextPlayer.prepareAsync()
            }.onFailure {
                file.delete()
                speakLocalFallback()
            }
        }
    }

    LaunchedEffect(autoSpeak, text) {
        if (autoSpeak && text.isNotBlank()) speak()
    }

    IconButton(
        enabled = text.isNotBlank() && (ready || onSynthesizeSpeech != null),
        onClick = { speak() },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = if (languageCode == "ar") "استمع إلى الرد" else "Listen to response",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
