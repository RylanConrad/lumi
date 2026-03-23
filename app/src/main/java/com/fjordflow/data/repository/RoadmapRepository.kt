package com.fjordflow.data.repository

import com.fjordflow.data.db.dao.RoadmapDao
import com.fjordflow.data.db.entity.RoadmapNodeEntity
import kotlinx.coroutines.flow.Flow

class RoadmapRepository(private val dao: RoadmapDao) {
    fun getNodes(languageCode: String = "fr"): Flow<List<RoadmapNodeEntity>> =
        dao.getNodesForLanguage(languageCode)

    suspend fun completeNode(node: RoadmapNodeEntity, allNodes: List<RoadmapNodeEntity>) {
        dao.updateNode(node.copy(isCompleted = true))
        // Unlock the next node in sequence
        allNodes.firstOrNull { it.orderIndex == node.orderIndex + 1 }?.let {
            dao.updateNode(it.copy(isUnlocked = true))
        }
    }
}
