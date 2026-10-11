package com.marshall.pyerite.characterTransactionsModule.data

import com.marshall.pyerite.characterTransactionsModule.model.CharacterMarketTransaction
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionLocation
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsAccessException
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsConfig
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsDateFormatter
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsSnapshot
import com.marshall.pyerite.esiModule.api.EsiCharacterApi
import com.marshall.pyerite.esiModule.api.EsiUniverseApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.model.EsiCharacterWalletTransactionDto
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.parseEsiDateMillis
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.sdeModule.room.type.TypeDisplayIconRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

internal class CharacterTransactionsLoader(
    private val tokenManager: EveTokenManager,
    private val characterApi: EsiCharacterApi,
    private val universeApi: EsiUniverseApi,
    private val publicEsi: EsiPublicDataSource,
    private val roomProvider: RoomProvider,
) {
    suspend fun load(characterId: Long): CharacterTransactionsSnapshot = withContext(Dispatchers.IO) {
        requireWalletScope(characterId)
        val transactionDtos = fetchTransactions(characterId)
        val types = loadTypes(transactionDtos.map { it.typeId }.distinct())
        val locations = loadLocations(
            characterId = characterId,
            locationIds = transactionDtos.map { it.locationId }.distinct(),
        )
        CharacterTransactionsSnapshot(
            characterId = characterId,
            transactions = transactionDtos.mapNotNull { dto ->
                mapTransaction(dto, types, locations)
            }.sortedByDescending { it.dateEpochMs },
        )
    }

    private fun requireWalletScope(characterId: Long) {
        if (EveSsoScope.WALLET_CHARACTER !in tokenManager.grantedScopes(characterId)) {
            throw CharacterTransactionsAccessException()
        }
    }

    private suspend fun fetchTransactions(
        characterId: Long,
    ): List<EsiCharacterWalletTransactionDto> {
        val response = fetchTransactionPage(characterId)
        if (!response.isSuccessful) throw mapPagedError(response)
        return response.body().orEmpty()
    }

    private suspend fun fetchTransactionPage(
        characterId: Long,
    ): Response<List<EsiCharacterWalletTransactionDto>> {
        return tokenManager.executeWithAuthRetry(characterId) { auth ->
            val response = characterApi.fetchWalletTransactions(
                characterId = characterId,
                authorization = auth,
            )
            if (response.code() == EsiHttpStatus.UNAUTHORIZED) {
                throw HttpException(response)
            }
            response
        }
    }

    private fun <T> mapPagedError(response: Response<T>): Throwable {
        if (response.code() == EsiHttpStatus.FORBIDDEN) {
            return CharacterTransactionsAccessException()
        }
        return HttpException(response)
    }

    private suspend fun loadTypes(typeIds: List<Int>): Map<Int, TypeDisplayIconRow> {
        if (typeIds.isEmpty()) return emptyMap()
        val dao = roomProvider.getDatabase().sdeTypeDao()
        return typeIds.chunked(CharacterTransactionsConfig.TYPE_QUERY_CHUNK).flatMap { chunk ->
            runCatching { dao.getTypesForDisplay(chunk) }.getOrDefault(emptyList())
        }.associateBy { it.id }
    }

    private suspend fun loadLocations(
        characterId: Long,
        locationIds: List<Long>,
    ): Map<Long, CharacterTransactionLocation?> {
        if (locationIds.isEmpty()) return emptyMap()
        return locationIds.associateWith { id -> resolveLocation(characterId, id) }
    }

    private suspend fun resolveLocation(
        characterId: Long,
        locationId: Long,
    ): CharacterTransactionLocation? {
        val station = runCatching {
            roomProvider.getDatabase().mapDao().getStation(locationId)
        }.getOrNull()
        if (station != null) {
            val solarSystemId = station.solarSystemId?.toLong()
            val system = solarSystemId?.let { loadSolarSystem(it) }
            return CharacterTransactionLocation(
                securityStatus = system?.securityStatus ?: stationSecurity(solarSystemId),
                systemZhName = system?.systemZhName,
                systemEnName = system?.systemEnName,
                systemName = system?.systemName,
                placeName = station.name?.takeIf { it.isNotBlank() },
            )
        }
        val esiStation = if (locationId < CharacterTransactionsConfig.PLAYER_STRUCTURE_ID_MIN) {
            publicEsi.fetchStation(locationId)
        } else {
            null
        }
        if (esiStation != null) {
            val system = esiStation.systemId?.let { loadSolarSystem(it) }
            return CharacterTransactionLocation(
                securityStatus = system?.securityStatus
                    ?: esiStation.systemId?.let { publicEsi.fetchSolarSystemSecurity(it) },
                systemZhName = system?.systemZhName,
                systemEnName = system?.systemEnName,
                systemName = system?.systemName
                    ?: esiStation.systemId?.let { publicEsi.fetchSolarSystemName(it) },
                placeName = esiStation.name.takeIf { it.isNotBlank() },
            )
        }
        val structure = runCatching {
            tokenManager.executeWithAuthRetry(characterId) { auth ->
                universeApi.fetchStructure(locationId, auth)
            }
        }.getOrNull() ?: return null
        val system = structure.solarSystemId?.let { loadSolarSystem(it) }
        return CharacterTransactionLocation(
            securityStatus = system?.securityStatus
                ?: structure.solarSystemId?.let { publicEsi.fetchSolarSystemSecurity(it) },
            systemZhName = system?.systemZhName,
            systemEnName = system?.systemEnName,
            systemName = system?.systemName
                ?: structure.solarSystemId?.let { publicEsi.fetchSolarSystemName(it) },
            placeName = structure.name.takeIf { it.isNotBlank() },
        )
    }

    private suspend fun loadSolarSystem(solarSystemId: Long) = runCatching {
        roomProvider.getDatabase().mapDao().getSolarSystemLocation(solarSystemId)
    }.getOrNull()

    private suspend fun stationSecurity(solarSystemId: Long?): Double? {
        solarSystemId ?: return null
        return loadSolarSystem(solarSystemId)?.securityStatus
            ?: publicEsi.fetchSolarSystemSecurity(solarSystemId)
    }

    private fun mapTransaction(
        dto: EsiCharacterWalletTransactionDto,
        types: Map<Int, TypeDisplayIconRow>,
        locations: Map<Long, CharacterTransactionLocation?>,
    ): CharacterMarketTransaction? {
        val epochMs = parseEsiDateMillis(dto.date) ?: return null
        val type = types[dto.typeId]
        return CharacterMarketTransaction(
            transactionId = dto.transactionId,
            dateEpochMs = epochMs,
            dayKey = CharacterTransactionsDateFormatter.dayKey(epochMs),
            typeId = dto.typeId,
            locationId = dto.locationId,
            zhName = type?.zhName,
            enName = type?.enName,
            name = type?.name ?: dto.typeId.toString(),
            iconFilename = type?.iconFilename,
            quantity = dto.quantity.toLong(),
            unitPrice = dto.unitPrice,
            isBuy = dto.isBuy,
            location = locations[dto.locationId],
        )
    }
}
