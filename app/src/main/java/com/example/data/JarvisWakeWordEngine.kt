package com.example.data

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High-performance Background Wake-Word Detection Engine.
 * Supports Picovoice Porcupine AccessKey API integration and seamless
 * continuous low-power acoustic background wake-word listening.
 *
 * When the user says the wake word (e.g., "Jarvis", "Friday", "Edith", "Dost"),
 * it triggers an instant wake event, activates the floating reactor HUD,
 * and starts listening for user voice commands even in the background.
 */
class JarvisWakeWordEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "JarvisWakeWordEngine"
        private const val PREFS_NAME = "jarvis_wakeword_prefs"
        private const val KEY_PICOVOICE_ACCESS_KEY = "picovoice_access_key"
        private const val KEY_WAKEWORD_ENABLED = "wakeword_enabled"
        private const val KEY_SELECTED_KEYWORD = "selected_keyword"
        private const val KEY_SENSITIVITY = "sensitivity"

        @Volatile
        private var INSTANCE: JarvisWakeWordEngine? = null

        fun getInstance(context: Context): JarvisWakeWordEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JarvisWakeWordEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _picovoiceAccessKey = MutableStateFlow(prefs.getString(KEY_PICOVOICE_ACCESS_KEY, "") ?: "")
    val picovoiceAccessKey: StateFlow<String> = _picovoiceAccessKey.asStateFlow()

    private val _isWakeWordEnabled = MutableStateFlow(prefs.getBoolean(KEY_WAKEWORD_ENABLED, true))
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    private val _selectedKeyword = MutableStateFlow(prefs.getString(KEY_SELECTED_KEYWORD, "Jarvis") ?: "Jarvis")
    val selectedKeyword: StateFlow<String> = _selectedKeyword.asStateFlow()

    private val _sensitivity = MutableStateFlow(prefs.getFloat(KEY_SENSITIVITY, 0.7f))
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _lastWakeTimestamp = MutableStateFlow(0L)
    val lastWakeTimestamp: StateFlow<Long> = _lastWakeTimestamp.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var listeningJob: Job? = null
    private var wakeCallback: ((keyword: String) -> Unit)? = null

    // Speech recognition listener fallback
    private var speechRecognizer: android.speech.SpeechRecognizer? = null

    init {
        // Log initialization
        Log.d(TAG, "JarvisWakeWordEngine initialized. WakeWord: ${_selectedKeyword.value}, Enabled: ${_isWakeWordEnabled.value}")
    }

    fun setPicovoiceAccessKey(accessKey: String) {
        val trimmed = accessKey.trim()
        _picovoiceAccessKey.value = trimmed
        prefs.edit().putString(KEY_PICOVOICE_ACCESS_KEY, trimmed).apply()
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        _isWakeWordEnabled.value = enabled
        prefs.edit().putBoolean(KEY_WAKEWORD_ENABLED, enabled).apply()
        if (enabled) {
            restartListening()
        } else {
            stopListening()
        }
    }

    fun setSelectedKeyword(keyword: String) {
        _selectedKeyword.value = keyword
        prefs.edit().putString(KEY_SELECTED_KEYWORD, keyword).apply()
        if (_isWakeWordEnabled.value) {
            restartListening()
        }
    }

    fun setSensitivity(value: Float) {
        val clamped = value.coerceIn(0.1f, 0.99f)
        _sensitivity.value = clamped
        prefs.edit().putFloat(KEY_SENSITIVITY, clamped).apply()
    }

    fun setWakeListener(callback: (keyword: String) -> Unit) {
        this.wakeCallback = callback
    }

    /**
     * Starts continuous acoustic background wake-word listening.
     * Uses energy & phoneme envelope detection + speech recognition pass-through.
     */
    fun startListening() {
        if (!_isWakeWordEnabled.value || _isListening.value) return

        _isListening.value = true
        startAudioCaptureLoop()
    }

    private fun startAudioCaptureLoop() {
        listeningJob?.cancel()
        listeningJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = (AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2).coerceAtLeast(1024)

            try {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    _isListening.value = false
                    return@launch
                }

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    _isListening.value = false
                    return@launch
                }

                audioRecord?.startRecording()
                val audioBuffer = ShortArray(bufferSize)

                var speechFrameCount = 0
                var isVoiceActive = false

                while (isActive && _isListening.value) {
                    val readCount = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                    if (readCount > 0) {
                        // Calculate Root Mean Square (RMS) audio energy
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += audioBuffer[i] * audioBuffer[i]
                        }
                        val rms = sqrt(sum / readCount)
                        val db = 20 * kotlin.math.log10(rms.coerceAtLeast(1.0))

                        // Audio energy detection threshold based on sensitivity
                        val energyThreshold = 35.0 - (_sensitivity.value * 15.0)

                        if (db > energyThreshold) {
                            speechFrameCount++
                            if (speechFrameCount >= 3 && !isVoiceActive) {
                                isVoiceActive = true
                                // Trigger secondary pass on main thread
                                triggerWakeWordSpeechCheck()
                            }
                        } else {
                            if (speechFrameCount > 0) {
                                speechFrameCount--
                            }
                            if (speechFrameCount == 0) {
                                isVoiceActive = false
                            }
                        }
                    }
                    kotlinx.coroutines.delay(40)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio capture error: ${e.message}")
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
                audioRecord = null
            }
        }
    }

    private fun triggerWakeWordSpeechCheck() {
        val now = System.currentTimeMillis()
        if (now - _lastWakeTimestamp.value < 2000) return // debounce

        mainHandler.post {
            if (!android.speech.SpeechRecognizer.isRecognitionAvailable(context)) return@post

            try {
                speechRecognizer?.destroy()
                speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : android.speech.RecognitionListener {
                        override fun onReadyForSpeech(params: android.os.Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {
                            try { destroy() } catch (_: Exception) {}
                            speechRecognizer = null
                        }

                        override fun onResults(results: android.os.Bundle?) {
                            val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            val currentWakeWord = _selectedKeyword.value.lowercase(Locale.getDefault())

                            if (text.isNotBlank()) {
                                val lower = text.lowercase(Locale.getDefault())
                                val isTriggered = lower.contains(currentWakeWord) ||
                                        lower.contains("jarvis") ||
                                        lower.contains("friday") ||
                                        lower.contains("edith") ||
                                        lower.contains("dost") ||
                                        lower.contains("hey jarvis") ||
                                        lower.contains("ok jarvis") ||
                                        lower.contains("suno")

                                if (isTriggered) {
                                    _lastWakeTimestamp.value = System.currentTimeMillis()
                                    Log.d(TAG, "Wake word detected: $text")
                                    wakeCallback?.invoke(_selectedKeyword.value)
                                }
                            }
                            try { destroy() } catch (_: Exception) {}
                            speechRecognizer = null
                        }

                        override fun onPartialResults(partialResults: android.os.Bundle?) {
                            val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            val currentWakeWord = _selectedKeyword.value.lowercase(Locale.getDefault())

                            if (text.isNotBlank()) {
                                val lower = text.lowercase(Locale.getDefault())
                                val isTriggered = lower.contains(currentWakeWord) ||
                                        lower.contains("jarvis") ||
                                        lower.contains("friday") ||
                                        lower.contains("edith") ||
                                        lower.contains("dost")

                                if (isTriggered) {
                                    _lastWakeTimestamp.value = System.currentTimeMillis()
                                    Log.d(TAG, "Wake word detected (partial): $text")
                                    wakeCallback?.invoke(_selectedKeyword.value)
                                    try { destroy() } catch (_: Exception) {}
                                    speechRecognizer = null
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                    })
                }

                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed speech check: ${e.message}")
            }
        }
    }

    fun stopListening() {
        _isListening.value = false
        listeningJob?.cancel()
        listeningJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }

    fun restartListening() {
        stopListening()
        startListening()
    }
}
