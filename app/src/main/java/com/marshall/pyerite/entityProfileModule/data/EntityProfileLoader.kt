package com.marshall.pyerite.entityProfileModule.data

import com.marshall.pyerite.entityProfileModule.model.EntityEmploymentEntry
import com.marshall.pyerite.entityProfileModule.model.EntityProfile
import com.marshall.pyerite.entityProfileModule.model.EntityProfileConfig
import com.marshall.pyerite.entityProfileModule.model.EntityStandingParty
import com.marshall.pyerite.entityProfileModule.model.EntityStandingRow
import com.marshall.pyerite.entityProfileModule.model.EntityStandingSection
import com.marshall.pyerite.entityProfileModule.model.EntityStandingSectionKind
import com.marshall.pyerite.esiModule.api.EsiAllianceApi
import com.marshall.pyerite.esiModule.api.EsiCharacterApi
import com.marshall.pyerite.esiModule.api.EsiCorporationApi
import com.marshall.pyerite.esiModule.data.EsiPublicDataSource
import com.marshall.pyerite.esiModule.data.allianceLogoUrl
import com.marshall.pyerite.esiModule.data.corporationLogoUrl
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.esiModule.http.EsiConfig
import com.marshall.pyerite.esiModule.model.EsiCharacterPublic
import com.marshall.pyerite.esiModule.model.EsiContactDto
import com.marshall.pyerite.esiModule.model.EsiOrganization
import com.marshall.pyerite.esiModule.model.parseEsiDateMillis
import com.marshall.pyerite.eveAuthModule.token.EveTokenManager
import com.marshall.pyerite.ui.golbalComponents.UniverseEntityKind
import com.marshall.pyerite.ui.golbalComponents.UniverseEntityRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException

