package com.example.data

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class JarvisVoiceProfile(
    val id: String,
    val name: String,
    val localeDisplayName: String,
    val localeCode: String,
    val isNetworkVoice: Boolean,
    val latency: Int = 0
)

class JarvisSpeechManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val prefs = context.getSharedPreferences("jarvis_speech_prefs", Context.MODE_PRIVATE)

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveRmsDb = MutableStateFlow(0f)
    val liveRmsDb: StateFlow<Float> = _liveRmsDb.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<JarvisVoiceProfile>>(emptyList())
    val availableVoices: StateFlow<List<JarvisVoiceProfile>> = _availableVoices.asStateFlow()

    private val _currentVoiceId = MutableStateFlow(prefs.getString("selected_voice_id", "default_uk") ?: "default_uk")
    val currentVoiceId: StateFlow<String> = _currentVoiceId.asStateFlow()

    private val _isContinuousModeActive = MutableStateFlow(prefs.getBoolean("continuous_mode_enabled", true))
    val isContinuousModeActive: StateFlow<Boolean> = _isContinuousModeActive.asStateFlow()

    var speechPitch: Float = prefs.getFloat("speech_pitch", 0.90f)
        set(value) {
            field = value
            prefs.edit().putFloat("speech_pitch", value).apply()
            tts?.setPitch(value)
        }

    var speechRate: Float = prefs.getFloat("speech_rate", 1.05f)
        set(value) {
            field = value
            prefs.edit().putFloat("speech_rate", value).apply()
            tts?.setSpeechRate(value)
        }

    var isVoiceOutputEnabled: Boolean = prefs.getBoolean("voice_output_enabled", true)
        set(value) {
            field = value
            prefs.edit().putBoolean("voice_output_enabled", value).apply()
        }

    private var activeCompletionCallback: (() -> Unit)? = null

    init {
        initTts()
    }

    fun setContinuousMode(enabled: Boolean) {
        _isContinuousModeActive.value = enabled
        prefs.edit().putBoolean("continuous_mode_enabled", enabled).apply()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                applyVoiceSettings()
                loadAvailableVoices()

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        val cb = activeCompletionCallback
                        activeCompletionCallback = null
                        cb?.invoke()
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        val cb = activeCompletionCallback
                        activeCompletionCallback = null
                        cb?.invoke()
                    }
                })
                _isTtsReady.value = true
            }
        }
    }

    fun applyCharacterPreset(preset: CharacterPreset) {
        speechPitch = preset.defaultPitch
        speechRate = preset.defaultSpeed
        selectVoiceByLanguage(preset.defaultVoiceLang)
    }

    private fun selectVoiceByLanguage(langCode: String) {
        val systemVoices = tts?.voices
        if (!systemVoices.isNullOrEmpty()) {
            val matched = systemVoices.firstOrNull { it.locale.toString().startsWith(langCode, ignoreCase = true) }
            if (matched != null) {
                _currentVoiceId.value = matched.name
                prefs.edit().putString("selected_voice_id", matched.name).apply()
                tts?.voice = matched
                return
            }
        }
    }

    private fun loadAvailableVoices() {
        try {
            val voicesList = mutableListOf<JarvisVoiceProfile>()
            val systemVoices = tts?.voices
            if (!systemVoices.isNullOrEmpty()) {
                for (voice in systemVoices) {
                    val loc = voice.locale ?: Locale.getDefault()
                    val displayName = "${loc.displayLanguage} (${loc.displayCountry})"
                    val shortName = voice.name
                        .replace("en-gb-", "British ")
                        .replace("en-us-", "US ")
                        .replace("en-in-", "Indian ")
                        .replace("hi-in-", "Hindi ")
                        .replace("x-", "")
                        .replace("-local", "")
                        .replace("-network", "")
                    voicesList.add(
                        JarvisVoiceProfile(
                            id = voice.name,
                            name = shortName.ifBlank { voice.name },
                            localeDisplayName = displayName,
                            localeCode = loc.toString(),
                            isNetworkVoice = voice.isNetworkConnectionRequired,
                            latency = voice.latency
                        )
                    )
                }
            }

            if (voicesList.isEmpty()) {
                voicesList.add(JarvisVoiceProfile("default_uk", "British Jarvis (Classic)", "English (United Kingdom)", "en_GB", false))
                voicesList.add(JarvisVoiceProfile("default_us", "American Cybernetic", "English (United States)", "en_US", false))
                voicesList.add(JarvisVoiceProfile("default_in", "Indian Jarvis Pulse", "English (India)", "en_IN", false))
                voicesList.add(JarvisVoiceProfile("default_hi", "Hindi Neural Voice", "Hindi (India)", "hi_IN", false))
            }

            _availableVoices.value = voicesList
        } catch (e: Exception) {
            _availableVoices.value = listOf(
                JarvisVoiceProfile("default_uk", "British Jarvis (Classic)", "English (United Kingdom)", "en_GB", false),
                JarvisVoiceProfile("default_us", "American Cybernetic", "English (United States)", "en_US", false)
            )
        }
    }

    fun applyVoiceSettings() {
        val targetVoiceId = _currentVoiceId.value
        tts?.setPitch(speechPitch)
        tts?.setSpeechRate(speechRate)

        val systemVoices = tts?.voices
        if (!systemVoices.isNullOrEmpty()) {
            val matchedVoice = systemVoices.firstOrNull { it.name == targetVoiceId }
            if (matchedVoice != null) {
                tts?.voice = matchedVoice
                return
            }
        }

        when (targetVoiceId) {
            "default_uk" -> tts?.setLanguage(Locale.UK)
            "default_us" -> tts?.setLanguage(Locale.US)
            "default_in" -> tts?.setLanguage(Locale("en", "IN"))
            "default_hi" -> tts?.setLanguage(Locale("hi", "IN"))
            else -> {
                val ukResult = tts?.setLanguage(Locale.UK)
                if (ukResult == TextToSpeech.LANG_MISSING_DATA || ukResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
            }
        }
    }

    fun selectVoice(voiceId: String) {
        _currentVoiceId.value = voiceId
        prefs.edit().putString("selected_voice_id", voiceId).apply()
        applyVoiceSettings()
    }

    fun previewVoice(sampleText: String = "All systems calibrated and frequencies nominal.") {
        speak(sampleText)
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isVoiceOutputEnabled || text.isBlank()) {
            onComplete?.invoke()
            return
        }

        tts?.setPitch(speechPitch)
        tts?.setSpeechRate(speechRate)

        // If text contains Devanagari Hindi characters, speak with Hindi locale if supported
        val hasHindiDevanagari = text.any { it in '\u0900'..'\u097F' }
        if (hasHindiDevanagari) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                applyVoiceSettings()
            }
        } else {
            applyVoiceSettings()
        }

        activeCompletionCallback = onComplete

        val utteranceId = "JARVIS_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        _isSpeaking.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /**
     * Interruption / Barge-in: immediately stops TTS playback.
     */
    fun stopSpeaking() {
        activeCompletionCallback = null
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startListening(onResult: (String) -> Unit, onError: ((String) -> Unit)? = null) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError?.invoke("Speech recognition engine unavailable on this device.")
            return
        }

        stopSpeaking()
        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _liveRmsDb.value = (rmsdB.coerceIn(0f, 10f) / 10f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _liveRmsDb.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _liveRmsDb.value = 0f
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Recognition code: $error"
                    }
                    onError?.invoke(msg)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _liveRmsDb.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        _lastRecognizedText.value = text
                        onResult(text)
                    } else {
                        onError?.invoke("Empty speech result")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        _lastRecognizedText.value = text
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "en-IN", "hi-IN"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
            onError?.invoke("Could not start microphone listener: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
        _liveRmsDb.value = 0f
    }

    fun destroy() {
        stopSpeaking()
        stopListening()
        tts?.shutdown()
        tts = null
    }
}
