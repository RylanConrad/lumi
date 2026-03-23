package com.fjordflow.data.db.dao

import androidx.room.*
import com.fjordflow.data.db.entity.RoadmapNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoadmapDao {
    @Query("SELECT * FROM roadmap_nodes WHERE languageCode = :lang ORDER BY orderIndex ASC")
    fun getNodesForLanguage(lang: String = "fr"): Flow<List<RoadmapNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNodes(nodes: List<RoadmapNodeEntity>)

    @Update
    suspend fun updateNode(node: RoadmapNodeEntity)
}
