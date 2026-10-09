package com.marshall.pyerite.regionMarketModule.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.regionMarketModule.data.MarketRepository
import com.marshall.pyerite.sdeModule.room.type.TypeEntity
import com.marshall.pyerite.ui.golbalComponents.search.ListSearchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class MarketTypeSection(
    val id: Int,
    val title: String,
    val types: List<TypeEntity>,
    val unpublished: Boolean = false,
)

internal data class MarketTypeListUiState(
    val title: String = "",
    val sections: List<MarketTypeSection> = emptyList(),
)

internal class MarketTypeListViewModel(
    private val marketGroupId: Int,
    private val repository: MarketRepository,
) : ViewModel() {
    private val allSections = MutableStateFlow<List<MarketTypeSection>>(emptyList())
    private val _search = MutableStateFlow(ListSearchState())
    val search: StateFlow<ListSearchState> = _search.asStateFlow()

    private val _ui = MutableStateFlow(MarketTypeListUiState())
    val ui: StateFlow<MarketTypeListUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val title = repository.group(marketGroupId)?.name.orEmpty()
            val types = repository.typesInGroup(marketGroupId)
            val metaNames = repository.metaGroups().associate { it.id to it.name.orEmpty() }
            val sections = buildSections(types, metaNames)
            allSections.value = sections
            _ui.value = MarketTypeListUiState(title = title, sections = sections)
        }
    }

    fun setSearchActive(active: Boolean) {
        _search.update { it.copy(isActive = active) }
    }

    fun setSearchQuery(query: String) {
        _search.update { it.copy(query = query) }
        val trimmed = query.trim()
        val sections = if (trimmed.isEmpty()) {
            allSections.value
        } else {
            allSections.value.mapNotNull { section ->
                val matched = section.types.filter { type ->
                    type.name.orEmpty().contains(trimmed, ignoreCase = true) ||
                        type.zhName.orEmpty().contains(trimmed, ignoreCase = true) ||
                        type.enName.orEmpty().contains(trimmed, ignoreCase = true)
                }
                if (matched.isEmpty()) null else section.copy(types = matched)
            }
        }
        _ui.update { it.copy(sections = sections) }
    }

    fun cancelSearch() {
        _search.value = ListSearchState()
        _ui.update { it.copy(sections = allSections.value) }
    }

    private fun buildSections(
        types: List<TypeEntity>,
        metaNames: Map<Int, String>,
    ): List<MarketTypeSection> {
        val published = types.filter { it.published == true }
        val unpublished = types.filter { it.published != true }
        val sections = published.groupBy { it.metaGroupID }
            .entries
            .sortedBy { it.key ?: Int.MAX_VALUE }
            .map { (metaId, items) ->
                MarketTypeSection(
                    id = metaId ?: UNGROUPED_ID,
                    title = metaId?.let { metaNames[it] }.orEmpty(),
                    types = items.sortedBy { it.name.orEmpty() },
                )
            }
            .toMutableList()
        if (unpublished.isNotEmpty()) {
            sections += MarketTypeSection(
                id = UNPUBLISHED_ID,
                title = "",
                types = unpublished.sortedBy { it.name.orEmpty() },
                unpublished = true,
            )
        }
        return sections
    }

    companion object {
        const val UNGROUPED_ID = -2
        const val UNPUBLISHED_ID = -1
    }
}
