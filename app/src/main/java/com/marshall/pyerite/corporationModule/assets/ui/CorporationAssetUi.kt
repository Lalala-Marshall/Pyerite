package com.marshall.pyerite.corporationModule.assets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetItemRow
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetLocationKind
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetsConfig
import com.marshall.pyerite.corporationModule.assets.navHost.CorporationAssetsRoute
import com.marshall.pyerite.databaseHierarchyModule.navHost.DatabaseRoute
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.LocalizableName
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.compose.koinInject
import java.util.Locale
import kotlin.math.floor

@Composable
internal fun CorporationAssetPageScaffold(
    title: String,
    isLoading: Boolean,
    loadFailed: Boolean,
    permissionDenied: Boolean,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)?,
    showPageTitle: Boolean = true,
    scrollResultsToTop: Boolean = false,
    bottomBar: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    val showCollapsedTitle = rememberLazyListTitleCollapsed(listState)
    val refreshAction = pyeritePullRefreshTopBarAction(
        isRefreshing = isLoading,
        refreshFailed = loadFailed,
        onRefresh = onRefresh,
    )
    LaunchedEffect(scrollResultsToTop) {
        if (scrollResultsToTop) listState.scrollToItem(0)
    }
    PyeritePageScaffold(
        title = title,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
        endActions = listOfNotNull(refreshAction),
    ) { topBarPadding ->
        if (bottomBar == null) {
            PyeritePullToRefreshBox(
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(topBarPadding)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) {
                CorporationAssetList(
                    listState = listState,
                    title = title,
                    showPageTitle = showPageTitle,
                    permissionDenied = permissionDenied,
                    loadFailed = loadFailed,
                    onRefresh = onRefresh,
                    content = content,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(topBarPadding)
                    .corporationAssetsBottomInset(),
            ) {
                PyeritePullToRefreshBox(
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    CorporationAssetList(
                        listState = listState,
                        title = title,
                        showPageTitle = showPageTitle,
                        permissionDenied = permissionDenied,
                        loadFailed = loadFailed,
                        onRefresh = onRefresh,
                        content = content,
                    )
                }
                bottomBar()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Modifier.corporationAssetsBottomInset(): Modifier =
    windowInsetsPadding(
        WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom),
    )

@Composable
private fun CorporationAssetList(
    listState: LazyListState,
    title: String,
    showPageTitle: Boolean,
    permissionDenied: Boolean,
    loadFailed: Boolean,
    onRefresh: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (showPageTitle) {
            item(key = "page_title") {
                PageTitle(text = title)
            }
        }
        if (permissionDenied || loadFailed) {
            item(key = "status") {
                CorporationAssetStatusBanner(
                    permissionDenied = permissionDenied,
                    loadFailed = loadFailed,
                    onRetry = onRefresh,
                )
            }
        }
        content()
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.type_detail_section_gap)))
        }
    }
}

@Composable
internal fun CorporationAssetEmptyText() {
    Text(
        text = stringResource(R.string.corporation_assets_empty),
        color = colorResource(R.color.text_primary),
        fontSize = dimensionResource(R.dimen.type_detail_body_text_size).value.sp,
        modifier = Modifier.padding(
            start = dimensionResource(R.dimen.type_detail_page_title_start_padding),
            end = dimensionResource(R.dimen.detail_card_horizontal_padding),
            top = dimensionResource(R.dimen.type_detail_section_gap),
        ),
    )
}

@Composable
internal fun CorporationAssetSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = dimensionResource(R.dimen.list_section_header_text_size).value.sp,
        fontWeight = FontWeight.Black,
        color = colorResource(R.color.text_primary),
        modifier = Modifier.padding(
            start = dimensionResource(R.dimen.type_detail_page_title_start_padding),
            end = dimensionResource(R.dimen.detail_card_horizontal_padding),
            top = dimensionResource(R.dimen.type_detail_section_gap),
            bottom = dimensionResource(R.dimen.list_section_header_bottom_padding),
        ),
    )
}

@Composable
internal fun CorporationAssetCard(
    isFirst: Boolean,
    isLast: Boolean,
    content: @Composable () -> Unit,
) {
    val radius = dimensionResource(R.dimen.detail_card_corner_radius)
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(radius)
        isFirst -> RoundedCornerShape(topStart = radius, topEnd = radius)
        isLast -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        else -> RoundedCornerShape(0.dp)
    }
    Box(
        modifier = Modifier
            .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding))
            .clip(shape)
            .background(colorResource(R.color.second_background)),
    ) {
        content()
    }
}

