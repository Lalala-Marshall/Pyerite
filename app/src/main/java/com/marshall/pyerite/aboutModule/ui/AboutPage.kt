package com.marshall.pyerite.aboutModule.ui

import android.content.Intent
import android.graphics.BlurMaskFilter
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.aboutModule.data.AboutConfig
import com.marshall.pyerite.aboutModule.viewModel.AboutSdeUpdateStatus
import com.marshall.pyerite.aboutModule.viewModel.AboutViewModel
import com.marshall.pyerite.sdeModule.update.SdeUpdateBottomSheet
import com.marshall.pyerite.sdeModule.update.SdeUpdateUiState
import com.marshall.pyerite.sdeModule.update.SdeUpdateViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItem
import com.marshall.pyerite.ui.golbalComponents.BaseLazyColumnItemModel
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun AboutPage(
    navController: NavController,
    viewModel: AboutViewModel = koinViewModel(),
    sdeUpdateViewModel: SdeUpdateViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val updateUiState by sdeUpdateViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val onBack = navController.rememberNavigateUpAction()
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    val scrollState = rememberScrollState()
    val versionLabel = stringResource(R.string.about_version, state.versionName)
    val githubUrl = stringResource(R.string.about_github_url)
    val supportEmail = stringResource(R.string.about_support_email)
    val copyrightUrl = stringResource(R.string.about_copyright_url)
    val buildValue = state.sdeBuildLabel.ifBlank {
        stringResource(R.string.sde_update_build_unknown)
    }
    val sdeHint = stringResource(R.string.about_sde_build, buildValue)
    val sdeStatusLabel = state.sdeUpdateStatus.statusLabel()

    PyeritePageScaffold(
        title = "",
        showCollapsedTitle = false,
        onBack = onBack,
    ) { topBarPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val topInset = topBarPadding.calculateTopPadding()
            val headerHeight = (maxHeight * AboutConfig.HEADER_HEIGHT_FRACTION - topInset)
                .coerceAtLeast(dimensionResource(R.dimen.about_header_min_height))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset)
                    .verticalScroll(scrollState)
                    .padding(bottom = bottomPadding),
            ) {
                AboutHeader(
                    versionLabel = versionLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight),
                )
                BaseContainer(
                    useSystemBarsPadding = false,
                ) {
                    AboutSdeRow(
                        hint = sdeHint,
                        statusLabel = sdeStatusLabel,
                        onClick = sdeUpdateViewModel::openUpdateSheet,
                    )
                }
                Spacer(modifier = Modifier.height(sectionGap))
                BaseContainer(
                    useSystemBarsPadding = false,
                ) {
                    AboutAvatarRow(
                        title = stringResource(R.string.about_author),
                        hint = stringResource(R.string.about_author_name),
                        avatarRes = R.drawable.about_author_avatar,
                        showDivider = true,
                    )
                    AboutLinkRow(
                        title = stringResource(R.string.about_github),
                        hint = githubUrl,
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, githubUrl.toUri()),
                            )
                        },
                    )
                    AboutMailRow(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.fromParts(AboutConfig.MAILTO_SCHEME, supportEmail, null),
                                ),
                            )
                        },
                    )
                    AboutLinkRow(
                        title = stringResource(R.string.about_copyright),
                        hint = stringResource(R.string.about_copyright_file),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, copyrightUrl.toUri()),
                            )
                        },
                    )
                    AboutAvatarRow(
                        title = stringResource(R.string.about_thanks),
                        hint = stringResource(R.string.about_thanks_hint),
                        avatarRes = R.drawable.about_partner_avatar,
                        showDivider = false,
                    )
                }
            }
        }
    }

    val sheetState = updateUiState as? SdeUpdateUiState.Sheet
    if (sheetState != null) {
        key(sheetState.instanceId) {
            SdeUpdateBottomSheet(
                content = sheetState.content,
                onDismiss = sdeUpdateViewModel::dismissSheet,
                onConfirmDownload = sdeUpdateViewModel::startDownload,
                onRetryDownload = sdeUpdateViewModel::retryDownload,
                onRetryCheck = sdeUpdateViewModel::retryCheck,
            )
        }
    }
}

@Composable
private fun AboutHeader(
    versionLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AboutAppIcon()
        Text(
            text = stringResource(R.string.app_name),
            color = colorResource(R.color.text_primary),
            fontSize = dimensionResource(R.dimen.about_name_text_size).value.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = dimensionResource(R.dimen.about_name_gap)),
        )
        Text(
            text = versionLabel,
            color = colorResource(R.color.text_caption),
            fontSize = dimensionResource(R.dimen.about_version_text_size).value.sp,
            modifier = Modifier.padding(top = dimensionResource(R.dimen.about_version_gap)),
        )
    }
}

