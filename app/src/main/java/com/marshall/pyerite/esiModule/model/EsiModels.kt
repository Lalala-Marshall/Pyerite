package com.marshall.pyerite.esiModule.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EsiCharacterDto(
    val name: String,
    @SerialName("corporation_id") val corporationId: Long? = null,
    @SerialName("alliance_id") val allianceId: Long? = null,
    val birthday: String? = null,
    @SerialName("security_status") val securityStatus: Double? = null,
    val description: String? = null,
    @SerialName("faction_id") val factionId: Long? = null,
    @SerialName("race_id") val raceId: Int? = null,
)

@Serializable
internal data class EsiOrganizationDto(
    val name: String,
    val ticker: String? = null,
    val description: String? = null,
    @SerialName("alliance_id") val allianceId: Long? = null,
    @SerialName("ceo_id") val ceoId: Long? = null,
    @SerialName("faction_id") val factionId: Long? = null,
    @SerialName("member_count") val memberCount: Int? = null,
    @SerialName("date_founded") val dateFounded: String? = null,
    @SerialName("executor_corporation_id") val executorCorporationId: Long? = null,
)

@Serializable
internal data class EsiCharacterSkillsDto(
    @SerialName("total_sp") val totalSp: Long = 0L,
    @SerialName("unallocated_sp") val unallocatedSp: Long = 0L,
    val skills: List<EsiCharacterSkillDto> = emptyList(),
)

@Serializable
internal data class EsiCharacterSkillDto(
    @SerialName("skill_id") val skillId: Int,
    @SerialName("active_skill_level") val activeSkillLevel: Int = 0,
    @SerialName("trained_skill_level") val trainedSkillLevel: Int = 0,
    @SerialName("skillpoints_in_skill") val skillpointsInSkill: Long = 0L,
)

@Serializable
internal data class EsiCharacterClonesDto(
    @SerialName("home_location") val homeLocation: EsiCloneLocationDto? = null,
    @SerialName("jump_clones") val jumpClones: List<EsiJumpCloneDto> = emptyList(),
    @SerialName("last_clone_jump_date") val lastCloneJumpDate: String? = null,
    @SerialName("last_station_change_date") val lastStationChangeDate: String? = null,
)

@Serializable
internal data class EsiCloneLocationDto(
    @SerialName("location_id") val locationId: Long,
    @SerialName("location_type") val locationType: String,
)

@Serializable
internal data class EsiJumpCloneDto(
    @SerialName("jump_clone_id") val jumpCloneId: Int,
    @SerialName("location_id") val locationId: Long,
    @SerialName("location_type") val locationType: String,
    val implants: List<Int> = emptyList(),
    val name: String? = null,
)

@Serializable
internal data class EsiSkillQueueEntryDto(
    @SerialName("skill_id") val skillId: Int,
    @SerialName("finished_level") val finishedLevel: Int,
    @SerialName("queue_position") val queuePosition: Int = 0,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("finish_date") val finishDate: String? = null,
    @SerialName("training_start_sp") val trainingStartSp: Long? = null,
    @SerialName("level_end_sp") val levelEndSp: Long? = null,
    @SerialName("level_start_sp") val levelStartSp: Long? = null,
)

@Serializable
internal data class EsiCharacterAttributesDto(
    val perception: Int = 0,
    val memory: Int = 0,
    val willpower: Int = 0,
    val intelligence: Int = 0,
    val charisma: Int = 0,
    @SerialName("bonus_remaps") val bonusRemaps: Int = 0,
    @SerialName("last_remap_date") val lastRemapDate: String? = null,
    @SerialName("accrued_remap_cooldown_date") val accruedRemapCooldownDate: String? = null,
)

@Serializable
internal data class EsiCharacterLocationDto(
    @SerialName("solar_system_id") val solarSystemId: Long,
    @SerialName("station_id") val stationId: Long? = null,
    @SerialName("structure_id") val structureId: Long? = null,
)

@Serializable
internal data class EsiUniverseSystemDto(
    val name: String,
    @SerialName("security_status") val securityStatus: Double? = null,
    @SerialName("star_id") val starId: Long? = null,
)

@Serializable
internal data class EsiUniverseStarDto(
    @SerialName("type_id") val typeId: Int,
    @SerialName("solar_system_id") val solarSystemId: Long? = null,
)

