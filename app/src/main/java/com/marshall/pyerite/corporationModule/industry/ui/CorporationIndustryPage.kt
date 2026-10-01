package com.marshall.pyerite.corporationModule.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryBucket
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryConfig
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryJob
import com.marshall.pyerite.corporationModule.industry.model.toSections
import com.marshall.pyerite.corporationModule.industry.viewModel.CorporationIndustryViewModel
import com.marshall.pyerite.databaseHierarchyModule.navHost.DatabaseRoute
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
internal fun CorporationIndustryPage(
    navController: NavController,
    viewModel: CorporationIndustryViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = localeController.contentLanguage
    val pageTitle = stringResource(R.string.corporation_industry_page_title)
    val listState = rememberLazyListState()
    val showCollapsedTitle = rememberLazyListTitleCollapsed(listState)
    val onBack = navController.rememberNavigateUpAction()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasRunningJobs = uiState.jobs.any { job ->
        val bucket = job.bucket(nowMs)
        bucket == CorporationIndustryBucket.DUE_SOON || bucket == CorporationIndustryBucket.ACTIVE
    }
    LaunchedEffect(hasRunningJobs) {
        if (!hasRunningJobs) return@LaunchedEffect
        while (true) {
            delay(CorporationIndustryConfig.MILLIS_PER_SECOND.milliseconds)
            nowMs = System.currentTimeMillis()
        }
    }
    val sections = uiState.jobs.toSections(uiState.filter, nowMs, language)
    val rosterReady = !uiState.isLoading && !uiState.permissionDenied && !uiState.loadFailed
    val showEmpty = rosterReady && sections.isEmpty
    val showJobs = !uiState.permissionDenied && !sections.isEmpty
    var showFilter by remember { mutableStateOf(false) }
    val refreshAction = pyeritePullRefreshTopBarAction(
        isRefreshing = uiState.isLoading,
        refreshFailed = uiState.loadFailed,
        onRefresh = viewModel::refresh,
    )
    val settingsAction = PyeriteTopBarActionItem(
        onClick = { showFilter = true },
        icon = Icons.Filled.Settings,
        contentDescription = stringResource(R.string.corporation_industry_filter_settings),
    )

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
        endActions = listOfNotNull(refreshAction, settingsAction),
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
                    item(key = "summary") {
                        SummarySection(
                            manufacturing = sections.summary.manufacturing,
                            research = sections.summary.research,
                            reaction = sections.summary.reaction,
                        )
                    }
                    if (sections.ready.isNotEmpty()) {
                        item(key = "ready") {
                            JobSection(
                                title = stringResource(R.string.corporation_industry_section_ready),
                                jobs = sections.ready,
                                bucket = CorporationIndustryBucket.READY,
                                language = language,
                                nowMs = nowMs,
                                onOpenType = { typeId ->
                                    navController.navigate(DatabaseRoute.TypeDetail.create(typeId))
                                },
                            )
                        }
                    }
                    if (sections.dueSoon.isNotEmpty()) {
                        item(key = "due_soon") {
                            JobSection(
                                title = stringResource(R.string.corporation_industry_section_due_soon),
                                jobs = sections.dueSoon,
                                bucket = CorporationIndustryBucket.DUE_SOON,
                                language = language,
                                nowMs = nowMs,
                                onOpenType = { typeId ->
                                    navController.navigate(DatabaseRoute.TypeDetail.create(typeId))
                                },
                            )
                        }
                    }
                    if (sections.active.isNotEmpty()) {
                        item(key = "active") {
                            JobSection(
                                title = stringResource(R.string.corporation_industry_section_active),
                                jobs = sections.active,
                                bucket = CorporationIndustryBucket.ACTIVE,
                                language = language,
                                nowMs = nowMs,
                                onOpenType = { typeId ->
                                    navController.navigate(DatabaseRoute.TypeDetail.create(typeId))
                                },
                            )
                        }
                    }
                    if (sections.history.isNotEmpty()) {
                        item(key = "history") {
                            JobSection(
                                title = stringResource(R.string.corporation_industry_section_history),
                                jobs = sections.history,
                                bucket = CorporationIndustryBucket.HISTORY,
                                language = language,
                                nowMs = nowMs,
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
        CorporationIndustryFilterSheet(
            filter = uiState.filter,
            installers = sections.installers,
            systems = sections.systems,
            language = language,
            onHideClosedChange = viewModel::setHideClosed,
            onToggleActivity = viewModel::toggleActivity,
            onToggleAllActivities = viewModel::toggleAllActivities,
            onToggleInstaller = viewModel::toggleInstaller,
            onToggleAllInstallers = viewModel::toggleAllInstallers,
            onToggleSystem = viewModel::toggleSystem,
            onToggleAllSystems = viewModel::toggleAllSystems,
            onDismiss = { showFilter = false },
        )
    }
}

@Composable
private fun SummarySection(
    manufacturing: Int,
    research: Int,
    reaction: Int,
) {
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    BaseContainer(
        title = stringResource(R.string.corporation_industry_section_summary),
        useSystemBarsPadding = false,
        modifier = Modifier.padding(top = sectionGap),
    ) {
        SummaryCountRow(
            label = stringResource(R.string.corporation_industry_summary_manufacturing),
            count = manufacturing,
            colorRes = R.color.corporation_industry_manufacturing,
            showDivider = true,
        )
        SummaryCountRow(
            label = stringResource(R.string.corporation_industry_summary_research),
            count = research,
            colorRes = R.color.corporation_industry_research,
            showDivider = true,
        )
        SummaryCountRow(
            label = stringResource(R.string.corporation_industry_summary_reaction),
            count = reaction,
            colorRes = R.color.corporation_industry_reaction,
            showDivider = false,
        )
    }
}

@Composable
private fun SummaryCountRow(
    label: String,
    count: Int,
    colorRes: Int,
    showDivider: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.detail_row_horizontal_padding),
                vertical = dimensionResource(R.dimen.detail_row_vertical_padding_single_line),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val labelSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp
        val labelLineHeight = dimensionResource(R.dimen.sub_menu_label_line_height).value.sp
        Text(
            text = label,
            color = colorResource(R.color.text_primary),
            fontSize = labelSize,
            lineHeight = labelLineHeight,
        )
        Text(
            text = integerCount(count),
            color = colorResource(colorRes),
            fontSize = labelSize,
            lineHeight = labelLineHeight,
        )
    }
    if (showDivider) {
        HorizontalDivider(
            thickness = dimensionResource(R.dimen.detail_divider_thickness),
            color = colorResource(R.color.border),
        )
    }
}

