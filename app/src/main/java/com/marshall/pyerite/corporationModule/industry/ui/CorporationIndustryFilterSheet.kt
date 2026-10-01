package com.marshall.pyerite.corporationModule.industry.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryActivity
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryFilter
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryInstallerOption
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustrySystemOption
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
internal fun CorporationIndustryFilterSheet(
    filter: CorporationIndustryFilter,
    installers: List<CorporationIndustryInstallerOption>,
    systems: List<CorporationIndustrySystemOption>,
    language: ContentLanguage,
    onHideClosedChange: (Boolean) -> Unit,
    onToggleActivity: (CorporationIndustryActivity) -> Unit,
    onToggleAllActivities: () -> Unit,
    onToggleInstaller: (Long) -> Unit,
    onToggleAllInstallers: (Set<Long>) -> Unit,
    onToggleSystem: (Long) -> Unit,
    onToggleAllSystems: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    val installerIds = installers.map { it.installerId }.toSet()
    val systemIds = systems.map { it.solarSystemId }.toSet()
    CorporationIndustryModalSheet(
        title = stringResource(R.string.corporation_industry_filter_settings),
        endLabel = stringResource(R.string.corporation_structures_sheet_done),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding),
        ) {
            BaseContainer(useSystemBarsPadding = false) {
                BaseLazyColumnItem(
                    model = BaseLazyColumnItemModel(
                        showLeadingIcon = false,
                        itemName = stringResource(R.string.corporation_industry_filter_hide_title),
                        itemHint = stringResource(R.string.corporation_industry_filter_hide_hint),
                        showChevron = false,
                        onClick = { onHideClosedChange(!filter.hideClosed) },
                    ),
                    showDivider = false,
                    trailingContent = {
                        val checkedTrack = colorResource(R.color.character_status_positive)
                        Switch(
                            checked = filter.hideClosed,
                            onCheckedChange = onHideClosedChange,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = checkedTrack,
                                checkedBorderColor = checkedTrack,
                                checkedThumbColor = colorResource(R.color.white),
                            ),
                        )
                    },
                )
            }
            Spacer(modifier = Modifier.height(sectionGap))
            BaseContainer(
                title = stringResource(R.string.corporation_industry_filter_activity),
                titleTrailingContent = {
                    CorporationIndustrySelectAllButton(
                        allSelected = filter.allActivitiesSelected,
                        onClick = onToggleAllActivities,
                    )
                },
                useSystemBarsPadding = false,
            ) {
                CorporationIndustryActivity.entries.forEachIndexed { index, activity ->
                    BaseLazyColumnItem(
                        model = BaseLazyColumnItemModel(
                            showLeadingIcon = false,
                            itemName = stringResource(activity.titleRes),
                            showChevron = false,
                            onClick = { onToggleActivity(activity) },
                        ),
                        showDivider = index < CorporationIndustryActivity.entries.lastIndex,
                        trailingContent = {
                            CorporationIndustryItemCheck(
                                selected = activity !in filter.excludedActivities,
                            )
                        },
                    )
                }
            }
            if (installers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(sectionGap))
                BaseContainer(
                    title = stringResource(R.string.corporation_industry_filter_installer),
                    titleTrailingContent = {
                        CorporationIndustrySelectAllButton(
                            allSelected = filter.allInstallersSelected(installerIds),
                            onClick = { onToggleAllInstallers(installerIds) },
                        )
                    },
                    useSystemBarsPadding = false,
                ) {
                    installers.forEachIndexed { index, installer ->
                        val name = installer.name.ifBlank {
                            stringResource(R.string.corporation_industry_unknown_installer)
                        }
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                iconUrl = installer.portraitUrl,
                                itemName = name,
                                showChevron = false,
                                onClick = { onToggleInstaller(installer.installerId) },
                            ),
                            showDivider = index < installers.lastIndex,
                            trailingContent = {
                                CorporationIndustryItemCheck(
                                    selected = installer.installerId !in filter.excludedInstallerIds,
                                )
                            },
                        )
                    }
                }
            }
            if (systems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(sectionGap))
                BaseContainer(
                    title = stringResource(R.string.corporation_industry_filter_system),
                    titleTrailingContent = {
                        CorporationIndustrySelectAllButton(
                            allSelected = filter.allSystemsSelected(systemIds),
                            onClick = { onToggleAllSystems(systemIds) },
                        )
                    },
                    useSystemBarsPadding = false,
                ) {
                    systems.forEachIndexed { index, system ->
                        val name = system.displayName(language).ifBlank {
                            stringResource(R.string.corporation_industry_unknown_location)
                        }
                        BaseLazyColumnItem(
                            model = BaseLazyColumnItemModel(
                                showLeadingIcon = false,
                                itemName = name,
                                itemNameAnnotated = corporationIndustryPlaceLabel(
                                    name = name,
                                    security = system.securityStatus,
                                ),
                                showChevron = false,
                                onClick = { onToggleSystem(system.solarSystemId) },
                            ),
                            showDivider = index < systems.lastIndex,
                            trailingContent = {
                                CorporationIndustryItemCheck(
                                    selected = system.solarSystemId !in filter.excludedSolarSystemIds,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