@Serializable
internal data class EsiUniverseTypeDto(
    val name: String,
)

@Serializable
internal data class EsiCharacterShipDto(
    @SerialName("ship_item_id") val shipItemId: Long,
    @SerialName("ship_type_id") val shipTypeId: Int,
    @SerialName("ship_name") val shipName: String? = null,
)

@Serializable
internal data class EsiCharacterFatigueDto(
    @SerialName("jump_fatigue_expire_date") val jumpFatigueExpireDate: String? = null,
    @SerialName("last_jump_date") val lastJumpDate: String? = null,
    @SerialName("last_update_date") val lastUpdateDate: String? = null,
)

@Serializable
internal data class EsiCharacterMedalDto(
    @SerialName("medal_id") val medalId: Int,
    val title: String = "",
    val description: String = "",
    val reason: String = "",
    val date: String? = null,
    @SerialName("corporation_id") val corporationId: Long? = null,
    @SerialName("issuer_id") val issuerId: Long? = null,
)

@Serializable
internal data class EsiCharacterOnlineDto(
    val online: Boolean = false,
)

@Serializable
internal data class EsiUniverseStructureDto(
    val name: String,
    @SerialName("solar_system_id") val solarSystemId: Long? = null,
    @SerialName("type_id") val typeId: Int? = null,
)

@Serializable
internal data class EsiUniverseStationDto(
    val name: String,
    @SerialName("type_id") val typeId: Int,
    @SerialName("system_id") val systemId: Long? = null,
)

internal data class EsiCharacterPublic(
    val characterId: Long,
    val name: String,
    val corporationId: Long?,
    val allianceId: Long?,
    val birthday: String? = null,
    val securityStatus: Double? = null,
    val description: String? = null,
    val factionId: Long? = null,
    val raceId: Int? = null,
)

@Serializable
internal data class EsiMailHeaderDto(
    @SerialName("mail_id") val mailId: Long,
    val subject: String? = null,
    val from: Long? = null,
    val timestamp: String? = null,
    @SerialName("is_read") val isRead: Boolean? = null,
    val labels: List<Int> = emptyList(),
    val recipients: List<EsiMailRecipientDto> = emptyList(),
)

@Serializable
internal data class EsiMailBodyDto(
    val body: String? = null,
    val from: Long? = null,
    val labels: List<Int> = emptyList(),
    val read: Boolean? = null,
    val recipients: List<EsiMailRecipientDto> = emptyList(),
    val subject: String? = null,
    val timestamp: String? = null,
)

@Serializable
internal data class EsiMailRecipientDto(
    @SerialName("recipient_id") val recipientId: Long,
    @SerialName("recipient_type") val recipientType: String,
)

@Serializable
internal data class EsiSendMailRequestDto(
    val recipients: List<EsiMailRecipientDto>,
    val subject: String,
    val body: String,
)

@Serializable
internal data class EsiMailingListDto(
    @SerialName("mailing_list_id") val mailingListId: Long,
    val name: String,
)

@Serializable
internal data class EsiMailLabelsDto(
    val labels: List<EsiMailLabelDto> = emptyList(),
)

@Serializable
internal data class EsiMailLabelDto(
    @SerialName("label_id") val labelId: Int,
    val name: String? = null,
)

/** System mail label IDs returned by ESI `/mail/labels`. */
internal object EsiMailLabelId {
    const val INBOX = 1
    const val SENT = 2
    const val CORPORATION = 4
    const val ALLIANCE = 8
}

/** Query parameter names for ESI mail list routes. */
internal object EsiMailQuery {
    const val LABELS = "labels"
}

/** Query parameter names for ESI calendar list routes. */
internal object EsiCalendarQuery {
    const val FROM_EVENT = "from_event"
}

/** Wire `event_response` / `response` values from ESI calendar routes. */
internal object EsiCalendarEventResponseValue {
    const val ACCEPTED = "accepted"
    const val DECLINED = "declined"
    const val NOT_RESPONDED = "not_responded"
    const val TENTATIVE = "tentative"
    const val UNDECIDED = "undecided"
}

/** Wire `owner_type` values from ESI calendar event details. */
internal object EsiCalendarOwnerTypeValue {
    const val EVE_SERVER = "eve_server"
    const val CORPORATION = "corporation"
    const val FACTION = "faction"
    const val CHARACTER = "character"
    const val ALLIANCE = "alliance"
}

