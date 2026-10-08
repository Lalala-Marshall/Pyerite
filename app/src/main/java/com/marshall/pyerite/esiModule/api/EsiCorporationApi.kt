package com.marshall.pyerite.esiModule.api

import com.marshall.pyerite.esiModule.model.EsiContactDto
import com.marshall.pyerite.esiModule.model.EsiCorporationAssetDto
import com.marshall.pyerite.esiModule.model.EsiCorporationAssetLocationDto
import com.marshall.pyerite.esiModule.model.EsiCorporationAssetNameDto
import com.marshall.pyerite.esiModule.model.EsiCorporationDivisionsDto
import com.marshall.pyerite.esiModule.model.EsiCorporationIndustryJobDto
import com.marshall.pyerite.esiModule.model.EsiCorporationMemberTrackingDto
import com.marshall.pyerite.esiModule.model.EsiCorporationStructureDto
import com.marshall.pyerite.esiModule.model.EsiIndustryJobsQuery
import com.marshall.pyerite.esiModule.model.EsiCorporationWalletDto
import com.marshall.pyerite.esiModule.model.EsiCorporationWalletJournalDto
import com.marshall.pyerite.esiModule.model.EsiCorporationWalletTransactionDto
import com.marshall.pyerite.esiModule.model.EsiOrganizationDto
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ESI `/corporations/...` routes (OpenAPI 3.1, no `/latest`, no trailing slash).
 */
internal interface EsiCorporationApi {
    @Headers("Accept: application/json")
    @GET("corporations/{corporation_id}")
    suspend fun fetchCorporation(@Path("corporation_id") corporationId: Long): EsiOrganizationDto

    @Headers("Accept: application/json")
    @GET("corporations/{corporation_id}/contacts")
    suspend fun fetchContacts(
        @Path("corporation_id") corporationId: Long,
        @Header("Authorization") authorization: String,
    ): List<EsiContactDto>

    @Headers("Accept: application/json")
    @GET("corporations/{corporation_id}/wallets")
    suspend fun fetchWallets(
        @Path("corporation_id") corporationId: Long,
        @Header("Authorization") authorization: String,
    ): List<EsiCorporationWalletDto>

    @Headers("Accept: application/json")
    @GET("corporations/{corporation_id}/wallets/{division}/journal")
    suspend fun fetchWalletJournal(
        @Path("corporation_id") corporationId: Long,
        @Path("division") division: Int,
        @Header("Authorization") authorization: String,
        @Query(EsiPagedQuery.PAGE) page: Int,
    ): Response<List<EsiCorporationWalletJournalDto>>

    @Headers("Accept: application/json")
    @GET("corporations/{corporation_id}/wallets/{division}/transactions")
    suspend fun fetchWalletTransactions(
        @Path("corporation_id") corporationId: Long,
        @Path("division") division: Int,
        @Header("Authorization") authorization: String,
        @Query(EsiPagedQuery.PAGE) page: Int,
    ): Response<List<EsiCorporationWalletTransactionDto>>

    @Headers("Accept: application/json")
    @GET("corporations/{corporation_id}/structures")
    suspend fun fetchStructures(
        @Path("corporation_id") corporationId: Long,
        @Header("Authorization") authorization: String,
        @Query(EsiPagedQuery.PAGE) page: Int,
    ): Response<List<EsiCorporationStructureDto>>

    @Headers("Accept: application/json")
    @GET("corporations/{corporation_id}/membertracking")
    suspend fun fetchMemberTracking(
        @Path("corporation_id") corporationId: Long,
        @Header("Authorization") authorization: String,
        @Query(EsiPagedQuery.PAGE) page: Int,
    ): Response<List<EsiCorporationMemberTrackingDto>>

    @Headers("Accept: application/json")
    @GET("corporations/{corporation_id}/industry/jobs")
    suspend fun fetchIndustryJobs(
        @Path("corporation_id") corporationId: Long,
        @Header("Authorization") authorization: String,
        @Query(EsiPagedQuery.PAGE) page: Int,
        @Query(EsiIndustryJobsQuery.INCLUDE_COMPLETED) includeCompleted: Boolean,
    ): Response<List<EsiCorporationIndustryJobDto>>

    @Headers("Accept: application/json")
    @GET("corporations/{corporation_id}/assets")
    suspend fun fetchAssets(
        @Path("corporation_id") corporationId: Long,
        @Header("Authorization") authorization: String,
        @Query(EsiPagedQuery.PAGE) page: Int,
    ): Response<List<EsiCorporationAssetDto>>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("corporations/{corporation_id}/assets/names")
    suspend fun fetchAssetNames(
        @Path("corporation_id") corporationId: Long,
        @Header("Authorization") authorization: String,
        @Body itemIds: List<Long>,
    ): List<EsiCorporationAssetNameDto>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("corporations/{corporation_id}/assets/locations")
    suspend fun fetchAssetLocations(
        @Path("corporation_id") corporationId: Long,
        @Header("Authorization") authorization: String,
        @Body itemIds: List<Long>,
    ): List<EsiCorporationAssetLocationDto>

    @Headers("Accept: application/json")
    @GET("corporations/{corporation_id}/divisions")
    suspend fun fetchDivisions(
        @Path("corporation_id") corporationId: Long,
        @Header("Authorization") authorization: String,
    ): EsiCorporationDivisionsDto
}
