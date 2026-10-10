package com.marshall.pyerite.personalAssetsModule.model

internal data class PersonalAssetsSettings(
    val aggregateCharacters: Boolean = false,
    val mergeSameLocation: Boolean = false,
    /** Checked characters other than the page's anchor character. */
    val extraCharacterIds: Set<Long> = emptySet(),
) {
    fun characterIds(anchorCharacterId: Long): List<Long> {
        if (!aggregateCharacters) return listOf(anchorCharacterId)
        return buildList {
            add(anchorCharacterId)
            extraCharacterIds.forEach { id ->
                if (id != anchorCharacterId && id !in this) add(id)
            }
        }
    }
}
