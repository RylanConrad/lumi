package com.fjordflow.data.translation

import com.fjordflow.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import org.json.JSONObject

object GeminiClient {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 512
            }
        )
    }

    suspend fun translate(text: String, context: String): TranslationResult {
        require(BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            "GEMINI_API_KEY is not set. Add it to local.properties."
        }

        val prompt = buildPrompt(text, context)
        val response = model.generateContent(prompt)
        val raw = response.text ?: throw IllegalStateException("Empty response from Gemini")
        return parseResponse(raw)
    }

    private fun buildPrompt(text: String, context: String): String {
        val isSentence = text.trim().contains(" ")
        return if (isSentence) {
            """
            You are a precise French–English translator.
            
            Target Sentence: "$text"
            
            Instructions:
            1. Provide a natural English translation of the sentence.
            2. In 'contextNotes', briefly explain any interesting grammar points, idioms, or cultural nuances present in this sentence.
            
            Reply with JSON only:
            {
              "translation": "<English translation>",
              "partOfSpeech": "sentence",
              "contextNotes": "<explanation of grammar/idioms>"
            }
            """.trimIndent()
        } else {
            """
            You are a precise French–English dictionary assistant.
            
            Target Word/Phrase: "$text"
            Context Sentence: "$context"
            
            Instructions:
            1. Provide the English translation of "$text" as it is used in the specific context provided.
            2. If it's a verb, provide the infinitive in the 'contextNotes'.
            3. In 'contextNotes', briefly explain the grammar or nuance of this word in this specific sentence.
            
            Reply with JSON only:
            {
              "translation": "<English translation>",
              "partOfSpeech": "<noun, verb, etc.>",
              "contextNotes": "<brief explanation>"
            }
            """.trimIndent()
        }
    }

    private fun parseResponse(raw: String): TranslationResult {
        val json = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        return try {
            val obj = JSONObject(json)
            TranslationResult(
                translation  = obj.optString("translation", ""),
                partOfSpeech = obj.optString("partOfSpeech", ""),
                contextNotes = obj.optString("contextNotes", ""),
                exampleFr    = "",
                exampleEn    = ""
            )
        } catch (e: Exception) {
            TranslationResult(translation = raw.take(200))
        }
    }
}
