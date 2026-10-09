package com.marshall.pyerite.regionMarketModule.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SavedMarketStructure(
    val structureId: Long,
    val name: String,
    val systemId: Int,
    val regionId: Int,
    val security: Double? = null,
    val typeId: Int? = null,
    val iconFileName: String? = null,
    val characterId: Long,
)