internal fun LazyListScope.corporationAssetItemCards(
    sectionKey: String,
    title: String,
    rows: List<CorporationAssetItemRow>,
    onItemClick: (CorporationAssetItemRow) -> Unit,
) {
    item(key = "$sectionKey-title") {
        CorporationAssetSectionTitle(title)
    }
    itemsIndexed(
        items = rows,
        key = { _, row -> "$sectionKey-${row.itemId}" },
    ) { index, row ->
        CorporationAssetCard(isFirst = index == 0, isLast = index == rows.lastIndex) {
            CorporationAssetItemLine(
                row = row,
                showDivider = index < rows.lastIndex,
                onClick = { onItemClick(row) },
            )
        }
    }
}

@Composable
internal fun CorporationAssetItemLine(
    row: CorporationAssetItemRow,
    showDivider: Boolean,
    onClick: () -> Unit,
    pathHint: String? = null,
    localeController: LocaleController = koinInject(),
) {
    val typeName = row.displayName(localeController).ifBlank {
        stringResource(R.string.corporation_assets_unknown_type)
    }
    val multiplierHint = if (row.quantity > 1L) {
        stringResource(
            R.string.corporation_assets_item_multiplier,
            formatAssetCount(row.quantity),
        )
    } else {
        null
    }
    val hints = buildList {
        pathHint?.takeIf { it.isNotBlank() }?.let { path ->
            add(BaseLazyColumnItemHint(text = path))
        }
        row.customName?.takeIf { it.isNotBlank() }?.let { name ->
            add(BaseLazyColumnItemHint(text = name))
        }
        if (row.isOffice) {
            add(
                BaseLazyColumnItemHint(
                    text = stringResource(
                        R.string.corporation_assets_contains_types,
                        formatAssetCount(row.contentTypeCount.toLong()),
                    ),
                ),
            )
        }
    }
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconFileName = row.iconFilename?.takeIf { it.isNotBlank() },
            iconOnLightPlate = !row.iconFilename.isNullOrBlank(),
            showLeadingIcon = !row.iconFilename.isNullOrBlank(),
            itemName = typeName,
            itemNameAnnotated = corporationAssetItemTitle(typeName, multiplierHint),
            itemHints = hints,
            showChevron = true,
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}

@Composable
private fun corporationAssetItemTitle(
    typeName: String,
    multiplierHint: String?,
): AnnotatedString? {
    if (multiplierHint.isNullOrBlank()) return null
    val primaryColor = colorResource(R.color.text_primary)
    val hintColor = colorResource(R.color.hint_text)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = primaryColor)) {
            append(typeName)
        }
        append(CorporationAssetsConfig.SECURITY_STATUS_NAME_GAP)
        withStyle(SpanStyle(color = hintColor)) {
            append(multiplierHint)
        }
    }
}

@Composable
internal fun CorporationAssetPlaceLine(
    iconFilename: String?,
    iconRes: Int? = null,
    securityStatus: Double?,
    placeName: String,
    bracketName: String? = null,
    hint: String,
    showChevron: Boolean,
    showDivider: Boolean,
    onClick: (() -> Unit)?,
) {
    val hasFileIcon = !iconFilename.isNullOrBlank()
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = iconRes ?: R.drawable.ic_database,
            iconFileName = iconFilename?.takeIf { it.isNotBlank() },
            iconOnLightPlate = hasFileIcon,
            showLeadingIcon = hasFileIcon || iconRes != null,
            itemName = placeName,
            itemNameAnnotated = corporationAssetPlaceTitle(
                security = securityStatus,
                placeName = placeName,
                bracketName = bracketName,
            ),
            itemHint = hint,
            showChevron = showChevron,
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}

