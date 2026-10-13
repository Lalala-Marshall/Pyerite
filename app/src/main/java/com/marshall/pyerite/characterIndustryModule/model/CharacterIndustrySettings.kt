package com.marshall.pyerite.characterIndustryModule.model

internal data class CharacterIndustrySettings(
    val aggregateCharacters: Boolean = false,
    /** Checked characters other than the page's anchor character. */
    val extraCharacterIds: Set<Long> = emptySet(),
) {
    fun includedCharacterIds(anchorCharacterId: Long): List<Long> {
        if (!aggregateCharacters) return listOf(anchorCharacterId)
        return buildList {
            add(anchorCharacterId)
            extraCharacterIds.forEach { id ->
                if (id != anchorCharacterId && id !in this) add(id)
            }
        }
    }
}
