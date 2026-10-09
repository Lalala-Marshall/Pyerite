package com.marshall.pyerite.mainPageModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.marshall.pyerite.sdeModule.update.SdeUpdateBottomSheet
import com.marshall.pyerite.sdeModule.update.SdeUpdateUiState
import com.marshall.pyerite.sdeModule.update.SdeUpdateViewModel
import com.marshall.pyerite.databaseHierarchyModule.navHost.DatabaseRoute
import com.marshall.pyerite.regionMarketModule.navHost.RegionMarketRoute
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainPage(
    navController: NavController,
    sdeUpdateViewModel: SdeUpdateViewModel = koinViewModel(),
    characterViewModel: CharacterViewModel = koinViewModel(),
    characterClonesViewModel: CharacterClonesViewModel = koinViewModel(),
    characterSkillsViewModel: CharacterSkillsViewModel = koinViewModel(),
) {
    val uiState by sdeUpdateViewModel.uiState.collectAsState()
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

    val updateActionLabel = when (uiState) {
        is SdeUpdateUiState.CheckFailed -> stringResource(R.string.sde_update_check_manually)
        else -> stringResource(R.string.sde_update_available)
    }
    val showUpdateIcon = uiState is SdeUpdateUiState.UpdateReady ||
        uiState is SdeUpdateUiState.CheckFailed
    val pageTitle = stringResource(R.string.main_page)
    val characterSectionTitle = stringResource(R.string.character)
    val corporationSectionTitle = stringResource(R.string.corporation)
    val dataSectionTitle = stringResource(R.string.data)
    val showCorporationSection =
        currentLoggedInCharacter?.hasCorporationManagementAccess == true
    val refreshTopBarAction = pyeritePullRefreshTopBarAction(
        isRefreshing = isRefreshing,
        refreshFailed = refreshFailed,
        onRefresh = onPullToRefresh,
    )
    val endActions = buildList {
        refreshTopBarAction?.let(::add)
        if (showUpdateIcon) {
            add(
                PyeriteTopBarActionItem(
                    onClick = { sdeUpdateViewModel.openUpdateSheet() },
                    icon = Icons.Default.ArrowDownward,
                    contentDescription = stringResource(R.string.sde_update_download),
                    label = updateActionLabel,
                    accentColor = colorResource(R.color.sde_update_accent),
                    iconBadge = true,
                ),
            )
        }
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

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        endActions = endActions,
    ) { topBarPadding ->
        PyeritePullToRefreshBox(
            onRefresh = onPullToRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(topBarPadding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "page_title") {
                    PageTitle(text = pageTitle)
                }
                item(key = "current_character") {
                    MainPageCharacterCard(
                        currentCharacter = currentCharacter,
                        onClick = { navController.navigate(CharacterRoute.Root.route) },
                    )
                }
                item(key = "character_section_header") {
                    MainPageSectionHeader(title = characterSectionTitle)
                }
                item(key = "character_section_entries") {
                    MainPageSectionCard(
                        bottomSpacing = dimensionResource(R.dimen.character_main_card_bottom_spacing),
                    ) {
                        MainPageCharacterSheetItem(
                            skillPointsHint = characterSheetHint,
                            showDivider = true,
                            onClick = {
                                val characterId = currentCharacter?.characterId
                                    ?: return@MainPageCharacterSheetItem
                                navController.navigate(CharacterSheetRoute.Sheet.create(characterId))
                            },
                        )
                        MainPageCloneStatusItem(
                            nextCloneJumpEpochMs = nextCloneJumpEpochMs,
                            showDivider = true,
                            onClick = {
                                val characterId = currentCharacter?.characterId
                                    ?: return@MainPageCloneStatusItem
                                navController.navigate(
                                    CharacterClonesRoute.Status.create(characterId),
                                )
                            },
                        )
                        MainPageSkillQueueItem(
                            status = skillQueueStatus,
                            detailsReady = skillsUiState.detailsReady,
                            showDivider = true,
                            onClick = {
                                val characterId = currentCharacter?.characterId
                                    ?: return@MainPageSkillQueueItem
                                navController.navigate(
                                    CharacterSkillsRoute.Queue.create(characterId),
                                )
                            },
                        )
                        MainPageCharacterMailItem(
                            showDivider = true,
                            onClick = {
                                val characterId = currentCharacter?.characterId
                                    ?: return@MainPageCharacterMailItem
                                navController.navigate(CharacterMailRoute.Root.create(characterId))
                            },
                        )
                        MainPageCharacterCalendarItem(
                            showDivider = true,
                            onClick = {
                                val characterId = currentCharacter?.characterId
                                    ?: return@MainPageCharacterCalendarItem
                                navController.navigate(
                                    CharacterCalendarRoute.Root.create(characterId),
                                )
                            },
                        )
                        MainPagePersonalPropertyItem(
                            walletBalanceHint = personalPropertyHint,
                            showDivider = true,
                            onClick = {
                                val characterId = currentCharacter?.characterId
                                    ?: return@MainPagePersonalPropertyItem
                                navController.navigate(
                                    PersonalPropertyRoute.Root.create(characterId),
                                )
                            },
                        )
                        MainPageLoyaltyPointsItem(
                            showDivider = true,
                            onClick = {
                                val characterId = currentCharacter?.characterId
                                    ?: return@MainPageLoyaltyPointsItem
                                navController.navigate(
                                    LoyaltyPointsRoute.Hub.create(characterId),
                                )
                            },
                        )
                        MainPagePeoplePlacesItem(
                            showDivider = true,
                            onClick = {
                                val characterId = currentCharacter?.characterId
                                    ?: return@MainPagePeoplePlacesItem
                                navController.navigate(
                                    PeoplePlacesRoute.Search.create(characterId),
                                )
                            },
                        )
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
                if (showCorporationSection) {
                    item(key = "corporation_section_header") {
                        MainPageSectionHeader(title = corporationSectionTitle)
                    }
                    item(key = "corporation_section_entries") {
                        MainPageSectionCard(
                            bottomSpacing = dimensionResource(R.dimen.character_main_card_bottom_spacing),
                        ) {
                            MainPageCorporationWalletItem(
                                showDivider = true,
                                onClick = {
                                    val characterId = currentCharacter?.characterId
                                        ?: return@MainPageCorporationWalletItem
                                    navController.navigate(
                                        CorporationWalletRoute.Wallets.create(characterId),
                                    )
                                },
                            )
                            MainPageCorporationMembersItem(
                                showDivider = true,
                                onClick = {
                                    val characterId = currentCharacter?.characterId
                                        ?: return@MainPageCorporationMembersItem
                                    navController.navigate(
                                        CorporationMembersRoute.Members.create(characterId),
                                    )
                                },
                            )
                            MainPageCorporationStructuresItem(
                                showDivider = true,
                                onClick = {
                                    val characterId = currentCharacter?.characterId
                                        ?: return@MainPageCorporationStructuresItem
                                    navController.navigate(
                                        CorporationStructuresRoute.Structures.create(characterId),
                                    )
                                },
                            )
                            MainPageCorporationIndustryItem(
                                showDivider = true,
                                onClick = {
                                    val characterId = currentCharacter?.characterId
                                        ?: return@MainPageCorporationIndustryItem
                                    navController.navigate(
                                        CorporationIndustryRoute.Industry.create(characterId),
                                    )
                                },
                            )
                            MainPageCorporationAssetsItem(
                                showDivider = true,
                                onClick = {
                                    val characterId = currentCharacter?.characterId
                                        ?: return@MainPageCorporationAssetsItem
                                    navController.navigate(
                                        CorporationAssetsRoute.Regions.create(characterId),
                                    )
                                },
                            )
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
                item(key = "data_section_header") {
                    MainPageSectionHeader(title = dataSectionTitle)
                }
                item(key = "data_section_entries") {
                    MainPageSectionCard {
                        MainPageDatabaseItem(
                            onClick = { navController.navigate(DatabaseRoute.Root.route) },
                        )
                        MainPageRegionMarketItem(
                            onClick = { navController.navigate(RegionMarketRoute.Root.route) },
                        )
                        MainPageMarketWatchlistItem(
                            onClick = { navController.navigate(RegionMarketRoute.Watchlists.route) },
                        )
                    }
                }
            }
        }
    }

    val sheetState = uiState as? SdeUpdateUiState.Sheet
    if (sheetState != null) {
        key(sheetState.instanceId) {
            SdeUpdateBottomSheet(
                content = sheetState.content,
                onDismiss = sdeUpdateViewModel::dismissSheet,
                onConfirmDownload = sdeUpdateViewModel::startDownload,
                onRetryDownload = sdeUpdateViewModel::retryDownload,
                onRetryCheck = sdeUpdateViewModel::retryCheck,
            )
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

    androidx.compose.material3.Text(
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
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_database,
            itemName = stringResource(R.string.database),
            onClick = onClick,
        ),
        showDivider = true,
    )
}

@Composable
private fun MainPageRegionMarketItem(onClick: () -> Unit) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_region_market,
            iconTint = androidx.compose.ui.graphics.Color.Unspecified,
            itemName = stringResource(R.string.region_market),
            onClick = onClick,
        ),
        showDivider = true,
    )
}

@Composable
private fun MainPageMarketWatchlistItem(onClick: () -> Unit) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_market_watchlist,
            iconTint = androidx.compose.ui.graphics.Color.Unspecified,
            itemName = stringResource(R.string.market_watchlist),
            onClick = onClick,
        ),
        showDivider = false,
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
