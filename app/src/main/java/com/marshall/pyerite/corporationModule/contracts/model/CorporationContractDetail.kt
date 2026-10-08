package com.marshall.pyerite.corporationModule.contracts.model

import com.marshall.pyerite.localization.LocalizableName

internal data class CorporationContractPlace(
    val name: String,
    val securityStatus: Double?,
    val iconFileName: String?,
)

internal data class CorporationContractParty(
    val name: String,
    val affiliation: String?,
    val iconUrl: String?,
)

internal data class CorporationContractOfferedItem(
    val typeId: Int,
    val quantity: Long,
    override val zhName: String?,
    override val enName: String?,
    override val name: String?,
    val iconFileName: String?,
) : LocalizableName

internal data class CorporationContractDetail(
    val contract: CorporationContract,
    val place: CorporationContractPlace?,
    val issuer: CorporationContractParty?,
    val assignee: CorporationContractParty?,
    val assigneeIsPublic: Boolean,
    val items: List<CorporationContractOfferedItem>,
    val itemsLoadFailed: Boolean,
)
