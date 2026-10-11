package com.marshall.pyerite.corporationModule.structures.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.structures.model.CorporationStructure
import com.marshall.pyerite.corporationModule.structures.model.CorporationStructureService
import com.marshall.pyerite.corporationModule.structures.model.CorporationStructuresDateFormatter
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.util.DurationDisplayFormatter
import com.marshall.pyerite.util.formatDurationDisplay
import org.koin.compose.koinInject

@Composable
internal fun CorporationStructureRow(
    structure: CorporationStructure,
    language: ContentLanguage,
    nowMs: Long,
    emphasizeLowFuel: Boolean,
    showDivider: Boolean,
    iconManager: IconManager = koinInject(),
) {
    val hintColor = colorResource(R.color.hint_text)
    val valueColor = colorResource(R.color.text_caption)
    val stateColor = structureStateColor(structure.state)
    val lowFuelColor = colorResource(R.color.corporation_structure_state_hull)
    val ringColor = if (emphasizeLowFuel) lowFuelColor else stateColor
    val bodySize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp
    val bodyLineHeight = dimensionResource(R.dimen.detail_row_label_subtitle_line_height).value.sp
    val titleSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp
    val titleLineHeight = dimensionResource(R.dimen.sub_menu_label_line_height).value.sp
    val lineGap = dimensionResource(R.dimen.detail_row_label_subtitle_spacing)
    val structureName = structure.name.ifBlank {
        stringResource(R.string.corporation_structures_unknown_name)
    }
    val iconFile = structure.iconFilename?.let { iconManager.getIconFile(it) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.detail_row_horizontal_padding),
                    vertical = dimensionResource(R.dimen.detail_row_icon_vertical_padding_single_line),
                ),
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.detail_row_icon_gap),
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.corporation_structure_status_ring_size))
                    .border(
                        width = dimensionResource(R.dimen.corporation_structure_status_ring_width),
                        color = ringColor,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (iconFile != null) {
                    AsyncImage(
                        model = iconFile,
                        contentDescription = structureName,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.corporation_structure_icon_size))
                            .clip(CircleShape),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(lineGap),
            ) {
                Text(
                    text = structureName,
                    color = colorResource(R.color.text_primary),
                    fontSize = titleSize,
                    lineHeight = titleLineHeight,
                    fontWeight = FontWeight.Bold,
                    maxLines = CorporationStructureRowLayout.NAME_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                val fuelExpiresAt = structure.fuelExpiresAtMillis
                if (fuelExpiresAt != null) {
                    LabeledValue(
                        label = stringResource(R.string.corporation_structures_fuel_expires),
                        value = CorporationStructuresDateFormatter.displayDateTime(fuelExpiresAt),
                        valueColor = valueColor,
                        labelColor = hintColor,
                        textSize = bodySize,
                        lineHeight = bodyLineHeight,
                    )
                    val remainingValue = if (
                        CorporationStructuresDateFormatter.isExpired(fuelExpiresAt, nowMs)
                    ) {
                        stringResource(R.string.corporation_structures_fuel_expired)
                    } else {
                        val remaining = formatDurationDisplay(
                            totalSeconds = CorporationStructuresDateFormatter.remainingSeconds(
                                expiresAtMillis = fuelExpiresAt,
                                nowMs = nowMs,
                            ),
                            precision = DurationDisplayFormatter.Precision.HOUR,
                            language = language,
                            maxUnit = DurationDisplayFormatter.MaxUnit.DAY,
                        )
                        stringResource(
                            R.string.corporation_structures_fuel_remaining_value,
                            remaining,
                        )
                    }
                    LabeledValue(
                        label = stringResource(R.string.corporation_structures_fuel_remaining_label),
                        value = remainingValue,
                        valueColor = if (emphasizeLowFuel) lowFuelColor else valueColor,
                        labelColor = hintColor,
                        textSize = bodySize,
                        lineHeight = bodyLineHeight,
                    )
                }
                LabeledValue(
                    label = stringResource(R.string.corporation_structures_status),
                    value = stringResource(structure.state.labelRes()),
                    valueColor = stateColor,
                    labelColor = hintColor,
                    textSize = bodySize,
                    lineHeight = bodyLineHeight,
                )
                if (structure.services.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.corporation_structures_services),
                        color = hintColor,
                        fontSize = bodySize,
                        lineHeight = bodyLineHeight,
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.corporation_structure_service_tag_gap),
                        ),
                    ) {
                        structure.services.forEach { service ->
                            CorporationStructureServiceTag(service = service)
                        }
                    }
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(color = colorResource(R.color.border))
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    valueColor: Color,
    labelColor: Color,
    textSize: TextUnit,
    lineHeight: TextUnit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = labelColor,
            fontSize = textSize,
            lineHeight = lineHeight,
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = textSize,
            lineHeight = lineHeight,
            maxLines = CorporationStructureRowLayout.VALUE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CorporationStructureServiceTag(
    service: CorporationStructureService,
) {
    val online = service.status.isOnline
    val label = service.kind?.let { stringResource(it.labelRes()) } ?: service.name
    Text(
        text = label,
        color = colorResource(
            if (online) {
                R.color.corporation_structure_service_online_text
            } else {
                R.color.corporation_structure_service_offline_text
            },
        ),
        fontSize = dimensionResource(R.dimen.detail_row_label_subtitle_text_size).value.sp,
        lineHeight = dimensionResource(R.dimen.detail_row_label_subtitle_line_height).value.sp,
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    dimensionResource(R.dimen.corporation_structure_service_tag_corner),
                ),
            )
            .background(
                colorResource(
                    if (online) {
                        R.color.corporation_structure_service_online_background
                    } else {
                        R.color.corporation_structure_service_offline_background
                    },
                ),
            )
            .padding(
                horizontal = dimensionResource(
                    R.dimen.corporation_structure_service_tag_horizontal_padding,
                ),
                vertical = dimensionResource(
                    R.dimen.corporation_structure_service_tag_vertical_padding,
                ),
            ),
    )
}

private object CorporationStructureRowLayout {
    const val NAME_MAX_LINES = 2
    const val VALUE_MAX_LINES = 1
}
