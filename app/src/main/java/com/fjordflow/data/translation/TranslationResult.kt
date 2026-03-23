package com.fjordflow.data.translation

data class TranslationResult(
    val translation: String,
    val partOfSpeech: String = "",
    val exampleFr: String = "",
    val exampleEn: String = "",
    val contextNotes: String = ""
)
