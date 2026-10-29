package com.marshall.pyerite.databaseHierarchyModule.typeDetailPage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.marshall.pyerite.R
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.displayName
import com.marshall.pyerite.localization.opposite
import com.marshall.pyerite.localization.translatedName
import com.marshall.pyerite.sdeModule.room.type.TypeEntity
import com.marshall.pyerite.sdeModule.room.dogma.TypeTraitDetail
import com.marshall.pyerite.databaseHierarchyModule.util.formatTraitBonusAnnotated
import com.marshall.pyerite.databaseHierarchyModule.util.formatTypeDescriptionAnnotated
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.CopyNameLongPressHost
import com.marshall.pyerite.ui.golbalComponents.PyeriteIconShape
import org.koin.compose.koinInject

private data class TraitBlocks(
    val roleTraits: List<TypeTraitDetail>,
    val skillTraitGroups: List<Pair<Int, List<TypeTraitDetail>>>,
)

private fun buildTraitBlocks(traits: List<TypeTraitDetail>): TraitBlocks {
    if (traits.isEmpty()) return TraitBlocks(emptyList(), emptyList())
    val sorted = traits.sortedWith(
        compareBy<TypeTraitDetail> { it.importance ?: Int.MAX_VALUE }
            .thenBy { it.skill }
    )
    val roleTraits = sorted.filter { it.skill == -1 }
    val skillIds = sorted.filter { it.skill > 0 }.map { it.skill }.distinct()
    val skillTraitGroups = skillIds
        .sortedBy { sid ->
            sorted.filter { it.skill == sid }
                .minOfOrNull { it.importance ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
        }
        .map { sid -> sid to sorted.filter { it.skill == sid } }
    return TraitBlocks(roleTraits, skillTraitGroups)
}

@Composable
fun TypeSummarySection(
    entity: TypeEntity,
    traits: List<TypeTraitDetail>,
    iconManager: IconManager = koinInject(),
    localeController: LocaleController = koinInject(),
) {
    val linkColor = colorResource(R.color.hyperlink_text)
    val traitBlocks = remember(traits) { buildTraitBlocks(traits) }
    val hasTraitUi = traitBlocks.roleTraits.isNotEmpty() || traitBlocks.skillTraitGroups.isNotEmpty()
    val cardPadding = dimensionResource(R.dimen.detail_card_horizontal_padding)
    val summaryIconSize = dimensionResource(R.dimen.type_detail_summary_icon_size)
    val summaryContentGap = dimensionResource(R.dimen.type_detail_summary_content_gap)
    val summaryTitleTextSize = dimensionResource(R.dimen.type_detail_summary_title_text_size).value.sp
    val summaryCaptionTextSize = dimensionResource(R.dimen.type_detail_summary_caption_text_size).value.sp
    val bodyTextSize = dimensionResource(R.dimen.type_detail_body_text_size).value.sp
    val bodyLineHeight = dimensionResource(R.dimen.type_detail_body_line_height).value.sp
    val dividerVerticalPadding = dimensionResource(R.dimen.type_detail_summary_divider_vertical_padding)
    val dividerThickness = dimensionResource(R.dimen.detail_divider_thickness)
    val innerGapLarge = dimensionResource(R.dimen.type_detail_section_inner_gap_large)
    val innerGapMedium = dimensionResource(R.dimen.type_detail_section_inner_gap_medium)
    val innerGapSmall = dimensionResource(R.dimen.type_detail_section_inner_gap_small)
    val contentLanguage = localeController.contentLanguage
    val typeName = entity.displayName(localeController)
    val typeTranslation = entity.translatedName(contentLanguage)

    BaseContainer(useSystemBarsPadding = false) {
        CopyNameLongPressHost(
            name = typeName,
            translation = typeTranslation,
            translationLanguage = contentLanguage.opposite(),
            modifier = Modifier.padding(cardPadding),
            header = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier
                        .size(summaryIconSize)
                        .clip(PyeriteIconShape.shape),
                    painter = entity.iconFilename?.let {
                        rememberAsyncImagePainter(iconManager.getIconFile(it))
                    } ?: painterResource(R.drawable.ic_database),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(summaryContentGap))
                Column {
                    Text(
                        text = typeName,
                        fontSize = summaryTitleTextSize,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_primary)
                    )
                    Text(
                        text = "${entity.categoryName ?: ""} / ${entity.groupName ?: ""} / ID:${entity.id}",
                        fontSize = summaryCaptionTextSize,
                        color = colorResource(R.color.hint_text)
                    )
                }
            }
            },
        ) {
            val desc = entity.description
            if (!desc.isNullOrBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = dividerVerticalPadding),
                    thickness = dividerThickness,
                    color = colorResource(R.color.border)
                )
                Text(
                    text = formatTypeDescriptionAnnotated(desc),
                    fontSize = bodyTextSize,
                    color = colorResource(R.color.text_primary),
                    lineHeight = bodyLineHeight,
                )
            }

            if (hasTraitUi) {
                Spacer(
                    modifier = Modifier.height(
                        if (!desc.isNullOrBlank()) innerGapLarge else innerGapMedium,
                    ),
                )
                if (traitBlocks.roleTraits.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.type_detail_role_bonus_header),
                        fontSize = bodyTextSize,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_primary),
                        lineHeight = bodyLineHeight,
                    )
                    Spacer(modifier = Modifier.height(innerGapSmall))
                    traitBlocks.roleTraits.forEach { trait ->
                        TraitBulletLine(trait.content, linkColor)
                    }
                }
                traitBlocks.skillTraitGroups.forEachIndexed { index, (skillTypeId, list) ->
                    if (traitBlocks.roleTraits.isNotEmpty() || index > 0) {
                        Spacer(modifier = Modifier.height(innerGapMedium))
                    }
                    val skillLabel = list.firstOrNull()?.displayName(localeController).orEmpty()
                        .ifBlank {
                            stringResource(
                                R.string.type_detail_skill_bonus_unknown_name,
                                skillTypeId
                            )
                        }
                    Text(
                        text = buildAnnotatedString {
                            append("- ")
                            pushStyle(
                                SpanStyle(
                                    color = linkColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            append(skillLabel)
                            pop()
                            append(stringResource(R.string.type_detail_skill_bonus_header_suffix))
                        },
                        fontSize = bodyTextSize,
                        color = colorResource(R.color.text_primary),
                        lineHeight = bodyLineHeight,
                    )
                    Spacer(modifier = Modifier.height(innerGapSmall))
                    list.forEach { trait ->
                        TraitBulletLine(trait.content, linkColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun TraitBulletLine(content: String, linkColor: Color) {
    val bodyTextSize = dimensionResource(R.dimen.type_detail_body_text_size).value.sp
    val bodyLineHeight = dimensionResource(R.dimen.type_detail_body_line_height).value.sp
    val lineVerticalPadding = dimensionResource(R.dimen.type_detail_trait_line_vertical_padding)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = lineVerticalPadding),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "\u00B7 ",
            fontSize = bodyTextSize,
            color = colorResource(R.color.text_primary),
            lineHeight = bodyLineHeight,
        )
        Text(
            text = formatTraitBonusAnnotated(content, linkColor),
            fontSize = bodyTextSize,
            color = colorResource(R.color.text_primary),
            lineHeight = bodyLineHeight,
            modifier = Modifier.weight(1f)
        )
    }
}