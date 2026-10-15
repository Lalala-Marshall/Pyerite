package com.marshall.pyerite.databaseHierarchyModule.typeDetailPage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.marshall.pyerite.R
import com.marshall.pyerite.regionMarketModule.watchlist.ui.MarketWatchlistPickerDialog
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
internal fun TypeDetailMarketSlot(
    typeId: Int,
    onOpenRegionMarket: () -> Unit,
) {
    TypeDetailMarketSection(
        typeId = typeId,
        onOpenRegionMarket = onOpenRegionMarket,
    )
}

@Composable
fun TypeDetailMarketSection(
    typeId: Int,
    onOpenRegionMarket: () -> Unit = {},
) {
    var showWatchlistPicker by remember { mutableStateOf(false) }
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
                showChevron = true,
                onClick = { showWatchlistPicker = true },
            ),
            showDivider = false,
        )
    }
    if (showWatchlistPicker) {
        MarketWatchlistPickerDialog(
            typeId = typeId,
            onDismiss = { showWatchlistPicker = false },
        )
    }
}
