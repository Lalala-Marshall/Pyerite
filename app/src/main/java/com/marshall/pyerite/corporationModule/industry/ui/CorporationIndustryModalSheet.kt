package com.marshall.pyerite.corporationModule.industry.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CorporationIndustryModalSheet(
    title: String,
    endLabel: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetCorner = dimensionResource(R.dimen.character_mail_compose_sheet_corner)
    val sheetBackground = colorResource(R.color.search_field_idle_background)
    val sheetHorizontalPadding =
        dimensionResource(R.dimen.character_mail_compose_sheet_horizontal_padding)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        shape = RoundedCornerShape(topStart = sheetCorner, topEnd = sheetCorner),
        containerColor = sheetBackground,
        scrimColor = colorResource(R.color.search_scrim),
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(CorporationIndustryConfig.FILTER_SHEET_HEIGHT_FRACTION)
                .background(sheetBackground)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = sheetHorizontalPadding,
                    end = sheetHorizontalPadding,
                    top = dimensionResource(R.dimen.character_mail_compose_header_top_padding),
                ),
            ) {
                SheetHeader(
                    title = title,
                    endLabel = endLabel,
                    onEnd = onDismiss,
                )
            }
            content()
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    endLabel: String,
    onEnd: () -> Unit,
) {
    val buttonHeight = dimensionResource(R.dimen.top_bar_back_button_size)
    val actionColor = colorResource(R.color.hyperlink_text)
    val actionTextSize = dimensionResource(R.dimen.sub_menu_label_text_size).value.sp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight),
    ) {
        Text(
            text = title,
            color = colorResource(R.color.text_primary),
            fontSize = dimensionResource(R.dimen.list_section_header_text_size).value.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(
                    horizontal = dimensionResource(R.dimen.character_mail_compose_header_title_inset),
                ),
        )
        Text(
            text = endLabel,
            color = actionColor,
            fontSize = actionTextSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEnd,
                )
                .semantics { role = Role.Button },
        )
    }
}
