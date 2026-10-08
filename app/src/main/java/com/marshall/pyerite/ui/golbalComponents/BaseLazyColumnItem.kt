package com.marshall.pyerite.ui.golbalComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.marshall.pyerite.R
import com.marshall.pyerite.iconModule.manager.IconManager
import org.koin.compose.koinInject
import java.io.File

/**
 * Secondary line under the title. Add multiple entries for multi-line hints.
 * Prefer [text]; use [annotatedText] when a single line needs mixed colors.
 * Optional [iconUrl] / [iconRes] draw a leading adornment aligned with the
 * title-row leading slot on [BaseLazyColumnItem].
 * Optional [trailingText] sits on the same line, end-aligned.
 */
data class BaseLazyColumnItemHint(
    val text: String = "",
    val annotatedText: AnnotatedString? = null,
    val color: Color? = null,
    val trailingText: String = "",
    val trailingColor: Color? = null,
    val iconUrl: String? = null,
    val iconRes: Int? = null,
    /** SDE icon pack filename; resolved via [IconManager] when [iconUrl] / [iconRes] are unset. */
    val iconFileName: String? = null,
    /** Larger hint (between title and default subtitle), e.g. LP store cost line. */
    val emphasized: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

/**
 * Shared list / detail row. Variants via flags:
 * - title-only vs hints (text vertical padding 12 / 4)
 * - icon vertical padding 8 / 4; icon size from content **line count**
 *   (1 title + N hints), not measured wrap height
 * - title / hint text wrap within the text column
 * - optional leading icon, trailing value, chevron (right or expanded-down)
 * - colors / bold / indent / custom leading & title slots
 */
data class BaseLazyColumnItemModel(
    val iconRes: Int = R.drawable.ic_database,
    val iconFile: File? = null,
    /** Remote image URL (portrait / corp logo). Ignored when [iconFile] is set. */
    val iconUrl: String? = null,
    /** SDE icon pack filename; resolved via [IconManager] when [iconFile] is null. */
    val iconFileName: String? = null,
    /** Override default [R.dimen.base_lazy_column_item_icon_size]. */
    val iconSize: Dp? = null,
    /** Tint for [iconRes] painter (ignored for file / url images). */
    val iconTint: Color? = null,
    /** Light plate behind dark SDE type icons (night-mode contrast). */
    val iconOnLightPlate: Boolean = false,
    /** When false, no leading icon column (value-only / indented submenu labels). */
    val showLeadingIcon: Boolean = true,
    /** Extra start indent before the icon (nested submenu). */
    val leadingIndent: Boolean = false,
    val itemName: String,
    /** When set, drawn instead of [itemName] (e.g. mixed-color security + place name). */
    val itemNameAnnotated: AnnotatedString? = null,
    val itemNameColor: Color? = null,
    val itemNameBold: Boolean = false,
    /** When set, title is clamped to this many lines with ellipsis. Null wraps. */
    val itemNameMaxLines: Int? = null,
    /** Click on the title text only (not the whole row). */
    val itemNameOnClick: (() -> Unit)? = null,
    /** Prefer this over [itemHint] when multiple lines or custom colors are needed. */
    val itemHints: List<BaseLazyColumnItemHint> = emptyList(),
    /**
     * When true, title and hints share a fixed-width leading column so title text
     * lines up with hint icons. When false, only hint lines that have an icon
     * draw one — the title is not indented.
     */
    val alignHintLeadingColumn: Boolean = true,
    /** Single hint shorthand; used only when [itemHints] is empty. */
    val itemHint: String = "",
    /** Right-side value before the chevron (e.g. "等级 3", dogma units). */
    val trailingValue: String = "",
    val trailingValueColor: Color? = null,
    val showChevron: Boolean = true,
    /** When [showChevron] and true, shows down-chevron (expandable header). */
    val chevronExpanded: Boolean = false,
    /** Null disables click (no ripple). */
    val onClick: (() -> Unit)? = {},
)

@Composable
fun BaseLazyColumnItem(
    model: BaseLazyColumnItemModel,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    /**
     * Replaces the default leading icon (e.g. character avatar, vector Store icon).
     * Receives the same adaptive icon size as the default leading icon; clipped to
     * [PyeriteIconShape] so callers need not apply corner radius themselves.
     */
    leadingContent: (@Composable (iconSize: Dp) -> Unit)? = null,
    /**
     * Leading adornment on the title row (e.g. online dot), vertically aligned with
     * hint-line icons in a shared fixed-width column.
     */
    titleLeadingContent: (@Composable () -> Unit)? = null,
    /** Trailing content on the title row (e.g. status tag next to the name). */
    titleTrailingContent: (@Composable () -> Unit)? = null,
    /**
     * Custom trailing slot before the chevron. When set, replaces [BaseLazyColumnItemModel.trailingValue].
     */
    trailingContent: (@Composable () -> Unit)? = null,
    /**
     * Drawn under the main row, still inside the clickable / press ripple bounds
     * (e.g. skill-queue training progress).
     */
    belowContent: (@Composable () -> Unit)? = null,
    /**
     * When true, drops the bottom content padding so [belowContent] can sit flush
     * under the hint line.
     */
    omitContentBottomPadding: Boolean = false,
    /** Optional full-row background (e.g. mastery right-edge gradient). */
    backgroundBrush: Brush? = null,
    iconManager: IconManager = koinInject(),
) {
    val hints = model.resolvedHints()
    val hasSecondaryLine = hints.isNotEmpty()
    val textVerticalPadding = dimensionResource(
        if (hasSecondaryLine) {
            R.dimen.detail_row_vertical_padding_multi_line
        } else {
            R.dimen.detail_row_vertical_padding_single_line
        },
    )
    val textBottomPadding = if (omitContentBottomPadding) {
        0.dp
    } else {
        textVerticalPadding
    }
    val iconVerticalPadding = dimensionResource(
        if (hasSecondaryLine) {
            R.dimen.detail_row_icon_vertical_padding
        } else {
            R.dimen.detail_row_icon_vertical_padding_single_line
        },
    )
    val contentLineCount = BaseLazyColumnItemLayout.TITLE_LINE_COUNT + hints.size
    val iconSize = model.iconSize
        ?: baseLazyColumnItemAdaptiveIconSize(contentLineCount = contentLineCount)
    val iconGap = dimensionResource(R.dimen.detail_row_icon_gap)
    val rowHorizontalPadding = dimensionResource(R.dimen.detail_row_horizontal_padding)
    val leadingIndentWidth = dimensionResource(R.dimen.sub_menu_leading_indent)
    val titleValueGap = dimensionResource(R.dimen.detail_row_title_value_gap)
    val trailingGap = dimensionResource(R.dimen.detail_row_trailing_gap)
    val chevronSize = dimensionResource(R.dimen.detail_row_chevron_size)
    val titleTextSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp
    val titleLineHeight = dimensionResource(R.dimen.sub_menu_label_line_height).value.sp
    val valueTextSize = dimensionResource(R.dimen.sub_menu_value_text_size).value.sp
    val hintTextSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp
    val hintLineHeight = dimensionResource(R.dimen.detail_row_label_subtitle_line_height).value.sp
    val emphasizedHintTextSize =
        dimensionResource(R.dimen.detail_row_label_emphasized_subtitle_text_size).value.sp
    val emphasizedHintLineHeight =
        dimensionResource(R.dimen.detail_row_label_emphasized_subtitle_line_height).value.sp
    val hintSpacing = dimensionResource(R.dimen.detail_row_label_subtitle_spacing)
    val hintIconSize = dimensionResource(R.dimen.base_lazy_column_item_hint_icon_size)
    val hintIconGap = dimensionResource(R.dimen.base_lazy_column_item_hint_icon_gap)
    val titleColor = model.itemNameColor ?: colorResource(R.color.text_primary)
    val defaultHintColor = colorResource(R.color.hint_text)
    val trailingColor = model.trailingValueColor ?: defaultHintColor
    val showHintLeadingColumn = titleLeadingContent != null ||
        (model.alignHintLeadingColumn &&
            hints.any {
                !it.iconUrl.isNullOrBlank() || it.iconRes != null || !it.iconFileName.isNullOrBlank()
            })

    val rootModifier = modifier
        .fillMaxWidth()
        .then(
            if (backgroundBrush != null) {
                Modifier.background(backgroundBrush)
            } else {
                Modifier
            },
        )
        .then(
            if (model.onClick != null) {
                Modifier.clickable(onClick = model.onClick)
            } else {
                Modifier
            },
        )

    Column(modifier = rootModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = rowHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (model.leadingIndent) {
                Spacer(modifier = Modifier.width(leadingIndentWidth))
            }

            if (leadingContent != null) {
                Box(
                    modifier = Modifier
                        .padding(vertical = iconVerticalPadding)
                        .clip(PyeriteIconShape.shape),
                ) {
                    leadingContent(iconSize)
                }
                Spacer(modifier = Modifier.width(iconGap))
            } else if (model.showLeadingIcon) {
                BaseLazyColumnItemLeadingIcon(
                    model = model,
                    iconSize = iconSize,
                    iconVerticalPadding = iconVerticalPadding,
                    iconManager = iconManager,
                )
                Spacer(modifier = Modifier.width(iconGap))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = textVerticalPadding, bottom = textBottomPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(hintSpacing),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showHintLeadingColumn) {
                                Box(
                                    modifier = Modifier.size(hintIconSize),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    titleLeadingContent?.invoke()
                                }
                                Spacer(modifier = Modifier.width(hintIconGap))
                            }
                            val nameAnnotated = model.itemNameAnnotated
                            val titleMaxLines = model.itemNameMaxLines
                            val nameClick = model.itemNameOnClick
                            val titleModifier = Modifier
                                .weight(
                                    1f,
                                    fill = titleMaxLines != null && nameClick == null,
                                )
                                .then(
                                    if (nameClick != null) {
                                        Modifier.clickable(onClick = nameClick)
                                    } else {
                                        Modifier
                                    },
                                )
                            if (nameAnnotated != null) {
                                Text(
                                    text = nameAnnotated,
                                    fontWeight = if (model.itemNameBold) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    fontSize = titleTextSize,
                                    lineHeight = titleLineHeight,
                                    maxLines = titleMaxLines ?: Int.MAX_VALUE,
                                    overflow = if (titleMaxLines != null) {
                                        TextOverflow.Ellipsis
                                    } else {
                                        TextOverflow.Clip
                                    },
                                    modifier = titleModifier,
                                )
                            } else {
                                Text(
                                    text = model.itemName,
                                    color = titleColor,
                                    fontWeight = if (model.itemNameBold) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    fontSize = titleTextSize,
                                    lineHeight = titleLineHeight,
                                    maxLines = titleMaxLines ?: Int.MAX_VALUE,
                                    overflow = if (titleMaxLines != null) {
                                        TextOverflow.Ellipsis
                                    } else {
                                        TextOverflow.Clip
                                    },
                                    modifier = titleModifier,
                                )
                            }
                            if (titleTrailingContent != null) {
                                Spacer(modifier = Modifier.width(trailingGap))
                                titleTrailingContent()
                            }
                        }
                        hints.forEach { hint ->
                            val hintClick = hint.onClick
                            val hintHasIcon = !hint.iconUrl.isNullOrBlank() ||
                                hint.iconRes != null ||
                                !hint.iconFileName.isNullOrBlank()
                            val lineFontSize = if (hint.emphasized) {
                                emphasizedHintTextSize
                            } else {
                                hintTextSize
                            }
                            val lineLineHeight = if (hint.emphasized) {
                                emphasizedHintLineHeight
                            } else {
                                hintLineHeight
                            }
                            val hintTrailing = hint.trailingText
                            Row(
                                modifier = if (hintTrailing.isNotEmpty()) {
                                    Modifier.fillMaxWidth()
                                } else {
                                    Modifier
                                },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (showHintLeadingColumn || hintHasIcon) {
                                    Box(
                                        modifier = Modifier.size(hintIconSize),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        BaseLazyColumnItemHintIcon(
                                            iconUrl = hint.iconUrl,
                                            iconRes = hint.iconRes,
                                            iconFileName = hint.iconFileName,
                                            size = hintIconSize,
                                            iconManager = iconManager,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(hintIconGap))
                                }
                                val annotated = hint.annotatedText
                                val startModifier = Modifier
                                    .weight(
                                        1f,
                                        fill = hintTrailing.isNotEmpty() ||
                                            annotated != null ||
                                            hintClick == null,
                                    )
                                    .then(
                                        if (hintClick != null && annotated == null) {
                                            Modifier.clickable(onClick = hintClick)
                                        } else {
                                            Modifier
                                        },
                                    )
                                if (annotated != null) {
                                    Text(
                                        text = annotated,
                                        fontSize = lineFontSize,
                                        lineHeight = lineLineHeight,
                                        maxLines = if (hintTrailing.isNotEmpty()) 1 else Int.MAX_VALUE,
                                        overflow = if (hintTrailing.isNotEmpty()) {
                                            TextOverflow.Ellipsis
                                        } else {
                                            TextOverflow.Clip
                                        },
                                        modifier = startModifier,
                                    )
                                } else {
                                    Text(
                                        text = hint.text,
                                        color = hint.color
                                            ?: if (hintClick != null) {
                                                colorResource(R.color.hyperlink_text)
                                            } else {
                                                defaultHintColor
                                            },
                                        fontSize = lineFontSize,
                                        lineHeight = lineLineHeight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = startModifier,
                                    )
                                }
                                if (hintTrailing.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(titleValueGap))
                                    Text(
                                        text = hintTrailing,
                                        color = hint.trailingColor ?: defaultHintColor,
                                        fontSize = lineFontSize,
                                        lineHeight = lineLineHeight,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }

                    if (trailingContent != null) {
                        Spacer(modifier = Modifier.width(titleValueGap))
                        trailingContent()
                    } else if (model.trailingValue.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(titleValueGap))
                        Text(
                            text = model.trailingValue,
                            color = trailingColor,
                            fontSize = valueTextSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (model.showChevron) {
                        Spacer(modifier = Modifier.width(trailingGap))
                        Icon(
                            modifier = Modifier.size(chevronSize),
                            imageVector = if (model.chevronExpanded) {
                                Icons.Filled.KeyboardArrowDown
                            } else {
                                Icons.AutoMirrored.Filled.KeyboardArrowRight
                            },
                            contentDescription = null,
                            tint = if (model.chevronExpanded) {
                                colorResource(R.color.text_primary)
                            } else {
                                colorResource(R.color.hint_text)
                            },
                        )
                    }
                }

                // Inset divider (under text column) when there is no full-width belowContent.
                if (showDivider && belowContent == null) {
                    HorizontalDivider(
                        thickness = dimensionResource(R.dimen.detail_divider_thickness),
                        color = colorResource(R.color.border),
                    )
                }
            }
        }

        belowContent?.invoke()

        if (showDivider && belowContent != null) {
            HorizontalDivider(
                thickness = dimensionResource(R.dimen.detail_divider_thickness),
                color = colorResource(R.color.border),
            )
        }
    }
}

@Composable
private fun BaseLazyColumnItemHintIcon(
    iconUrl: String?,
    iconRes: Int?,
    iconFileName: String?,
    size: Dp,
    iconManager: IconManager,
) {
    val shape = PyeriteIconShape.shape
    val iconFile = iconFileName?.let { iconManager.getIconFile(it) }
    when {
        iconFile != null -> {
            Icon(
                modifier = Modifier
                    .size(size)
                    .clip(shape),
                painter = rememberAsyncImagePainter(iconFile),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }
        !iconUrl.isNullOrBlank() -> {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(shape),
                contentScale = ContentScale.Crop,
            )
        }
        iconRes != null -> {
            Icon(
                modifier = Modifier
                    .size(size)
                    .clip(shape),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }
    }
}

@Composable
private fun BaseLazyColumnItemLeadingIcon(
    model: BaseLazyColumnItemModel,
    iconSize: Dp,
    iconVerticalPadding: Dp,
    iconManager: IconManager,
) {
    val resolvedFile = model.iconFile
        ?: model.iconFileName?.let { iconManager.getIconFile(it) }
    val shape = PyeriteIconShape.shape
    val iconModifier = Modifier
        .padding(vertical = iconVerticalPadding)
        .size(iconSize)
        .clip(shape)
    val painterTint = model.iconTint ?: Color.Unspecified

    when {
        resolvedFile != null && model.iconOnLightPlate -> {
            Box(
                modifier = iconModifier.background(colorResource(R.color.white)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = resolvedFile,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.base_lazy_column_item_icon_plate_padding))
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(model.iconRes),
                    error = painterResource(model.iconRes),
                )
            }
        }
        resolvedFile != null -> {
            Icon(
                modifier = iconModifier,
                painter = rememberAsyncImagePainter(resolvedFile),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }
        !model.iconUrl.isNullOrBlank() -> {
            AsyncImage(
                model = model.iconUrl,
                contentDescription = null,
                modifier = iconModifier,
                contentScale = ContentScale.Crop,
            )
        }
        else -> {
            Icon(
                modifier = iconModifier,
                painter = painterResource(model.iconRes),
                contentDescription = null,
                tint = painterTint,
            )
        }
    }
}

private fun BaseLazyColumnItemModel.resolvedHints(): List<BaseLazyColumnItemHint> =
    when {
        itemHints.isNotEmpty() -> itemHints
        itemHint.isNotEmpty() -> listOf(BaseLazyColumnItemHint(text = itemHint))
        else -> emptyList()
    }

/**
 * Leading icon edge length from **logical** content line count
 * ([BaseLazyColumnItemLayout.TITLE_LINE_COUNT] title + N hint slots).
 * Wrapped visual lines do not change this size.
 */
@Composable
fun baseLazyColumnItemAdaptiveIconSize(contentLineCount: Int): Dp {
    val lineCount = contentLineCount.coerceAtLeast(BaseLazyColumnItemLayout.TITLE_LINE_COUNT)
    val hintLineCount = (lineCount - BaseLazyColumnItemLayout.TITLE_LINE_COUNT).coerceAtLeast(0)
    val hasSecondaryLine = hintLineCount > 0
    val textVerticalPadding = dimensionResource(
        if (hasSecondaryLine) {
            R.dimen.detail_row_vertical_padding_multi_line
        } else {
            R.dimen.detail_row_vertical_padding_single_line
        },
    )
    val iconVerticalPadding = dimensionResource(
        if (hasSecondaryLine) {
            R.dimen.detail_row_icon_vertical_padding
        } else {
            R.dimen.detail_row_icon_vertical_padding_single_line
        },
    )
    val titleLineHeight = dimensionResource(R.dimen.sub_menu_label_line_height)
    val hintLineHeight = dimensionResource(R.dimen.detail_row_label_subtitle_line_height)
    val hintSpacing = dimensionResource(R.dimen.detail_row_label_subtitle_spacing)
    val textContentHeight = titleLineHeight +
        hintLineHeight * hintLineCount +
        hintSpacing * hintLineCount
    return (textContentHeight + textVerticalPadding * 2 - iconVerticalPadding * 2)
        .coerceAtLeast(0.dp)
}

/** Logical line slots used for adaptive leading-icon sizing (not wrap height). */
object BaseLazyColumnItemLayout {
    const val TITLE_LINE_COUNT = 1
}

/**
 * Divider that starts to the right of the leading icon column (not a hardcoded inset).
 * [leadingIconSize] should match the row’s icon size so the line lines up with text.
 */
@Composable
fun ItemDivider(
    leadingIconSize: Dp = dimensionResource(R.dimen.base_lazy_column_item_icon_size),
) {
    val horizontalPadding = dimensionResource(R.dimen.detail_row_horizontal_padding)
    val iconGap = dimensionResource(R.dimen.detail_row_icon_gap)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
    ) {
        Spacer(modifier = Modifier.width(leadingIconSize + iconGap))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = dimensionResource(R.dimen.detail_divider_thickness),
            color = colorResource(R.color.border),
        )
    }
}
