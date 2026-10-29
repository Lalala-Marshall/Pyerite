package com.marshall.pyerite.settingsModule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.BuildConfig
import com.marshall.pyerite.R
import com.marshall.pyerite.sdeModule.update.SdeUpdateBottomSheet
import com.marshall.pyerite.sdeModule.update.SdeUpdateSheetContent
import com.marshall.pyerite.sdeModule.update.SdeUpdateUiState
import com.marshall.pyerite.sdeModule.update.SdeUpdateViewModel
import com.marshall.pyerite.settingsModule.navHost.SettingsRoute
import com.marshall.pyerite.settingsModule.viewModel.SettingsViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import com.marshall.pyerite.ui.theme.ThemeMode
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun SettingsPage(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    sdeUpdateViewModel: SdeUpdateViewModel = koinViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val sdeUpdateState by sdeUpdateViewModel.uiState.collectAsState()
    val showCorporationAffairs by viewModel.showCorporationAffairs.collectAsState()
    val cacheMegabytes by viewModel.cacheMegabytes.collectAsState()
    val clearingCache by viewModel.clearingCache.collectAsState()
    val scrollState = rememberScrollState()
    val pageTitle = stringResource(R.string.settings)
    val onBack = navController.rememberNavigateUpAction()
    val showCollapsedTitle = rememberScrollTitleCollapsed(scrollState)
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    var showClearConfirm by remember { mutableStateOf(false) }
    val themeHint = when (themeMode) {
        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
        ThemeMode.FOLLOW_SYSTEM -> stringResource(R.string.settings_theme_system)
    }
    val themeIcon = when (themeMode) {
        ThemeMode.LIGHT -> Icons.Outlined.LightMode
        ThemeMode.DARK -> Icons.Outlined.DarkMode
        ThemeMode.FOLLOW_SYSTEM -> Icons.Outlined.BrightnessAuto
    }
    val cacheHint = cacheMegabytes?.let { megabytes ->
        stringResource(R.string.settings_cache_size, megabytes)
    }.orEmpty()

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
    ) { topBarPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(topBarPadding)
                .verticalScroll(scrollState)
                .padding(bottom = bottomPadding),
        ) {
            PageTitle(text = pageTitle)
            Spacer(modifier = Modifier.height(sectionGap))
            BaseContainer(
                title = stringResource(R.string.settings_section_appearance),
                useSystemBarsPadding = false,
            ) {
                SettingsIconRow(
                    title = stringResource(R.string.settings_color_mode),
                    hint = themeHint,
                    icon = themeIcon,
                    iconContentDescription = themeHint,
                    showDivider = true,
                    showChevron = false,
                    onClick = viewModel::cycleTheme,
                )
                SettingsIconRow(
                    title = stringResource(R.string.settings_language),
                    hint = stringResource(R.string.settings_language_hint),
                    icon = Icons.Outlined.Translate,
                    iconContentDescription = stringResource(R.string.settings_language_hint),
                    showDivider = false,
                    onClick = { navController.navigate(SettingsRoute.Language.route) },
                )
            }
            Spacer(modifier = Modifier.height(sectionGap))
            BaseContainer(
                title = stringResource(R.string.settings_section_features),
                useSystemBarsPadding = false,
            ) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_show_corporation_affairs),
                    hint = stringResource(R.string.settings_show_corporation_affairs_hint),
                    checked = showCorporationAffairs,
                    showDivider = false,
                    onCheckedChange = viewModel::setShowCorporationAffairs,
                )
            }
            Spacer(modifier = Modifier.height(sectionGap))
            BaseContainer(
                title = stringResource(R.string.settings_section_other),
                useSystemBarsPadding = false,
            ) {
                BaseLazyColumnItem(
                    model = BaseLazyColumnItemModel(
                        showLeadingIcon = false,
                        itemName = stringResource(R.string.sde_update_sheet_title),
                        itemHint = sdeUpdateHint(sdeUpdateState),
                        onClick = sdeUpdateViewModel::openUpdateSheet,
                    ),
                    showDivider = true,
                )
                if (BuildConfig.DEBUG) {
                    BaseLazyColumnItem(
                        model = BaseLazyColumnItemModel(
                            showLeadingIcon = false,
                            itemName = stringResource(R.string.sde_update_debug_reinstall_bundled),
                            itemHint = stringResource(R.string.sde_update_debug_reinstall_bundled_hint),
                            onClick = sdeUpdateViewModel::reinstallBundledForDebug,
                        ),
                        showDivider = true,
                    )
                }
                BaseLazyColumnItem(
                    model = BaseLazyColumnItemModel(
                        showLeadingIcon = false,
                        itemName = stringResource(R.string.settings_notifications),
                        onClick = { navController.navigate(SettingsRoute.Notifications.route) },
                    ),
                    showDivider = false,
                )
            }
            Spacer(modifier = Modifier.height(sectionGap))
            BaseContainer(
                title = stringResource(R.string.settings_section_cache),
                useSystemBarsPadding = false,
            ) {
                val iconSize = dimensionResource(R.dimen.settings_trailing_icon_size)
                BaseLazyColumnItem(
                    model = BaseLazyColumnItemModel(
                        showLeadingIcon = false,
                        itemName = stringResource(R.string.settings_clear_cache),
                        itemHint = cacheHint,
                        showChevron = false,
                        onClick = null,
                    ),
                    showDivider = false,
                    trailingContent = {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            enabled = !clearingCache,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.settings_clear_cache),
                                tint = colorResource(R.color.character_delete),
                                modifier = Modifier.size(iconSize),
                            )
                        }
                    },
                )
            }
        }
    }

    val sheetState = sdeUpdateState as? SdeUpdateUiState.Sheet
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

    if (showClearConfirm) {
        ClearCacheConfirmDialog(
            onDismiss = { showClearConfirm = false },
            onConfirm = {
                showClearConfirm = false
                viewModel.clearCache()
            },
        )
    }
}

