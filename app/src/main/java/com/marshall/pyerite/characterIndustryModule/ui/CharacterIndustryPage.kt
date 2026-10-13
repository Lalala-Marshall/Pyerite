package com.marshall.pyerite.characterIndustryModule.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.characterIndustryModule.model.CharacterIndustrySlotSummary
import com.marshall.pyerite.characterIndustryModule.model.slotSummaryFor
import com.marshall.pyerite.characterIndustryModule.model.summed
import com.marshall.pyerite.characterIndustryModule.viewModel.CharacterIndustryUiState
import com.marshall.pyerite.characterIndustryModule.viewModel.CharacterIndustryViewModel
import com.marshall.pyerite.databaseHierarchyModule.navHost.DatabaseRoute
import com.marshall.pyerite.eveAuthModule.model.EveSsoScope
import com.marshall.pyerite.industryCommon.model.IndustryBucket
import com.marshall.pyerite.industryCommon.model.IndustryConfig
import com.marshall.pyerite.industryCommon.model.toSections
import com.marshall.pyerite.industryCommon.ui.IndustryFilterSheet
import com.marshall.pyerite.industryCommon.ui.IndustryJobSection
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
internal fun CharacterIndustryPage(
    navController: NavController,
    viewModel: CharacterIndustryViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = localeController.contentLanguage
    val pageTitle = stringResource(R.string.character_industry_page_title)
    val listState = rememberLazyListState()
    val showCollapsedTitle = rememberLazyListTitleCollapsed(listState)
    val onBack = navController.rememberNavigateUpAction()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasRunningJobs = uiState.jobs.any { job ->
        val bucket = job.bucket(nowMs)
        bucket == IndustryBucket.DUE_SOON || bucket == IndustryBucket.ACTIVE
    }
    LaunchedEffect(hasRunningJobs) {
        if (!hasRunningJobs) return@LaunchedEffect
        while (true) {
            delay(IndustryConfig.MILLIS_PER_SECOND.milliseconds)
            nowMs = System.currentTimeMillis()
        }
    }
    val sections = uiState.jobs.toSections(uiState.filter, nowMs, language)
    val rosterReady = !uiState.isLoading && !uiState.permissionDenied && !uiState.loadFailed
    val showEmpty = rosterReady && sections.isEmpty
    val showJobs = !uiState.permissionDenied && !sections.isEmpty
    val showSummary = !uiState.permissionDenied && (showJobs || rosterReady)
    val showInstaller = uiState.settings.aggregateCharacters
    var showFilter by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSlotDetails by remember { mutableStateOf(false) }
    val selectableCharacters = remember(uiState.loggedInCharacters) {
        uiState.loggedInCharacters.filter { character ->
            EveSsoScope.INDUSTRY_CHARACTER_JOBS in character.grantedScopes
        }
    }
    val included = remember(
        uiState.settings,
        uiState.loggedInCharacters,
        uiState.jobs,
        uiState.slotsByCharacterId,
        viewModel.characterId,
    ) {
        slotDetails(uiState, viewModel.characterId)
    }
    val summary = included
        .filter { detail -> detail.characterId in uiState.slotsByCharacterId }
        .map { it.summary }
        .summed()
    val refreshAction = pyeritePullRefreshTopBarAction(
        isRefreshing = uiState.isLoading,
        refreshFailed = uiState.loadFailed,
        onRefresh = viewModel::refresh,
    )
    val filterAction = PyeriteTopBarActionItem(
        onClick = { showFilter = true },
        icon = Icons.Filled.FilterList,
        contentDescription = stringResource(R.string.corporation_industry_filter_settings),
    )
    val settingsAction = PyeriteTopBarActionItem(
        onClick = { showSettings = true },
        icon = Icons.Outlined.Settings,
        contentDescription = stringResource(R.string.character_industry_settings),
    )

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
        endActions = listOfNotNull(refreshAction, filterAction, settingsAction),
    ) { topBarPadding ->
        PyeritePullToRefreshBox(
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(topBarPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "page_title") {
                    PageTitle(text = pageTitle)
                }
                if (uiState.permissionDenied || uiState.loadFailed) {
                    item(key = "status") {
                        IndustryStatusBanner(
                            permissionDenied = uiState.permissionDenied,
                            loadFailed = uiState.loadFailed,
                            onRetry = viewModel::refresh,
                        )
                    }
                }
                if (showSummary) {
                    item(key = "summary") {
                        SummarySection(
                            summary = summary,
                            onInfoClick = { showSlotDetails = true },
                        )
                    }
                }
                if (showEmpty) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.corporation_industry_empty),
                            color = colorResource(R.color.text_primary),
                            fontSize = dimensionResource(R.dimen.type_detail_body_text_size).value.sp,
                            modifier = Modifier.padding(
                                start = dimensionResource(R.dimen.type_detail_page_title_start_padding),
                                end = dimensionResource(R.dimen.detail_card_horizontal_padding),
                                top = dimensionResource(R.dimen.type_detail_section_gap),
                            ),
                        )
                    }
                }
                if (showJobs) {
                    if (sections.ready.isNotEmpty()) {
                        item(key = "ready") {
                            IndustryJobSection(
                                title = stringResource(R.string.corporation_industry_section_ready),
                                jobs = sections.ready,
                                bucket = IndustryBucket.READY,
                                language = language,
                                nowMs = nowMs,
                                showInstaller = showInstaller,
                                onOpenType = { typeId ->
                                    navController.navigate(DatabaseRoute.TypeDetail.create(typeId))
                                },
                            )
                        }
                    }
                    if (sections.dueSoon.isNotEmpty()) {
                        item(key = "due_soon") {
                            IndustryJobSection(
                                title = stringResource(R.string.corporation_industry_section_due_soon),
                                jobs = sections.dueSoon,
                                bucket = IndustryBucket.DUE_SOON,
                                language = language,
                                nowMs = nowMs,
                                showInstaller = showInstaller,
                                onOpenType = { typeId ->
                                    navController.navigate(DatabaseRoute.TypeDetail.create(typeId))
                                },
                            )
                        }
                    }
                    if (sections.active.isNotEmpty()) {
                        item(key = "active") {
                            IndustryJobSection(
                                title = stringResource(R.string.corporation_industry_section_active),
                                jobs = sections.active,
                                bucket = IndustryBucket.ACTIVE,
                                language = language,
                                nowMs = nowMs,
                                showInstaller = showInstaller,
                                onOpenType = { typeId ->
                                    navController.navigate(DatabaseRoute.TypeDetail.create(typeId))
                                },
                            )
                        }
                    }
                    if (sections.history.isNotEmpty()) {
                        item(key = "history") {
                            IndustryJobSection(
                                title = stringResource(R.string.corporation_industry_section_history),
                                jobs = sections.history,
                                bucket = IndustryBucket.HISTORY,
                                language = language,
                                nowMs = nowMs,
                                showInstaller = showInstaller,
                                onOpenType = { typeId ->
                                    navController.navigate(DatabaseRoute.TypeDetail.create(typeId))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilter) {
        IndustryFilterSheet(
            filter = uiState.filter,
            installers = emptyList(),
            systems = sections.systems,
            language = language,
            showInstallers = false,
            onHideClosedChange = viewModel::setHideClosed,
            onToggleActivity = viewModel::toggleActivity,
            onToggleAllActivities = viewModel::toggleAllActivities,
            onToggleSystem = viewModel::toggleSystem,
            onToggleAllSystems = viewModel::toggleAllSystems,
            onDismiss = { showFilter = false },
        )
    }
    if (showSettings) {
        CharacterIndustrySettingsSheet(
            initial = uiState.settings,
            anchorCharacterId = viewModel.characterId,
            characters = selectableCharacters,
            onCommit = viewModel::updateSettings,
            onDismiss = { showSettings = false },
        )
    }
    if (showSlotDetails) {
        CharacterIndustrySlotDetailSheet(
            characters = included,
            onDismiss = { showSlotDetails = false },
        )
    }
}

private fun slotDetails(
    state: CharacterIndustryUiState,
    anchorCharacterId: Long,
): List<CharacterIndustrySlotDetail> {
    val ids = state.settings.includedCharacterIds(anchorCharacterId)
    return ids.map { id ->
        val character = state.loggedInCharacters.find { it.characterId == id }
        CharacterIndustrySlotDetail(
            characterId = id,
            name = character?.name.orEmpty(),
            portraitUrl = character?.portraitUrl,
            summary = slotSummaryFor(
                characterId = id,
                jobs = state.jobs,
                capacity = state.slotsByCharacterId[id],
            ),
        )
    }
}

@Composable
private fun SummarySection(
    summary: CharacterIndustrySlotSummary,
    onInfoClick: () -> Unit,
) {
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    BaseContainer(
        title = stringResource(R.string.corporation_industry_section_summary),
        titleTrailingContent = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.character_industry_slot_details),
                tint = colorResource(R.color.hyperlink_text),
                modifier = Modifier
                    .size(dimensionResource(R.dimen.detail_row_chevron_size))
                    .clickable(role = Role.Button, onClick = onInfoClick),
            )
        },
        useSystemBarsPadding = false,
        modifier = Modifier.padding(top = sectionGap),
    ) {
        CharacterIndustrySlotLines(summary = summary)
    }
}

@Composable
private fun IndustryStatusBanner(
    permissionDenied: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
) {
    val messageRes = when {
        permissionDenied -> R.string.character_industry_permission_denied
        loadFailed -> R.string.character_industry_load_failed
        else -> return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(messageRes),
            color = colorResource(R.color.text_primary),
            modifier = Modifier.weight(1f),
        )
        if (loadFailed && !permissionDenied) {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.character_sheet_retry))
            }
        }
    }
}
