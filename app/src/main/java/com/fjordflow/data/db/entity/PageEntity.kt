package com.fjordflow.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val pageNumber: Int,
    val content: String,
    val addedAt: Long = System.currentTimeMillis()
)