@Composable
internal fun CorporationAssetCapacityBlock(
    usedVolume: Double,
    capacity: Double?,
) {
    val finiteCapacity = capacity?.takeIf { it > CorporationAssetsConfig.UNLIMITED_CAPACITY_MAX }
    val ratio = if (finiteCapacity == null) {
        CorporationAssetsConfig.CAPACITY_EMPTY_FRACTION.toDouble()
    } else {
        usedVolume / finiteCapacity
    }
    val fraction = ratio.toFloat().coerceIn(
        CorporationAssetsConfig.CAPACITY_EMPTY_FRACTION,
        CorporationAssetsConfig.CAPACITY_FULL_FRACTION,
    )
    val barColor = colorResource(
        when {
            finiteCapacity != null && ratio >= CorporationAssetsConfig.CAPACITY_FULL_RATIO ->
                R.color.corporation_assets_capacity_full
            finiteCapacity != null && ratio >= CorporationAssetsConfig.CAPACITY_WARN_RATIO ->
                R.color.corporation_assets_capacity_warn
            else -> R.color.corporation_assets_capacity_ok
        },
    )
    val usedText = formatAssetCount(usedVolume)
    val percent = floor(ratio * CorporationAssetsConfig.CAPACITY_PERCENT_SCALE).toInt().coerceAtLeast(0)
    val horizontal = dimensionResource(R.dimen.detail_row_horizontal_padding)
    val vertical = dimensionResource(R.dimen.detail_row_vertical_padding_single_line)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontal, vertical = vertical),
    ) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.corporation_assets_capacity_bar_height))
                .clip(
                    RoundedCornerShape(
                        dimensionResource(R.dimen.corporation_assets_capacity_bar_corner),
                    ),
                ),
            color = barColor,
            trackColor = colorResource(R.color.corporation_assets_capacity_free),
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        val labelSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp
        val labelColor = colorResource(R.color.hint_text)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.detail_row_label_subtitle_spacing)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.corporation_assets_contents_volume, usedText),
                color = labelColor,
                fontSize = labelSize,
                lineHeight = labelSize,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
            )
            Text(
                text = if (finiteCapacity == null) {
                    stringResource(R.string.corporation_assets_capacity_unlimited)
                } else {
                    stringResource(
                        R.string.corporation_assets_capacity_volume,
                        formatAssetCount(finiteCapacity),
                    )
                },
                color = labelColor,
                fontSize = labelSize,
                lineHeight = labelSize,
                modifier = Modifier.alignByBaseline(),
            )
            if (finiteCapacity != null) {
                Text(
                    text = stringResource(R.string.corporation_assets_capacity_percent, percent),
                    color = barColor,
                    fontSize = labelSize,
                    lineHeight = labelSize,
                    modifier = Modifier
                        .padding(start = dimensionResource(R.dimen.detail_row_label_subtitle_spacing))
                        .alignByBaseline(),
                )
            }
        }
    }
}

@Composable
internal fun corporationAssetSectionLabel(
    labelRes: Int,
    hangarDivision: Int?,
    hangarCustomName: String?,
): String {
    hangarCustomName?.takeIf { it.isNotBlank() }?.let { return it }
    if (hangarDivision != null) {
        return stringResource(R.string.corporation_assets_hangar_division, hangarDivision)
    }
    return stringResource(labelRes)
}

@Composable
internal fun corporationAssetPlaceName(
    place: LocalizableName,
    kind: CorporationAssetLocationKind,
    localeController: LocaleController,
): String {
    if (kind == CorporationAssetLocationKind.ASSET_SAFETY) {
        return stringResource(R.string.corporation_assets_asset_safety)
    }
    return place.displayName(localeController).ifBlank {
        stringResource(R.string.corporation_assets_unknown_place)
    }
}

internal fun NavController.openCorporationAssetItem(characterId: Long, row: CorporationAssetItemRow) {
    if (row.isContainer) {
        navigate(CorporationAssetsRoute.Container.create(characterId, row.itemId))
    } else if (row.typeId > 0) {
        navigate(DatabaseRoute.TypeDetail.create(row.typeId))
    }
}

@Composable
private fun corporationAssetPlaceTitle(
    security: Double?,
    placeName: String,
    bracketName: String?,
): AnnotatedString? {
    val hasSecurity = security != null
    val hasBracket = !bracketName.isNullOrBlank()
    if (!hasSecurity && !hasBracket) return null
    val primaryColor = colorResource(R.color.text_primary)
    val hintColor = colorResource(R.color.hint_text)
    return buildAnnotatedString {
        if (security != null) {
            withStyle(SpanStyle(color = assetSecurityColor(security))) {
                append(
                    String.format(
                        Locale.US,
                        CorporationAssetsConfig.SYSTEM_SECURITY_FORMAT,
                        security,
                    ),
                )
            }
            append(CorporationAssetsConfig.SECURITY_STATUS_NAME_GAP)
        }
        withStyle(SpanStyle(color = primaryColor)) {
            append(placeName)
        }
        if (hasBracket) {
            withStyle(SpanStyle(color = hintColor)) {
                append(CorporationAssetsConfig.BUILDING_NAME_OPEN)
                append(bracketName)
                append(CorporationAssetsConfig.BUILDING_NAME_CLOSE)
            }
        }
    }
}

@Composable
private fun assetSecurityColor(security: Double): Color = when {
    security <= CorporationAssetsConfig.SECURITY_NEGATIVE_MAX ->
        colorResource(R.color.character_security_negative)
    security < CorporationAssetsConfig.SECURITY_LOW_MAX ->
        colorResource(R.color.character_security_low)
    else -> colorResource(R.color.character_security_high)
}

private fun formatAssetCount(count: Long): String =
    NumberDisplayFormatter.format(count, NumberDisplayFormatter.Style.FULL)

private fun formatAssetCount(count: Double): String =
    NumberDisplayFormatter.format(count, NumberDisplayFormatter.Style.FULL)

@Composable
private fun CorporationAssetStatusBanner(
    permissionDenied: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
) {
    val messageRes = when {
        permissionDenied -> R.string.corporation_assets_permission_denied
        loadFailed -> R.string.corporation_assets_load_failed
        else -> return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding)),
        verticalAlignment = Alignment.CenterVertically,
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
