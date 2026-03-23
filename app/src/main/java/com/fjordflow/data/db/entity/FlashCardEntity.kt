package com.fjordflow.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SRS flashcard linked to a saved word.
 * easeFactor and interval follow a simplified SM-2 algorithm.
 */
@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("wordId")]
)
data class FlashCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val wordId: Int,
    val front: String,                  // French word or sentence
    val back: String,                   // English translation
    val easeFactor: Float = 2.5f,       // SM-2 ease factor (min 1.3)
    val intervalDays: Int = 1,          // current review interval in days
    val dueDate: Long = System.currentTimeMillis(),
    val reviewCount: Int = 0
)
