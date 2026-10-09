package com.marshall.pyerite.eveAuthModule.model

/** Logged-in character id and name. No tokens or scopes. */
data class EveSessionIdentity(
    val characterId: Long,
    val characterName: String,
)
