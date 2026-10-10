package com.marshall.pyerite.esiModule.api

import com.marshall.pyerite.esiModule.model.EsiCalendarEventDetailDto
import com.marshall.pyerite.esiModule.model.EsiCalendarEventSummaryDto
import com.marshall.pyerite.esiModule.model.EsiCalendarQuery
import com.marshall.pyerite.esiModule.model.EsiCharacterAssetDto
import com.marshall.pyerite.esiModule.model.EsiCorporationAssetLocationDto
import com.marshall.pyerite.esiModule.model.EsiCorporationAssetNameDto
import com.marshall.pyerite.esiModule.model.EsiCharacterAttributesDto
import com.marshall.pyerite.esiModule.model.EsiCharacterClonesDto
import com.marshall.pyerite.esiModule.model.EsiCharacterContractDto
import com.marshall.pyerite.esiModule.model.EsiCharacterDto
import com.marshall.pyerite.esiModule.model.EsiCharacterFatigueDto
import com.marshall.pyerite.esiModule.model.EsiCharacterLocationDto
import com.marshall.pyerite.esiModule.model.EsiCharacterMedalDto
import com.marshall.pyerite.esiModule.model.EsiCharacterOnlineDto
import com.marshall.pyerite.esiModule.model.EsiCharacterOrderDto
import com.marshall.pyerite.esiModule.model.EsiCharacterRolesDto
import com.marshall.pyerite.esiModule.model.EsiCharacterShipDto
import com.marshall.pyerite.esiModule.model.EsiCharacterSkillsDto
import com.marshall.pyerite.esiModule.model.EsiContactDto
import com.marshall.pyerite.esiModule.model.EsiContractItemDto
import com.marshall.pyerite.esiModule.model.EsiCorporationHistoryDto
import com.marshall.pyerite.esiModule.model.EsiMailBodyDto
import com.marshall.pyerite.esiModule.model.EsiMailHeaderDto
import com.marshall.pyerite.esiModule.model.EsiMailLabelsDto
import com.marshall.pyerite.esiModule.model.EsiMailingListDto
import com.marshall.pyerite.esiModule.model.EsiMailQuery
import com.marshall.pyerite.esiModule.model.EsiLoyaltyPointsDto
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.esiModule.model.EsiSearchQuery
import com.marshall.pyerite.esiModule.model.EsiSearchResultDto
import com.marshall.pyerite.esiModule.model.EsiSendMailRequestDto
import com.marshall.pyerite.esiModule.model.EsiSkillQueueEntryDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ESI `/characters/...` routes (OpenAPI 3.1, no `/latest`, no trailing slash).
 */
internal interface EsiCharacterApi {
    @Headers("Accept: application/json")
    @GET("characters/{character_id}")
    suspend fun fetchCharacter(@Path("character_id") characterId: Long): EsiCharacterDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/corporationhistory")
    suspend fun fetchCorporationHistory(
        @Path("character_id") characterId: Long,
    ): List<EsiCorporationHistoryDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/contacts")
    suspend fun fetchContacts(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): List<EsiContactDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/wallet")
    suspend fun fetchWallet(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): ResponseBody

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/roles")
    suspend fun fetchRoles(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): EsiCharacterRolesDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/skills")
    suspend fun fetchSkills(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): EsiCharacterSkillsDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/skillqueue")
    suspend fun fetchSkillQueue(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): List<EsiSkillQueueEntryDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/attributes")
    suspend fun fetchAttributes(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): EsiCharacterAttributesDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/location")
    suspend fun fetchLocation(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): EsiCharacterLocationDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/ship")
    suspend fun fetchShip(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): EsiCharacterShipDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/fatigue")
    suspend fun fetchFatigue(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): EsiCharacterFatigueDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/clones")
    suspend fun fetchClones(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): EsiCharacterClonesDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/implants")
    suspend fun fetchImplants(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): List<Int>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/medals")
    suspend fun fetchMedals(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): List<EsiCharacterMedalDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/online")
    suspend fun fetchOnline(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): EsiCharacterOnlineDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/calendar")
    suspend fun fetchCalendarEvents(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
        @Query(EsiCalendarQuery.FROM_EVENT) fromEvent: Long? = null,
    ): List<EsiCalendarEventSummaryDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/calendar/{event_id}")
    suspend fun fetchCalendarEvent(
        @Path("character_id") characterId: Long,
        @Path("event_id") eventId: Long,
        @Header("Authorization") authorization: String,
    ): EsiCalendarEventDetailDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/mail")
    suspend fun fetchMailHeaders(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
        @Query(EsiMailQuery.LABELS) labels: List<Int>? = null,
    ): List<EsiMailHeaderDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/mail/labels")
    suspend fun fetchMailLabels(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): EsiMailLabelsDto

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/mail/lists")
    suspend fun fetchMailingLists(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): List<EsiMailingListDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/mail/{mail_id}")
    suspend fun fetchMail(
        @Path("character_id") characterId: Long,
        @Path("mail_id") mailId: Long,
        @Header("Authorization") authorization: String,
    ): EsiMailBodyDto

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("characters/{character_id}/mail")
    suspend fun sendMail(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
        @Body mail: EsiSendMailRequestDto,
    ): ResponseBody

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/assets")
    suspend fun fetchAssets(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
        @Query(EsiPagedQuery.PAGE) page: Int,
    ): Response<List<EsiCharacterAssetDto>>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("characters/{character_id}/assets/names")
    suspend fun fetchAssetNames(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
        @Body itemIds: List<Long>,
    ): List<EsiCorporationAssetNameDto>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("characters/{character_id}/assets/locations")
    suspend fun fetchAssetLocations(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
        @Body itemIds: List<Long>,
    ): List<EsiCorporationAssetLocationDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/orders")
    suspend fun fetchOrders(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): List<EsiCharacterOrderDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/contracts")
    suspend fun fetchContracts(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
        @Query(EsiPagedQuery.PAGE) page: Int,
    ): List<EsiCharacterContractDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/contracts/{contract_id}/items")
    suspend fun fetchContractItems(
        @Path("character_id") characterId: Long,
        @Path("contract_id") contractId: Long,
        @Header("Authorization") authorization: String,
    ): List<EsiContractItemDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/loyalty/points")
    suspend fun fetchLoyaltyPoints(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
    ): List<EsiLoyaltyPointsDto>

    @Headers("Accept: application/json")
    @GET("characters/{character_id}/search")
    suspend fun search(
        @Path("character_id") characterId: Long,
        @Header("Authorization") authorization: String,
        @Query(EsiSearchQuery.CATEGORIES) categories: String,
        @Query(EsiSearchQuery.SEARCH) search: String,
        @Query(EsiSearchQuery.STRICT) strict: Boolean,
    ): EsiSearchResultDto
}