@Composable
private fun AboutAppIcon() {
    val iconSize = dimensionResource(R.dimen.about_app_icon_size)
    val darkTheme = isSystemInDarkTheme()
    val glowColor = colorResource(R.color.about_icon_glow)
    val glowWidth = dimensionResource(R.dimen.about_icon_glow_width)
    val glowBlur = dimensionResource(R.dimen.about_icon_glow_blur)
    val glowOutset = if (darkTheme) glowWidth + glowBlur else 0.dp
    val cornerRadius = dimensionResource(R.dimen.about_app_icon_corner_radius)
    val iconShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = Modifier.size(iconSize + glowOutset * 2),
        contentAlignment = Alignment.Center,
    ) {
        if (darkTheme) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val iconSizePx = iconSize.toPx()
                        val inset = glowOutset.toPx()
                        val strokePx = glowWidth.toPx()
                        val blurPx = glowBlur.toPx()
                        val cornerPx = cornerRadius.toPx()
                        val path = android.graphics.Path().apply {
                            addRoundRect(
                                android.graphics.RectF(0f, 0f, iconSizePx, iconSizePx),
                                cornerPx,
                                cornerPx,
                                android.graphics.Path.Direction.CW,
                            )
                        }
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                color = glowColor.toArgb()
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = strokePx
                                if (blurPx > 0f) {
                                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                                }
                            }
                            canvas.nativeCanvas.save()
                            canvas.nativeCanvas.translate(inset, inset)
                            canvas.nativeCanvas.drawPath(path, paint)
                            canvas.nativeCanvas.restore()
                        }
                    },
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_pyerite_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .size(iconSize)
                .clip(iconShape),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun AboutSdeRow(
    hint: String,
    statusLabel: String?,
    onClick: () -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            itemName = stringResource(R.string.about_sde_database),
            itemHint = hint,
            onClick = onClick,
        ),
        showDivider = false,
        leadingContent = { iconSize ->
            AboutMaterialIcon(image = Icons.Outlined.Storage, iconSize = iconSize)
        },
        titleTrailingContent = if (statusLabel == null) {
            null
        } else {
            {
                Text(
                    text = statusLabel,
                    color = colorResource(R.color.text_caption),
                    fontSize = dimensionResource(R.dimen.about_status_text_size).value.sp,
                )
            }
        },
    )
}

@Composable
private fun AboutAvatarRow(
    title: String,
    hint: String,
    avatarRes: Int,
    showDivider: Boolean,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            itemName = title,
            itemHint = hint,
            showChevron = false,
            onClick = null,
        ),
        showDivider = showDivider,
        leadingContent = { iconSize ->
            AboutMaterialIcon(image = Icons.Outlined.Person, iconSize = iconSize)
        },
        trailingContent = {
            AboutAvatar(
                avatarRes = avatarRes,
                contentDescription = hint,
            )
        },
    )
}

@Composable
private fun AboutLinkRow(
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            itemName = title,
            itemHint = hint,
            onClick = onClick,
        ),
        showDivider = true,
        leadingContent = { iconSize ->
            AboutMaterialIcon(image = Icons.Outlined.Link, iconSize = iconSize)
        },
    )
}

@Composable
private fun AboutMailRow(
    onClick: () -> Unit,
) {
    BaseLazyColumnItem(
        model = BaseLazyColumnItemModel(
            iconRes = R.drawable.ic_character_mail,
            iconTint = colorResource(R.color.about_icon),
            itemName = stringResource(R.string.about_report_issue),
            itemHint = stringResource(R.string.about_support_email),
            onClick = onClick,
        ),
        showDivider = true,
    )
}

@Composable
private fun AboutAvatar(
    avatarRes: Int,
    contentDescription: String,
) {
    Image(
        painter = painterResource(avatarRes),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(dimensionResource(R.dimen.about_avatar_size))
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun AboutSdeUpdateStatus.statusLabel(): String? {
    return when (this) {
        AboutSdeUpdateStatus.UpdateAvailable -> stringResource(R.string.sde_update_available)
        AboutSdeUpdateStatus.UpToDate -> stringResource(R.string.sde_update_up_to_date)
        AboutSdeUpdateStatus.Checking -> stringResource(R.string.sde_update_checking)
        AboutSdeUpdateStatus.CheckFailed -> stringResource(R.string.about_sde_check_failed)
        AboutSdeUpdateStatus.Unknown -> null
    }
}
