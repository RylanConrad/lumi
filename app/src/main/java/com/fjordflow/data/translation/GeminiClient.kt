package com.fjordflow.data.translation

import android.graphics.Bitmap
import com.fjordflow.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import org.json.JSONObject

object GeminiClient {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.1f
                maxOutputTokens = 2048
            }
        )
    }

    private val visionModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.0f
                maxOutputTokens = 4096
            }
        )
    }

    /**
     * Extracts text from a book page image using Gemini Vision.
     * Handles curved pages, varied lighting, and complex layouts naturally —
     * things traditional OCR cannot reliably do.
     */
    suspend fun scanPage(bitmap: Bitmap): String {
        val prompt = """
            Extract all the text from this book page exactly as written.
            Rules:
            - Output ONLY the extracted text, nothing else.
            - Preserve reading order (top to bottom, left to right).
            - Separate paragraphs with a single blank line.
            - Do NOT translate or modify the text in any way.
            - Ignore page numbers, chapter headers, and running titles at the top/bottom edges.
            - Re-join any words hyphenated at line breaks (e.g. "connais-↵sance" → "connaissance").
        """.trimIndent()

        val response = visionModel.generateContent(
            content {
                image(bitmap)
                text(prompt)
            }
        )
        return response.text?.trim() ?: ""
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

    /**
     * Uses AI to reconstruct a scanned page from messy OCR text.
     * Specifically optimized for French book pages.
     */
    suspend fun reconstructPage(rawOcrText: String): String {
        val prompt = """
            SYSTEM: You are a professional book editor. Your task is to clean up messy OCR text from a French book page.
            
            INPUT TEXT:
            $rawOcrText
            
            STRICT RULES:
            1. Output ONLY the reconstructed French text. 
            2. Do NOT include any preamble, introduction, or conversational text (e.g., do NOT say "Here is the text" or "Reconstructed text:").
            3. Remove all page headers, footers, and page numbers.
            4. Re-join words that were split across lines (e.g., "en-suite" becomes "ensuite").
            5. Reconstruct the text into proper, readable paragraphs.
            6. Fix obvious OCR character errors while preserving the original French wording.
            7. Do NOT translate.
            
            FINAL CHECK: If your response contains anything other than the cleaned French text, it is a failure.
        """.trimIndent()

        val response = model.generateContent(prompt)
        // Aggressively trim and remove common AI conversational markers just in case
        return response.text?.trim()
            ?.removePrefix("Voici le texte nettoyé :")
            ?.removePrefix("Voici le texte :")
            ?.trim() ?: rawOcrText
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
