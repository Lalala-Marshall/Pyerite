package com.marshall.pyerite.corporationModule.assets.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetContentSection
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetItemRow
import com.marshall.pyerite.corporationModule.assets.model.dedupedForDisplay
import com.marshall.pyerite.corporationModule.assets.viewModel.CorporationAssetContainerViewModel
import com.marshall.pyerite.databaseHierarchyModule.navHost.DatabaseRoute
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun CorporationAssetContainerPage(
    navController: NavController,
    viewModel: CorporationAssetContainerViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = localeController.contentLanguage
    val container = uiState.snapshot?.containers?.get(viewModel.itemId)
    val typeName = container?.displayName(localeController).orEmpty().ifBlank {
        stringResource(R.string.corporation_assets_unknown_type)
    }
    val categoryTitle = container?.categoryTitleRes?.let { titleRes ->
        corporationAssetSectionLabel(
            labelRes = titleRes,
            hangarDivision = container.hangarDivision,
            hangarCustomName = container.hangarCustomName,
        )
    }
    val pageTitle = when {
        categoryTitle != null -> categoryTitle
        container?.customName?.isNotBlank() == true -> container.customName
        container == null -> stringResource(R.string.corporation_assets_page_title)
        else -> typeName
    }
    val slotSections = remember(container, language) {
        container?.slotSections.orEmpty().map { section ->
            section to section.items.dedupedForDisplay(language)
        }
    }
    val contentSections = remember(container, language) {
        container?.contentSections.orEmpty().map { section ->
            section to section.items.dedupedForDisplay(language)
        }
    }
    val titledContent = ArrayList<Triple<CorporationAssetContentSection, List<CorporationAssetItemRow>, String>>(
        contentSections.size,
    )
    for ((section, rows) in contentSections) {
        titledContent += Triple(
            section,
            rows,
            corporationAssetSectionLabel(
                labelRes = section.titleRes,
                hangarDivision = section.hangarDivision,
                hangarCustomName = section.hangarCustomName,
            ),
        )
    }
    val showEmpty = uiState.snapshot != null &&
        !uiState.permissionDenied &&
        !uiState.isLoading &&
        container == null &&
        !uiState.loadFailed
    CorporationAssetPageScaffold(
        title = pageTitle,
        isLoading = uiState.isLoading,
        loadFailed = uiState.loadFailed,
        permissionDenied = uiState.permissionDenied,
        onRefresh = viewModel::refresh,
        onBack = navController.rememberNavigateUpAction(),
    ) {
        if (showEmpty) {
            item(key = "empty") { CorporationAssetEmptyText() }
        }
        if (container != null && !uiState.permissionDenied) {
            if (container.showsCapacityHeader) {
                item(key = "container-header") {
                    BaseContainer(
                        useSystemBarsPadding = false,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.type_detail_section_gap),
                        ),
                    ) {
                        val headerHints = buildList {
                            container.customName?.takeIf { it.isNotBlank() }?.let { name ->
                                add(BaseLazyColumnItemHint(text = name))
                            }
                            container.officeContentTypeCount?.let { count ->
                                add(
                                    BaseLazyColumnItemHint(
                                        text = stringResource(
                                            R.string.corporation_assets_contains_types,
                                            NumberDisplayFormatter.format(
                                                count.toLong(),
                                                NumberDisplayFormatter.Style.FULL,
                                            ),
                                        ),
                                    ),
                                )
                            }
                        }
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                iconFileName = container.iconFilename?.takeIf { it.isNotBlank() },
                                iconOnLightPlate = !container.iconFilename.isNullOrBlank(),
                                showLeadingIcon = !container.iconFilename.isNullOrBlank(),
                                itemName = typeName,
                                itemHints = headerHints,
                                showChevron = container.typeId > 0,
                                onClick = if (container.typeId > 0) {
                                    {
                                        navController.navigate(
                                            DatabaseRoute.TypeDetail.create(container.typeId),
                                        )
                                    }
                                } else {
                                    null
                                },
                            ),
                            showDivider = true,
                        )
                        CorporationAssetCapacityBlock(
                            usedVolume = container.usedVolume,
                            capacity = container.capacity,
                        )
                    }
                }
            }
            slotSections.forEach { (section, rows) ->
                if (rows.isEmpty()) return@forEach
                item(key = "slot-${section.group.name}") {
                    BaseContainer(
                        title = stringResource(section.group.labelRes),
                        useSystemBarsPadding = false,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.type_detail_section_gap),
                        ),
                    ) {
                        rows.forEachIndexed { index, row ->
                            CorporationAssetItemLine(
                                row = row,
                                showDivider = index < rows.lastIndex,
                                onClick = {
                                    if (row.typeId > 0) {
                                        navController.navigate(
                                            DatabaseRoute.TypeDetail.create(row.typeId),
                                        )
                                    }
                                },
                                localeController = localeController,
                            )
                        }
                    }
                }
            }
            titledContent.forEach { (section, rows, title) ->
                if (rows.isEmpty()) return@forEach
                corporationAssetItemCards(
                    sectionKey = "content-${section.sortOrder}-${section.hangarDivision}-${section.titleRes}",
                    title = title,
                    rows = rows,
                    onItemClick = { row ->
                        navController.openCorporationAssetItem(viewModel.characterId, row)
                    },
                )
            }
        }
    }
}
