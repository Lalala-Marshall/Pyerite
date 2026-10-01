package com.marshall.pyerite.corporationModule.industry.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.marshall.pyerite.R
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryBucket
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryConfig
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryDateFormatter
import com.marshall.pyerite.corporationModule.industry.model.CorporationIndustryJob
import com.marshall.pyerite.iconModule.manager.IconManager
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.ui.golbalComponents.CharacterAvatar
import com.marshall.pyerite.ui.golbalComponents.PyeriteIconShape
import com.marshall.pyerite.util.NumberDisplayFormatter
import com.marshall.pyerite.util.formatDurationDisplay
import org.koin.compose.koinInject
import java.util.Locale

@Composable
internal fun CorporationIndustryJobRow(
    job: CorporationIndustryJob,
    bucket: CorporationIndustryBucket,
    language: ContentLanguage,
    nowMs: Long,
    onClick: () -> Unit,
    iconManager: IconManager = koinInject(),
) {
    val lineGap = dimensionResource(R.dimen.corporation_industry_job_line_gap)
    val titleStyle = industryTitleStyle()
    val captionStyle = industryCaptionStyle()
    val horizontalPadding = dimensionResource(R.dimen.detail_row_horizontal_padding)
    val blueprintName = job.blueprintDisplayName(language).ifBlank {
        stringResource(R.string.corporation_industry_unknown_blueprint)
    }
    val activityName = stringResource(job.activity.titleRes)
    val familyColor = colorResource(job.activity.family.colorRes)
    val hintColor = colorResource(R.color.hint_text)
    val positiveColor = colorResource(R.color.character_status_positive)
    val runsLabel = stringResource(
        R.string.corporation_industry_runs,
        NumberDisplayFormatter.format(job.runs.toLong(), NumberDisplayFormatter.Style.FULL),
    )
    val clickable = if (job.detailTypeId > 0) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    val countdown = job.remainingMillis(nowMs)?.let { remainingMs ->
        formatDurationDisplay(
            totalSeconds = remainingMs / CorporationIndustryConfig.MILLIS_PER_SECOND,
            includeSeconds = true,
            language = language,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickable)
            .padding(
                horizontal = horizontalPadding,
                vertical = dimensionResource(R.dimen.corporation_industry_job_vertical_padding),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(lineGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.detail_row_icon_gap),
            ),
        ) {
            BlueprintIcon(
                iconFilename = job.blueprintIconFilename,
                contentDescription = blueprintName,
                iconManager = iconManager,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.character_skill_queue_icon_gap),
                    ),
                ) {
                    Icon(
                        painter = painterResource(job.activity.iconRes),
                        contentDescription = activityName,
                        tint = colorResource(R.color.text_primary),
                        modifier = Modifier.size(
                            dimensionResource(R.dimen.corporation_industry_activity_icon_size),
                        ),
                    )
                    Text(
                        text = blueprintName,
                        color = colorResource(R.color.text_primary),
                        style = titleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = runsLabel,
                        color = hintColor,
                        style = captionStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (countdown != null) {
                        Text(
                            text = countdown,
                            color = hintColor,
                            style = captionStyle,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (bucket == CorporationIndustryBucket.READY) {
                CompletedBadge()
            }
        }
        val progress = job.progressFraction(nowMs)
        IndustryProgressBar(
            progress = progress,
            fillColor = familyColor,
            animateShimmer = progress < CorporationIndustryConfig.PROGRESS_COMPLETE,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.detail_row_icon_gap),
            ),
        ) {
            val locationName = job.facilityName.ifBlank {
                job.systemDisplayName(language)
            }.ifBlank {
                stringResource(R.string.corporation_industry_unknown_location)
            }
            Text(
                text = corporationIndustryPlaceLabel(
                    name = locationName,
                    security = job.securityStatus,
                ),
                modifier = Modifier.weight(1f),
                style = captionStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            InstallerLabel(
                portraitUrl = job.installerPortraitUrl,
                name = job.installerName.ifBlank {
                    stringResource(R.string.corporation_industry_unknown_installer)
                },
            )
        }
        FourthLine(
            bucket = bucket,
            activityName = activityName,
            familyColor = familyColor,
            positiveColor = positiveColor,
            hintColor = hintColor,
            historyTitle = job.status.historyTitleRes?.let { stringResource(it) },
            endDateMillis = job.endDateMillis,
            historyTimeMillis = job.historyTimeMillis(),
            language = language,
            captionStyle = captionStyle,
        )
    }
        if (job.detailTypeId > 0) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colorResource(R.color.hint_text),
                modifier = Modifier
                    .padding(start = dimensionResource(R.dimen.detail_row_trailing_gap))
                    .size(dimensionResource(R.dimen.detail_row_chevron_size)),
            )
        }
    }
}

