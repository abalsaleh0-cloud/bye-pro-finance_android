package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.finance.ai.ElevenLabsSpeechProvider
import com.example.finance.ai.SecureVoiceSettings
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

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

/**
 * Cloud-first natural speech with a local Android TTS fallback when offline or
 * when no ElevenLabs credential has been configured. Credentials are protected
 * by an AES-GCM key held in Android Keystore.
 */
@Composable
fun SpeakTextButton(
    text: String,
    languageCode: String,
    autoSpeak: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val provider = remember(context) { ElevenLabsSpeechProvider(context.applicationContext) }
    val hasCloudCredentials = remember(context) { SecureVoiceSettings(context).hasApiKey() }
    val latestText by rememberUpdatedState(text)
    val latestLanguage by rememberUpdatedState(languageCode)
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var speaking by remember { mutableStateOf(false) }

    DisposableEffect(languageCode) {
        val engine = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = if (languageCode == "ar") Locale("ar") else Locale.US
                tts?.setSpeechRate(0.96f)
                tts?.setPitch(1.0f)
            }
        }
        tts = engine
        onDispose {
            runCatching { player?.stop() }
            player?.release()
            audioFile?.delete()
            engine.stop()
            engine.shutdown()
            tts = null
            player = null
            audioFile = null
            ready = false
            speaking = false
        }
    }

    fun speakLocally() {
        val engine = tts ?: return
        if (!ready || latestText.isBlank()) return
        engine.language = if (latestLanguage == "ar") Locale("ar") else Locale.US
        engine.setSpeechRate(0.96f)
        engine.setPitch(1.0f)
        engine.speak(latestText, TextToSpeech.QUEUE_FLUSH, null, "bye-pro-ai-local-fallback")
        speaking = true
    }

    fun speak() {
        if (latestText.isBlank()) return
        scope.launch {
            speaking = true
            val audio = runCatching { provider.synthesize(latestText, latestLanguage) }.getOrNull()
            if (audio.isNullOrEmpty()) {
                speaking = false
                speakLocally()
                return@launch
            }
            val file = File(context.cacheDir, "bye-pro-voice-${UUID.randomUUID()}.mp3")
            runCatching {
                file.writeBytes(audio)
                player?.release()
                audioFile?.delete()
                val next = MediaPlayer()
                next.setDataSource(file.absolutePath)
                next.setOnPreparedListener { it.start() }
                next.setOnCompletionListener { completed ->
                    completed.release()
                    file.delete()
                    if (player === completed) player = null
                    if (audioFile == file) audioFile = null
                    speaking = false
                }
                next.setOnErrorListener { failed, _, _ ->
                    failed.release()
                    file.delete()
                    if (player === failed) player = null
                    if (audioFile == file) audioFile = null
                    speaking = false
                    speakLocally()
                    true
                }
                player = next
                audioFile = file
                next.prepareAsync()
            }.onFailure {
                file.delete()
                speaking = false
                speakLocally()
            }
        }
    }

    LaunchedEffect(autoSpeak, text) {
        if (autoSpeak && text.isNotBlank()) speak()
    }

    IconButton(
        enabled = text.isNotBlank() && (ready || hasCloudCredentials),
        onClick = { speak() },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = if (languageCode == "ar") "استمع إلى الرد" else "Listen to response",
            tint = if (speaking) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
        )
    }
}
