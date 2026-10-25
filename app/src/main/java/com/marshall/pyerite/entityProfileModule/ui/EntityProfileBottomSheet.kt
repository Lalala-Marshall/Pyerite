package com.marshall.pyerite.entityProfileModule.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.marshall.pyerite.R
import com.marshall.pyerite.entityProfileModule.data.EveDescriptionDocument
import com.marshall.pyerite.entityProfileModule.data.parseEveEntityDescription
import com.marshall.pyerite.entityProfileModule.model.EntityEmploymentEntry
import com.marshall.pyerite.entityProfileModule.model.EntityProfile
import com.marshall.pyerite.entityProfileModule.model.EntityProfileConfig
import com.marshall.pyerite.entityProfileModule.model.EntityStandingRow
import com.marshall.pyerite.entityProfileModule.model.EntityStandingSection
import com.marshall.pyerite.entityProfileModule.model.EntityStandingSectionKind
import com.marshall.pyerite.entityProfileModule.viewModel.EntityProfileViewModel
import com.marshall.pyerite.eveAuthModule.sso.EveSsoConfig
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemHint
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.CharacterAvatar
import com.marshall.pyerite.ui.golbalComponents.PyeriteIconShape
import com.marshall.pyerite.ui.golbalComponents.PyeriteSegmentedControl
import com.marshall.pyerite.ui.golbalComponents.PyeriteSegmentedOption
import com.marshall.pyerite.ui.golbalComponents.UniverseEntityKind
import com.marshall.pyerite.ui.golbalComponents.UniverseEntityRef
import com.marshall.pyerite.util.DurationDisplayFormatter
import com.marshall.pyerite.util.formatDurationDisplay
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private enum class EntityProfileTab {
    STANDINGS,
    EMPLOYMENT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntityProfileBottomSheet(
    viewModel: EntityProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val page = uiState.current ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetCorner = dimensionResource(R.dimen.character_mail_compose_sheet_corner)
    val sheetBackground = colorResource(R.color.search_field_idle_background)
    val sheetHorizontalPadding =
        dimensionResource(R.dimen.character_mail_compose_sheet_horizontal_padding)

    ModalBottomSheet(
        onDismissRequest = viewModel::dismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        shape = RoundedCornerShape(topStart = sheetCorner, topEnd = sheetCorner),
        containerColor = sheetBackground,
        scrimColor = colorResource(R.color.search_scrim),
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(EntityProfileConfig.HEIGHT_FRACTION)
                .background(sheetBackground)
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = sheetHorizontalPadding,
                    end = sheetHorizontalPadding,
                    top = dimensionResource(R.dimen.character_mail_compose_header_top_padding),
                ),
            ) {
                EntityProfileSheetHeader(
                    canPop = uiState.canPop,
                    onBack = viewModel::pop,
                    onDone = viewModel::dismiss,
                )
            }
            EntityProfileSheetBody(
                pageRef = page.ref,
                profile = page.profile,
                isLoading = page.isLoading,
                loadFailed = page.loadFailed,
                onRetry = viewModel::retry,
                onOpenEntity = viewModel::openChild,
            )
        }
    }
}

@Composable
private fun EntityProfileSheetHeader(
    canPop: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val buttonHeight = dimensionResource(R.dimen.top_bar_back_button_size)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight),
    ) {
        if (canPop) {
            EntityProfileHeaderAction(
                label = stringResource(R.string.entity_profile_back),
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        EntityProfileDonePill(
            onClick = onDone,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun EntityProfileHeaderAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = colorResource(R.color.hyperlink_text),
        fontSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { role = Role.Button },
    )
}

@Composable
private fun EntityProfileDonePill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.entity_profile_done),
        color = colorResource(R.color.text_primary),
        fontSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.entity_profile_done_pill_corner)))
            .background(colorResource(R.color.skill_plan_dialog_button_background))
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.entity_profile_done_pill_horizontal_padding),
                vertical = dimensionResource(R.dimen.entity_profile_done_pill_vertical_padding),
            )
            .semantics { role = Role.Button },
    )
}

