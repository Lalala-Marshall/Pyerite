package com.marshall.pyerite.charactersListModule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.marshall.pyerite.R
import com.marshall.pyerite.charactersListModule.model.CharacterSummary
import com.marshall.pyerite.ui.golbalComponents.CharacterAvatar
import com.marshall.pyerite.ui.golbalComponents.PyeriteIconShape

@Composable
fun MainPageCharacterCard(
    currentCharacter: CharacterSummary?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true,
) {
    val cardCornerRadius = dimensionResource(R.dimen.detail_card_corner_radius)
    val shape = RoundedCornerShape(cardCornerRadius)
    val horizontalPadding = dimensionResource(R.dimen.detail_card_horizontal_padding)
    val innerPadding = dimensionResource(R.dimen.character_card_inner_padding)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(bottom = dimensionResource(R.dimen.character_main_card_bottom_spacing))
            .clip(shape)
            .background(colorResource(R.color.second_background), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(innerPadding),
    ) {
        if (currentCharacter == null) {
            MainPageEmptyCharacterRow(showChevron = showChevron)
        } else {
            MainPageSelectedCharacterRow(
                character = currentCharacter,
                showChevron = showChevron,
            )
        }
    }
}

@Composable
private fun MainPageEmptyCharacterRow(showChevron: Boolean) {
    val avatarSize = dimensionResource(R.dimen.character_main_avatar_size)

    MainPageCharacterRow(avatarSize = avatarSize, showChevron = showChevron) {
        Text(
            text = stringResource(R.string.character_add),
            modifier = Modifier.weight(1f),
            color = colorResource(R.color.text_primary),
            fontWeight = FontWeight.SemiBold,
            fontSize = dimensionResource(R.dimen.character_main_name_text_size).value.sp,
        )
    }
}

@Composable
private fun MainPageSelectedCharacterRow(
    character: CharacterSummary,
    showChevron: Boolean,
) {
    val avatarSize = dimensionResource(R.dimen.character_main_avatar_size)
    val orgTextSize = dimensionResource(R.dimen.character_org_text_size).value.sp
    val orgLineHeight = dimensionResource(R.dimen.character_org_icon_size).value.sp

    MainPageCharacterRow(
        avatarSize = avatarSize,
        portraitUrl = character.portraitUrl,
        showChevron = showChevron,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .height(avatarSize),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = character.name,
                color = colorResource(R.color.text_primary),
                fontWeight = FontWeight.Bold,
                fontSize = dimensionResource(R.dimen.character_main_name_text_size).value.sp,
                lineHeight = orgLineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = compactTextStyle(orgLineHeight),
            )
            CharacterOrgLine(
                iconUrl = character.corporationIconUrl,
                label = character.corporationName,
                textSize = orgTextSize,
                lineHeight = orgLineHeight,
            )
            CharacterOrgLine(
                iconUrl = character.allianceIconUrl,
                label = character.allianceName,
                textSize = orgTextSize,
                lineHeight = orgLineHeight,
            )
        }
    }
}

@Composable
private fun MainPageCharacterRow(
    avatarSize: Dp,
    portraitUrl: String? = null,
    showChevron: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(avatarSize),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CharacterAvatar(
            portraitUrl = portraitUrl,
            size = avatarSize,
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.character_main_avatar_gap)))
        content()
        if (showChevron) {
            MainPageCharacterChevron()
        }
    }
}

@Composable
private fun CharacterOrgLine(
    iconUrl: String?,
    label: String?,
    textSize: TextUnit = dimensionResource(R.dimen.character_org_text_size).value.sp,
    lineHeight: TextUnit = textSize,
) {
    val hasIcon = !iconUrl.isNullOrBlank()
    val hasLabel = !label.isNullOrBlank()
    if (!hasIcon && !hasLabel) {
        Text(
            text = stringResource(R.string.character_org_none_placeholder),
            color = colorResource(R.color.hint_text),
            fontSize = textSize,
            lineHeight = lineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = compactTextStyle(lineHeight),
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CharacterOrgIcon(iconUrl = iconUrl)
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.character_org_icon_gap)))
        Text(
            text = if (hasLabel) {
                label
            } else {
                stringResource(R.string.character_org_name_placeholder)
            },
            color = colorResource(R.color.text_primary),
            fontSize = textSize,
            lineHeight = lineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = compactTextStyle(lineHeight),
        )
    }
}

@Composable
private fun compactTextStyle(lineHeight: TextUnit): TextStyle = TextStyle(
    lineHeight = lineHeight,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

@Composable
private fun CharacterOrgIcon(iconUrl: String?) {
    val iconSize = dimensionResource(R.dimen.character_org_icon_size)
    if (iconUrl.isNullOrBlank()) {
        Text(
            text = stringResource(R.string.character_org_icon_placeholder),
            color = colorResource(R.color.hint_text),
            fontSize = dimensionResource(R.dimen.character_org_text_size).value.sp,
            lineHeight = iconSize.value.sp,
            maxLines = 1,
            style = compactTextStyle(iconSize.value.sp),
        )
    } else {
        AsyncImage(
            model = iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .clip(PyeriteIconShape.shape),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun MainPageCharacterChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(dimensionResource(R.dimen.detail_row_chevron_size)),
        tint = colorResource(R.color.hint_text),
    )
}
