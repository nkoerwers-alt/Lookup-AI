
package com.lookupai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

private val VoiceBackground = Color(0xFF101010)
private val VoiceSurface = Color(0xFF1C1C1C)
private val VoiceWhite = Color(0xFFF5F5F5)
private val VoiceMuted = Color(0xFF929292)

@Composable
fun FennVoiceMode(
    viewModel: ChatViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var listening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Tap the microphone to speak") }
    var waitingForReply by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var pendingSpeech by remember { mutableStateOf<String?>(null) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
            status = if (granted) {
                "Permission granted. Tap the microphone to speak."
            } else {
                "Microphone permission is required."
            }
        }

    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    // Initialize text-to-speech safely.
    DisposableEffect(context) {
        var engine: TextToSpeech? = null

        engine = TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val languageResult = engine?.setLanguage(Locale.getDefault())

                ttsReady =
                    languageResult == TextToSpeech.LANG_AVAILABLE ||
                    languageResult == TextToSpeech.LANG_COUNTRY_AVAILABLE

                if (!ttsReady) {
                    status = "Your device does not support this voice language."
                }
            } else {
                ttsReady = false
                status = "Voice playback could not start."
            }
        }

        tts = engine

        onDispose {
            ttsReady = false
            engine?.stop()
            engine?.shutdown()
            tts = null
        }
    }

    // Initialize speech recognition.
    DisposableEffect(recognizer) {
        if (recognizer != null) {
            recognizer.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: android.os.Bundle?) {
                        listening = true
                        status = "Listening..."
                    }

                    override fun onBeginningOfSpeech() {
                        status = "I'm listening"
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        listening = false
                        status = "Thinking..."
                    }

                    override fun onError(errorCode: Int) {
                        listening = false

                        status = when (errorCode) {
                            SpeechRecognizer.ERROR_NO_MATCH ->
                                "I didn't catch that. Try again."

                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "I didn't hear anything."

                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "Microphone permission is required."

                            else ->
                                "Speech recognition failed. Try again."
                        }
                    }

                    override fun onResults(results: android.os.Bundle?) {
                        listening = false

                        val spokenText = results
                            ?.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                            )
                            ?.firstOrNull()
                            ?.trim()

                        if (!spokenText.isNullOrBlank()) {
                            waitingForReply = true
                            status = "Fenn is thinking..."
                            viewModel.askAI(spokenText)
                        } else {
                            status = "I didn't catch that. Try again."
                        }
                    }

                    override fun onPartialResults(
                        partialResults: android.os.Bundle?
                    ) {}

                    override fun onEvent(
                        eventType: Int,
                        params: android.os.Bundle?
                    ) {}
                }
            )
        }

        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
        }
    }

    // Wait for Fenn's response before speaking it.
    LaunchedEffect(messages, loading, waitingForReply, error) {
        if (!waitingForReply || loading) {
            return@LaunchedEffect
        }

        if (error != null) {
            waitingForReply = false
            status = error ?: "Something went wrong."
            return@LaunchedEffect
        }

        val latest = messages.lastOrNull()

        if (latest != null && !latest.isUser) {
            waitingForReply = false
            pendingSpeech = latest.text
            status = "Fenn replied"
        }
    }

    // Speak the reply when the TTS engine is ready.
    LaunchedEffect(ttsReady, pendingSpeech) {
        val text = pendingSpeech

        if (ttsReady && !text.isNullOrBlank()) {
            tts?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "fenn_reply"
            )

            pendingSpeech = null
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            status = error ?: "Something went wrong."
        }
    }

    fun startListening() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (recognizer == null) {
            status = "Speech recognition isn't available on this device."
            return
        }

        tts?.stop()

        val speechIntent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )
                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    1
                )
            }

        try {
            recognizer.startListening(speechIntent)
        } catch (_: Exception) {
            listening = false
            status = "Couldn't start the microphone. Try again."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoiceBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    recognizer?.cancel()
                    tts?.stop()
                    onClose()
                }
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = VoiceWhite
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                "FENN VOICE",
                color = VoiceWhite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.weight(1f))

        // Microphone visual
        Box(
            modifier = Modifier
                .size(190.dp)
                .background(VoiceSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (listening) 145.dp else 125.dp)
                    .background(Color(0xFF303030), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🎙",
                    fontSize = 58.sp
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        Text(
            "Talk to Fenn",
            color = VoiceWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            status,
            color = VoiceMuted,
            fontSize = 15.sp
        )

        if (loading) {
            Spacer(Modifier.height(20.dp))

            CircularProgressIndicator(
                color = VoiceWhite,
                modifier = Modifier.size(25.dp),
                strokeWidth = 2.dp
            )
        }

        Spacer(Modifier.weight(1f))

        // Main microphone button
        Button(
            onClick = {
                if (listening) {
                    recognizer?.cancel()
                    listening = false
                    status = "Tap the microphone to speak"
                } else if (!loading) {
                    startListening()
                }
            },
            enabled = !loading,
            modifier = Modifier.size(82.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = VoiceWhite,
                contentColor = Color.Black,
                disabledContainerColor = Color.DarkGray
            )
        ) {
            Text(
                if (listening) "■" else "🎙",
                fontSize = if (listening) 30.sp else 34.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            if (listening) "TAP TO STOP" else "TAP TO SPEAK",
            color = VoiceMuted,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(20.dp))

        // Stop voice playback
        OutlinedButton(
            onClick = {
                tts?.stop()
                pendingSpeech = null
                status = "Voice playback stopped"
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = VoiceWhite
            )
        ) {
            Text(
                "■",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.width(8.dp))

            Text("Stop speaking")
        }

        Spacer(Modifier.height(24.dp))
    }
}
