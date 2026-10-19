package com.marshall.pyerite.aboutModule.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.marshall.pyerite.R
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel

@Composable
fun MainPageAboutItem(
    onClick: (() -> Unit)?,
    showDivider: Boolean = false,
    showChevron: Boolean = true,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            itemName = stringResource(R.string.about),
            showChevron = showChevron,
            onClick = onClick,
        ),
        showDivider = showDivider,
        leadingContent = { iconSize ->
            AboutMaterialIcon(
                image = Icons.Outlined.Info,
                iconSize = iconSize,
            )
        },
    )
}

@Composable
internal fun AboutMaterialIcon(
    image: ImageVector,
    iconSize: Dp,
) {
    Icon(
        imageVector = image,
        contentDescription = null,
        tint = colorResource(R.color.about_icon),
        modifier = Modifier.size(iconSize),
    )
}
