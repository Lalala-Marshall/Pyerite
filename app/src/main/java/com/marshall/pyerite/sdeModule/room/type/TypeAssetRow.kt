package com.marshall.pyerite.sdeModule.room.type

import com.marshall.pyerite.localization.LocalizableName

/** Type projection used to name, icon, and measure corporation assets. */
data class TypeAssetRow(
    val id: Int,
    override val name: String?,
    override val zhName: String?,
    override val enName: String?,
    val iconFilename: String?,
    val bpcIconFilename: String?,
    val volume: Double?,
    val repackagedVolume: Double?,
    val capacity: Double?,
    val categoryId: Int?,
) : LocalizableName
