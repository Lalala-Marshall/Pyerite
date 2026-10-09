package com.marshall.pyerite.regionMarketModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
internal fun MarketSectionHeader(
    title: String,
    addTopGap: Boolean,
) {
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    Text(
        text = title,
        fontSize = dimensionResource(R.dimen.list_section_header_text_size).value.sp,
        fontWeight = FontWeight.Black,
        color = colorResource(R.color.text_primary),
        modifier = Modifier.padding(
            start = dimensionResource(R.dimen.type_detail_page_title_start_padding),
            bottom = dimensionResource(R.dimen.list_section_header_bottom_padding),
            top = if (addTopGap) sectionGap else 0.dp,
        ),
    )
}

@Composable
internal fun MarketSectionItem(
    model: BaseLazyColumnItemModel,
    indexInSection: Int,
    sectionItemCount: Int,
    showDivider: Boolean,
    titleTrailingContent: (@Composable () -> Unit)? = null,
) {
    val radius = dimensionResource(R.dimen.detail_card_corner_radius)
    val shape = marketSectionShape(indexInSection, sectionItemCount, radius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.detail_card_horizontal_padding))
            .clip(shape)
            .background(colorResource(R.color.second_background), shape),
    ) {
        BaseLazyColumnItem(
            model = model,
            showDivider = showDivider,
            titleTrailingContent = titleTrailingContent,
        )
    }
}

@Composable
internal fun MarketListBottomSpacer() {
    Spacer(Modifier.height(dimensionResource(R.dimen.type_detail_bottom_padding)))
}

private fun marketSectionShape(index: Int, count: Int, radius: Dp): Shape {
    val top = index == 0
    val bottom = index == count - 1
    return when {
        top && bottom -> RoundedCornerShape(radius)
        top -> RoundedCornerShape(topStart = radius, topEnd = radius)
        bottom -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        else -> RoundedCornerShape(0.dp)
    }
}