@Composable
private fun ColumnScope.EntityProfileSheetBody(
    pageRef: UniverseEntityRef,
    profile: EntityProfile?,
    isLoading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onOpenEntity: (UniverseEntityRef) -> Unit,
) {
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    val scrollState = rememberScrollState()
    val rawDescription = profile?.descriptionHtml.orEmpty()
    val descriptionDocument = remember(rawDescription) { parseEveEntityDescription(rawDescription) }
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = bottomPadding),
    ) {
        if (loadFailed && profile == null) {
            EntityProfileLoadFailed(onRetry = onRetry)
            return
        }
        if (profile == null && isLoading) {
            Text(
                text = stringResource(R.string.character_sheet_loading),
                color = colorResource(R.color.hint_text),
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding),
                    vertical = sectionGap,
                ),
            )
            return
        }
        val loaded = profile ?: return
        Spacer(modifier = Modifier.height(sectionGap))
        EntityProfileHeaderSection(
            profile = loaded,
            onOpenEntity = onOpenEntity,
        )
        Spacer(modifier = Modifier.height(sectionGap))
        EntityProfileLinksCard(profile = loaded)
        if (descriptionDocument.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(sectionGap))
            EntityProfileDescriptionCard(
                document = descriptionDocument,
                onOpenEntity = onOpenEntity,
            )
        }
        Spacer(modifier = Modifier.height(sectionGap))
        EntityProfileDetailsCard(
            pageRef = pageRef,
            profile = loaded,
            onOpenEntity = onOpenEntity,
        )
    }
}

@Composable
private fun EntityProfileLoadFailed(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding),
                vertical = dimensionResource(R.dimen.type_detail_section_gap),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.entity_profile_load_failed),
            color = colorResource(R.color.text_primary),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.entity_profile_retry))
        }
    }
}

@Composable
private fun EntityProfileHeaderSection(
    profile: EntityProfile,
    onOpenEntity: (UniverseEntityRef) -> Unit,
) {
    val context = LocalContext.current
    val copySuccess = stringResource(R.string.entity_profile_copy_id_success)
    val copyLabel = stringResource(R.string.entity_profile_copy_id)
    val nonePlaceholder = stringResource(R.string.character_org_none_placeholder)
    val avatarSize = dimensionResource(R.dimen.entity_profile_header_avatar_size)
    val nameGap = dimensionResource(R.dimen.entity_profile_header_name_gap)
    val orgGap = dimensionResource(R.dimen.entity_profile_header_org_gap)
    val showNpcTag = profile.isNpcCorporation &&
        profile.ref.kind == UniverseEntityKind.CORPORATION
    val corpClick = profile.corporationId
        ?.takeIf { profile.ref.kind != UniverseEntityKind.CORPORATION }
        ?.let { id ->
            { onOpenEntity(UniverseEntityRef(UniverseEntityKind.CORPORATION, id)) }
        }
    val allianceClick = profile.allianceId
        ?.takeIf { profile.ref.kind != UniverseEntityKind.ALLIANCE }
        ?.let { id ->
            { onOpenEntity(UniverseEntityRef(UniverseEntityKind.ALLIANCE, id)) }
        }

    Column {
        BaseContainer(title = null, useSystemBarsPadding = false) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.detail_row_horizontal_padding),
                        vertical = dimensionResource(R.dimen.detail_row_vertical_padding_single_line),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CharacterAvatar(
                    portraitUrl = profile.iconUrl,
                    size = avatarSize,
                    shape = PyeriteIconShape.shape,
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.detail_row_icon_gap)))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            color = colorResource(R.color.text_primary),
                            fontWeight = FontWeight.Bold,
                            fontSize = dimensionResource(R.dimen.entity_profile_header_name_text_size).value.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (showNpcTag) {
                            Spacer(
                                modifier = Modifier.width(
                                    dimensionResource(R.dimen.entity_profile_npc_tag_gap),
                                ),
                            )
                            EntityProfileNpcTag()
                        }
                    }
                    if (profile.isCeo) {
                        Spacer(modifier = Modifier.height(nameGap))
                        Text(
                            text = stringResource(R.string.entity_profile_ceo),
                            color = colorResource(R.color.text_primary),
                            fontSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp,
                        )
                    }
                    if (profile.ref.kind != UniverseEntityKind.CORPORATION) {
                        Spacer(modifier = Modifier.height(orgGap))
                        EntityProfileOrgLine(
                            name = profile.corporationName?.takeIf { it.isNotBlank() }
                                ?: nonePlaceholder,
                            iconUrl = profile.corporationIconUrl,
                            onClick = corpClick,
                        )
                    }
                    if (profile.ref.kind != UniverseEntityKind.ALLIANCE &&
                        (profile.allianceId != null || !profile.allianceName.isNullOrBlank())
                    ) {
                        Spacer(modifier = Modifier.height(orgGap))
                        EntityProfileOrgLine(
                            name = profile.allianceName?.takeIf { it.isNotBlank() }
                                ?: nonePlaceholder,
                            iconUrl = profile.allianceIconUrl,
                            onClick = allianceClick,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.detail_card_horizontal_padding),
                    end = dimensionResource(R.dimen.detail_card_horizontal_padding),
                    top = dimensionResource(R.dimen.entity_profile_header_id_top_gap),
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier = Modifier.clickable {
                    copyEntityId(context, profile.ref.id, copyLabel, copySuccess)
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.entity_profile_id, profile.ref.id),
                    color = colorResource(R.color.hyperlink_text),
                    fontSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp,
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.entity_profile_copy_icon_gap)))
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = copyLabel,
                    tint = colorResource(R.color.hyperlink_text),
                    modifier = Modifier.size(dimensionResource(R.dimen.entity_profile_copy_icon_size)),
                )
            }
        }
    }
}