@Serializable
internal data class EsiCalendarEventSummaryDto(
    @SerialName("event_id") val eventId: Long,
    @SerialName("event_date") val eventDate: String? = null,
    val title: String? = null,
    val importance: Int = 0,
    @SerialName("event_response") val eventResponse: String? = null,
)

@Serializable
internal data class EsiCalendarEventDetailDto(
    @SerialName("event_id") val eventId: Long,
    val date: String? = null,
    val duration: Long = 0L,
    val importance: Int = 0,
    @SerialName("owner_id") val ownerId: Long = 0L,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("owner_type") val ownerType: String? = null,
    val response: String? = null,
    val text: String? = null,
    val title: String? = null,
)

@Serializable
internal data class EsiUniverseNameDto(
    val id: Long,
    val name: String,
    val category: String,
)

/** Wire body from POST `/universe/ids`. Other categories are ignored. */
@Serializable
internal data class EsiUniverseIdsDto(
    val alliances: List<EsiUniverseIdNameDto> = emptyList(),
    val characters: List<EsiUniverseIdNameDto> = emptyList(),
    val corporations: List<EsiUniverseIdNameDto> = emptyList(),
)

@Serializable
internal data class EsiUniverseIdNameDto(
    val id: Long,
    val name: String,
)

/** Wire `category` values from POST `/universe/names` and ESI search. */
internal object EsiUniverseNameCategory {
    const val CHARACTER = "character"
    const val CORPORATION = "corporation"
    const val ALLIANCE = "alliance"
    const val STATION = "station"
    const val STRUCTURE = "structure"
}

/** Query parameter names for GET `/characters/{id}/search`. */
internal object EsiSearchQuery {
    const val CATEGORIES = "categories"
    const val SEARCH = "search"
    const val STRICT = "strict"
    const val CATEGORY_SEPARATOR = ","
    const val UNIVERSE_NAMES_BATCH_SIZE = 1_000
}

@Serializable
internal data class EsiSearchResultDto(
    val character: List<Long> = emptyList(),
    val corporation: List<Long> = emptyList(),
    val alliance: List<Long> = emptyList(),
    val station: List<Long> = emptyList(),
    val structure: List<Long> = emptyList(),
)

@Serializable
internal data class EsiContactDto(
    @SerialName("contact_id") val contactId: Long,
    @SerialName("contact_type") val contactType: String,
    val standing: Double = 0.0,
)

