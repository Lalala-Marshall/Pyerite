package com.marshall.pyerite.regionMarketModule.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.regionMarketModule.data.MarketRepository
import com.marshall.pyerite.regionMarketModule.data.MarketStructureAccessException
import com.marshall.pyerite.regionMarketModule.data.SelectedMarketStore
import com.marshall.pyerite.regionMarketModule.model.MarketHistoryPoint
import com.marshall.pyerite.regionMarketModule.model.MarketHistoryRange
import com.marshall.pyerite.regionMarketModule.model.MarketOrder
import com.marshall.pyerite.regionMarketModule.model.MarketOrderLocation
import com.marshall.pyerite.regionMarketModule.model.MarketSelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class MarketDetailUiState(
    val typeName: String = "",
    val categoryName: String = "",
    val groupName: String = "",
    val iconFileName: String? = null,
    val locationName: String = "",
    val loading: Boolean = true,
    val updatePage: Int = 0,
    val updatePageCount: Int = 0,
    val failed: Boolean = false,
    val structureAccessDenied: Boolean = false,
    val lowestSell: Double? = null,
    val history: List<MarketHistoryPoint> = emptyList(),
    val hideLocations: Boolean = false,
    val range: MarketHistoryRange = MarketHistoryRange.YEAR,
    val typeId: Int = 0,
    val followingGlobal: Boolean = true,
    val displayPlaceKey: String = "",
)

internal class MarketDetailViewModel(
    private val typeId: Int,
    initialPlaceKey: String?,
    private val repository: MarketRepository,
    private val selectionStore: SelectedMarketStore,
    private val localeController: LocaleController,
) : ViewModel() {
    private val _ui = MutableStateFlow(MarketDetailUiState(typeId = typeId))
    val ui: StateFlow<MarketDetailUiState> = _ui.asStateFlow()

    private var loadJob: Job? = null
    @Volatile
    private var loadToken: Int = 0

    private val displayPlace = MarketDisplayPlace(
        initialPlaceKey = initialPlaceKey,
        selectionStore = selectionStore,
    )

    init {
        viewModelScope.launch {
            val type = repository.type(typeId)
            _ui.value = _ui.value.copy(
                typeName = type?.displayName(localeController).orEmpty(),
                categoryName = type?.categoryName.orEmpty(),
                groupName = type?.groupName.orEmpty(),
                iconFileName = type?.iconFilename,
            )
        }
        displayPlace.bind(viewModelScope) { selection ->
            load(selection, forceRefresh = false)
        }
    }

    fun refresh() {
        load(displayPlace.refreshSelection(), forceRefresh = true)
    }

    fun followGlobal() {
        displayPlace.followGlobal(viewModelScope) { selection ->
            load(selection, forceRefresh = false)
        }
    }

    fun setRange(range: MarketHistoryRange) {
        _ui.value = _ui.value.copy(range = range)
    }

    private fun load(selection: MarketSelection, forceRefresh: Boolean) {
        loadJob?.cancel()
        val token = ++loadToken
        loadJob = viewModelScope.launch {
            _ui.value = _ui.value.copy(
                loading = true,
                updatePage = 0,
                updatePageCount = 0,
                failed = false,
                structureAccessDenied = false,
                followingGlobal = displayPlace.followingGlobal,
                displayPlaceKey = selection.persistKey,
            )
            val resolved = repository.resolve(selection, typeId)
            _ui.value = _ui.value.copy(
                locationName = resolved.displayName,
                hideLocations = resolved.hideLocations,
            )
            try {
                val quote = repository.loadQuote(
                    typeId = typeId,
                    selection = selection,
                    includeHistory = true,
                    forceRefresh = forceRefresh,
                    onPage = { page, pageCount ->
                        if (token == loadToken) {
                            _ui.value = _ui.value.copy(updatePage = page, updatePageCount = pageCount)
                        }
                    },
                )
                if (token != loadToken) return@launch
                _ui.value = _ui.value.copy(
                    loading = false,
                    failed = false,
                    structureAccessDenied = quote.structureAccessDenied,
                    lowestSell = quote.lowestSell,
                    history = quote.history,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: MarketStructureAccessException) {
                if (token != loadToken) return@launch
                _ui.value = _ui.value.copy(
                    loading = false,
                    structureAccessDenied = true,
                    lowestSell = null,
                    history = emptyList(),
                )
            } catch (_: Exception) {
                if (token != loadToken) return@launch
                _ui.value = _ui.value.copy(loading = false, failed = true, lowestSell = null, history = emptyList())
            }
        }
    }
}

internal class MarketOrdersViewModel(
    private val typeId: Int,
    initialPlaceKey: String?,
    private val repository: MarketRepository,
    private val selectionStore: SelectedMarketStore,
    private val localeController: LocaleController,
) : ViewModel() {
    private val _orders = MutableStateFlow<List<MarketOrder>>(emptyList())
    val orders: StateFlow<List<MarketOrder>> = _orders.asStateFlow()

    private val _locations = MutableStateFlow<Map<Long, MarketOrderLocation>>(emptyMap())
    val locations: StateFlow<Map<Long, MarketOrderLocation>> = _locations.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    private val _hideLocations = MutableStateFlow(false)
    val hideLocations: StateFlow<Boolean> = _hideLocations.asStateFlow()

    private val _structureAccessDenied = MutableStateFlow(false)
    val structureAccessDenied: StateFlow<Boolean> = _structureAccessDenied.asStateFlow()

    private val _locationName = MutableStateFlow("")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _followingGlobal = MutableStateFlow(initialPlaceKey.isNullOrBlank())
    val followingGlobal: StateFlow<Boolean> = _followingGlobal.asStateFlow()

    private val _displayPlaceKey = MutableStateFlow(initialPlaceKey.orEmpty())
    val displayPlaceKey: StateFlow<String> = _displayPlaceKey.asStateFlow()

    private var loadJob: Job? = null

    private val displayPlace = MarketDisplayPlace(
        initialPlaceKey = initialPlaceKey,
        selectionStore = selectionStore,
    )

    init {
        viewModelScope.launch {
            _title.value = repository.type(typeId)?.displayName(localeController).orEmpty()
        }
        displayPlace.bind(viewModelScope) { selection ->
            load(selection, forceRefresh = false)
        }
    }

    fun refresh() {
        load(displayPlace.refreshSelection(), forceRefresh = true)
    }

    fun followGlobal() {
        displayPlace.followGlobal(viewModelScope) { selection ->
            load(selection, forceRefresh = false)
        }
    }

    private fun load(selection: MarketSelection, forceRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _loading.value = true
            _failed.value = false
            _followingGlobal.value = displayPlace.followingGlobal
            _displayPlaceKey.value = selection.persistKey
            val resolved = repository.resolve(selection, typeId)
            _locationName.value = resolved.displayName
            _hideLocations.value = resolved.hideLocations
            try {
                val quote = repository.loadQuote(
                    typeId = typeId,
                    selection = selection,
                    includeHistory = false,
                    forceRefresh = forceRefresh,
                )
                _orders.value = quote.orders
                _structureAccessDenied.value = quote.structureAccessDenied
                _locations.value = if (resolved.hideLocations) {
                    emptyMap()
                } else {
                    repository.orderLocations(quote.orders, resolved.structureCharacterId)
                }
                _loading.value = false
            } catch (_: Exception) {
                _orders.value = emptyList()
                _failed.value = true
                _loading.value = false
            }
        }
    }
}
