package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.runtime.rememberUpdatedState
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

/**
 * Prefer a Syrian Arabic voice when the device has one, then a voice explicitly
 * named as feminine. Android exposes no reliable gender metadata, so fall back
 * to the highest-quality Arabic voice available on the device.
 */
private fun configureAssistantVoice(engine: TextToSpeech, languageCode: String) {
    val isArabic = languageCode == "ar"
    val targetLocale = if (isArabic) Locale("ar", "SY") else Locale.US
    val available = engine.voices.orEmpty()
    val languageVoices = available.filter { it.locale.language == targetLocale.language }
    val femaleMarkers = listOf("female", "woman", "zira", "samantha", "susan", "laila", "layla", "hoda", "noura", "noora")
    val preferred = languageVoices.sortedWith(
        compareByDescending<android.speech.tts.Voice> { isArabic && it.locale.country.equals("SY", ignoreCase = true) }
            .thenByDescending { voice -> femaleMarkers.any { marker -> voice.name.contains(marker, ignoreCase = true) } }
            .thenByDescending { it.quality }
    ).firstOrNull()
    val selectedLocale = when {
        languageVoices.isNotEmpty() -> languageVoices.first().locale
        isArabic -> Locale("ar")
        else -> Locale.US
    }
    engine.language = selectedLocale
    if (preferred != null) engine.voice = preferred
    engine.setSpeechRate(0.94f)
    engine.setPitch(1.02f)
}

@Composable
fun SpeakTextButton(
    text: String,
    languageCode: String,
    autoSpeak: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }

    DisposableEffect(languageCode) {
        val engine = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                configureAssistantVoice(tts ?: engine, languageCode)

            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
            ready = false
        }
    }

    LaunchedEffect(autoSpeak, ready, text) {
        if (autoSpeak && ready && text.isNotBlank()) {
            configureAssistantVoice(tts ?: return@LaunchedEffect, languageCode)

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bye-pro-ai-auto-response")
        }
    }

    IconButton(
        enabled = ready && text.isNotBlank(),
        onClick = {
            tts?.let { configureAssistantVoice(it, languageCode) }

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bye-pro-ai-response")
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = if (languageCode == "ar") "استمع إلى الرد" else "Listen to response",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