@Composable
private fun EntityProfileOrgLine(
    name: String,
    iconUrl: String?,
    onClick: (() -> Unit)?,
) {
    val rowModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CharacterAvatar(
            portraitUrl = iconUrl,
            size = dimensionResource(R.dimen.base_lazy_column_item_hint_icon_size),
            shape = PyeriteIconShape.shape,
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.base_lazy_column_item_hint_icon_gap)))
        Text(
            text = name,
            color = colorResource(
                if (onClick != null) R.color.hyperlink_text else R.color.text_primary,
            ),
            fontSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EntityProfileLinksCard(profile: EntityProfile) {
    val context = LocalContext.current
    BaseContainer(title = null, useSystemBarsPadding = false) {
        Column {
            if (profile.factionName != null) {
                BaseLazyColumnItem(
                    model = BaseLazyColumnItemModel(
                        iconUrl = profile.factionIconUrl,
                        itemName = stringResource(R.string.entity_profile_faction),
                        itemHints = listOf(
                            BaseLazyColumnItemHint(
                                text = profile.factionName,
                                color = colorResource(R.color.text_primary),
                            ),
                        ),
                        showChevron = false,
                        onClick = null,
                    ),
                    showDivider = true,
                )
            }
            EntityProfileExternalLinkRow(
                label = stringResource(R.string.entity_profile_evewho),
                url = EntityProfileConfig.eveWhoUrl(profile.ref.kind, profile.ref.id),
                showDivider = true,
                onOpen = { openExternalUrl(context, it) },
            )
            EntityProfileExternalLinkRow(
                label = stringResource(R.string.entity_profile_zkillboard),
                url = EntityProfileConfig.zKillUrl(profile.ref.kind, profile.ref.id),
                showDivider = false,
                onOpen = { openExternalUrl(context, it) },
            )
        }
    }
}

@Composable
private fun EntityProfileExternalLinkRow(
    label: String,
    url: String,
    showDivider: Boolean,
    onOpen: (String) -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            showLeadingIcon = false,
            itemName = label,
            itemNameColor = colorResource(R.color.hyperlink_text),
            showChevron = false,
            onClick = { onOpen(url) },
        ),
        showDivider = showDivider,
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = stringResource(R.string.entity_profile_open_external),
                tint = colorResource(R.color.hyperlink_text),
                modifier = Modifier.size(dimensionResource(R.dimen.entity_profile_link_icon_size)),
            )
        },
    )
}

@Composable
private fun EntityProfileDescriptionCard(
    document: EveDescriptionDocument,
    onOpenEntity: (UniverseEntityRef) -> Unit,
) {
    val context = LocalContext.current
    val defaultColor = colorResource(R.color.text_primary)
    val linkColor = colorResource(R.color.hyperlink_text)
    val background = colorResource(R.color.second_background)
    val baseFontSize = dimensionResource(R.dimen.type_detail_body_text_size).value.sp
    val annotated = remember(document, defaultColor, linkColor, background, baseFontSize, context, onOpenEntity) {
        document.toAnnotatedString(
            defaultColor = defaultColor,
            linkColor = linkColor,
            background = background,
            baseFontSize = baseFontSize,
            onWebLink = { url -> openExternalUrl(context, url) },
            onEntityLink = onOpenEntity,
        )
    }
    BaseContainer(
        title = stringResource(R.string.entity_profile_description),
        useSystemBarsPadding = false,
    ) {
        SelectionContainer(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = annotated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.entity_profile_description_padding)),
                color = defaultColor,
                fontSize = baseFontSize,
                lineHeight = document.preferredLineHeight(baseFontSize),
            )
        }
    }
}