@Composable
private fun JobSection(
    title: String,
    jobs: List<CorporationIndustryJob>,
    bucket: CorporationIndustryBucket,
    language: ContentLanguage,
    nowMs: Long,
    onOpenType: (Int) -> Unit,
) {
    BaseContainer(
        title = title,
        titleTrailingContent = {
            Text(
                text = stringResource(
                    R.string.corporation_industry_section_count,
                    integerCount(jobs.size),
                ),
                color = colorResource(R.color.hint_text),
                fontSize = dimensionResource(R.dimen.list_section_subheader_text_size).value.sp,
            )
        },
        useSystemBarsPadding = false,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.type_detail_section_gap)),
    ) {
        jobs.forEachIndexed { index, job ->
            CorporationIndustryJobRow(
                job = job,
                bucket = bucket,
                language = language,
                nowMs = nowMs,
                onClick = { onOpenType(job.detailTypeId) },
            )
            if (index < jobs.lastIndex) {
                HorizontalDivider(
                    thickness = dimensionResource(R.dimen.detail_divider_thickness),
                    color = colorResource(R.color.border),
                )
            }
        }
    }
}

private fun integerCount(count: Int): String =
    NumberDisplayFormatter.format(count.toLong(), NumberDisplayFormatter.Style.FULL)

@Composable
private fun IndustryStatusBanner(
    permissionDenied: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
) {
    val messageRes = when {
        permissionDenied -> R.string.corporation_industry_permission_denied
        loadFailed -> R.string.corporation_industry_load_failed
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