@Composable
internal fun corporationIndustryPlaceLabel(
    name: String,
    security: Double?,
): AnnotatedString {
    val textColor = colorResource(R.color.text_primary)
    return buildAnnotatedString {
        if (security != null) {
            withStyle(SpanStyle(color = industrySecurityColor(security))) {
                append(
                    String.format(
                        Locale.US,
                        CorporationIndustryConfig.SYSTEM_SECURITY_FORMAT,
                        security,
                    ),
                )
            }
            withStyle(SpanStyle(color = textColor)) {
                append(CorporationIndustryConfig.SECURITY_STATUS_NAME_GAP)
                append(name)
            }
        } else {
            withStyle(SpanStyle(color = textColor)) {
                append(name)
            }
        }
    }
}

@Composable
internal fun industrySecurityColor(security: Double): Color = when {
    security <= CorporationIndustryConfig.SECURITY_NEGATIVE_MAX ->
        colorResource(R.color.character_security_negative)
    security < CorporationIndustryConfig.SECURITY_LOW_MAX ->
        colorResource(R.color.character_security_low)
    else -> colorResource(R.color.character_security_high)
}

@Composable
private fun BlueprintIcon(
    iconFilename: String?,
    contentDescription: String,
    iconManager: IconManager,
) {
    val iconSize = dimensionResource(R.dimen.corporation_industry_blueprint_icon_size)
    val iconFile = iconFilename?.let { iconManager.getIconFile(it) }
    Box(
        modifier = Modifier
            .size(iconSize)
            .clip(PyeriteIconShape.shape)
            .background(colorResource(R.color.main_background)),
        contentAlignment = Alignment.Center,
    ) {
        if (iconFile != null) {
            AsyncImage(
                model = iconFile,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun IndustryProgressBar(
    progress: Float,
    fillColor: Color,
    animateShimmer: Boolean,
) {
    val shape = RoundedCornerShape(
        dimensionResource(R.dimen.skill_queue_training_progress_corner_radius),
    )
    val clamped = progress.coerceIn(
        CorporationIndustryConfig.PROGRESS_EMPTY,
        CorporationIndustryConfig.PROGRESS_COMPLETE,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.skill_queue_training_progress_height))
            .clip(shape)
            .background(colorResource(R.color.character_skill_progress_track)),
    ) {
        if (clamped > CorporationIndustryConfig.PROGRESS_EMPTY) {
            IndustryProgressFill(
                progress = clamped,
                fillColor = fillColor,
                shimmerColor = colorResource(R.color.character_skill_progress_shimmer),
                corner = dimensionResource(R.dimen.skill_queue_training_progress_corner_radius),
                animateShimmer = animateShimmer && clamped < CorporationIndustryConfig.PROGRESS_COMPLETE,
            )
        }
    }
}

@Composable
private fun IndustryProgressFill(
    progress: Float,
    fillColor: Color,
    shimmerColor: Color,
    corner: Dp,
    animateShimmer: Boolean,
) {
    val shape = RoundedCornerShape(corner)
    val shimmerProgress by rememberInfiniteTransition(label = "industry_progress_shimmer")
        .animateFloat(
            initialValue = CorporationIndustryConfig.PROGRESS_EMPTY,
            targetValue = CorporationIndustryConfig.PROGRESS_COMPLETE,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = CorporationIndustryConfig.PROGRESS_SHIMMER_DURATION_MS,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "industry_progress_shimmer_x",
        )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(progress)
            .clip(shape)
            .background(fillColor),
    ) {
        if (animateShimmer && maxWidth > Dp.Hairline) {
            val bandWidth = maxWidth * CorporationIndustryConfig.PROGRESS_SHIMMER_WIDTH_FRACTION
            val travel = maxWidth + bandWidth
            val offsetX = -bandWidth + travel * shimmerProgress
            Box(
                modifier = Modifier
                    .offset(x = offsetX)
                    .width(bandWidth)
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                shimmerColor.copy(
                                    alpha = CorporationIndustryConfig.PROGRESS_SHIMMER_PEAK_ALPHA,
                                ),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun CompletedBadge() {
    val badgeSize = dimensionResource(R.dimen.corporation_industry_status_badge_size)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .clip(CircleShape)
                .background(colorResource(R.color.character_status_positive)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.corporation_industry_completed),
                tint = colorResource(R.color.white),
                modifier = Modifier.size(
                    dimensionResource(R.dimen.corporation_industry_status_icon_size),
                ),
            )
        }
        Text(
            text = stringResource(R.string.corporation_industry_completed),
            color = colorResource(R.color.character_status_positive),
            style = industryBadgeCaptionStyle(),
            maxLines = 1,
            modifier = Modifier.padding(
                top = dimensionResource(R.dimen.corporation_industry_status_caption_gap),
            ),
        )
    }
}

@Composable
private fun InstallerLabel(
    portraitUrl: String,
    name: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.entity_profile_standing_name_gap),
        ),
    ) {
        CharacterAvatar(
            portraitUrl = portraitUrl,
            size = dimensionResource(R.dimen.corporation_industry_installer_avatar_size),
        )
        Text(
            text = name,
            color = colorResource(R.color.text_primary),
            style = industryCaptionStyle(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(
                max = dimensionResource(R.dimen.corporation_industry_installer_name_max_width),
            ),
        )
    }
}