@Composable
private fun EntityProfileDetailsCard(
    pageRef: UniverseEntityRef,
    profile: EntityProfile,
    onOpenEntity: (UniverseEntityRef) -> Unit,
) {
    val showEmployment = profile.ref.kind == UniverseEntityKind.CHARACTER
    var selectedTab by remember(pageRef) {
        mutableStateOf(EntityProfileTab.STANDINGS)
    }
    BaseContainer(title = null, useSystemBarsPadding = false) {
        Column {
            if (showEmployment) {
                PyeriteSegmentedControl(
                    options = listOf(
                        PyeriteSegmentedOption(
                            EntityProfileTab.STANDINGS,
                            stringResource(R.string.entity_profile_tab_standings),
                        ),
                        PyeriteSegmentedOption(
                            EntityProfileTab.EMPLOYMENT,
                            stringResource(R.string.entity_profile_tab_employment),
                        ),
                    ),
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                )
            }
            when {
                selectedTab == EntityProfileTab.EMPLOYMENT && showEmployment -> {
                    EntityProfileEmploymentList(
                        entries = profile.employment,
                        detailsReady = profile.detailsReady,
                        onOpenEntity = onOpenEntity,
                    )
                }
                else -> {
                    EntityProfileStandingsList(
                        sections = profile.standingSections,
                        detailsReady = profile.detailsReady,
                    )
                }
            }
        }
    }
}

@Composable
private fun EntityProfileStandingsList(
    sections: List<EntityStandingSection>,
    detailsReady: Boolean,
) {
    if (!detailsReady && sections.isEmpty()) {
        EntityProfilePendingPlaceholder()
        return
    }
    Column {
        sections.forEachIndexed { index, section ->
            Text(
                text = standingSectionTitle(section.kind),
                color = colorResource(R.color.hint_text),
                fontSize = dimensionResource(R.dimen.entity_profile_section_title_text_size).value.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.entity_profile_section_title_padding),
                    end = dimensionResource(R.dimen.entity_profile_section_title_padding),
                    top = dimensionResource(R.dimen.entity_profile_section_title_padding),
                    bottom = dimensionResource(R.dimen.detail_row_label_subtitle_spacing),
                ),
            )
            section.rows.forEach { row ->
                EntityProfileStandingRow(row = row)
            }
            if (index != sections.lastIndex) {
                HorizontalDivider(
                    thickness = dimensionResource(R.dimen.detail_divider_thickness),
                    color = colorResource(R.color.border),
                )
            }
        }
    }
}

@Composable
private fun standingSectionTitle(kind: EntityStandingSectionKind): String = when (kind) {
    EntityStandingSectionKind.PERSONAL -> stringResource(R.string.entity_profile_standings_personal)
    EntityStandingSectionKind.CORPORATION ->
        stringResource(R.string.entity_profile_standings_corporation)
    EntityStandingSectionKind.ALLIANCE -> stringResource(R.string.entity_profile_standings_alliance)
}

