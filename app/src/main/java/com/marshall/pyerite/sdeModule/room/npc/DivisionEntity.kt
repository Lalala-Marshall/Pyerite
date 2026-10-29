package com.marshall.pyerite.sdeModule.room.npc

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Matches the prepackaged SDE `divisions` table. `name` is already the active
 * language of that sqlite file, so this row is not [com.marshall.pyerite.localization.LocalizableName].
 */
@Entity(tableName = "divisions")
data class DivisionEntity(
    @PrimaryKey
    @ColumnInfo(name = "division_id")
    val divisionId: Int,
    val name: String? = null,
)
