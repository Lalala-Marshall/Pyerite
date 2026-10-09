package com.marshall.pyerite.sdeModule.room.market

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Matches the prepackaged SDE `marketGroups` table.
 * [name] is already in the language of the open sqlite file.
 */
@Entity(tableName = "marketGroups")
data class MarketGroupEntity(
    @PrimaryKey
    @ColumnInfo(name = "group_id")
    val id: Int,
    val name: String? = null,
    @ColumnInfo(name = "icon_name") val iconName: String? = null,
    @ColumnInfo(name = "parentgroup_id") val parentGroupId: Int? = null,
    @ColumnInfo(name = "show", defaultValue = "1") val show: Int? = 1,
)
