package com.marshall.pyerite.contractsCommon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.marshall.pyerite.R
import com.marshall.pyerite.contractsCommon.model.ContractAmountViewpoint
import com.marshall.pyerite.contractsCommon.model.CorporationContract
import com.marshall.pyerite.contractsCommon.model.CorporationContractDetail
import com.marshall.pyerite.contractsCommon.model.contractAmountParts
import com.marshall.pyerite.contractsCommon.model.CorporationContractDetailUiState
import com.marshall.pyerite.contractsCommon.model.CorporationContractOfferedItem
import com.marshall.pyerite.contractsCommon.model.CorporationContractsConfig
import com.marshall.pyerite.contractsCommon.model.CorporationContractsDateFormatter
import com.marshall.pyerite.contractsCommon.model.showsStatusChangedAt
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeriteIconShape
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.compose.koinInject
import java.io.File
import java.util.Locale

@Composable
internal fun ContractDetailScreen(
    uiState: CorporationContractDetailUiState,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)?,
    onItemClick: (Int) -> Unit,
    permissionDeniedMessage: String = stringResource(R.string.corporation_contracts_permission_denied),
    loadFailedMessage: String = stringResource(R.string.corporation_contracts_detail_load_failed),
    missingMessage: String = stringResource(R.string.corporation_contracts_detail_missing),
    amountViewpoint: ContractAmountViewpoint = ContractAmountViewpoint.CORPORATION_ISSUER,
    characterId: Long = 0L,
    localeController: LocaleController = koinInject(),
    iconManager: IconManager = koinInject(),
) {
    val pageTitle = stringResource(R.string.corporation_contracts_detail)
    val listState = rememberLazyListState()
    val showCollapsedTitle = rememberLazyListTitleCollapsed(listState)
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val refreshAction = pyeritePullRefreshTopBarAction(
        isRefreshing = uiState.isLoading,
        refreshFailed = uiState.loadFailed,
        onRefresh = onRefresh,
    )
    val detail = uiState.detail
    val nowMs = System.currentTimeMillis()

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
        endActions = listOfNotNull(refreshAction),
    ) { topBarPadding ->
        PyeritePullToRefreshBox(
            onRefresh = onRefresh,
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
                if (uiState.permissionDenied || uiState.loadFailed || uiState.missing) {
                    item(key = "status") {
                        ContractDetailStatusBanner(
                            permissionDenied = uiState.permissionDenied,
                            loadFailed = uiState.loadFailed,
                            missing = uiState.missing,
                            permissionDeniedMessage = permissionDeniedMessage,
                            loadFailedMessage = loadFailedMessage,
                            missingMessage = missingMessage,
                            onRetry = onRefresh,
                        )
                    }
                }
                if (detail != null) {
                    item(key = "info") {
                        Column(modifier = Modifier.padding(top = sectionGap)) {
                            ContractInfoSection(
                                detail = detail,
                                nowMs = nowMs,
                                iconManager = iconManager,
                                amountViewpoint = amountViewpoint,
                                characterId = characterId,
                            )
                        }
                    }
                    item(key = "items") {
                        Column(modifier = Modifier.padding(top = sectionGap)) {
                            OfferedItemsSection(
                                items = detail.items,
                                itemsLoadFailed = detail.itemsLoadFailed,
                                localeController = localeController,
                                onItemClick = onItemClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContractInfoSection(
    detail: CorporationContractDetail,
    nowMs: Long,
    iconManager: IconManager,
    amountViewpoint: ContractAmountViewpoint,
    characterId: Long,
) {
    val contract = detail.contract
    val placeholder = stringResource(R.string.character_sheet_value_placeholder)
    val hintColor = colorResource(R.color.hint_text)
    val typeTitle = stringResource(R.string.corporation_contracts_detail_type)
    val locationTitle = stringResource(R.string.corporation_contracts_detail_location)
    val issuerTitle = stringResource(R.string.corporation_contracts_detail_issuer)
    val assigneeTitle = stringResource(R.string.corporation_contracts_detail_assignee)
    val priceTitle = stringResource(R.string.corporation_contracts_detail_price)
    val issuedTitle = stringResource(R.string.corporation_contracts_detail_issued)
    val expiresTitle = stringResource(R.string.corporation_contracts_detail_expires)
    val typeName = stringResource(contract.type.titleRes)
    val statusName = stringResource(contract.status.titleRes)
    val statusBracket = stringResource(R.string.corporation_contracts_detail_status, statusName)
    val statusColor = corporationContractStatusTextColor(contract.status)
    val typeHint = buildAnnotatedString {
        withStyle(SpanStyle(color = hintColor)) {
            append(typeName)
        }
        withStyle(SpanStyle(color = statusColor)) {
            append(statusBracket)
        }
    }
    val placeName = detail.place?.name?.takeIf { it.isNotBlank() } ?: placeholder
    val security = detail.place?.securityStatus
    val securityColor = contractSecurityColor(security)
    val locationHint = security?.let { value ->
        contractPlaceHint(
            security = value,
            securityColor = securityColor,
            placeName = placeName,
            nameColor = hintColor,
        )
    }
    val issuerName = detail.issuer?.name?.takeIf { it.isNotBlank() } ?: placeholder
    val affiliation = detail.issuer?.affiliation.orEmpty()
    val issuerLine = stringResource(
        R.string.corporation_contracts_detail_issuer_line,
        issuerName,
        affiliation,
    )
    val issuerHint = if (affiliation.isBlank()) issuerName else issuerLine
    val publicLabel = stringResource(R.string.corporation_contracts_detail_public)
    val assigneeName = detail.assignee?.name?.takeIf { it.isNotBlank() }
    val assigneeHint = when {
        detail.assigneeIsPublic -> publicLabel
        assigneeName != null -> assigneeName
        else -> placeholder
    }
    val priceHint = contractAmountAnnotated(
        parts = contractAmountParts(
            contract = contract,
            viewpoint = amountViewpoint,
            characterId = characterId,
        ),
        withCompactPair = true,
    )
    val issuedHint = CorporationContractsDateFormatter.displayDateTime(contract.issuedAtMs)
    val expiresHint = contractExpiresHint(contract = contract, nowMs = nowMs, placeholder = placeholder)
    val buildingFile = detail.place?.iconFileName?.let { iconManager.getIconFile(it) }
    val issuerIcon = detail.issuer?.iconUrl?.takeIf { it.isNotBlank() }
    val assigneeIcon = detail.assignee?.iconUrl?.takeIf { it.isNotBlank() && !detail.assigneeIsPublic }

    BaseContainer(
        title = stringResource(R.string.corporation_contracts_detail_section_info),
        useSystemBarsPadding = false,
    ) {
        ContractDetailField(
            title = typeTitle,
            hint = typeName,
            hintAnnotated = typeHint,
            showDivider = true,
        )
        ContractDetailField(
            title = locationTitle,
            hint = placeName,
            hintAnnotated = locationHint,
            showDivider = true,
            trailing = buildingFile?.let { file ->
                {
                    ContractBuildingIcon(
                        file = file,
                        contentDescription = locationTitle,
                    )
                }
            },
        )
        ContractDetailField(
            title = issuerTitle,
            hint = issuerHint,
            hintColor = hintColor,
            showDivider = true,
            trailing = issuerIcon?.let { url ->
                {
                    ContractPartyIcon(
                        url = url,
                        contentDescription = issuerTitle,
                    )
                }
            },
        )
        ContractDetailField(
            title = assigneeTitle,
            hint = assigneeHint,
            hintColor = hintColor,
            showDivider = true,
            trailing = assigneeIcon?.let { url ->
                {
                    ContractPartyIcon(
                        url = url,
                        contentDescription = assigneeTitle,
                    )
                }
            },
        )
        ContractDetailField(
            title = priceTitle,
            hint = priceHint.text,
            hintAnnotated = priceHint,
            showDivider = true,
        )
        ContractDetailField(
            title = issuedTitle,
            hint = issuedHint,
            hintColor = hintColor,
            showDivider = true,
        )
        ContractDetailField(
            title = expiresTitle,
            hint = expiresHint,
            hintColor = hintColor,
            showDivider = false,
        )
    }
}

private fun contractPlaceHint(
    security: Double,
    securityColor: Color,
    placeName: String,
    nameColor: Color,
): AnnotatedString {
    val securityText = String.format(
        Locale.US,
        CorporationContractsConfig.SYSTEM_SECURITY_FORMAT,
        security,
    )
    return buildAnnotatedString {
        withStyle(SpanStyle(color = securityColor)) {
            append(securityText)
        }
        append(CorporationContractsConfig.SECURITY_STATUS_NAME_GAP)
        withStyle(SpanStyle(color = nameColor)) {
            append(placeName)
        }
    }
}

@Composable
private fun contractExpiresHint(
    contract: CorporationContract,
    nowMs: Long,
    placeholder: String,
): String {
    val closed = contract.status.showsStatusChangedAt
    val epoch = if (closed) contract.completedAtMs ?: contract.expiresAtMs else contract.expiresAtMs
    val dateText = epoch?.let { CorporationContractsDateFormatter.displayDateTime(it) }.orEmpty()
    val days = epoch?.let { CorporationContractsDateFormatter.daysUntil(it, nowMs) } ?: 0
    val withRemaining = stringResource(
        R.string.corporation_contracts_detail_expires_remaining,
        dateText,
        days,
    )
    return when {
        epoch == null -> placeholder
        closed -> dateText
        else -> withRemaining
    }
}

@Composable
private fun contractSecurityColor(security: Double?): Color {
    val negative = colorResource(R.color.character_security_negative)
    val low = colorResource(R.color.character_security_low)
    val high = colorResource(R.color.character_security_high)
    return when {
        security == null -> high
        security <= CorporationContractsConfig.SECURITY_NEGATIVE_MAX -> negative
        security < CorporationContractsConfig.SECURITY_LOW_MAX -> low
        else -> high
    }
}

@Composable
private fun OfferedItemsSection(
    items: List<CorporationContractOfferedItem>,
    itemsLoadFailed: Boolean,
    localeController: LocaleController,
    onItemClick: (Int) -> Unit,
) {
    val emptyText = stringResource(R.string.corporation_contracts_detail_items_empty)
    val failedText = stringResource(R.string.corporation_contracts_detail_items_failed)
    val unknownType = stringResource(R.string.corporation_assets_unknown_type)
    val sorted = items.sortedWith(
        compareBy<CorporationContractOfferedItem> { it.displayName(localeController) }
            .thenBy { it.typeId },
    )
    BaseContainer(
        title = stringResource(R.string.corporation_contracts_detail_items),
        useSystemBarsPadding = false,
    ) {
        when {
            itemsLoadFailed -> ContractDetailNote(failedText)
            sorted.isEmpty() -> ContractDetailNote(emptyText)
            else -> {
                sorted.forEachIndexed { index, item ->
                    val typeName = item.displayName(localeController).ifBlank { unknownType }
                    val quantity = stringResource(
                        R.string.corporation_assets_item_multiplier,
                        NumberDisplayFormatter.format(
                            item.quantity,
                            NumberDisplayFormatter.Style.FULL,
                        ),
                    )
                    val hasIcon = !item.iconFileName.isNullOrBlank()
                    BaseLazyColumnItem(
                        model = BaseLazyColumnItemModel(
                            iconFileName = item.iconFileName?.takeIf { it.isNotBlank() },
                            iconOnLightPlate = hasIcon,
                            showLeadingIcon = hasIcon,
                            itemName = typeName,
                            itemNameMaxLines = 1,
                            trailingValue = quantity,
                            showChevron = true,
                            onClick = { onItemClick(item.typeId) },
                        ),
                        showDivider = index < sorted.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContractDetailField(
    title: String,
    hint: String,
    hintAnnotated: AnnotatedString? = null,
    hintColor: Color? = null,
    showDivider: Boolean,
    trailing: (@Composable () -> Unit)? = null,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = title,
            itemNameMaxLines = 1,
            itemHints = listOf(
                BaseLazyColumnItemHint(
                    text = hint,
                    annotatedText = hintAnnotated,
                    color = hintColor,
                ),
            ),
            showChevron = false,
            onClick = null,
        ),
        showDivider = showDivider,
        trailingContent = trailing,
    )
}

@Composable
private fun ContractDetailNote(text: String) {
    Text(
        text = text,
        color = colorResource(R.color.hint_text),
        fontSize = dimensionResource(R.dimen.type_detail_body_text_size).value.sp,
        modifier = Modifier.padding(dimensionResource(R.dimen.detail_card_horizontal_padding)),
    )
}

@Composable
private fun ContractPartyIcon(
    url: String,
    contentDescription: String,
) {
    val iconSize = dimensionResource(R.dimen.base_lazy_column_item_icon_size)
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(iconSize)
            .clip(PyeriteIconShape.shape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun ContractBuildingIcon(
    file: File,
    contentDescription: String,
) {
    val iconSize = dimensionResource(R.dimen.base_lazy_column_item_icon_size)
    Box(
        modifier = Modifier
            .size(iconSize)
            .clip(PyeriteIconShape.shape)
            .background(colorResource(R.color.main_background)),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = file,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun ContractDetailStatusBanner(
    permissionDenied: Boolean,
    loadFailed: Boolean,
    missing: Boolean,
    permissionDeniedMessage: String,
    loadFailedMessage: String,
    missingMessage: String,
    onRetry: () -> Unit,
) {
    val message = when {
        permissionDenied -> permissionDeniedMessage
        missing -> missingMessage
        loadFailed -> loadFailedMessage
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
            text = message,
            color = colorResource(R.color.text_primary),
            modifier = Modifier.weight(1f),
        )
        if (!permissionDenied) {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.character_sheet_retry))
            }
        }
    }
}
