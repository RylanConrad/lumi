package com.fjordflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String = "Unknown",
    val content: String = "",
    val type: String = "TEXT",
    val progress: Float = 0f,
    val addedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = System.currentTimeMillis()
)
