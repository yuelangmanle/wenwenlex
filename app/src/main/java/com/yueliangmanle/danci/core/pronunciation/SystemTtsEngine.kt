package com.yueliangmanle.danci.core.pronunciation

import android.content.Context
import android.speech.tts.TextToSpeech
import com.yueliangmanle.danci.core.model.PronunciationAccent
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine

class SystemTtsEngine(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    @Volatile
    private var engine: TextToSpeech? = null

    suspend fun speak(
        text: String,
        accent: PronunciationAccent,
    ): Boolean {
        val tts = obtainEngine() ?: return false
        return withContext(Dispatchers.Main) {
            val languageResult = tts.setLanguage(accent.toLocale())
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                return@withContext false
            }
            tts.stop()
            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "wenwenlex-${System.currentTimeMillis()}",
            ) != TextToSpeech.ERROR
        }
    }

    private suspend fun obtainEngine(): TextToSpeech? {
        engine?.let { return it }
        return mutex.withLock {
            engine?.let { return it }
            val created = withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<TextToSpeech?> { continuation ->
                    var pendingEngine: TextToSpeech? = null
                    pendingEngine = TextToSpeech(appContext) { status ->
                        if (!continuation.isActive) {
                            pendingEngine?.shutdown()
                            return@TextToSpeech
                        }
                        if (status == TextToSpeech.SUCCESS) {
                            continuation.resume(pendingEngine)
                        } else {
                            pendingEngine?.shutdown()
                            continuation.resume(null)
                        }
                    }
                }
            }
            engine = created
            created
        }
    }
}

private fun PronunciationAccent.toLocale(): Locale =
    when (this) {
        PronunciationAccent.UK -> Locale.UK
        PronunciationAccent.US -> Locale.US
        PronunciationAccent.AUTO -> Locale.US
    }