@Serializable
internal data class EsiCorporationHistoryDto(
    @SerialName("corporation_id") val corporationId: Long,
    @SerialName("record_id") val recordId: Long? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

/** Wire `recipient_type` values from ESI mail headers / bodies. */
internal object EsiMailRecipientType {
    const val CHARACTER = EsiUniverseNameCategory.CHARACTER
    const val CORPORATION = EsiUniverseNameCategory.CORPORATION
    const val ALLIANCE = EsiUniverseNameCategory.ALLIANCE
    const val MAILING_LIST = "mailing_list"
}

internal data class EsiOrganization(
    val id: Long,
    val name: String,
    val ticker: String?,
    val description: String? = null,
    val allianceId: Long? = null,
    val ceoId: Long? = null,
    val factionId: Long? = null,
    val memberCount: Int? = null,
    val dateFounded: String? = null,
    val executorCorporationId: Long? = null,
)

/** Query parameter and response header names for ESI paginated list routes. */
internal object EsiPagedQuery {
    const val PAGE = "page"
    const val PAGES_HEADER = "X-Pages"
}

/** Query names for `GET /corporations/{id}/industry/jobs`. */
internal object EsiIndustryJobsQuery {
    const val INCLUDE_COMPLETED = "include_completed"
}

internal object EsiHttpStatus {
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
}

@Serializable
internal data class EsiCorporationAssetDto(
    @SerialName("item_id") val itemId: Long,
    @SerialName("type_id") val typeId: Int,
    val quantity: Int = 0,
    @SerialName("location_id") val locationId: Long,
    @SerialName("location_type") val locationType: String,
    @SerialName("location_flag") val locationFlag: String,
    @SerialName("is_blueprint_copy") val isBlueprintCopy: Boolean = false,
    @SerialName("is_singleton") val isSingleton: Boolean = false,
)

@Serializable
internal data class EsiCorporationAssetNameDto(
    @SerialName("item_id") val itemId: Long,
    val name: String = "",
)

@Serializable
internal data class EsiCorporationAssetLocationDto(
    @SerialName("item_id") val itemId: Long,
    val position: EsiCorporationAssetPositionDto,
)

@Serializable
internal data class EsiCorporationAssetPositionDto(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
)

@Serializable
internal data class EsiCharacterAssetDto(
    @SerialName("item_id") val itemId: Long,
    @SerialName("type_id") val typeId: Int,
    val quantity: Int = 0,
    @SerialName("is_blueprint_copy") val isBlueprintCopy: Boolean = false,
    @SerialName("is_singleton") val isSingleton: Boolean = false,
)

@Serializable
internal data class EsiCharacterOrderDto(
    @SerialName("order_id") val orderId: Long,
    @SerialName("type_id") val typeId: Int,
    val price: Double = 0.0,
    @SerialName("volume_remain") val volumeRemain: Int = 0,
    @SerialName("is_buy_order") val isBuyOrder: Boolean = false,
    @SerialName("is_corporation") val isCorporation: Boolean = false,
    val escrow: Double = 0.0,
)

/** Wire `type` values from ESI contracts. */
internal object EsiContractTypeValue {
    const val ITEM_EXCHANGE = "item_exchange"
    const val AUCTION = "auction"
    const val COURIER = "courier"
}

/** Wire `status` values from ESI contracts. */
internal object EsiContractStatusValue {
    const val OUTSTANDING = "outstanding"
    const val IN_PROGRESS = "in_progress"
    const val FINISHED_ISSUER = "finished_issuer"
    const val FINISHED_CONTRACTOR = "finished_contractor"
    const val FINISHED = "finished"
    const val CANCELLED = "cancelled"
    const val REJECTED = "rejected"
    const val FAILED = "failed"
    const val DELETED = "deleted"
    const val REVERSED = "reversed"
}

@Serializable
internal data class EsiCharacterContractDto(
    @SerialName("contract_id") val contractId: Long,
    val type: String,
    val status: String,
    @SerialName("issuer_id") val issuerId: Long,
    @SerialName("for_corporation") val forCorporation: Boolean = false,
    val price: Double = 0.0,
)

@Serializable
internal data class EsiCorporationContractDto(
    @SerialName("contract_id") val contractId: Long,
    @SerialName("issuer_id") val issuerId: Long = 0,
    @SerialName("issuer_corporation_id") val issuerCorporationId: Long = 0,
    @SerialName("assignee_id") val assigneeId: Long = 0,
    @SerialName("start_location_id") val startLocationId: Long = 0,
    val type: String,
    val status: String,
    val title: String? = null,
    @SerialName("for_corporation") val forCorporation: Boolean = false,
    @SerialName("date_issued") val dateIssued: String? = null,
    @SerialName("date_expired") val dateExpired: String? = null,
    @SerialName("date_completed") val dateCompleted: String? = null,
    val price: Double = 0.0,
    val reward: Double = 0.0,
    val buyout: Double = 0.0,
    val volume: Double = 0.0,
)

@Serializable
internal data class EsiContractItemDto(
    @SerialName("type_id") val typeId: Int,
    val quantity: Int = 0,
    @SerialName("is_included") val isIncluded: Boolean = false,
)

@Serializable
internal data class EsiMarketPriceDto(
    @SerialName("type_id") val typeId: Int,
    @SerialName("average_price") val averagePrice: Double? = null,
    @SerialName("adjusted_price") val adjustedPrice: Double? = null,
)

/** Query names for public and structure market order routes. */
internal object EsiMarketQuery {
    const val TYPE_ID = "type_id"
    const val ORDER_TYPE = "order_type"
    const val ORDER_TYPE_ALL = "all"
}

@Serializable
internal data class EsiMarketOrderDto(
    val duration: Int = 0,
    @SerialName("is_buy_order") val isBuyOrder: Boolean = false,
    val issued: String = "",
    @SerialName("location_id") val locationId: Long = 0,
    @SerialName("min_volume") val minVolume: Int = 1,
    @SerialName("order_id") val orderId: Long = 0,
    val price: Double = 0.0,
    val range: String = "",
    @SerialName("system_id") val systemId: Int? = null,
    @SerialName("type_id") val typeId: Int = 0,
    @SerialName("volume_remain") val volumeRemain: Long = 0,
    @SerialName("volume_total") val volumeTotal: Long = 0,
)

@Serializable
internal data class EsiMarketHistoryDto(
    val average: Double = 0.0,
    val date: String = "",
    val highest: Double = 0.0,
    val lowest: Double = 0.0,
    @SerialName("order_count") val orderCount: Long = 0,
    val volume: Long = 0,
)

@Serializable
internal data class EsiLoyaltyPointsDto(
    @SerialName("corporation_id") val corporationId: Long,
    @SerialName("loyalty_points") val loyaltyPoints: Long,
)

@Serializable
internal data class EsiCharacterRolesDto(
    val roles: List<String> = emptyList(),
    @SerialName("roles_at_hq") val rolesAtHq: List<String> = emptyList(),
    @SerialName("roles_at_base") val rolesAtBase: List<String> = emptyList(),
    @SerialName("roles_at_other") val rolesAtOther: List<String> = emptyList(),
)

@Serializable
internal data class EsiCorporationWalletDto(
    val division: Int,
    val balance: Double = 0.0,
)

@Serializable
internal data class EsiCorporationDivisionNameDto(
    val division: Int,
    val name: String? = null,
)

@Serializable
internal data class EsiCorporationDivisionsDto(
    val hangar: List<EsiCorporationDivisionNameDto> = emptyList(),
    val wallet: List<EsiCorporationDivisionNameDto> = emptyList(),
)

@Serializable
internal data class EsiCorporationWalletJournalDto(
    val id: Long,
    val date: String,
    val amount: Double = 0.0,
    val balance: Double? = null,
    val description: String? = null,
    val reason: String? = null,
    @SerialName("ref_type") val refType: String,
    @SerialName("first_party_id") val firstPartyId: Long? = null,
    @SerialName("second_party_id") val secondPartyId: Long? = null,
    @SerialName("context_id") val contextId: Long? = null,
    @SerialName("context_id_type") val contextIdType: String? = null,
)

@Serializable
internal data class EsiCorporationMemberTrackingDto(
    @SerialName("character_id") val characterId: Long,
    @SerialName("location_id") val locationId: Long? = null,
    @SerialName("ship_type_id") val shipTypeId: Int? = null,
)

@Serializable
internal data class EsiCorporationStructureServiceDto(
    val name: String,
    val state: String,
)

@Serializable
internal data class EsiCorporationStructureDto(
    @SerialName("structure_id") val structureId: Long,
    @SerialName("type_id") val typeId: Int,
    @SerialName("system_id") val systemId: Long,
    val name: String? = null,
    val state: String,
    @SerialName("fuel_expires") val fuelExpires: String? = null,
    val services: List<EsiCorporationStructureServiceDto>? = null,
)

@Serializable
internal data class EsiCorporationIndustryJobDto(
    @SerialName("job_id") val jobId: Long,
    @SerialName("installer_id") val installerId: Long,
    @SerialName("facility_id") val facilityId: Long,
    @SerialName("activity_id") val activityId: Int,
    @SerialName("blueprint_id") val blueprintId: Long = 0L,
    @SerialName("blueprint_type_id") val blueprintTypeId: Int = 0,
    @SerialName("blueprint_location_id") val blueprintLocationId: Long = 0L,
    @SerialName("output_location_id") val outputLocationId: Long = 0L,
    val runs: Int = 0,
    val cost: Double? = null,
    @SerialName("licensed_runs") val licensedRuns: Int? = null,
    val probability: Double? = null,
    @SerialName("product_type_id") val productTypeId: Int? = null,
    val status: String,
    val duration: Int = 0,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("pause_date") val pauseDate: String? = null,
    @SerialName("completed_date") val completedDate: String? = null,
    @SerialName("completed_character_id") val completedCharacterId: Long? = null,
    @SerialName("successful_runs") val successfulRuns: Int? = null,
)

@Serializable
internal data class EsiCorporationWalletTransactionDto(
    @SerialName("transaction_id") val transactionId: Long,
    val date: String,
    @SerialName("type_id") val typeId: Int,
    val quantity: Int = 0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("is_buy") val isBuy: Boolean = false,
    @SerialName("location_id") val locationId: Long,
    @SerialName("client_id") val clientId: Long? = null,
    @SerialName("journal_ref_id") val journalRefId: Long? = null,
)
