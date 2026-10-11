package com.marshall.pyerite.characterWalletJournalModule.data

import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalAccessException
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalConfig
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDateFormatter
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDescription
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDescriptionKind
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalEntry
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalFilterType
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalSnapshot
import com.marshall.pyerite.esiModule.api.EsiCharacterApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.model.EsiCharacterWalletJournalDto
import com.marshall.pyerite.esiModule.model.EsiHttpStatus
import com.marshall.pyerite.esiModule.model.EsiPagedQuery
import com.marshall.pyerite.esiModule.model.parseEsiDateMillis
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.walletJournal.model.WalletJournalCategory
import com.marshall.pyerite.walletJournal.model.WalletJournalRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

internal class CharacterWalletJournalLoader(
    private val tokenManager: EveTokenManager,
    private val characterApi: EsiCharacterApi,
    private val publicEsi: EsiPublicDataSource,
) {
    suspend fun load(characterId: Long): CharacterWalletJournalSnapshot =
        withContext(Dispatchers.IO) {
            requireWalletScope(characterId)
            val journalDtos = fetchJournal(characterId)
            val partyNames = loadPartyNames(journalDtos)
            CharacterWalletJournalSnapshot(
                characterId = characterId,
                journal = journalDtos.mapNotNull { dto -> mapJournal(dto, partyNames) }
                    .sortedByDescending { it.dateEpochMs },
            )
        }

    private fun requireWalletScope(characterId: Long) {
        if (EveSsoScope.WALLET_CHARACTER !in tokenManager.grantedScopes(characterId)) {
            throw CharacterWalletJournalAccessException()
        }
    }

    private suspend fun fetchJournal(characterId: Long): List<EsiCharacterWalletJournalDto> {
        val byId = LinkedHashMap<Long, EsiCharacterWalletJournalDto>()
        var page = CharacterWalletJournalConfig.FIRST_PAGE
        var totalPages = CharacterWalletJournalConfig.FIRST_PAGE
        while (page <= totalPages && page <= CharacterWalletJournalConfig.JOURNAL_MAX_PAGES) {
            val response = fetchJournalPage(characterId, page)
            if (!response.isSuccessful) {
                if (page > CharacterWalletJournalConfig.FIRST_PAGE &&
                    response.code() == EsiHttpStatus.NOT_FOUND
                ) {
                    break
                }
                throw mapPagedError(response)
            }
            val chunk = response.body().orEmpty()
            chunk.forEach { entry -> byId[entry.id] = entry }
            totalPages = resolveTotalPages(
                headerPages = response.headers()[EsiPagedQuery.PAGES_HEADER]?.toIntOrNull(),
                chunkSize = chunk.size,
                page = page,
            )
            page++
        }
        return byId.values.toList()
    }

    private suspend fun fetchJournalPage(
        characterId: Long,
        page: Int,
    ): Response<List<EsiCharacterWalletJournalDto>> {
        return tokenManager.executeWithAuthRetry(characterId) { auth ->
            val response = characterApi.fetchWalletJournal(
                characterId = characterId,
                authorization = auth,
                page = page,
            )
            if (response.code() == EsiHttpStatus.UNAUTHORIZED) {
                throw HttpException(response)
            }
            response
        }
    }

    private fun resolveTotalPages(
        headerPages: Int?,
        chunkSize: Int,
        page: Int,
    ): Int = when {
        headerPages != null -> headerPages
        chunkSize < CharacterWalletJournalConfig.JOURNAL_PAGE_SIZE -> page
        else -> page + 1
    }

    private fun <T> mapPagedError(response: Response<T>): Throwable {
        if (response.code() == EsiHttpStatus.FORBIDDEN) {
            return CharacterWalletJournalAccessException()
        }
        return HttpException(response)
    }

    private suspend fun loadPartyNames(
        journal: List<EsiCharacterWalletJournalDto>,
    ): Map<Long, String> {
        val ids = buildList {
            journal.forEach { entry ->
                entry.firstPartyId?.let(::add)
                entry.secondPartyId?.let(::add)
            }
        }
        if (ids.isEmpty()) return emptyMap()
        return publicEsi.fetchUniverseNames(ids).associate { it.id to it.name }
    }

    private fun mapJournal(
        dto: EsiCharacterWalletJournalDto,
        partyNames: Map<Long, String>,
    ): CharacterWalletJournalEntry? {
        val epochMs = parseEsiDateMillis(dto.date) ?: return null
        val category = WalletJournalRef.categoryFor(dto.refType)
        val firstName = dto.firstPartyId?.let { partyNames[it] }
        val secondName = dto.secondPartyId?.let { partyNames[it] }
        val fallback = dto.description?.takeIf { it.isNotBlank() }
            ?: dto.reason.orEmpty()
        val kind = if (category == WalletJournalCategory.INDUSTRY) {
            CharacterWalletJournalDescriptionKind.INDUSTRY_PROJECT
        } else {
            CharacterWalletJournalDescriptionKind.FALLBACK
        }
        return CharacterWalletJournalEntry(
            id = dto.id,
            dateEpochMs = epochMs,
            dayKey = CharacterWalletJournalDateFormatter.dayKey(epochMs),
            amount = dto.amount,
            balance = dto.balance,
            refType = dto.refType,
            titleRes = CharacterWalletJournalFilterType.titleResFor(dto.refType),
            description = CharacterWalletJournalDescription(
                kind = kind,
                firstPartyName = firstName,
                secondPartyName = secondName,
                contextId = dto.contextId,
                fallbackText = fallback,
            ),
        )
    }
}
