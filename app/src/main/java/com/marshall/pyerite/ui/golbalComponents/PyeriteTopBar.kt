package com.marshall.pyerite.ui.golbalComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.marshall.pyerite.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut


@Composable
fun rememberTopBarTotalHeight(): Dp {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barHeight = dimensionResource(R.dimen.top_bar_nav_row_height)
    return statusBarTop + barHeight
}

@Composable
fun NavController.rememberNavigateUpAction(): (() -> Unit)? {
    val canNavigateBack = previousBackStackEntry != null
    return remember(this, canNavigateBack) {
        if (canNavigateBack) {
            { navigateUp() }
        } else {
            null
        }
    }
}

@Composable
fun rememberLazyListTitleCollapsed(
    listState: LazyListState,
    pageTitleIndex: Int = 0,
    enabled: Boolean = true,
): Boolean {
    if (!enabled) return false
    return remember(listState, pageTitleIndex) {
        derivedStateOf { listState.isPageTitleScrolledOff(pageTitleIndex) }
    }.value
}

@Composable
fun rememberLazyListSearchPinned(
    listState: LazyListState,
    pageTitleIndex: Int = 0,
    enabled: Boolean = true,
): Boolean {
    if (!enabled) return false
    return remember(listState, pageTitleIndex) {
        derivedStateOf { listState.firstVisibleItemIndex > pageTitleIndex }
    }.value
}

@Composable
fun rememberScrollTitleCollapsed(
    scrollState: ScrollState,
    enabled: Boolean = true,
): Boolean {
    if (!enabled) return false
    val thresholdPx = with(LocalDensity.current) {
        dimensionResource(R.dimen.page_title_collapse_threshold).roundToPx()
    }
    return remember(scrollState, thresholdPx) {
        derivedStateOf { scrollState.value >= thresholdPx }
    }.value
}

private fun LazyListState.isPageTitleScrolledOff(pageTitleIndex: Int): Boolean {
    val layoutInfo = layoutInfo
    val titleItem = layoutInfo.visibleItemsInfo.find { it.index == pageTitleIndex }
    return if (titleItem == null) {
        layoutInfo.visibleItemsInfo.firstOrNull()?.index?.let { it > pageTitleIndex } ?: false
    } else {
        titleItem.offset + titleItem.size <= 0
    }
}

@Composable
fun PyeritePageScaffold(
    title: String,
    showCollapsedTitle: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    startActions: List<PyeriteTopBarActionItem> = emptyList(),
    endActions: List<PyeriteTopBarActionItem> = emptyList(),
    content: @Composable (topBarPadding: PaddingValues) -> Unit,
) {
    val topBarTotalHeight = rememberTopBarTotalHeight()
    val topBarPadding = remember(topBarTotalHeight) {
        PaddingValues(top = topBarTotalHeight)
    }

    Box(modifier = modifier.fillMaxSize()) {
        content(topBarPadding)
        PyeriteTopBar(
            title = title,
            showTitle = showCollapsedTitle,
            showScrim = showCollapsedTitle,
            onBack = onBack,
            leadingContent = leadingContent,
            startActions = startActions,
            endActions = endActions,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(20f),
        )
    }
}

@Composable
fun PyeriteTopBar(
    title: String,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
    showScrim: Boolean = showTitle,
    onBack: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    startActions: List<PyeriteTopBarActionItem> = emptyList(),
    endActions: List<PyeriteTopBarActionItem> = emptyList(),
    pinnedSearch: (@Composable () -> Unit)? = null,
    pinnedSearchHeight: Dp = 0.dp,
) {
    val navRowHeight = dimensionResource(R.dimen.top_bar_nav_row_height)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barTotalHeight = statusBarTop + navRowHeight
    val hasPinnedSearch = pinnedSearch != null
    val headerHeight = barTotalHeight + if (hasPinnedSearch) pinnedSearchHeight else 0.dp
    val scrimBase = colorResource(R.color.main_background)
    val headerClickBlocker = remember { MutableInteractionSource() }
    val leadingActions = buildList {
        if (onBack != null) {
            add(
                PyeriteTopBarActionItem(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                ),
            )
        }
        addAll(startActions)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clickable(
                interactionSource = headerClickBlocker,
                indication = null,
                onClick = {},
            ),
    ) {
        AnimatedVisibility(
            visible = showScrim,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to scrimBase.copy(alpha = 0.96f),
                                0.45f to scrimBase.copy(alpha = 0.88f),
                                0.82f to scrimBase.copy(alpha = 0.78f),
                                1f to scrimBase.copy(alpha = 0f),
                            ),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            TopBarNavRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navRowHeight)
                    .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding)),
                leading = {
                    if (leadingContent != null || leadingActions.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                dimensionResource(R.dimen.top_bar_end_action_gap),
                            ),
                        ) {
                            leadingContent?.invoke()
                            if (leadingActions.isNotEmpty()) {
                                PyeriteTopBarActions(actions = leadingActions)
                            }
                        }
                    }
                },
                title = {
                    TopBarCenterTitle(title = title, showTitle = showTitle)
                },
                trailing = {
                    if (endActions.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                dimensionResource(R.dimen.top_bar_end_action_gap),
                            ),
                        ) {
                            endActions.forEach { action ->
                                PyeriteTopBarActions(actions = listOf(action))
                            }
                        }
                    }
                },
            )

            if (hasPinnedSearch) {
                pinnedSearch.invoke()
            }
        }
    }
}

@Composable
private fun TopBarNavRow(
    leading: @Composable () -> Unit,
    title: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            Box(contentAlignment = Alignment.CenterStart) { leading() }
            Box(contentAlignment = Alignment.Center) { title() }
            Box(contentAlignment = Alignment.CenterEnd) { trailing() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val leadingPlaceable = measurables[0].measure(loose)
        val trailingPlaceable = measurables[2].measure(loose)
        val sideWidth = maxOf(leadingPlaceable.width, trailingPlaceable.width)
        val titleMaxWidth = (constraints.maxWidth - sideWidth * 2).coerceAtLeast(0)
        val titlePlaceable = measurables[1].measure(loose.copy(maxWidth = titleMaxWidth))
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            titlePlaceable.placeRelative(
                x = (width - titlePlaceable.width) / 2,
                y = (height - titlePlaceable.height) / 2,
            )
            leadingPlaceable.placeRelative(
                x = 0,
                y = (height - leadingPlaceable.height) / 2,
            )
            trailingPlaceable.placeRelative(
                x = width - trailingPlaceable.width,
                y = (height - trailingPlaceable.height) / 2,
            )
        }
    }
}

@Composable
private fun TopBarCenterTitle(
    title: String,
    showTitle: Boolean,
) {
    AnimatedVisibility(
        visible = showTitle,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(R.color.text_primary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.top_bar_end_action_gap)),
        )
    }
}
