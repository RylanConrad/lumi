package com.fjordflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "roadmap_nodes")
data class RoadmapNodeEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val languageCode: String = "fr",
    val orderIndex: Int,
    val category: String,       // e.g. "Foundations", "Grammar", "Advanced"
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false
)
