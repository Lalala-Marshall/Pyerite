package com.marshall.pyerite.databaseHierarchyModule.typeDetailPage

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import androidx.compose.ui.res.stringResource

@Composable
internal fun TypeDetailMarketSlot(
    onOpenRegionMarket: () -> Unit,
) {
    TypeDetailMarketSection(onOpenRegionMarket = onOpenRegionMarket)
}

@Composable
fun TypeDetailMarketSection(
    onOpenRegionMarket: () -> Unit = {},
) {
    BaseContainer(useSystemBarsPadding = false) {
        BaseLazyColumnItem(
            model = BaseLazyColumnItemModel(
                iconRes = R.drawable.ic_region_market,
                iconTint = Color.Unspecified,
                itemName = stringResource(R.string.region_market),
                showChevron = true,
                onClick = onOpenRegionMarket,
            ),
            showDivider = true,
        )
        BaseLazyColumnItem(
            model = BaseLazyColumnItemModel(
                showLeadingIcon = false,
                itemName = stringResource(R.string.market_add_watchlist),
                showChevron = false,
                onClick = null,
            ),
            showDivider = false,
        )
    }
}
