package com.marshall.pyerite.regionMarketModule.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.regionMarketModule.model.MarketConfig
import com.marshall.pyerite.regionMarketModule.model.MarketHistoryPoint
import com.marshall.pyerite.regionMarketModule.model.MarketHistoryRange
import com.marshall.pyerite.util.NumberDisplayFormatter
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

@Composable
internal fun MarketHistoryChart(
    history: List<MarketHistoryPoint>,
    range: MarketHistoryRange,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val points = remember(history, range) { history.within(range) }
    val chartHeight = dimensionResource(R.dimen.market_history_chart_height)
    val volumeColor = colorResource(R.color.hint_text)
    val priceColor = colorResource(R.color.hyperlink_text)
    val modelProducer = remember { CartesianChartModelProducer() }
    val axisFallback = stringResource(R.string.type_detail_market_placeholder_value)
    val scale = remember(points) { priceAxisScale(points) }
    val rangeProvider = remember(scale) {
        CartesianLayerRangeProvider.fixed(minY = scale.min, maxY = scale.max)
    }
    val monthStarts = remember(points) { monthStartIndices(points) }
    val xPlacer = remember(monthStarts) { MonthStartAxisItemPlacer(monthStarts) }
    val yPlacer = remember {
        VerticalAxis.ItemPlacer.count(
            count = { MarketConfig.AXIS_TICK_COUNT },
            shiftTopLines = false,
        )
    }
    val priceFormatter = remember(axisFallback) {
        CartesianValueFormatter { _, value, _ ->
            NumberDisplayFormatter.format(value, NumberDisplayFormatter.Style.COMPACT)
                .ifEmpty { axisFallback }
        }
    }
    val monthFormatter = remember(points, locale, axisFallback) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt()
            val point = points.getOrNull(index) ?: return@CartesianValueFormatter axisFallback
            monthLabel(point.date, locale).ifEmpty { axisFallback }
        }
    }

    LaunchedEffect(points, scale) {
        val scaled = scaledVolumes(points, scale)
        modelProducer.runTransaction {
            if (points.isEmpty()) {
                columnModel { series(0.0) }
                lineModel { series(0.0) }
            } else {
                columnModel { series(scaled) }
                lineModel { series(points.map { it.average }) }
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    LineComponent(Fill(volumeColor), VOLUME_COLUMN_WIDTH),
                ),
                rangeProvider = rangeProvider,
            ),
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(LineCartesianLayer.LineFill.single(Fill(priceColor))),
                ),
                rangeProvider = rangeProvider,
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = priceFormatter,
                itemPlacer = yPlacer,
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = monthFormatter,
                itemPlacer = xPlacer,
            ),
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight),
    )
}

private fun List<MarketHistoryPoint>.within(range: MarketHistoryRange): List<MarketHistoryPoint> {
    val cutoff = cutoffDate(range.days)
    return filter { it.date >= cutoff }.sortedBy { it.date }
}

private fun cutoffDate(days: Int): String {
    val calendar = Calendar.getInstance(Locale.US)
    calendar.add(Calendar.DAY_OF_YEAR, -days)
    return SimpleDateFormat(MarketConfig.DATE_PATTERN, Locale.US).format(calendar.time)
}

private fun monthLabel(date: String, locale: Locale): String {
    val parser = SimpleDateFormat(MarketConfig.DATE_PATTERN, Locale.US)
    val parsed = parser.parse(date) ?: return ""
    val formatted = SimpleDateFormat(MarketConfig.MONTH_PATTERN, locale).format(parsed)
    return if (locale.language.equals(Locale.CHINESE.language, ignoreCase = true)) {
        formatted
    } else {
        formatted.uppercase(locale)
    }
}

/** First history point of each calendar month, matching Tritanium's x labels. */
private fun monthStartIndices(points: List<MarketHistoryPoint>): List<Int> {
    if (points.isEmpty()) return emptyList()
    val parser = SimpleDateFormat(MarketConfig.DATE_PATTERN, Locale.US)
    val indices = mutableListOf<Int>()
    var lastMonth = Int.MIN_VALUE
    points.forEachIndexed { index, point ->
        val date = parser.parse(point.date) ?: return@forEachIndexed
        val calendar = Calendar.getInstance(Locale.US)
        calendar.time = date
        val month = calendar.get(Calendar.MONTH)
        if (index == 0 || month != lastMonth) indices += index
        lastMonth = month
    }
    return indices
}

/**
 * Vertical domain is the history's own low and high, with Tritanium's 15% pad.
 * A flat series still gets a small band so the line is not pinned to the axis edge.
 */
private fun priceAxisScale(points: List<MarketHistoryPoint>): PriceAxisScale {
    if (points.isEmpty()) return PriceAxisScale(min = 0.0, max = 1.0)
    val minPrice = points.minOf { it.average }
    val maxPrice = points.maxOf { it.average }
    val span = (maxPrice - minPrice).takeIf { it > 0.0 }
        ?: max(maxPrice * MarketConfig.AXIS_FLAT_PRICE_SPAN_FRACTION, 1.0)
    val min = (minPrice - span * MarketConfig.AXIS_PRICE_PADDING).coerceAtLeast(0.0)
    val max = maxPrice + span * MarketConfig.AXIS_PRICE_PADDING
    return PriceAxisScale(min = min, max = if (max > min) max else min + span)
}

/** Map daily volume onto the price axis so both series share one scale, matching Tritanium. */
private fun scaledVolumes(points: List<MarketHistoryPoint>, scale: PriceAxisScale): List<Double> {
    if (points.isEmpty()) return emptyList()
    val span = (scale.max - scale.min).takeIf { it > 0.0 } ?: 1.0
    val maxVolume = points.maxOf { it.volume }.toDouble().takeIf { it > 0.0 } ?: 1.0
    return points.map { point ->
        scale.min + (point.volume / maxVolume) * span * MarketConfig.VOLUME_AXIS_HEIGHT_FRACTION
    }
}

private class MonthStartAxisItemPlacer(
    private val labelIndices: List<Int>,
) : HorizontalAxis.ItemPlacer {
    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = labelIndices.map { it.toDouble() }.filter { value ->
        value >= visibleXRange.start - LABEL_OVERFLOW_STEPS &&
            value <= visibleXRange.endInclusive + LABEL_OVERFLOW_STEPS
    }

    override fun getWidthMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
    ): List<Double> = labelIndices.map { it.toDouble() }

    override fun getHeightMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = labelIndices.take(1).map { it.toDouble() }.ifEmpty { listOf(0.0) }

    override fun getStartLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = maxLabelWidth / 2f

    override fun getEndLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = maxLabelWidth / 2f
}

private data class PriceAxisScale(
    val min: Double,
    val max: Double,
)

private const val LABEL_OVERFLOW_STEPS = 1.0

private val VOLUME_COLUMN_WIDTH = 4.dp
