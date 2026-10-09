package com.marshall.pyerite.regionMarketModule.watchlist.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.regionMarketModule.data.MarketRepository
import com.marshall.pyerite.regionMarketModule.model.MarketConfig
import com.marshall.pyerite.regionMarketModule.viewModel.MarketTypeSection
import com.marshall.pyerite.sdeModule.room.market.MarketGroupEntity
import com.marshall.pyerite.sdeModule.room.type.TypeEntity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

internal sealed interface MarketWatchlistPickerLevel {
    data class Groups(val parentGroupId: Int?) : MarketWatchlistPickerLevel
    data class Types(val marketGroupId: Int) : MarketWatchlistPickerLevel
}

internal data class MarketWatchlistPickerUi(
    val title: String = "",
    val query: String = "",
    val canGoBack: Boolean = false,
    val level: MarketWatchlistPickerLevel = MarketWatchlistPickerLevel.Groups(null),
    val groups: List<MarketGroupEntity> = emptyList(),
    val childIdsWithChildren: Set<Int> = emptySet(),
    val searchTypes: List<TypeEntity> = emptyList(),
    val sections: List<MarketTypeSection> = emptyList(),
    val selectedTypeIds: List<Int> = emptyList(),
)

@OptIn(FlowPreview::class)
internal class MarketWatchlistAddItemViewModel(
    private val repository: MarketRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(MarketWatchlistPickerUi())
    val ui: StateFlow<MarketWatchlistPickerUi> = _ui.asStateFlow()

    private val stack = MutableStateFlow(listOf<MarketWatchlistPickerLevel>(MarketWatchlistPickerLevel.Groups(null)))
    private val queryText = MutableStateFlow("")
    private val selectedIds = linkedSetOf<Int>()
    private val sectionCache = mutableMapOf<Int, List<MarketTypeSection>>()
    private val publishMutex = Mutex()

    private var allGroups: List<MarketGroupEntity> = emptyList()
    private var metaNames: Map<Int, String> = emptyMap()

    init {
        viewModelScope.launch {
            allGroups = repository.visibleGroups()
            metaNames = repository.metaGroups().associate { it.id to it.name.orEmpty() }
            publish(stack.value, queryText.value)
            queryText
                .debounce(MarketConfig.SEARCH_DEBOUNCE_MS.milliseconds)
                .collect { query -> publish(stack.value, query) }
        }
    }

    fun setQuery(query: String) {
        _ui.update { it.copy(query = query) }
        queryText.value = query
    }

    fun openGroup(groupId: Int, hasChildren: Boolean) {
        val next = if (hasChildren) {
            MarketWatchlistPickerLevel.Groups(groupId)
        } else {
            MarketWatchlistPickerLevel.Types(groupId)
        }
        stack.value = stack.value + next
        viewModelScope.launch { publish(stack.value, queryText.value) }
    }

    fun pop() {
        if (stack.value.size <= 1) return
        stack.value = stack.value.dropLast(1)
        viewModelScope.launch { publish(stack.value, queryText.value) }
    }

    fun toggleType(typeId: Int) {
        if (!selectedIds.add(typeId)) selectedIds.remove(typeId)
        _ui.update { it.copy(selectedTypeIds = selectedIds.toList()) }
    }

    fun toggleSection(typeIds: List<Int>) {
        if (typeIds.isEmpty()) return
        if (typeIds.all { it in selectedIds }) {
            selectedIds.removeAll(typeIds.toSet())
        } else {
            typeIds.forEach { selectedIds.add(it) }
        }
        _ui.update { it.copy(selectedTypeIds = selectedIds.toList()) }
    }

    fun consumeSelection(): List<Int> {
        val ids = selectedIds.toList()
        selectedIds.clear()
        stack.value = listOf(MarketWatchlistPickerLevel.Groups(null))
        queryText.value = ""
        _ui.update { it.copy(query = "", selectedTypeIds = emptyList()) }
        viewModelScope.launch { publish(stack.value, "") }
        return ids
    }

    private suspend fun publish(levels: List<MarketWatchlistPickerLevel>, query: String) {
        publishMutex.withLock {
            if (allGroups.isEmpty()) return
            val level = levels.last()
            val byParent = allGroups.groupBy { it.parentGroupId }
            val parentsWithChildren = byParent.keys.filterNotNull().toSet()
            when (level) {
                is MarketWatchlistPickerLevel.Groups -> publishGroups(
                    parentId = level.parentGroupId,
                    query = query.trim(),
                    byParent = byParent,
                    parentsWithChildren = parentsWithChildren,
                    canGoBack = levels.size > 1,
                )
                is MarketWatchlistPickerLevel.Types -> publishTypes(
                    marketGroupId = level.marketGroupId,
                    query = query.trim(),
                    canGoBack = levels.size > 1,
                )
            }
        }
    }

    private suspend fun publishGroups(
        parentId: Int?,
        query: String,
        byParent: Map<Int?, List<MarketGroupEntity>>,
        parentsWithChildren: Set<Int>,
        canGoBack: Boolean,
    ) {
        val children = byParent[parentId].orEmpty()
        val title = parentId?.let { id -> allGroups.find { it.id == id }?.name }.orEmpty()
        if (query.isEmpty()) {
            _ui.update {
                it.copy(
                    title = title,
                    canGoBack = canGoBack,
                    level = MarketWatchlistPickerLevel.Groups(parentId),
                    groups = children,
                    childIdsWithChildren = parentsWithChildren,
                    searchTypes = emptyList(),
                    sections = emptyList(),
                    selectedTypeIds = selectedIds.toList(),
                )
            }
            return
        }
        val matchedGroups = children.filter { group ->
            group.name.orEmpty().contains(query, ignoreCase = true)
        }
        val types = repository.searchTypes(descendantIds(parentId, byParent), query)
        _ui.update {
            it.copy(
                title = title,
                canGoBack = canGoBack,
                level = MarketWatchlistPickerLevel.Groups(parentId),
                groups = matchedGroups,
                childIdsWithChildren = parentsWithChildren,
                searchTypes = types,
                sections = emptyList(),
                selectedTypeIds = selectedIds.toList(),
            )
        }
    }

    private suspend fun publishTypes(
        marketGroupId: Int,
        query: String,
        canGoBack: Boolean,
    ) {
        val sections = sectionsFor(marketGroupId)
        val visible = if (query.isEmpty()) {
            sections
        } else {
            sections.mapNotNull { section ->
                val matched = section.types.filter { type ->
                    type.name.orEmpty().contains(query, ignoreCase = true) ||
                        type.zhName.orEmpty().contains(query, ignoreCase = true) ||
                        type.enName.orEmpty().contains(query, ignoreCase = true)
                }
                if (matched.isEmpty()) null else section.copy(types = matched)
            }
        }
        val title = allGroups.find { it.id == marketGroupId }?.name.orEmpty()
        _ui.update {
            it.copy(
                title = title,
                canGoBack = canGoBack,
                level = MarketWatchlistPickerLevel.Types(marketGroupId),
                groups = emptyList(),
                childIdsWithChildren = emptySet(),
                searchTypes = emptyList(),
                sections = visible,
                selectedTypeIds = selectedIds.toList(),
            )
        }
    }

    private suspend fun sectionsFor(marketGroupId: Int): List<MarketTypeSection> {
        sectionCache[marketGroupId]?.let { return it }
        val types = repository.typesInGroup(marketGroupId)
        val sections = buildSections(types, metaNames)
        sectionCache[marketGroupId] = sections
        return sections
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
                    id = metaId ?: MarketTypeListUngrouped.ID,
                    title = metaId?.let { metaNames[it] }.orEmpty(),
                    types = items,
                )
            }
            .toMutableList()
        if (unpublished.isNotEmpty()) {
            sections += MarketTypeSection(
                id = MarketTypeListUnpublished.ID,
                title = "",
                types = unpublished,
                unpublished = true,
            )
        }
        return sections
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

private object MarketTypeListUngrouped {
    const val ID = -2
}

private object MarketTypeListUnpublished {
    const val ID = -1
}
