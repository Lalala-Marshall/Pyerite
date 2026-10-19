package com.marshall.pyerite.personalPropertyModule.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.personalPropertyModule.model.PersonalPropertyConfig
import com.marshall.pyerite.util.NumberDisplayFormatter
import kotlin.math.floor

internal data class PersonalPropertySliceUi(
    val label: String,
    val isk: Double?,
    val color: Color,
)

@Composable
internal fun PersonalPropertyDistributionChart(
    slices: List<PersonalPropertySliceUi>,
    detailsReady: Boolean,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val legendGap = dimensionResource(R.dimen.personal_property_legend_row_gap)
    val chartGap = dimensionResource(R.dimen.personal_property_chart_legend_gap)
    val contentPadding = dimensionResource(R.dimen.personal_property_distribution_padding)
    val emptyRingColor = colorResource(R.color.border)
    val totalIsk = slices.fold(0.0) { acc, slice -> acc + (slice.isk ?: 0.0) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(PersonalPropertyChartConfig.LEGEND_WIDTH_WEIGHT)
                .padding(end = chartGap),
            verticalArrangement = Arrangement.spacedBy(legendGap),
        ) {
            slices.forEach { slice ->
                DistributionLegendRow(
                    slice = slice,
                    totalIsk = totalIsk,
                    detailsReady = detailsReady,
                    placeholder = placeholder,
                )
            }
        }
        Box(
            modifier = Modifier.weight(PersonalPropertyChartConfig.CHART_WIDTH_WEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                val ringWidth = size.minDimension * PersonalPropertyChartConfig.RING_STROKE_FRACTION
                val inset = ringWidth / 2f
                val arcSize = Size(
                    width = size.minDimension - ringWidth,
                    height = size.minDimension - ringWidth,
                )
                val topLeft = Offset(inset, inset)
                val stroke = Stroke(width = ringWidth, cap = StrokeCap.Butt)
                val positive = slices.mapNotNull { slice ->
                    val value = slice.isk
                    if (value != null && value > 0.0) slice to value else null
                }
                val total = positive.fold(0.0) { acc, entry -> acc + entry.second }
                if (total <= 0.0) {
                    drawArc(
                        color = emptyRingColor,
                        startAngle = 0f,
                        sweepAngle = PersonalPropertyChartConfig.FULL_CIRCLE_DEGREES,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                    return@Canvas
                }
                var startAngle = PersonalPropertyChartConfig.START_ANGLE_DEGREES
                positive.forEachIndexed { index, (slice, value) ->
                    val sweep = if (index == positive.lastIndex) {
                        PersonalPropertyChartConfig.FULL_CIRCLE_DEGREES - startAngle +
                            PersonalPropertyChartConfig.START_ANGLE_DEGREES
                    } else {
                        (value / total * PersonalPropertyChartConfig.FULL_CIRCLE_DEGREES).toFloat()
                    }
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                    startAngle += sweep
                }
            }
        }
    }
}

@Composable
private fun DistributionLegendRow(
    slice: PersonalPropertySliceUi,
    totalIsk: Double,
    detailsReady: Boolean,
    placeholder: String,
) {
    val dotSize = dimensionResource(R.dimen.personal_property_legend_dot_size)
    val dotGap = dimensionResource(R.dimen.personal_property_legend_dot_gap)
    val valueLineGap = dimensionResource(R.dimen.personal_property_legend_value_line_gap)
    val isk = slice.isk
    val showValues = detailsReady && isk != null
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(valueLineGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(color = slice.color, shape = CircleShape),
            )
            Spacer(modifier = Modifier.width(dotGap))
            Text(
                text = slice.label,
                color = colorResource(R.color.text_primary),
                fontSize = dimensionResource(R.dimen.personal_property_legend_label_text_size).value.sp,
                maxLines = PersonalPropertyChartConfig.LEGEND_LABEL_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = if (showValues) {
                formatDistributionSlice(isk, totalIsk)
            } else {
                placeholder
            },
            color = colorResource(R.color.hint_text),
            fontSize = dimensionResource(R.dimen.personal_property_legend_value_text_size).value.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = dotSize + dotGap),
        )
    }
}

@Composable
internal fun formatCompactIsk(
    value: Double?,
    detailsReady: Boolean,
    placeholder: String,
): String {
    if (!detailsReady || value == null) return placeholder
    if (value == 0.0) return stringResource(R.string.personal_property_isk_zero)
    return stringResource(
        R.string.personal_property_isk_value,
        NumberDisplayFormatter.format(value, NumberDisplayFormatter.Style.COMPACT),
    )
}

@Composable
internal fun formatDetailIsk(
    value: Double?,
    detailsReady: Boolean,
    placeholder: String,
    zeroAsIntegerIsk: Boolean = false,
): String {
    if (!detailsReady || value == null) return placeholder
    if (zeroAsIntegerIsk && value == 0.0) {
        return stringResource(R.string.personal_property_isk_zero)
    }
    return stringResource(
        R.string.personal_property_isk_value,
        NumberDisplayFormatter.format(value, NumberDisplayFormatter.Style.FULL),
    )
}

@Composable
private fun formatDistributionSlice(value: Double, totalIsk: Double): String {
    val percent = floor(
        (if (totalIsk <= 0.0 || value <= 0.0) 0.0 else value / totalIsk) *
            PersonalPropertyConfig.PERCENT_SCALE,
    ).toInt().toString()
    val compactIsk = if (value == 0.0) {
        PersonalPropertyChartConfig.ZERO_COMPACT_ISK
    } else {
        NumberDisplayFormatter.format(value, NumberDisplayFormatter.Style.COMPACT)
    }
    return stringResource(R.string.personal_property_distribution_slice, percent, compactIsk)
}

private object PersonalPropertyChartConfig {
    const val FULL_CIRCLE_DEGREES = 360f
    const val START_ANGLE_DEGREES = -90f
    const val ZERO_COMPACT_ISK = "0"
    const val LEGEND_LABEL_MAX_LINES = 2
    const val LEGEND_WIDTH_WEIGHT = 0.5f
    const val CHART_WIDTH_WEIGHT = 0.5f
    const val RING_STROKE_FRACTION = 0.15f
}
