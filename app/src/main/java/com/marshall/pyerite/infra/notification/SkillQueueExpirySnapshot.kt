package com.marshall.pyerite.infra.notification

import kotlinx.serialization.Serializable

@Serializable
internal data class SkillQueueExpirySnapshot(
    val characterId: Long,
    val characterName: String = "",
    val queueEndEpochMs: Long? = null,
    val notifiedForQueueEndEpochMs: Long? = null,
)
