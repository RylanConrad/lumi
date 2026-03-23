package com.fjordflow.ui.screens.roadmap

import androidx.lifecycle.*
import com.fjordflow.data.db.entity.RoadmapNodeEntity
import com.fjordflow.data.repository.RoadmapRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RoadmapUiState(
    val nodes: List<RoadmapNodeEntity> = emptyList(),
    val selectedNode: RoadmapNodeEntity? = null
)

class RoadmapViewModel(private val repo: RoadmapRepository) : ViewModel() {

    val uiState: StateFlow<RoadmapUiState> = repo.getNodes()
        .map { RoadmapUiState(nodes = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoadmapUiState())

    private val _selectedNode = MutableStateFlow<RoadmapNodeEntity?>(null)
    val selectedNode: StateFlow<RoadmapNodeEntity?> = _selectedNode.asStateFlow()

    fun selectNode(node: RoadmapNodeEntity) { _selectedNode.value = node }
    fun dismissNode() { _selectedNode.value = null }

    fun completeNode(node: RoadmapNodeEntity) {
        viewModelScope.launch {
            repo.completeNode(node, uiState.value.nodes)
            _selectedNode.value = null
        }
    }

    class Factory(private val repo: RoadmapRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RoadmapViewModel(repo) as T
    }
}
