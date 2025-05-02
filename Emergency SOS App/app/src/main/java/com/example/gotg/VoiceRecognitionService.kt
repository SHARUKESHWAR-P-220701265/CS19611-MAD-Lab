package com.example.gotg

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast

class VoiceRecognitionService(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastResult: String? = null
    private var onResultCallback: ((String?) -> Unit)? = null

    fun startListening() {
        if (isListening) return
        lastResult = null
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }
                override fun onResults(results: Bundle?) {
                    lastResult = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.getOrNull(0)
                    isListening = false
                    onResultCallback?.invoke(lastResult)
                }
                override fun onError(error: Int) {
                    isListening = false
                    onResultCallback?.invoke(null)
                }
                override fun onEndOfSpeech() {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error starting voice recognition", Toast.LENGTH_SHORT).show()
            isListening = false
            onResultCallback?.invoke(null)
        }
    }

    fun stopAndGetResult(onResult: (String?) -> Unit) {
        onResultCallback = onResult
        if (isListening) {
            speechRecognizer?.stopListening()
        } else {
            onResult(lastResult)
        }
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    fun cancelListening() {
        isListening = false
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
} 