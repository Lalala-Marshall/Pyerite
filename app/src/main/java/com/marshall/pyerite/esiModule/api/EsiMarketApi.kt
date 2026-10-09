package com.marshall.pyerite.esiModule.api

import com.marshall.pyerite.esiModule.model.EsiMarketHistoryDto
import com.marshall.pyerite.esiModule.model.EsiMarketOrderDto
import com.marshall.pyerite.esiModule.model.EsiMarketPriceDto
import com.marshall.pyerite.esiModule.model.EsiMarketQuery
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ESI `/markets/...` routes (OpenAPI 3.1, no `/latest`, no trailing slash).
 */
internal interface EsiMarketApi {
    @Headers("Accept: application/json")
    @GET("markets/prices")
    suspend fun fetchPrices(): List<EsiMarketPriceDto>

    @Headers("Accept: application/json")
    @GET("markets/{region_id}/orders")
    suspend fun fetchRegionOrders(
        @Path("region_id") regionId: Int,
        @Query(EsiMarketQuery.TYPE_ID) typeId: Int,
        @Query(EsiMarketQuery.ORDER_TYPE) orderType: String,
        @Query(EsiPagedQuery.PAGE) page: Int,
    ): Response<List<EsiMarketOrderDto>>

    @Headers("Accept: application/json")
    @GET("markets/{region_id}/history")
    suspend fun fetchRegionHistory(
        @Path("region_id") regionId: Int,
        @Query(EsiMarketQuery.TYPE_ID) typeId: Int,
    ): List<EsiMarketHistoryDto>

    @Headers("Accept: application/json")
    @GET("markets/structures/{structure_id}")
    suspend fun fetchStructureOrders(
        @Path("structure_id") structureId: Long,
        @Header("Authorization") authorization: String,
        @Query(EsiPagedQuery.PAGE) page: Int,
    ): Response<List<EsiMarketOrderDto>>
}
