package com.marshall.pyerite.corporationModule.contracts.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractDisplayLimit
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractStatus
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractType
import com.marshall.pyerite.corporationModule.contracts.model.CorporationContractsFilter
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

internal enum class CorporationContractsSheetPage {
    SETTINGS,
    DISPLAY_LIMIT,
}

@Composable
internal fun CorporationContractsSettingsSheet(
    page: CorporationContractsSheetPage,
    filter: CorporationContractsFilter,
    onOpenDisplayLimit: () -> Unit,
    onBack: () -> Unit,
    onSelectDisplayLimit: (CorporationContractDisplayLimit) -> Unit,
    onToggleType: (CorporationContractType) -> Unit,
    onToggleAllTypes: () -> Unit,
    onToggleStatus: (CorporationContractStatus) -> Unit,
    onToggleAllStatuses: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (page) {
        CorporationContractsSheetPage.SETTINGS ->
            stringResource(R.string.corporation_contracts_settings)
        CorporationContractsSheetPage.DISPLAY_LIMIT ->
            stringResource(R.string.corporation_contracts_limit_max)
    }
    val startLabel = if (page == CorporationContractsSheetPage.DISPLAY_LIMIT) {
        stringResource(R.string.corporation_structures_sheet_back)
    } else {
        null
    }
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    CorporationContractsModalSheet(
        title = title,
        onDismiss = onDismiss,
        startLabel = startLabel,
        onStart = if (page == CorporationContractsSheetPage.DISPLAY_LIMIT) onBack else null,
        endLabel = stringResource(R.string.corporation_structures_sheet_done),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding),
        ) {
            when (page) {
                CorporationContractsSheetPage.SETTINGS -> SettingsPage(
                    filter = filter,
                    sectionGap = sectionGap,
                    onOpenDisplayLimit = onOpenDisplayLimit,
                    onToggleType = onToggleType,
                    onToggleAllTypes = onToggleAllTypes,
                    onToggleStatus = onToggleStatus,
                    onToggleAllStatuses = onToggleAllStatuses,
                )
                CorporationContractsSheetPage.DISPLAY_LIMIT -> {
                    BaseContainer(useSystemBarsPadding = false) {
                        val options = CorporationContractDisplayLimit.entries
                        options.forEachIndexed { index, option ->
                            DisplayLimitOptionRow(
                                option = option,
                                selected = option == filter.displayLimit,
                                showDivider = index < options.lastIndex,
                                onClick = { onSelectDisplayLimit(option) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    filter: CorporationContractsFilter,
    sectionGap: Dp,
    onOpenDisplayLimit: () -> Unit,
    onToggleType: (CorporationContractType) -> Unit,
    onToggleAllTypes: () -> Unit,
    onToggleStatus: (CorporationContractStatus) -> Unit,
    onToggleAllStatuses: () -> Unit,
) {
    BaseContainer(
        title = stringResource(R.string.corporation_contracts_limit_section),
        useSystemBarsPadding = false,
    ) {
        BaseLazyColumnItem(
            model = BaseLazyColumnItemModel(
                showLeadingIcon = false,
                itemName = stringResource(R.string.corporation_contracts_limit_max),
                trailingValue = filter.displayLimit.displayLabel(),
                onClick = onOpenDisplayLimit,
            ),
            showDivider = false,
        )
    }
    Text(
        text = stringResource(R.string.corporation_contracts_limit_hint),
        color = colorResource(R.color.text_caption),
        fontSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp,
        lineHeight = dimensionResource(R.dimen.detail_row_label_subtitle_line_height).value.sp,
        modifier = Modifier.padding(
            start = dimensionResource(R.dimen.detail_card_horizontal_padding),
            end = dimensionResource(R.dimen.detail_card_horizontal_padding),
            top = dimensionResource(R.dimen.corporation_contracts_price_title_gap),
        ),
    )
    Spacer(modifier = Modifier.height(sectionGap))
    BaseContainer(
        title = stringResource(R.string.corporation_contracts_filter_type),
        titleTrailingContent = {
            CorporationContractsSelectAllButton(
                allSelected = filter.allTypesSelected,
                onClick = onToggleAllTypes,
            )
        },
        useSystemBarsPadding = false,
    ) {
        CorporationContractType.entries.forEachIndexed { index, type ->
            BaseLazyColumnItem(
                model = BaseLazyColumnItemModel(
                    showLeadingIcon = false,
                    itemName = stringResource(type.titleRes),
                    showChevron = false,
                    onClick = { onToggleType(type) },
                ),
                showDivider = index < CorporationContractType.entries.lastIndex,
                trailingContent = {
                    CorporationContractsItemCheck(selected = type in filter.types)
                },
            )
        }
    }
    Spacer(modifier = Modifier.height(sectionGap))
    BaseContainer(
        title = stringResource(R.string.corporation_contracts_filter_status),
        titleTrailingContent = {
            CorporationContractsSelectAllButton(
                allSelected = filter.allStatusesSelected,
                onClick = onToggleAllStatuses,
            )
        },
        useSystemBarsPadding = false,
    ) {
        CorporationContractStatus.entries.forEachIndexed { index, status ->
            StatusFilterRow(
                status = status,
                selected = status in filter.statuses,
                showDivider = index < CorporationContractStatus.entries.lastIndex,
                onClick = { onToggleStatus(status) },
            )
        }
    }
}

@Composable
private fun DisplayLimitOptionRow(
    option: CorporationContractDisplayLimit,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = option.displayLabel(),
            showChevron = false,
            onClick = onClick,
        ),
        showDivider = showDivider,
        trailingContent = { CorporationContractsItemCheck(selected = selected) },
    )
}

@Composable
private fun StatusFilterRow(
    status: CorporationContractStatus,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics { role = Role.Checkbox }
                .padding(
                    horizontal = dimensionResource(R.dimen.detail_row_horizontal_padding),
                    vertical = dimensionResource(R.dimen.detail_row_vertical_padding_single_line),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CorporationContractStatusTag(status = status)
            Spacer(modifier = Modifier.weight(1f))
            CorporationContractsItemCheck(selected = selected)
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = dimensionResource(R.dimen.detail_divider_thickness),
                color = colorResource(R.color.border),
            )
        }
    }
}

@Composable
private fun CorporationContractDisplayLimit.displayLabel(): String {
    val count = maxCount
    return if (count == null) {
        stringResource(R.string.corporation_contracts_limit_unlimited)
    } else {
        stringResource(R.string.corporation_contracts_limit_count, count)
    }
}
