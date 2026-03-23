package com.fjordflow.data.translation

import android.util.Log
import com.fjordflow.BuildConfig

object TranslationService {

    suspend fun translate(word: String, context: String = ""): TranslationResult {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return mockLookup(word)
        }
        return try {
            GeminiClient.translate(word, context)
        } catch (e: Exception) {
            Log.e("TranslationService", "Gemini call failed: ${e.message}", e)
            // Fallback to dictionary for common words if Gemini fails
            val mock = mockLookup(word)
            if (mock.translation != "No translation found") {
                mock
            } else {
                TranslationResult(translation = "Error: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun mockLookup(word: String): TranslationResult {
        val translation = mockDictionary[word.lowercase().trim()]
            ?: "No translation found"
        return TranslationResult(translation = translation)
    }

    private val mockDictionary = mapOf(
        "le" to "the (masc.)", "la" to "the (fem.)", "les" to "the (plural)",
        "un" to "a (masc.)", "une" to "a (fem.)",
        "se" to "himself / herself / itself (reflexive)",
        "couchait" to "was laying down / was setting (sun)",
        "soleil" to "sun", "montagnes" to "mountains", "quand" to "when",
        "décida" to "decided", "de" to "of / to / from", "partir" to "to leave",
        "elle" to "she", "prit" to "took", "son" to "her / his",
        "manteau" to "coat", "ferma" to "closed", "porte" to "door",
        "à" to "to / at", "clé" to "key", "et" to "and",
        "marcha" to "walked", "vers" to "towards", "gare" to "train station",
        "ville" to "city / town", "était" to "was", "silencieuse" to "silent (fem.)",
        "heure" to "hour / time", "pas" to "steps / not",
        "résonnaient" to "echoed", "pavés" to "cobblestones",
        "dans" to "in / inside", "poche" to "pocket", "lettre" to "letter",
        "jamais" to "never", "courage" to "courage", "envoyer" to "to send",
        "lèvres" to "lips", "puis" to "then", "temps" to "time / weather",
        "recommencer" to "to start over", "nuit" to "night", "jour" to "day",
        "maison" to "house", "eau" to "water", "pain" to "bread",
        "ami" to "friend (masc.)", "livre" to "book", "vie" to "life",
        "monde" to "world", "cœur" to "heart", "lumière" to "light",
        "ombre" to "shadow", "rêve" to "dream", "espoir" to "hope",
        "amour" to "love"
    )
}