@Composable
private fun EntityProfileStandingRow(row: EntityStandingRow) {
    val avatarSize = dimensionResource(R.dimen.entity_profile_standing_avatar_size)
    val nameGap = dimensionResource(R.dimen.entity_profile_standing_name_gap)
    val valuePadding = dimensionResource(R.dimen.entity_profile_standing_value_horizontal_padding)
    val nameTextSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp
    val nameColor = colorResource(R.color.text_primary)
    val standingValueTextSize = dimensionResource(R.dimen.sub_menu_value_text_size).value.sp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.detail_row_horizontal_padding),
                vertical = dimensionResource(R.dimen.entity_profile_standing_row_vertical_padding),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.from.name,
            color = nameColor,
            fontSize = nameTextSize,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(nameGap))
        CharacterAvatar(
            portraitUrl = row.from.iconUrl,
            size = avatarSize,
            shape = PyeriteIconShape.shape,
        )
        Box(
            modifier = Modifier.padding(horizontal = valuePadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = EntityProfileConfig.STANDING_COLUMN_SAMPLE,
                fontWeight = FontWeight.SemiBold,
                fontSize = standingValueTextSize,
                color = Color.Transparent,
                modifier = Modifier.clearAndSetSemantics { },
            )
            Text(
                text = EntityProfileConfig.formatStanding(row.standing),
                color = standingValueColor(row.standing),
                fontWeight = FontWeight.SemiBold,
                fontSize = standingValueTextSize,
                textAlign = TextAlign.Center,
            )
        }
        CharacterAvatar(
            portraitUrl = row.toward.iconUrl,
            size = avatarSize,
            shape = PyeriteIconShape.shape,
        )
        Spacer(modifier = Modifier.width(nameGap))
        Text(
            text = row.toward.name,
            color = nameColor,
            fontSize = nameTextSize,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun standingValueColor(value: Double): Color = when {
    abs(value) < EntityProfileConfig.STANDING_ZERO_EPSILON ->
        colorResource(R.color.text_primary)
    value > 0.0 -> colorResource(R.color.character_status_positive)
    else -> colorResource(R.color.character_security_negative)
}

@Composable
private fun EntityProfileEmploymentList(
    entries: List<EntityEmploymentEntry>,
    detailsReady: Boolean,
    onOpenEntity: (UniverseEntityRef) -> Unit,
) {
    if (!detailsReady && entries.isEmpty()) {
        EntityProfilePendingPlaceholder()
        return
    }
    Column {
        entries.forEachIndexed { index, entry ->
            EntityProfileEmploymentRow(
                entry = entry,
                showDivider = index != entries.lastIndex,
                onOpenEntity = onOpenEntity,
            )
        }
    }
}

@Composable
private fun EntityProfileEmploymentRow(
    entry: EntityEmploymentEntry,
    showDivider: Boolean,
    onOpenEntity: (UniverseEntityRef) -> Unit,
) {
    val present = stringResource(R.string.entity_profile_employment_present)
    val placeholder = stringResource(R.string.character_sheet_value_placeholder)
    val startText = formatEntityProfileDate(entry.startEpochMs)
    val endText = entry.endEpochMs?.let(::formatEntityProfileDate) ?: present
    val endMs = entry.endEpochMs ?: System.currentTimeMillis()
    val durationSeconds = ((endMs - entry.startEpochMs).coerceAtLeast(0L)) /
        EveSsoConfig.MILLIS_PER_SECOND
    val durationText = formatDurationDisplay(
        totalSeconds = durationSeconds,
        includeSeconds = false,
        maxUnit = DurationDisplayFormatter.MaxUnit.DAY,
    )
    val rangeText = stringResource(
        R.string.entity_profile_employment_range,
        startText,
        endText,
        durationText,
    )
    val corpName = entry.corporationName.ifBlank { placeholder }
    val allianceClick = entry.allianceId?.let { id ->
        { onOpenEntity(UniverseEntityRef(UniverseEntityKind.ALLIANCE, id)) }
    }
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconUrl = entry.corporationIconUrl,
            iconSize = dimensionResource(R.dimen.base_lazy_column_item_icon_size),
            itemName = corpName,
            itemNameColor = colorResource(R.color.hyperlink_text),
            itemHints = buildList {
                entry.allianceName?.takeIf { it.isNotBlank() }?.let { allianceName ->
                    add(
                        BaseLazyColumnItemHint(
                            text = allianceName,
                            color = colorResource(
                                if (allianceClick != null) {
                                    R.color.hyperlink_text
                                } else {
                                    R.color.text_primary
                                },
                            ),
                            iconUrl = entry.allianceIconUrl,
                            onClick = allianceClick,
                        ),
                    )
                }
                add(BaseLazyColumnItemHint(text = rangeText))
            },
            showChevron = false,
            onClick = null,
            alignHintLeadingColumn = false,
            itemNameOnClick = {
                onOpenEntity(
                    UniverseEntityRef(UniverseEntityKind.CORPORATION, entry.corporationId),
                )
            },
        ),
        showDivider = showDivider,
        titleTrailingContent = if (entry.isNpc) {
            { EntityProfileNpcTag() }
        } else {
            null
        },
    )
}

@Composable
private fun EntityProfileNpcTag() {
    val tagTextSize = dimensionResource(R.dimen.entity_profile_npc_tag_text_size).value.sp
    Text(
        text = stringResource(R.string.entity_profile_npc_tag),
        color = colorResource(R.color.white),
        fontSize = tagTextSize,
        lineHeight = tagTextSize,
        fontWeight = FontWeight.SemiBold,
        style = TextStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.entity_profile_npc_tag_corner)))
            .background(colorResource(R.color.entity_profile_npc_tag))
            .padding(
                horizontal = dimensionResource(R.dimen.entity_profile_npc_tag_horizontal_padding),
                vertical = dimensionResource(R.dimen.entity_profile_npc_tag_vertical_padding),
            ),
    )
}

@Composable
private fun EntityProfilePendingPlaceholder() {
    Text(
        text = stringResource(R.string.character_sheet_loading),
        color = colorResource(R.color.hint_text),
        modifier = Modifier.padding(dimensionResource(R.dimen.entity_profile_section_title_padding)),
    )
}

private fun formatEntityProfileDate(epochMs: Long): String {
    return SimpleDateFormat(EntityProfileConfig.DISPLAY_DATE_PATTERN, Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }.format(Date(epochMs))
}

private fun copyEntityId(
    context: Context,
    id: Long,
    label: String,
    successMessage: String,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, id.toString()))
    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
}

private fun openExternalUrl(context: Context, url: String) {
    val uri = url.toUri()
    runCatching {
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    }.onFailure {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
