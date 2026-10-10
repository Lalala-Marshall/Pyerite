package com.marshall.pyerite.personalAssetsModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.marshall.pyerite.databaseHierarchyModule.navHost.DatabaseRoute
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.LocalizableName
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.esiModule.data.portraitUrl
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetItemRow
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetLocationKind
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetOwnerRef
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsConfig
import com.marshall.pyerite.personalAssetsModule.navHost.PersonalAssetsRoute
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.CharacterAvatar
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeriteIconShape
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.PyeritePullToRefreshBox
import com.marshall.pyerite.ui.golbalComponents.PyeriteTopBarActionItem
import com.marshall.pyerite.ui.golbalComponents.pyeritePullRefreshTopBarAction
import com.marshall.pyerite.ui.golbalComponents.rememberLazyListTitleCollapsed
import com.marshall.pyerite.util.NumberDisplayFormatter
import org.koin.compose.koinInject
import java.util.Locale
import kotlin.math.floor

@Composable
internal fun PersonalAssetPageScaffold(
    title: String,
    isLoading: Boolean,
    loadFailed: Boolean,
    permissionDenied: Boolean,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)?,
    showPageTitle: Boolean = true,
    scrollResultsToTop: Boolean = false,
    extraEndActions: List<PyeriteTopBarActionItem> = emptyList(),
    showLoadingIcon: Boolean = false,
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
        endActions = listOfNotNull(refreshAction) + extraEndActions,
    ) { topBarPadding ->
        if (bottomBar == null) {
            PyeritePullToRefreshBox(
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(topBarPadding)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) {
                PersonalAssetList(
                    listState = listState,
                    title = title,
                    showPageTitle = showPageTitle,
                    permissionDenied = permissionDenied,
                    loadFailed = loadFailed,
                    showLoadingIcon = showLoadingIcon,
                    onRefresh = onRefresh,
                    content = content,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(topBarPadding)
                    .personalAssetsBottomInset(),
            ) {
                PyeritePullToRefreshBox(
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    PersonalAssetList(
                        listState = listState,
                        title = title,
                        showPageTitle = showPageTitle,
                        permissionDenied = permissionDenied,
                        loadFailed = loadFailed,
                        showLoadingIcon = showLoadingIcon,
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
private fun Modifier.personalAssetsBottomInset(): Modifier =
    windowInsetsPadding(
        WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom),
    )

@Composable
private fun PersonalAssetList(
    listState: LazyListState,
    title: String,
    showPageTitle: Boolean,
    permissionDenied: Boolean,
    loadFailed: Boolean,
    showLoadingIcon: Boolean,
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
                PersonalAssetStatusBanner(
                    permissionDenied = permissionDenied,
                    loadFailed = loadFailed,
                    onRetry = onRefresh,
                )
            }
        }
        if (showLoadingIcon && !permissionDenied) {
            item(key = "loading_icon") { PersonalAssetLoadingIcon() }
        }
        content()
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.type_detail_section_gap)))
        }
    }
}

@Composable
internal fun PersonalAssetLoadingIcon() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.type_detail_section_gap)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(dimensionResource(R.dimen.character_pull_refresh_icon_size)),
            color = colorResource(R.color.hyperlink_text),
        )
    }
}

@Composable
internal fun PersonalAssetEmptyText() {
    Text(
        text = stringResource(R.string.personal_assets_empty),
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
internal fun PersonalAssetSectionTitle(title: String) {
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
internal fun PersonalAssetCard(
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

internal fun LazyListScope.personalAssetItemCards(
    sectionKey: String,
    title: String,
    rows: List<PersonalAssetItemRow>,
    onItemClick: (PersonalAssetItemRow) -> Unit,
) {
    item(key = "$sectionKey-title") {
        PersonalAssetSectionTitle(title)
    }
    itemsIndexed(
        items = rows,
        key = { _, row -> "$sectionKey-${row.itemId}" },
    ) { index, row ->
        PersonalAssetCard(isFirst = index == 0, isLast = index == rows.lastIndex) {
            PersonalAssetItemLine(
                row = row,
                showDivider = index < rows.lastIndex,
                onClick = { onItemClick(row) },
            )
        }
    }
}

@Composable
internal fun PersonalAssetItemLine(
    row: PersonalAssetItemRow,
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
    }
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconFileName = row.iconFilename?.takeIf { it.isNotBlank() },
            iconOnLightPlate = !row.iconFilename.isNullOrBlank(),
            showLeadingIcon = !row.iconFilename.isNullOrBlank(),
            itemName = typeName,
            itemNameAnnotated = personalAssetItemTitle(typeName, multiplierHint),
            itemHints = hints,
            showChevron = true,
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}

@Composable
internal fun PersonalAssetPlaceLine(
    iconFilename: String?,
    securityStatus: Double?,
    placeName: String,
    hint: String,
    owners: List<PersonalAssetOwnerRef> = emptyList(),
    avatarsOnly: Boolean = false,
    showChevron: Boolean,
    showDivider: Boolean,
    onClick: (() -> Unit)?,
) {
    val hasFileIcon = !iconFilename.isNullOrBlank()
    val hints = buildList {
        if (avatarsOnly && owners.isNotEmpty()) {
            add(
                BaseLazyColumnItemHint(
                    iconUrls = owners.map { owner -> portraitUrl(owner.characterId) },
                ),
            )
        } else {
            owners.firstOrNull()?.let { owner ->
                add(
                    BaseLazyColumnItemHint(
                        text = owner.name,
                        iconUrl = portraitUrl(owner.characterId),
                    ),
                )
            }
        }
        if (hint.isNotBlank()) add(BaseLazyColumnItemHint(text = hint))
    }
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_database,
            iconFileName = iconFilename?.takeIf { it.isNotBlank() },
            iconOnLightPlate = hasFileIcon,
            showLeadingIcon = hasFileIcon,
            itemName = placeName,
            itemNameAnnotated = personalAssetPlaceTitle(
                security = securityStatus,
                placeName = placeName,
            ),
            itemHints = hints,
            alignHintLeadingColumn = !avatarsOnly && owners.isNotEmpty(),
            showChevron = showChevron,
            onClick = onClick,
        ),
        showDivider = showDivider,
    )
}

@Composable
internal fun PersonalAssetCharacterLine(
    name: String,
    portraitUrl: String?,
    hint: String,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = name,
            itemHint = hint,
            showChevron = true,
            onClick = onClick,
        ),
        showDivider = showDivider,
        leadingContent = { iconSize ->
            CharacterAvatar(
                portraitUrl = portraitUrl,
                size = iconSize,
                shape = PyeriteIconShape.shape,
            )
        },
    )
}

