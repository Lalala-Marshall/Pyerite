package com.marshall.pyerite.ui.golbalComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.marshall.pyerite.R

@Composable
fun CharacterAvatar(
    portraitUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = dimensionResource(R.dimen.character_main_avatar_size),
    corporationIconUrl: String? = null,
    allianceIconUrl: String? = null,
    showBadges: Boolean = false,
    contentDescription: String? = null,
    /** Home / character list stay circular; character sheet uses [PyeriteIconShape]. */
    shape: Shape = CircleShape,
) {
    val resolvedContentDescription = contentDescription
        ?: stringResource(R.string.character_portrait)
    val badgeSize = dimensionResource(R.dimen.character_badge_size)
    val badgeOffset = dimensionResource(R.dimen.character_badge_offset)

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .border(
                    width = dimensionResource(R.dimen.character_avatar_border_width),
                    color = colorResource(R.color.border),
                    shape = shape,
                )
                .background(colorResource(R.color.main_background), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (portraitUrl.isNullOrBlank()) {
                Icon(
                    painter = painterResource(R.drawable.ic_character_avatar_placeholder),
                    contentDescription = resolvedContentDescription,
                    modifier = Modifier.size(size * 0.55f),
                    tint = colorResource(R.color.hint_text),
                )
            } else {
                AsyncImage(
                    model = portraitUrl,
                    contentDescription = resolvedContentDescription,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        if (showBadges) {
            if (!corporationIconUrl.isNullOrBlank()) {
                CharacterBadgeIcon(
                    iconUrl = corporationIconUrl,
                    contentDescription = stringResource(R.string.character_corp_icon),
                    shape = shape,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = -badgeOffset, y = badgeOffset)
                        .size(badgeSize),
                )
            }
            if (!allianceIconUrl.isNullOrBlank()) {
                CharacterBadgeIcon(
                    iconUrl = allianceIconUrl,
                    contentDescription = stringResource(R.string.character_alliance_icon),
                    shape = shape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = badgeOffset, y = badgeOffset)
                        .size(badgeSize),
                )
            }
        }
    }
}

@Composable
private fun CharacterBadgeIcon(
    iconUrl: String,
    contentDescription: String,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = dimensionResource(R.dimen.character_avatar_border_width),
                color = colorResource(R.color.border),
                shape = shape,
            )
            .background(colorResource(R.color.character_org_badge_background), shape),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = iconUrl,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
