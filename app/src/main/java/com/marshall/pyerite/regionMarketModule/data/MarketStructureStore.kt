package com.marshall.pyerite.regionMarketModule.data

import android.content.Context
import androidx.core.content.edit
import com.marshall.pyerite.infra.network.PyeriteJson
import com.marshall.pyerite.regionMarketModule.model.SavedMarketStructure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer

/** Player structures the user added as market locations. */
internal class MarketStructureStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val listSerializer = ListSerializer(SavedMarketStructure.serializer())

    private val _structures = MutableStateFlow(read())
    val structures: StateFlow<List<SavedMarketStructure>> = _structures.asStateFlow()

    fun add(structure: SavedMarketStructure) {
        val next = _structures.value.filterNot { it.structureId == structure.structureId } + structure
        write(next)
    }

    fun find(structureId: Long): SavedMarketStructure? =
        _structures.value.firstOrNull { it.structureId == structureId }

    private fun write(structures: List<SavedMarketStructure>) {
        prefs.edit { putString(KEY_STRUCTURES, PyeriteJson.encodeToString(listSerializer, structures)) }
        _structures.value = structures
    }

    private fun read(): List<SavedMarketStructure> {
        val raw = prefs.getString(KEY_STRUCTURES, null) ?: return emptyList()
        return runCatching { PyeriteJson.decodeFromString(listSerializer, raw) }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "pyerite_region_market"
        const val KEY_STRUCTURES = "market_structures"
    }
}