@Composable
internal fun PersonalAssetCapacityBlock(
    usedVolume: Double,
    capacity: Double?,
) {
    val finiteCapacity = capacity?.takeIf { it > PersonalAssetsConfig.UNLIMITED_CAPACITY_MAX }
    val ratio = if (finiteCapacity == null) {
        PersonalAssetsConfig.CAPACITY_EMPTY_FRACTION.toDouble()
    } else {
        usedVolume / finiteCapacity
    }
    val fraction = ratio.toFloat().coerceIn(
        PersonalAssetsConfig.CAPACITY_EMPTY_FRACTION,
        PersonalAssetsConfig.CAPACITY_FULL_FRACTION,
    )
    val barColor = colorResource(
        when {
            finiteCapacity != null && ratio >= PersonalAssetsConfig.CAPACITY_FULL_RATIO ->
                R.color.corporation_assets_capacity_full
            finiteCapacity != null && ratio >= PersonalAssetsConfig.CAPACITY_WARN_RATIO ->
                R.color.corporation_assets_capacity_warn
            else -> R.color.corporation_assets_capacity_ok
        },
    )
    val usedText = formatAssetCount(usedVolume)
    val percent = floor(ratio * PersonalAssetsConfig.CAPACITY_PERCENT_SCALE).toInt().coerceAtLeast(0)
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
                    stringResource(R.string.corporation_assets_capacity_volume, formatAssetCount(finiteCapacity))
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
internal fun personalAssetSectionLabel(
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
internal fun personalAssetPlaceName(
    place: LocalizableName,
    kind: PersonalAssetLocationKind,
    localeController: LocaleController,
): String {
    if (kind == PersonalAssetLocationKind.ASSET_SAFETY) {
        return stringResource(R.string.corporation_assets_asset_safety)
    }
    return place.displayName(localeController).ifBlank {
        stringResource(R.string.corporation_assets_unknown_place)
    }
}

internal fun NavController.openPersonalAssetItem(characterId: Long, row: PersonalAssetItemRow) {
    if (row.isContainer) {
        navigate(PersonalAssetsRoute.Container.create(characterId, row.itemId))
    } else if (row.typeId > 0) {
        navigate(DatabaseRoute.TypeDetail.create(row.typeId))
    }
}

@Composable
private fun personalAssetItemTitle(typeName: String, multiplierHint: String?): AnnotatedString? {
    if (multiplierHint.isNullOrBlank()) return null
    val primaryColor = colorResource(R.color.text_primary)
    val hintColor = colorResource(R.color.hint_text)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = primaryColor)) { append(typeName) }
        append(PersonalAssetsConfig.SECURITY_STATUS_NAME_GAP)
        withStyle(SpanStyle(color = hintColor)) { append(multiplierHint) }
    }
}

@Composable
private fun personalAssetPlaceTitle(security: Double?, placeName: String): AnnotatedString? {
    if (security == null) return null
    val primaryColor = colorResource(R.color.text_primary)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = assetSecurityColor(security))) {
            append(String.format(Locale.US, PersonalAssetsConfig.SYSTEM_SECURITY_FORMAT, security))
        }
        append(PersonalAssetsConfig.SECURITY_STATUS_NAME_GAP)
        withStyle(SpanStyle(color = primaryColor)) { append(placeName) }
    }
}

@Composable
private fun assetSecurityColor(security: Double): Color = when {
    security <= PersonalAssetsConfig.SECURITY_NEGATIVE_MAX ->
        colorResource(R.color.character_security_negative)
    security < PersonalAssetsConfig.SECURITY_LOW_MAX ->
        colorResource(R.color.character_security_low)
    else -> colorResource(R.color.character_security_high)
}

private fun formatAssetCount(count: Long): String =
    NumberDisplayFormatter.format(count, NumberDisplayFormatter.Style.FULL)

private fun formatAssetCount(count: Double): String =
    NumberDisplayFormatter.format(count, NumberDisplayFormatter.Style.FULL)

@Composable
private fun PersonalAssetStatusBanner(
    permissionDenied: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
) {
    val messageRes = when {
        permissionDenied -> R.string.personal_assets_permission_denied
        loadFailed -> R.string.personal_assets_load_failed
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
