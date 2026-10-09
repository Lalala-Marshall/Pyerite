package com.marshall.pyerite.regionMarketModule.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.regionMarketModule.data.MarketRepository
import com.marshall.pyerite.regionMarketModule.model.MarketConfig
import com.marshall.pyerite.sdeModule.room.market.MarketGroupEntity
import com.marshall.pyerite.sdeModule.room.type.TypeEntity
import com.marshall.pyerite.ui.golbalComponents.search.ListSearchState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal data class MarketBrowserUiState(
    val title: String = "",
    val groups: List<MarketGroupEntity> = emptyList(),
    val childIdsWithChildren: Set<Int> = emptySet(),
    val searchTypes: List<TypeEntity> = emptyList(),
    val searching: Boolean = false,
)

@OptIn(FlowPreview::class)
internal class MarketBrowserViewModel(
    parentGroupId: Int,
    private val repository: MarketRepository,
) : ViewModel() {
    private val parentId = parentGroupId.takeIf { it != MarketConfig.NO_PARENT_GROUP }

    private val groups = MutableStateFlow<List<MarketGroupEntity>>(emptyList())
    private val _search = MutableStateFlow(ListSearchState())
    val search: StateFlow<ListSearchState> = _search.asStateFlow()

    private val _ui = MutableStateFlow(MarketBrowserUiState())
    val ui: StateFlow<MarketBrowserUiState> = _ui.asStateFlow()

    private val query = _search
        .map { it.query.trim() }
        .debounce(MarketConfig.SEARCH_DEBOUNCE_MS.milliseconds)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        viewModelScope.launch {
            val all = repository.visibleGroups()
            groups.value = all
            val title = parentId?.let { repository.group(it)?.name }.orEmpty()
            publish(all, title, query.value)
        }
        viewModelScope.launch {
            query.collect { text ->
                val all = groups.value
                if (all.isEmpty()) return@collect
                val title = _ui.value.title
                publish(all, title, text)
            }
        }
    }

    fun setSearchActive(active: Boolean) {
        _search.update { it.copy(isActive = active) }
    }

    fun setSearchQuery(query: String) {
        _search.update { it.copy(query = query) }
    }

    fun cancelSearch() {
        _search.value = ListSearchState()
    }

    private suspend fun publish(all: List<MarketGroupEntity>, title: String, queryText: String) {
        val byParent = all.groupBy { it.parentGroupId }
        val children = byParent[parentId].orEmpty()
        val parentsWithChildren = byParent.keys.filterNotNull().toSet()
        if (queryText.isBlank()) {
            _ui.value = MarketBrowserUiState(
                title = title,
                groups = children,
                childIdsWithChildren = parentsWithChildren,
            )
            return
        }
        _ui.update { it.copy(searching = true, title = title) }
        val matchedGroups = children.filter { group ->
            group.name.orEmpty().contains(queryText, ignoreCase = true)
        }
        val subtree = descendantIds(parentId, byParent)
        val types = repository.searchTypes(subtree, queryText)
        _ui.value = MarketBrowserUiState(
            title = title,
            groups = matchedGroups,
            childIdsWithChildren = parentsWithChildren,
            searchTypes = types,
            searching = false,
        )
    }

    private fun descendantIds(
        root: Int?,
        byParent: Map<Int?, List<MarketGroupEntity>>,
    ): List<Int> {
        val pending = ArrayDeque<Int>()
        if (root == null) {
            byParent[null].orEmpty().forEach { pending.add(it.id) }
        } else {
            byParent[root].orEmpty().forEach { pending.add(it.id) }
            pending.add(root)
        }
        val ids = mutableListOf<Int>()
        while (pending.isNotEmpty()) {
            val id = pending.removeFirst()
            ids += id
            byParent[id].orEmpty().forEach { pending.add(it.id) }
        }
        return ids
    }
}
