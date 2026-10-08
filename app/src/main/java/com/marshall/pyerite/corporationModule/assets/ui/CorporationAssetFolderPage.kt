package com.marshall.pyerite.corporationModule.assets.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetFolderKey
import com.marshall.pyerite.corporationModule.assets.model.dedupedForDisplay
import com.marshall.pyerite.corporationModule.assets.viewModel.CorporationAssetFolderViewModel
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun CorporationAssetFolderPage(
    navController: NavController,
    viewModel: CorporationAssetFolderViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val language = localeController.contentLanguage
    val folderKey = CorporationAssetFolderKey(
        kind = viewModel.locationKey.kind,
        locationId = viewModel.locationKey.locationId,
        routeFlag = viewModel.routeFlag,
    )
    val folder = uiState.snapshot?.folders?.get(folderKey)
    val rows = remember(folder, language) { folder?.items.orEmpty().dedupedForDisplay(language) }
    val pageTitle = if (folder == null) {
        stringResource(R.string.corporation_assets_page_title)
    } else {
        corporationAssetSectionLabel(
            labelRes = folder.labelRes,
            hangarDivision = folder.hangarDivision,
            hangarCustomName = folder.hangarCustomName,
        )
    }
    val showEmpty = uiState.snapshot != null &&
        !uiState.permissionDenied &&
        !uiState.isLoading &&
        (folder == null || rows.isEmpty()) &&
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
        if (folder != null && rows.isNotEmpty() && !uiState.permissionDenied) {
            corporationAssetItemCards(
                sectionKey = "folder-${folderKey.routeFlag}",
                title = pageTitle,
                rows = rows,
                onItemClick = { row ->
                    navController.openCorporationAssetItem(viewModel.characterId, row)
                },
            )
        }
    }
}
