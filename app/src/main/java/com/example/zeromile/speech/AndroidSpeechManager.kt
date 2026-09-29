package com.example.zeromile.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.zeromile.data.model.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SpeechState(
    val isListening: Boolean = false,
    val interimText: String = "",
    val finalText: String = "",
    val rmsLevel: Float = 0f,
    val isSupported: Boolean = true,
    val errorMessage: String? = null
)

class AndroidSpeechManager(private val context: Context) {
    private val tag = "AndroidSpeechManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(SpeechState())
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        checkSupport()
    }

    private fun checkSupport() {
        val supported = SpeechRecognizer.isRecognitionAvailable(context)
        _state.update { it.copy(isSupported = supported) }
        if (!supported) {
            Log.w(tag, "Speech recognition is not natively available on this system")
        }
    }

    private fun getOrCreateRecognizer(): SpeechRecognizer? {
        if (speechRecognizer == null) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.update { 
                    it.copy(
                        isSupported = false,
                        errorMessage = "Voice input isn't supported on this device."
                    ) 
                }
                return null
            }

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to instantiate SpeechRecognizer", e)
                _state.update { 
                    it.copy(
                        isSupported = false,
                        errorMessage = "Voice input isn't supported on this device."
                    ) 
                }
                return null
            }
        }
        return speechRecognizer
    }

    fun startListening(language: VoiceLanguage) {
        mainHandler.post {
            _state.update { 
                it.copy(
                    isListening = true,
                    interimText = "",
                    errorMessage = null,
                    rmsLevel = 0f
                ) 
            }

            val recognizer = getOrCreateRecognizer()
            if (recognizer == null) {
                _state.update { it.copy(isListening = false) }
                return@post
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.localeCode)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(language.localeCode, "en-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            try {
                recognizer.cancel()
                recognizer.startListening(intent)
            } catch (e: Exception) {
                Log.e(tag, "Error starting speech recognition", e)
                _state.update { 
                    it.copy(
                        isListening = false,
                        errorMessage = "Something went wrong with voice input."
                    ) 
                }
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(tag, "Error stopping speech recognizer", e)
            }
            _state.update { it.copy(isListening = false, rmsLevel = 0f) }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(tag, "Error canceling speech recognizer", e)
            }
            _state.update { it.copy(isListening = false, interimText = "", rmsLevel = 0f) }
        }
    }

    fun resetState() {
        _state.update { 
            it.copy(
                isListening = false,
                interimText = "",
                finalText = "",
                rmsLevel = 0f,
                errorMessage = null
            ) 
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w(tag, "Error destroying speech recognizer", e)
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.update { it.copy(isListening = true, errorMessage = null) }
            }

            override fun onBeginningOfSpeech() {
                _state.update { it.copy(isListening = true) }
            }

            override fun onRmsChanged(rmsdB: Float) {
                _state.update { it.copy(rmsLevel = rmsdB) }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _state.update { it.copy(isListening = false, rmsLevel = 0f) }
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        "Microphone access is needed for voice input."
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "We didn't hear anything. Try speaking again."
                    SpeechRecognizer.ERROR_AUDIO, SpeechRecognizer.ERROR_CLIENT ->
                        "Something went wrong with voice input."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Network issue with voice recognition. You can retry or type instead."
                    else ->
                        "Something went wrong with voice input."
                }
                Log.w(tag, "Speech recognition error: $error ($errorMsg)")
                _state.update { 
                    it.copy(
                        isListening = false,
                        interimText = "",
                        rmsLevel = 0f,
                        errorMessage = errorMsg
                    ) 
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""
                _state.update { 
                    it.copy(
                        isListening = false,
                        interimText = "",
                        finalText = recognizedText,
                        rmsLevel = 0f,
                        errorMessage = if (recognizedText.isBlank()) "We didn't hear anything. Try speaking again." else null
                    ) 
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull() ?: ""
                if (partialText.isNotBlank()) {
                    _state.update { it.copy(interimText = partialText) }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