@Composable
private fun FourthLine(
    bucket: CorporationIndustryBucket,
    activityName: String,
    familyColor: Color,
    positiveColor: Color,
    hintColor: Color,
    historyTitle: String?,
    endDateMillis: Long?,
    historyTimeMillis: Long?,
    language: ContentLanguage,
    captionStyle: TextStyle,
) {
    val left: String
    val leftColor: Color
    val right: String?
    when (bucket) {
        CorporationIndustryBucket.READY -> {
            left = stringResource(R.string.corporation_industry_deliverable, activityName)
            leftColor = positiveColor
            right = endDateMillis?.let { end ->
                stringResource(
                    R.string.corporation_industry_completed_at,
                    CorporationIndustryDateFormatter.format(end, language),
                )
            }
        }
        CorporationIndustryBucket.DUE_SOON,
        CorporationIndustryBucket.ACTIVE,
        -> {
            left = activityName
            leftColor = familyColor
            right = endDateMillis?.let { end ->
                stringResource(
                    R.string.corporation_industry_finishes_at,
                    CorporationIndustryDateFormatter.format(end, language),
                )
            }
        }
        CorporationIndustryBucket.HISTORY -> {
            left = historyTitle.orEmpty()
            leftColor = hintColor
            right = historyTimeMillis?.let { time ->
                CorporationIndustryDateFormatter.format(time, language)
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = left,
            color = leftColor,
            style = captionStyle,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (right != null) {
            Text(
                text = right,
                color = hintColor,
                style = captionStyle,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun industryTitleStyle(): TextStyle = industryTextStyle(
    sizeRes = R.dimen.corporation_industry_title_text_size,
    lineHeightRes = R.dimen.corporation_industry_title_line_height,
    fontWeight = FontWeight.Medium,
)

@Composable
private fun industryCaptionStyle(): TextStyle = industryTextStyle(
    sizeRes = R.dimen.corporation_industry_caption_text_size,
    lineHeightRes = R.dimen.corporation_industry_caption_line_height,
)

@Composable
private fun industryBadgeCaptionStyle(): TextStyle = industryTextStyle(
    sizeRes = R.dimen.corporation_industry_badge_caption_text_size,
    lineHeightRes = R.dimen.corporation_industry_badge_caption_line_height,
)

@Composable
private fun industryTextStyle(
    sizeRes: Int,
    lineHeightRes: Int,
    fontWeight: FontWeight? = null,
): TextStyle = TextStyle(
    fontSize = dimensionResource(sizeRes).value.sp,
    lineHeight = dimensionResource(lineHeightRes).value.sp,
    fontWeight = fontWeight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)
