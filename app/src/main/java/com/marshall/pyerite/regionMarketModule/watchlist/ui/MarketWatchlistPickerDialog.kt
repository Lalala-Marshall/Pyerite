package com.marshall.pyerite.regionMarketModule.watchlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.marshall.pyerite.R
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.regionMarketModule.watchlist.model.MarketWatchlistConfig
import com.marshall.pyerite.regionMarketModule.watchlist.viewModel.MarketWatchlistViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun MarketWatchlistPickerDialog(
    typeId: Int,
    marketKey: String = "",
    onDismiss: () -> Unit,
    viewModel: MarketWatchlistViewModel = koinViewModel(),
) {
    val rows by viewModel.rows.collectAsState()
    val localeController: LocaleController = koinInject()
    var showCreate by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val alreadyInList = stringResource(R.string.market_watchlist_already_in_list)

    LaunchedEffect(localeController.contentLanguage) {
        viewModel.onContentLanguage(localeController.contentLanguage)
    }

    if (showCreate) {
        MarketWatchlistAddDialog(
            heading = stringResource(R.string.market_watchlist_add),
            onDismiss = { showCreate = false },
            onConfirm = { title ->
                val id = viewModel.create(title, marketKey)
                viewModel.addType(id, typeId)
                onDismiss()
            },
        )
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(MarketWatchlistConfig.DIALOG_WIDTH_FRACTION),
            shape = RoundedCornerShape(dimensionResource(R.dimen.skill_plan_dialog_corner)),
            color = colorResource(R.color.main_background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.skill_plan_dialog_content_padding)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.market_add_watchlist),
                    color = colorResource(R.color.text_primary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                val shownNotice = notice
                if (shownNotice != null) {
                    Text(
                        text = shownNotice,
                        color = colorResource(R.color.hint_text),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.skill_plan_dialog_label_field_gap),
                        ),
                    )
                }
                if (rows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.market_watchlist_empty),
                        color = colorResource(R.color.hint_text),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.skill_plan_dialog_content_padding),
                        ),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(R.dimen.skill_plan_dialog_label_field_gap))
                            .heightIn(max = dimensionResource(R.dimen.market_watchlist_picker_list_max_height))
                            .verticalScroll(rememberScrollState()),
                    ) {
                        BaseContainer(title = null, useSystemBarsPadding = false) {
                            rows.forEachIndexed { index, row ->
                                BaseLazyColumnItem(
                                    model = BaseLazyColumnItemModel(
                                        showLeadingIcon = false,
                                        itemName = row.title,
                                        itemHint = row.locationName,
                                        onClick = {
                                            val added = viewModel.addType(row.id, typeId)
                                            if (added) {
                                                onDismiss()
                                            } else {
                                                notice = alreadyInList
                                            }
                                        },
                                    ),
                                    showDivider = index != rows.lastIndex,
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.market_watchlist_add),
                    color = colorResource(R.color.hyperlink_text),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.skill_plan_dialog_content_padding))
                        .clickable {
                            notice = null
                            showCreate = true
                        },
                )
                Text(
                    text = stringResource(R.string.market_cancel),
                    color = colorResource(R.color.hint_text),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.skill_plan_dialog_button_gap))
                        .clickable(onClick = onDismiss),
                )
            }
        }
    }
}