internal class EntityProfileLoader(
    private val publicEsi: EsiPublicDataSource,
    private val tokenManager: EveTokenManager,
    private val characterApi: EsiCharacterApi,
    private val corporationApi: EsiCorporationApi,
    private val allianceApi: EsiAllianceApi,
) {
    suspend fun loadHeader(ref: UniverseEntityRef): EntityProfile = withContext(Dispatchers.IO) {
        when (ref.kind) {
            UniverseEntityKind.CHARACTER -> loadCharacterHeader(ref.id)
            UniverseEntityKind.CORPORATION -> loadCorporationHeader(ref.id)
            UniverseEntityKind.ALLIANCE -> loadAllianceHeader(ref.id)
        }
    }

    suspend fun loadDetails(
        header: EntityProfile,
        viewerCharacterId: Long,
    ): EntityProfile = withContext(Dispatchers.IO) {
        coroutineScope {
            val standingsDeferred = async {
                runCatching { loadStandings(header, viewerCharacterId) }.getOrDefault(emptyList())
            }
            val employmentDeferred = async {
                if (header.ref.kind == UniverseEntityKind.CHARACTER) {
                    runCatching { loadEmployment(header.ref.id) }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }
            header.copy(
                standingSections = standingsDeferred.await(),
                employment = employmentDeferred.await(),
                detailsReady = true,
            )
        }
    }

    private suspend fun loadCharacterHeader(characterId: Long): EntityProfile {
        val public = publicEsi.fetchCharacter(characterId)
        val corporation = public.corporationId?.let { id ->
            runCatching { publicEsi.fetchCorporation(id) }.getOrNull()
        }
        val allianceId = public.allianceId ?: corporation?.allianceId
        val alliance = allianceId?.let { id ->
            runCatching { publicEsi.fetchAlliance(id) }.getOrNull()
        }
        val faction = resolveFaction(public.factionId)
        val isCeo = corporation?.ceoId == characterId
        return EntityProfile(
            ref = UniverseEntityRef(UniverseEntityKind.CHARACTER, characterId),
            name = public.name,
            iconUrl = portraitUrl(characterId),
            corporationId = public.corporationId,
            corporationName = corporation?.name,
            corporationIconUrl = public.corporationId?.let { corporationLogoUrl(it) },
            isNpcCorporation = public.corporationId?.let(EntityProfileConfig::isNpcCorporation) == true,
            allianceId = allianceId,
            allianceName = alliance?.name,
            allianceIconUrl = allianceId?.let { allianceLogoUrl(it) },
            isCeo = isCeo,
            descriptionHtml = public.description,
            factionId = faction?.id,
            factionName = faction?.name,
            factionIconUrl = faction?.iconUrl,
        )
    }

    private suspend fun loadCorporationHeader(corporationId: Long): EntityProfile {
        val corporation = publicEsi.fetchCorporation(corporationId)
        val alliance = corporation.allianceId?.let { id ->
            runCatching { publicEsi.fetchAlliance(id) }.getOrNull()
        }
        val faction = resolveFaction(corporation.factionId?.takeUnless {
            EntityProfileConfig.isNpcCorporation(corporationId)
        })
        return EntityProfile(
            ref = UniverseEntityRef(UniverseEntityKind.CORPORATION, corporationId),
            name = corporation.name,
            iconUrl = corporationLogoUrl(corporationId),
            corporationId = corporationId,
            corporationName = corporation.name,
            corporationIconUrl = corporationLogoUrl(corporationId),
            isNpcCorporation = EntityProfileConfig.isNpcCorporation(corporationId),
            allianceId = corporation.allianceId,
            allianceName = alliance?.name,
            allianceIconUrl = corporation.allianceId?.let { allianceLogoUrl(it) },
            descriptionHtml = corporation.description,
            factionId = faction?.id,
            factionName = faction?.name,
            factionIconUrl = faction?.iconUrl,
        )
    }

    private suspend fun loadAllianceHeader(allianceId: Long): EntityProfile {
        val alliance = publicEsi.fetchAlliance(allianceId)
        val executorId = alliance.executorCorporationId
        val executor = executorId?.let { id ->
            runCatching { publicEsi.fetchCorporation(id) }.getOrNull()
        }
        val faction = resolveFaction(alliance.factionId)
        return EntityProfile(
            ref = UniverseEntityRef(UniverseEntityKind.ALLIANCE, allianceId),
            name = alliance.name,
            iconUrl = allianceLogoUrl(allianceId),
            corporationId = executorId,
            corporationName = executor?.name,
            corporationIconUrl = executorId?.let { corporationLogoUrl(it) },
            isNpcCorporation = executorId?.let(EntityProfileConfig::isNpcCorporation) == true,
            allianceId = allianceId,
            allianceName = alliance.name,
            allianceIconUrl = allianceLogoUrl(allianceId),
            descriptionHtml = alliance.description,
            factionId = faction?.id,
            factionName = faction?.name,
            factionIconUrl = faction?.iconUrl,
        )
    }

    private suspend fun resolveFaction(factionId: Long?): ResolvedFaction? {
        if (factionId == null) return null
        val name = publicEsi.fetchUniverseName(factionId) ?: return null
        return ResolvedFaction(
            id = factionId,
            name = name,
            iconUrl = corporationLogoUrl(factionId),
        )
    }

    private suspend fun loadStandings(
        header: EntityProfile,
        viewerCharacterId: Long,
    ): List<EntityStandingSection> {
        val viewer = runCatching { publicEsi.fetchCharacter(viewerCharacterId) }.getOrNull()
            ?: return emptyList()
        val viewerCorpId = viewer.corporationId
        val viewerCorp = viewerCorpId?.let { id ->
            runCatching { publicEsi.fetchCorporation(id) }.getOrNull()
        }
        val viewerAllianceId = viewer.allianceId ?: viewerCorp?.allianceId
        val viewerAlliance = viewerAllianceId?.let { id ->
            runCatching { publicEsi.fetchAlliance(id) }.getOrNull()
        }

        val characterContacts = fetchContactsOrEmpty(viewerCharacterId) { auth ->
            characterApi.fetchContacts(viewerCharacterId, auth)
        }
        val corporationContacts = viewerCorpId?.let { corpId ->
            fetchContactsOrEmpty(viewerCharacterId) { auth ->
                corporationApi.fetchContacts(corpId, auth)
            }
        }.orEmpty()
        val allianceContacts = viewerAllianceId?.let { allianceId ->
            fetchContactsOrEmpty(viewerCharacterId) { auth ->
                allianceApi.fetchContacts(allianceId, auth)
            }
        }.orEmpty()

        val fromParties = buildViewerParties(
            viewer = viewer,
            viewerCorpId = viewerCorpId,
            viewerCorp = viewerCorp,
            viewerAllianceId = viewerAllianceId,
            viewerAlliance = viewerAlliance,
        )

        val sections = mutableListOf<EntityStandingSection>()
        when (header.ref.kind) {
            UniverseEntityKind.CHARACTER -> {
                sections += standingSection(
                    kind = EntityStandingSectionKind.PERSONAL,
                    target = header.toParty(),
                    fromParties = fromParties,
                    characterContacts = characterContacts,
                    corporationContacts = corporationContacts,
                    allianceContacts = allianceContacts,
                )
                header.corporationId?.let { corpId ->
                    sections += standingSection(
                        kind = EntityStandingSectionKind.CORPORATION,
                        target = EntityStandingParty(
                            ref = UniverseEntityRef(UniverseEntityKind.CORPORATION, corpId),
                            name = header.corporationName.orEmpty(),
                            iconUrl = header.corporationIconUrl,
                        ),
                        fromParties = fromParties,
                        characterContacts = characterContacts,
                        corporationContacts = corporationContacts,
                        allianceContacts = allianceContacts,
                    )
                }
                header.allianceId?.let { allianceId ->
                    sections += standingSection(
                        kind = EntityStandingSectionKind.ALLIANCE,
                        target = EntityStandingParty(
                            ref = UniverseEntityRef(UniverseEntityKind.ALLIANCE, allianceId),
                            name = header.allianceName.orEmpty(),
                            iconUrl = header.allianceIconUrl,
                        ),
                        fromParties = fromParties,
                        characterContacts = characterContacts,
                        corporationContacts = corporationContacts,
                        allianceContacts = allianceContacts,
                    )
                }
            }
            UniverseEntityKind.CORPORATION -> {
                sections += standingSection(
                    kind = EntityStandingSectionKind.CORPORATION,
                    target = header.toParty(),
                    fromParties = fromParties,
                    characterContacts = characterContacts,
                    corporationContacts = corporationContacts,
                    allianceContacts = allianceContacts,
                )
                header.allianceId?.let { allianceId ->
                    sections += standingSection(
                        kind = EntityStandingSectionKind.ALLIANCE,
                        target = EntityStandingParty(
                            ref = UniverseEntityRef(UniverseEntityKind.ALLIANCE, allianceId),
                            name = header.allianceName.orEmpty(),
                            iconUrl = header.allianceIconUrl,
                        ),
                        fromParties = fromParties,
                        characterContacts = characterContacts,
                        corporationContacts = corporationContacts,
                        allianceContacts = allianceContacts,
                    )
                }
            }
            UniverseEntityKind.ALLIANCE -> {
                sections += standingSection(
                    kind = EntityStandingSectionKind.ALLIANCE,
                    target = header.toParty(),
                    fromParties = fromParties,
                    characterContacts = characterContacts,
                    corporationContacts = corporationContacts,
                    allianceContacts = allianceContacts,
                )
            }
        }
        return sections
    }

    private fun buildViewerParties(
        viewer: EsiCharacterPublic,
        viewerCorpId: Long?,
        viewerCorp: EsiOrganization?,
        viewerAllianceId: Long?,
        viewerAlliance: EsiOrganization?,
    ): List<EntityStandingParty> = buildList {
        add(
            EntityStandingParty(
                ref = UniverseEntityRef(UniverseEntityKind.CHARACTER, viewer.characterId),
                name = viewer.name,
                iconUrl = portraitUrl(viewer.characterId),
            ),
        )
        if (viewerCorpId != null) {
            add(
                EntityStandingParty(
                    ref = UniverseEntityRef(UniverseEntityKind.CORPORATION, viewerCorpId),
                    name = viewerCorp?.name.orEmpty(),
                    iconUrl = corporationLogoUrl(viewerCorpId),
                ),
            )
        }
        if (viewerAllianceId != null) {
            add(
                EntityStandingParty(
                    ref = UniverseEntityRef(UniverseEntityKind.ALLIANCE, viewerAllianceId),
                    name = viewerAlliance?.name.orEmpty(),
                    iconUrl = allianceLogoUrl(viewerAllianceId),
                ),
            )
        }
    }

    private fun standingSection(
        kind: EntityStandingSectionKind,
        target: EntityStandingParty,
        fromParties: List<EntityStandingParty>,
        characterContacts: List<EsiContactDto>,
        corporationContacts: List<EsiContactDto>,
        allianceContacts: List<EsiContactDto>,
    ): EntityStandingSection {
        val targetId = target.ref.id
        val rows = fromParties.map { from ->
            val contacts = when (from.ref.kind) {
                UniverseEntityKind.CHARACTER -> characterContacts
                UniverseEntityKind.CORPORATION -> corporationContacts
                UniverseEntityKind.ALLIANCE -> allianceContacts
            }
            EntityStandingRow(
                from = from,
                standing = contacts.standingToward(targetId),
                toward = target,
            )
        }
        return EntityStandingSection(kind = kind, target = target, rows = rows)
    }

    private suspend fun loadEmployment(characterId: Long): List<EntityEmploymentEntry> {
        val history = publicEsi.fetchCorporationHistory(characterId)
        val sorted = history.sortedBy { parseEsiDateMillis(it.startDate) ?: 0L }
        return sorted.mapIndexedNotNull { index, entry ->
            val startEpochMs = parseEsiDateMillis(entry.startDate) ?: return@mapIndexedNotNull null
            val endEpochMs = sorted.getOrNull(index + 1)?.let { next ->
                parseEsiDateMillis(next.startDate)
            }
            val corporation = runCatching {
                publicEsi.fetchCorporation(entry.corporationId)
            }.getOrNull()
            val allianceId = corporation?.allianceId
            val alliance = allianceId?.let { id ->
                runCatching { publicEsi.fetchAlliance(id) }.getOrNull()
            }
            EntityEmploymentEntry(
                corporationId = entry.corporationId,
                corporationName = corporation?.name.orEmpty(),
                corporationIconUrl = corporationLogoUrl(entry.corporationId),
                isNpc = EntityProfileConfig.isNpcCorporation(entry.corporationId),
                allianceId = allianceId,
                allianceName = alliance?.name,
                allianceIconUrl = allianceId?.let { allianceLogoUrl(it) },
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
            )
        }.sortedByDescending { it.startEpochMs }
    }

    private suspend fun fetchContactsOrEmpty(
        viewerCharacterId: Long,
        block: suspend (authorization: String) -> List<EsiContactDto>,
    ): List<EsiContactDto> {
        return runCatching {
            tokenManager.executeWithAuthRetry(viewerCharacterId, block)
        }.getOrElse { error ->
            if (error is HttpException && error.code() == EsiConfig.HttpStatus.FORBIDDEN) {
                emptyList()
            } else {
                emptyList()
            }
        }
    }

    private fun EntityProfile.toParty(): EntityStandingParty = EntityStandingParty(
        ref = ref,
        name = name,
        iconUrl = iconUrl,
    )

    private fun List<EsiContactDto>.standingToward(id: Long): Double =
        firstOrNull { it.contactId == id }?.standing ?: EntityProfileConfig.STANDING_NEUTRAL

    private data class ResolvedFaction(
        val id: Long,
        val name: String,
        val iconUrl: String,
    )
}
