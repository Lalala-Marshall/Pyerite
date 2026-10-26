package com.marshall.pyerite.mainPageModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.characterCalendarModule.navHost.CharacterCalendarRoute
import com.marshall.pyerite.characterCalendarModule.ui.MainPageCharacterCalendarItem
import com.marshall.pyerite.loyaltyPointsModule.navHost.LoyaltyPointsRoute
import com.marshall.pyerite.loyaltyPointsModule.ui.MainPageLoyaltyPointsItem
import com.marshall.pyerite.peoplePlacesModule.navHost.PeoplePlacesRoute
import com.marshall.pyerite.peoplePlacesModule.ui.MainPagePeoplePlacesItem
import com.marshall.pyerite.corporationModule.assets.navHost.CorporationAssetsRoute
import com.marshall.pyerite.corporationModule.assets.ui.MainPageCorporationAssetsItem
import com.marshall.pyerite.corporationModule.contracts.navHost.CorporationContractsRoute
import com.marshall.pyerite.corporationModule.contracts.ui.MainPageCorporationContractsItem
import com.marshall.pyerite.corporationModule.industry.navHost.CorporationIndustryRoute
import com.marshall.pyerite.corporationModule.industry.ui.MainPageCorporationIndustryItem
import com.marshall.pyerite.corporationModule.members.navHost.CorporationMembersRoute
import com.marshall.pyerite.corporationModule.members.ui.MainPageCorporationMembersItem
import com.marshall.pyerite.corporationModule.structures.navHost.CorporationStructuresRoute
import com.marshall.pyerite.corporationModule.structures.ui.MainPageCorporationStructuresItem
import com.marshall.pyerite.corporationModule.wallet.navHost.CorporationWalletRoute
import com.marshall.pyerite.corporationModule.wallet.ui.MainPageCorporationWalletItem
import com.marshall.pyerite.characterIndustryModule.navHost.CharacterIndustryRoute
import com.marshall.pyerite.characterIndustryModule.ui.MainPageCharacterIndustryItem
import com.marshall.pyerite.characterTransactionsModule.navHost.CharacterTransactionsRoute
import com.marshall.pyerite.characterTransactionsModule.ui.MainPageCharacterTransactionsItem
import com.marshall.pyerite.characterWalletJournalModule.navHost.CharacterWalletJournalRoute
import com.marshall.pyerite.characterWalletJournalModule.ui.MainPageCharacterWalletJournalItem
import com.marshall.pyerite.contractListModule.navHost.ContractListRoute
import com.marshall.pyerite.contractListModule.ui.MainPageContractListItem
import com.marshall.pyerite.personalAssetsModule.navHost.PersonalAssetsRoute
import com.marshall.pyerite.personalAssetsModule.ui.MainPagePersonalAssetsItem
import com.marshall.pyerite.personalPropertyModule.navHost.PersonalPropertyRoute
import com.marshall.pyerite.personalPropertyModule.ui.MainPagePersonalPropertyItem
import com.marshall.pyerite.characterClonesModule.navHost.CharacterClonesRoute
import com.marshall.pyerite.characterClonesModule.ui.MainPageCloneStatusItem
import com.marshall.pyerite.characterClonesModule.viewModel.CharacterClonesViewModel
import com.marshall.pyerite.characterMailModule.navHost.CharacterMailRoute
import com.marshall.pyerite.characterMailModule.ui.MainPageCharacterMailItem
import com.marshall.pyerite.characterMasteryModule.navHost.CharacterMasteryRoute
import com.marshall.pyerite.characterMasteryModule.ui.MainPageCharacterMasteryItem
import com.marshall.pyerite.characterSheetModule.navHost.CharacterSheetRoute
import com.marshall.pyerite.characterSheetModule.ui.MainPageCharacterSheetItem
import com.marshall.pyerite.characterSkillsModule.navHost.CharacterSkillsRoute
import com.marshall.pyerite.characterSkillsModule.ui.MainPageSkillQueueItem
import com.marshall.pyerite.characterSkillsModule.viewModel.CharacterSkillsViewModel
import com.marshall.pyerite.charactersListModule.navHost.CharacterRoute
import com.marshall.pyerite.charactersListModule.ui.MainPageCharacterCard
import com.marshall.pyerite.charactersListModule.viewModel.CharacterViewModel
import com.marshall.pyerite.sdeModule.update.SdeUpdateViewModel
import com.marshall.pyerite.aboutModule.navHost.AboutRoute
import com.marshall.pyerite.aboutModule.ui.MainPageAboutItem
import com.marshall.pyerite.settingsModule.navHost.SettingsRoute
import com.marshall.pyerite.settingsModule.ui.MainPageSettingsItem
import com.marshall.pyerite.agentsModule.navHost.AgentsRoute
import com.marshall.pyerite.databaseHierarchyModule.navHost.DatabaseRoute
import com.marshall.pyerite.infra.prefs.HomeLayoutPreferences
import com.marshall.pyerite.mainPageModule.model.HomePageEntry
import com.marshall.pyerite.regionMarketModule.navHost.RegionMarketRoute
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.LocalMainPageRowEdit
import com.marshall.pyerite.ui.golbalComponents.MainPageRowEdit
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarPortraitButton
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun MainPage(
    navController: NavController,
    sdeUpdateViewModel: SdeUpdateViewModel = koinViewModel(),
    characterViewModel: CharacterViewModel = koinViewModel(),
    characterClonesViewModel: CharacterClonesViewModel = koinViewModel(),
    characterSkillsViewModel: CharacterSkillsViewModel = koinViewModel(),
) {
    val currentCharacter by characterViewModel.currentCharacter.collectAsState()
    val loggedInCharacters by characterViewModel.loggedInCharacters.collectAsState()
    val isCharacterRefreshing by characterViewModel.isRefreshing.collectAsState()
    val clonesUiState by characterClonesViewModel.uiState.collectAsState()
    val skillsUiState by characterSkillsViewModel.uiState.collectAsState()
    val isUpdateCheckInFlight by sdeUpdateViewModel.isUpdateCheckInFlight.collectAsState()
    val isRefreshing =
        isCharacterRefreshing || clonesUiState.isLoading || skillsUiState.isLoading || isUpdateCheckInFlight
    val refreshFailed by characterViewModel.refreshFailed.collectAsState()
    val listState = rememberLazyListState()
    val showCollapsedTitle = rememberLazyListTitleCollapsed(listState)
    val characterSheetRowHeightPx = rememberSaveable { mutableIntStateOf(0) }
    var showCharacterPortraitSaved by rememberSaveable { mutableStateOf(false) }
    val nextCloneJumpEpochMs = clonesUiState.status.nextCloneJumpEpochMs
    val skillQueueStatus = skillsUiState.status

    val onPullToRefresh = remember(
        characterViewModel,
        characterClonesViewModel,
        characterSkillsViewModel,
        sdeUpdateViewModel,
    ) {
        {
            characterViewModel.refreshLoggedInCharacters()
            characterClonesViewModel.refresh()
            characterSkillsViewModel.refresh()
            sdeUpdateViewModel.refreshUpdateCheck()
        }
    }

    LaunchedEffect(currentCharacter?.characterId) {
        characterClonesViewModel.setCharacterId(currentCharacter?.characterId)
        characterSkillsViewModel.setCharacterId(currentCharacter?.characterId)
    }

    val currentLoggedInCharacter = remember(currentCharacter?.characterId, loggedInCharacters) {
        val characterId = currentCharacter?.characterId ?: return@remember null
        loggedInCharacters.find { it.characterId == characterId }
    }
    val characterSheetHint = currentLoggedInCharacter?.totalSp?.let { totalSp ->
        stringResource(
            R.string.character_sheet_skill_points,
            NumberDisplayFormatter.format(totalSp, NumberDisplayFormatter.Style.FULL),
        )
    }.orEmpty()
    val personalPropertyHint = currentLoggedInCharacter?.walletBalance?.let { balance ->
        stringResource(R.string.personal_property_home_wallet_hint, balance)
    }.orEmpty()

    val pageTitle = stringResource(R.string.main_page)
    val characterSectionTitle = stringResource(R.string.character)
    val corporationSectionTitle = stringResource(R.string.corporation)
    val businessSectionTitle = stringResource(R.string.business)
    val dataSectionTitle = stringResource(R.string.data)
    val otherSectionTitle = stringResource(R.string.main_section_other)
    val homeLayoutPreferences: HomeLayoutPreferences = koinInject()
    val showCorporationAffairs by homeLayoutPreferences.showCorporationAffairs.collectAsState()
    val hiddenEntryKeys by homeLayoutPreferences.hiddenEntryKeys.collectAsState()
    val savedHiddenEntries = remember(hiddenEntryKeys) {
        hiddenEntryKeys.mapNotNull(HomePageEntry::fromStorageKey).toSet()
    }
    var editingHome by remember { mutableStateOf(false) }
    var draftHiddenEntries by remember { mutableStateOf<Set<HomePageEntry>>(emptySet()) }
    val activeHiddenEntries = if (editingHome) draftHiddenEntries else savedHiddenEntries
    val characterSheetShown = isHomeEntryShown(
        entry = HomePageEntry.CHARACTER_SHEET,
        editing = editingHome,
        hidden = activeHiddenEntries,
    )
    val portraitAnchorKey = if (characterSheetShown) {
        MainPageListKey.CHARACTER_SECTION
    } else {
        MainPageListKey.CURRENT_CHARACTER
    }
    val characterSheetRowScrolledOff = remember(listState, portraitAnchorKey, characterSheetShown) {
        derivedStateOf {
            if (listState.layoutInfo.visibleItemsInfo.isEmpty()) {
                null
            } else {
                listState.isPortraitAnchorScrolledOff(
                    anchorKey = portraitAnchorKey,
                    rowHeightPx = characterSheetRowHeightPx.intValue,
                    useRowHeight = characterSheetShown,
                )
            }
        }
    }.value
    SideEffect {
        if (characterSheetRowScrolledOff != null) {
            showCharacterPortraitSaved = characterSheetRowScrolledOff
        }
    }
    val showCharacterPortrait = characterSheetRowScrolledOff ?: showCharacterPortraitSaved
    val enterHomeEdit = {
        draftHiddenEntries = savedHiddenEntries
        editingHome = true
    }
    val exitHomeEdit = {
        homeLayoutPreferences.setHiddenEntryKeys(draftHiddenEntries.map { it.name }.toSet())
        editingHome = false
    }
    val toggleHomeEntry = { entry: HomePageEntry ->
        draftHiddenEntries = if (entry in draftHiddenEntries) {
            draftHiddenEntries - entry
        } else {
            draftHiddenEntries + entry
        }
    }
    val showCharacterSection = HomePageEntry.characterSection.any {
        isHomeEntryShown(it, editingHome, activeHiddenEntries)
    }
    val showCorporationSection = showCorporationAffairs && HomePageEntry.corporationSection.any {
        isHomeEntryShown(it, editingHome, activeHiddenEntries)
    }
    val showDataSection = HomePageEntry.dataSection.any {
        isHomeEntryShown(it, editingHome, activeHiddenEntries)
    }
    val showBusinessSection = HomePageEntry.businessSection.any {
        isHomeEntryShown(it, editingHome, activeHiddenEntries)
    }
    val dividerAfter = { entry: HomePageEntry, section: List<HomePageEntry> ->
        homeEntryHasDividerAfter(
            entry = entry,
            section = section,
            editing = editingHome,
            hidden = activeHiddenEntries,
        )
    }
    val refreshTopBarAction = pyeritePullRefreshTopBarAction(
        isRefreshing = isRefreshing,
        refreshFailed = refreshFailed,
        onRefresh = onPullToRefresh,
    )
    val exitEditLabel = stringResource(R.string.main_page_exit_edit)
    val endActions = if (editingHome) {
        listOf(
            PyeriteTopBarActionItem(
                onClick = exitHomeEdit,
                icon = Icons.Filled.Close,
                contentDescription = exitEditLabel,
                label = exitEditLabel,
                showIcon = false,
            ),
        )
    } else {
        buildList {
            refreshTopBarAction?.let(::add)
            if (currentCharacter != null) {
                add(
                    PyeriteTopBarActionItem(
                        onClick = characterViewModel::clearCurrentCharacter,
                        icon = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(R.string.character_exit_current),
                        iconTint = colorResource(R.color.character_delete),
                    ),
                )
            }
        }
    }

    val selectedCharacter = currentCharacter
    val characterPortraitLeading: (@Composable () -> Unit)? = if (selectedCharacter == null) {
        null
    } else {
        {
            PyeriteTopBarPortraitButton(
                portraitUrl = selectedCharacter.portraitUrl,
                contentDescription = stringResource(R.string.character),
                visible = showCharacterPortrait,
                onClick = if (editingHome) {
                    null
                } else {
                    { navController.navigate(CharacterRoute.Root.route) }
                },
            )
        }
    }

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        leadingContent = characterPortraitLeading,
        endActions = endActions,
    ) { topBarPadding ->
        PyeritePullToRefreshBox(
            onRefresh = onPullToRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(topBarPadding),
        ) {
            val mainPageBottomPadding = dimensionResource(R.dimen.main_page_bottom_padding)
            val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = mainPageBottomPadding + navigationBarBottom,
                ),
            ) {
                item(key = MainPageListKey.PAGE_TITLE) {
                    PageTitle(text = pageTitle)
                }
                item(key = MainPageListKey.CURRENT_CHARACTER) {
                    MainPageCharacterCard(
                        currentCharacter = currentCharacter,
                        showChevron = !editingHome,
                        onClick = if (editingHome) {
                            null
                        } else {
                            { navController.navigate(CharacterRoute.Root.route) }
                        },
                    )
                }
                if (showCharacterSection) {
                    item(key = MainPageListKey.CHARACTER_SECTION_HEADER) {
                        MainPageSectionHeader(title = characterSectionTitle)
                    }
                    item(key = MainPageListKey.CHARACTER_SECTION) {
                        MainPageSectionCard(
                            bottomSpacing = dimensionResource(R.dimen.character_main_card_bottom_spacing),
                        ) {
                            HomeEditableEntry(
                                entry = HomePageEntry.CHARACTER_SHEET,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCharacterSheetItem(
                                    skillPointsHint = characterSheetHint,
                                    modifier = Modifier.onSizeChanged { size ->
                                        characterSheetRowHeightPx.intValue = size.height
                                    },
                                    showDivider = dividerAfter(
                                        HomePageEntry.CHARACTER_SHEET,
                                        HomePageEntry.characterSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCharacterSheetItem
                                        navController.navigate(CharacterSheetRoute.Sheet.create(characterId))
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CLONE_STATUS,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCloneStatusItem(
                                    nextCloneJumpEpochMs = nextCloneJumpEpochMs,
                                    showDivider = dividerAfter(
                                        HomePageEntry.CLONE_STATUS,
                                        HomePageEntry.characterSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCloneStatusItem
                                        navController.navigate(
                                            CharacterClonesRoute.Status.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.SKILL_QUEUE,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageSkillQueueItem(
                                    status = skillQueueStatus,
                                    detailsReady = skillsUiState.detailsReady,
                                    showDivider = dividerAfter(
                                        HomePageEntry.SKILL_QUEUE,
                                        HomePageEntry.characterSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageSkillQueueItem
                                        navController.navigate(
                                            CharacterSkillsRoute.Queue.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.MAIL,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCharacterMailItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.MAIL,
                                        HomePageEntry.characterSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCharacterMailItem
                                        navController.navigate(CharacterMailRoute.Root.create(characterId))
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CALENDAR,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCharacterCalendarItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.CALENDAR,
                                        HomePageEntry.characterSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCharacterCalendarItem
                                        navController.navigate(
                                            CharacterCalendarRoute.Root.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.PERSONAL_PROPERTY,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPagePersonalPropertyItem(
                                    walletBalanceHint = personalPropertyHint,
                                    showDivider = dividerAfter(
                                        HomePageEntry.PERSONAL_PROPERTY,
                                        HomePageEntry.characterSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPagePersonalPropertyItem
                                        navController.navigate(
                                            PersonalPropertyRoute.Root.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.LOYALTY_POINTS,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageLoyaltyPointsItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.LOYALTY_POINTS,
                                        HomePageEntry.characterSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageLoyaltyPointsItem
                                        navController.navigate(
                                            LoyaltyPointsRoute.Hub.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.PEOPLE_PLACES,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPagePeoplePlacesItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.PEOPLE_PLACES,
                                        HomePageEntry.characterSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPagePeoplePlacesItem
                                        navController.navigate(
                                            PeoplePlacesRoute.Search.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CHARACTER_MASTERY,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCharacterMasteryItem(
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCharacterMasteryItem
                                        navController.navigate(
                                            CharacterMasteryRoute.Overview.create(characterId),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                if (showCorporationSection) {
                    item(key = "corporation_section_header") {
                        MainPageSectionHeader(title = corporationSectionTitle)
                    }
                    item(key = "corporation_section_entries") {
                        MainPageSectionCard(
                            bottomSpacing = dimensionResource(R.dimen.character_main_card_bottom_spacing),
                        ) {
                            HomeEditableEntry(
                                entry = HomePageEntry.CORPORATION_WALLET,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCorporationWalletItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.CORPORATION_WALLET,
                                        HomePageEntry.corporationSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCorporationWalletItem
                                        navController.navigate(
                                            CorporationWalletRoute.Wallets.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CORPORATION_MEMBERS,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCorporationMembersItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.CORPORATION_MEMBERS,
                                        HomePageEntry.corporationSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCorporationMembersItem
                                        navController.navigate(
                                            CorporationMembersRoute.Members.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CORPORATION_STRUCTURES,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCorporationStructuresItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.CORPORATION_STRUCTURES,
                                        HomePageEntry.corporationSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCorporationStructuresItem
                                        navController.navigate(
                                            CorporationStructuresRoute.Structures.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CORPORATION_INDUSTRY,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCorporationIndustryItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.CORPORATION_INDUSTRY,
                                        HomePageEntry.corporationSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCorporationIndustryItem
                                        navController.navigate(
                                            CorporationIndustryRoute.Industry.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CORPORATION_ASSETS,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCorporationAssetsItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.CORPORATION_ASSETS,
                                        HomePageEntry.corporationSection,
                                    ),
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCorporationAssetsItem
                                        navController.navigate(
                                            CorporationAssetsRoute.Regions.create(characterId),
                                        )
                                    },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CORPORATION_CONTRACTS,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCorporationContractsItem(
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCorporationContractsItem
                                        navController.navigate(
                                            CorporationContractsRoute.Contracts.create(characterId),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                if (showDataSection) {
                    item(key = "data_section_header") {
                        MainPageSectionHeader(title = dataSectionTitle)
                    }
                    item(key = "data_section_entries") {
                        MainPageSectionCard {
                            HomeEditableEntry(
                                entry = HomePageEntry.DATABASE,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageDatabaseItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.DATABASE,
                                        HomePageEntry.dataSection,
                                    ),
                                    onClick = { navController.navigate(DatabaseRoute.Root.route) },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.AGENTS,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageAgentsItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.AGENTS,
                                        HomePageEntry.dataSection,
                                    ),
                                    onClick = { navController.navigate(AgentsRoute.Graph.route) },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.REGION_MARKET,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageRegionMarketItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.REGION_MARKET,
                                        HomePageEntry.dataSection,
                                    ),
                                    onClick = { navController.navigate(RegionMarketRoute.Root.route) },
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.MARKET_WATCHLIST,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageMarketWatchlistItem(
                                    showDivider = dividerAfter(
                                        HomePageEntry.MARKET_WATCHLIST,
                                        HomePageEntry.dataSection,
                                    ),
                                    onClick = {
                                        navController.navigate(RegionMarketRoute.Watchlists.route)
                                    },
                                )
                            }
                        }
                    }
                }
                if (showBusinessSection) {
                    item(key = "business_section_header") {
                        MainPageSectionHeader(title = businessSectionTitle)
                    }
                    item(key = "business_section_entries") {
                        MainPageSectionCard(
                            bottomSpacing = dimensionResource(R.dimen.character_main_card_bottom_spacing),
                        ) {
                            HomeEditableEntry(
                                entry = HomePageEntry.PERSONAL_ASSETS,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPagePersonalAssetsItem(
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPagePersonalAssetsItem
                                        navController.navigate(
                                            PersonalAssetsRoute.Regions.create(characterId),
                                        )
                                    },
                                    showDivider = dividerAfter(
                                        HomePageEntry.PERSONAL_ASSETS,
                                        HomePageEntry.businessSection,
                                    ),
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CONTRACT_LIST,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageContractListItem(
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageContractListItem
                                        navController.navigate(
                                            ContractListRoute.List.create(characterId),
                                        )
                                    },
                                    showDivider = dividerAfter(
                                        HomePageEntry.CONTRACT_LIST,
                                        HomePageEntry.businessSection,
                                    ),
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CHARACTER_TRANSACTIONS,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCharacterTransactionsItem(
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCharacterTransactionsItem
                                        navController.navigate(
                                            CharacterTransactionsRoute.List.create(characterId),
                                        )
                                    },
                                    showDivider = dividerAfter(
                                        HomePageEntry.CHARACTER_TRANSACTIONS,
                                        HomePageEntry.businessSection,
                                    ),
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.WALLET_JOURNAL,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCharacterWalletJournalItem(
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCharacterWalletJournalItem
                                        navController.navigate(
                                            CharacterWalletJournalRoute.List.create(characterId),
                                        )
                                    },
                                    showDivider = dividerAfter(
                                        HomePageEntry.WALLET_JOURNAL,
                                        HomePageEntry.businessSection,
                                    ),
                                )
                            }
                            HomeEditableEntry(
                                entry = HomePageEntry.CHARACTER_INDUSTRY,
                                editing = editingHome,
                                hidden = activeHiddenEntries,
                                onToggle = toggleHomeEntry,
                            ) {
                                MainPageCharacterIndustryItem(
                                    onClick = {
                                        val characterId = currentCharacter?.characterId
                                            ?: return@MainPageCharacterIndustryItem
                                        navController.navigate(
                                            CharacterIndustryRoute.Industry.create(characterId),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                item(key = "other_section_header") {
                    MainPageSectionHeader(title = otherSectionTitle)
                }
                item(key = "other_section_entries") {
                    MainPageSectionCard(
                        bottomSpacing = dimensionResource(R.dimen.character_main_card_bottom_spacing),
                    ) {
                        MainPageSettingsItem(
                            onClick = if (editingHome) {
                                null
                            } else {
                                { navController.navigate(SettingsRoute.Settings.route) }
                            },
                            showDivider = true,
                            showChevron = !editingHome,
                        )
                        MainPageAboutItem(
                            onClick = if (editingHome) {
                                null
                            } else {
                                { navController.navigate(AboutRoute.About.route) }
                            },
                            showChevron = !editingHome,
                        )
                    }
                }
                if (!editingHome) {
                    item(key = "home_edit_prompt") {
                        MainPageEditPrompt(onClick = enterHomeEdit)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        sdeUpdateViewModel.repairStaleCheckingState()
    }
}

@Composable
private fun MainPageSectionHeader(title: String) {
    val sectionHeaderTextSize = dimensionResource(R.dimen.list_section_header_text_size).value.sp
    val titleStartPadding = dimensionResource(R.dimen.type_detail_page_title_start_padding)
    val sectionHeaderBottomPadding = dimensionResource(R.dimen.list_section_header_bottom_padding)

    Text(
        text = title,
        fontSize = sectionHeaderTextSize,
        fontWeight = FontWeight.Black,
        color = colorResource(R.color.text_primary),
        modifier = Modifier.padding(
            start = titleStartPadding,
            bottom = sectionHeaderBottomPadding,
        ),
    )
}

@Composable
private fun MainPageDatabaseItem(
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_database,
            itemName = stringResource(R.string.database),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}

@Composable
private fun MainPageAgentsItem(
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_agents,
            iconTint = Color.Unspecified,
            itemName = stringResource(R.string.agents),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}

@Composable
private fun MainPageRegionMarketItem(
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_region_market,
            iconTint = Color.Unspecified,
            itemName = stringResource(R.string.region_market),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}

@Composable
private fun MainPageMarketWatchlistItem(
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_market_watchlist,
            iconTint = Color.Unspecified,
            itemName = stringResource(R.string.market_watchlist),
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}

@Composable
private fun MainPageSectionCard(
    bottomSpacing: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val cardCornerRadius = dimensionResource(R.dimen.detail_card_corner_radius)
    val shape = RoundedCornerShape(cardCornerRadius)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding))
            .padding(bottom = bottomSpacing)
            .clip(shape)
            .background(colorResource(R.color.second_background), shape),
    ) {
        content()
    }
}

private fun LazyListState.isPortraitAnchorScrolledOff(
    anchorKey: String,
    rowHeightPx: Int,
    useRowHeight: Boolean,
): Boolean {
    val anchor = layoutInfo.visibleItemsInfo.find { it.key == anchorKey }
    if (anchor != null) {
        val height = if (useRowHeight) rowHeightPx else anchor.size
        if (height <= 0) return false
        return anchor.offset + height <= 0
    }
    return layoutInfo.visibleItemsInfo.none { it.key == MainPageListKey.PAGE_TITLE }
}

private object MainPageListKey {
    const val PAGE_TITLE = "page_title"
    const val CURRENT_CHARACTER = "current_character"
    const val CHARACTER_SECTION_HEADER = "character_section_header"
    const val CHARACTER_SECTION = "character_section_entries"
}

private fun isHomeEntryShown(
    entry: HomePageEntry,
    editing: Boolean,
    hidden: Set<HomePageEntry>,
): Boolean = editing || entry !in hidden

private fun homeEntryHasDividerAfter(
    entry: HomePageEntry,
    section: List<HomePageEntry>,
    editing: Boolean,
    hidden: Set<HomePageEntry>,
): Boolean {
    val visible = section.filter { isHomeEntryShown(it, editing, hidden) }
    val index = visible.indexOf(entry)
    return index >= 0 && index < visible.lastIndex
}

@Composable
private fun HomeEditableEntry(
    entry: HomePageEntry,
    editing: Boolean,
    hidden: Set<HomePageEntry>,
    onToggle: (HomePageEntry) -> Unit,
    content: @Composable () -> Unit,
) {
    if (!isHomeEntryShown(entry, editing, hidden)) return
    if (!editing) {
        content()
        return
    }
    val entryHidden = entry in hidden
    CompositionLocalProvider(
        LocalMainPageRowEdit provides MainPageRowEdit(
            hidden = entryHidden,
            onToggle = { onToggle(entry) },
            contentDescription = stringResource(
                if (entryHidden) {
                    R.string.main_page_entry_hidden
                } else {
                    R.string.main_page_entry_visible
                },
            ),
        ),
    ) {
        content()
    }
}

@Composable
private fun MainPageEditPrompt(onClick: () -> Unit) {
    val verticalPadding = dimensionResource(R.dimen.list_section_header_bottom_padding)
    val horizontalPadding = dimensionResource(R.dimen.detail_row_horizontal_padding)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            TextButton(
                onClick = onClick,
                contentPadding = PaddingValues(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding,
                ),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(R.color.hyperlink_text),
                ),
            ) {
                Text(
                    text = stringResource(R.string.main_page_too_many_features),
                    fontSize = dimensionResource(R.dimen.list_section_subheader_text_size).value.sp,
                )
            }
        }
    }
}