@Composable
private fun sdeUpdateHint(state: SdeUpdateUiState): String {
    return when (state) {
        is SdeUpdateUiState.Checking -> stringResource(R.string.sde_update_checking)
        is SdeUpdateUiState.UpToDate -> stringResource(R.string.sde_update_up_to_date)
        is SdeUpdateUiState.UpdateReady -> stringResource(R.string.sde_update_available)
        is SdeUpdateUiState.CheckFailed -> stringResource(R.string.sde_update_check_failed_title)
        is SdeUpdateUiState.Idle -> stringResource(R.string.sde_update_check_manually)
        is SdeUpdateUiState.Sheet -> when (state.content) {
            is SdeUpdateSheetContent.Checking -> stringResource(R.string.sde_update_checking)
            is SdeUpdateSheetContent.CheckFailed -> stringResource(R.string.sde_update_check_failed_title)
            is SdeUpdateSheetContent.Prompt,
            is SdeUpdateSheetContent.Downloading,
            is SdeUpdateSheetContent.DownloadFailed,
            -> stringResource(R.string.sde_update_available)
            is SdeUpdateSheetContent.Completed,
            is SdeUpdateSheetContent.Info,
            -> stringResource(R.string.sde_update_up_to_date)
        }
    }
}

@Composable
private fun ClearCacheConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val dialogPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    val buttonGap = dimensionResource(R.dimen.type_detail_section_gap)
    val titleSize = dimensionResource(R.dimen.list_section_header_text_size).value.sp
    val bodySize = dimensionResource(R.dimen.type_detail_body_text_size).value.sp
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(dimensionResource(R.dimen.detail_card_corner_radius)),
            color = colorResource(R.color.second_background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dialogPadding),
            ) {
                Text(
                    text = stringResource(R.string.settings_clear_cache),
                    color = colorResource(R.color.text_primary),
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(buttonGap))
                Text(
                    text = stringResource(R.string.settings_clear_cache_message),
                    color = colorResource(R.color.text_primary),
                    fontSize = bodySize,
                )
                Spacer(modifier = Modifier.height(buttonGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.settings_clear_cache_cancel),
                            color = colorResource(R.color.text_primary),
                        )
                    }
                    Spacer(modifier = Modifier.width(buttonGap))
                    TextButton(onClick = onConfirm) {
                        Text(
                            text = stringResource(R.string.settings_clear_cache_confirm),
                            color = colorResource(R.color.character_delete),
                        )
                    }
                }
            }
        }
    }
}
