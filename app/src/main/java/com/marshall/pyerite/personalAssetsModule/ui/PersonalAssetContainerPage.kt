package com.marshall.pyerite.personalAssetsModule.ui

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
import com.marshall.pyerite.databaseHierarchyModule.navHost.DatabaseRoute
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.personalAssetsModule.model.sortedForDisplay
import com.marshall.pyerite.personalAssetsModule.viewModel.PersonalAssetContainerViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun PersonalAssetContainerPage(
    navController: NavController,
    viewModel: PersonalAssetContainerViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = localeController.contentLanguage
    val container = uiState.snapshot?.containers?.get(viewModel.itemId)
    val typeName = container?.displayName(localeController).orEmpty().ifBlank {
        stringResource(R.string.corporation_assets_unknown_type)
    }
    val categoryTitle = container?.categoryTitleRes?.let { titleRes ->
        personalAssetSectionLabel(
            labelRes = titleRes,
            hangarDivision = container.hangarDivision,
            hangarCustomName = container.hangarCustomName,
        )
    }
    val pageTitle = when {
        categoryTitle != null -> categoryTitle
        container?.customName?.isNotBlank() == true -> container.customName
        container == null -> stringResource(R.string.personal_assets_page_title)
        else -> typeName
    }
    val slotSections = remember(container, language) {
        container?.slotSections.orEmpty().map { section ->
            section to section.items.sortedForDisplay(language)
        }
    }
    val contentSections = remember(container, language) {
        container?.contentSections.orEmpty().map { section ->
            section to section.items.sortedForDisplay(language)
        }
    }
    val titledContent = contentSections.map { (section, rows) ->
        Triple(
            section,
            rows,
            personalAssetSectionLabel(
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
    PersonalAssetPageScaffold(
        title = pageTitle,
        isLoading = uiState.isLoading,
        loadFailed = uiState.loadFailed,
        permissionDenied = uiState.permissionDenied,
        showLoadingIcon = uiState.showLoadingIcon,
        onRefresh = viewModel::refresh,
        onBack = navController.rememberNavigateUpAction(),
    ) {
        if (showEmpty) {
            item(key = "empty") { PersonalAssetEmptyText() }
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
                        PersonalAssetCapacityBlock(
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
                            PersonalAssetItemLine(
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
                personalAssetItemCards(
                    sectionKey = "content-${section.sortOrder}-${section.hangarDivision}-${section.titleRes}",
                    title = title,
                    rows = rows,
                    onItemClick = { row ->
                        navController.openPersonalAssetItem(viewModel.characterId, row)
                    },
                )
            }
        }
    }
}
