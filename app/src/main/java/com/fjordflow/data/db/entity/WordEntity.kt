package com.fjordflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val context: String,        // the sentence it was found in
    val translation: String,
    val languageCode: String = "fr",
    val savedAt: Long = System.currentTimeMillis()
)
