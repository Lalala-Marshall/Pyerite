package com.marshall.pyerite.sdeModule.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marshall.pyerite.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdeUpdateBottomSheet(
    content: SdeUpdateSheetContent,
    onDismiss: () -> Unit,
    onConfirmDownload: () -> Unit,
    onRetryDownload: () -> Unit,
    onRetryCheck: () -> Unit,
) {
    val isDismissLocked = content.isDismissLocked()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newState ->
            !isDismissLocked || newState != SheetValue.Hidden
        },
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (!isDismissLocked) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        sheetGesturesEnabled = !isDismissLocked,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = !isDismissLocked,
            shouldDismissOnClickOutside = !isDismissLocked,
        ),
        containerColor = colorResource(R.color.second_background),
    ) {
        SdeUpdateSheetBody(
            content = content,
            onDismiss = onDismiss,
            onConfirmDownload = onConfirmDownload,
            onRetryDownload = onRetryDownload,
            onRetryCheck = onRetryCheck,
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun SdeUpdateSheetBody(
    content: SdeUpdateSheetContent,
    onDismiss: () -> Unit,
    onConfirmDownload: () -> Unit,
    onRetryDownload: () -> Unit,
    onRetryCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val captionColor = colorResource(R.color.text_caption)
    val textColor = colorResource(R.color.text_primary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = sheetTitle(content),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )

        when (content) {
            is SdeUpdateSheetContent.Checking -> {
                SdeUpdateBuildInfo(
                    installedBuild = content.installedBuild,
                    remoteBuild = null,
                    installedIconVersion = content.installedIconVersion,
                    remoteIconVersion = null,
                    versionVisibility = SdeUpdateVersionVisibility.All,
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                Text(
                    text = stringResource(R.string.sde_update_checking_message),
                    fontSize = 14.sp,
                    color = captionColor,
                )
            }

            is SdeUpdateSheetContent.CheckFailed -> {
                SdeUpdateBuildInfo(
                    installedBuild = content.installedBuild,
                    remoteBuild = null,
                    installedIconVersion = content.installedIconVersion,
                    remoteIconVersion = null,
                    versionVisibility = SdeUpdateVersionVisibility.All,
                )
                Text(
                    text = resolveCheckFailedMessage(content.message),
                    fontSize = 14.sp,
                    color = captionColor,
                )
                Text(
                    text = stringResource(R.string.sde_update_check_failed_hint),
                    fontSize = 14.sp,
                    color = captionColor,
                )
                SdeUpdateSheetActions(
                    primaryLabel = stringResource(R.string.sde_update_check_manually),
                    onPrimaryClick = onRetryCheck,
                    secondaryLabel = stringResource(R.string.sde_update_cancel),
                    onSecondaryClick = onDismiss,
                    sideBySide = true,
                )
            }

            is SdeUpdateSheetContent.Prompt -> {
                SdeUpdateBuildInfo(
                    installedBuild = content.installedBuild,
                    remoteBuild = content.remoteBuild,
                    installedIconVersion = content.installedIconVersion,
                    remoteIconVersion = content.remoteIconVersion,
                    versionVisibility = content.versionVisibility,
                )
                Text(
                    text = stringResource(R.string.sde_update_prompt_message),
                    fontSize = 14.sp,
                    color = captionColor,
                )
                SdeUpdateSheetActions(
                    primaryLabel = stringResource(R.string.sde_update_confirm),
                    onPrimaryClick = onConfirmDownload,
                    secondaryLabel = stringResource(R.string.sde_update_cancel),
                    onSecondaryClick = onDismiss,
                    sideBySide = true,
                )
            }

            is SdeUpdateSheetContent.Downloading -> {
                SdeUpdateBuildInfo(
                    installedBuild = content.installedBuild,
                    remoteBuild = content.remoteBuild,
                    installedIconVersion = content.installedIconVersion,
                    remoteIconVersion = content.remoteIconVersion,
                    versionVisibility = content.versionVisibility,
                )
                if (content.applying) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { content.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        strokeCap = StrokeCap.Butt,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
                Text(
                    text = if (content.applying) {
                        stringResource(R.string.sde_update_applying)
                    } else {
                        stringResource(R.string.sde_update_downloading, content.progressPercent)
                    },
                    fontSize = 14.sp,
                    color = captionColor,
                )
            }

            is SdeUpdateSheetContent.Completed -> {
                SdeUpdateBuildInfo(
                    installedBuild = content.previousBuild,
                    remoteBuild = content.newBuild,
                    installedIconVersion = content.previousIconVersion,
                    remoteIconVersion = content.newIconVersion,
                    versionVisibility = content.versionVisibility,
                )
                Text(
                    text = stringResource(R.string.sde_update_complete_message),
                    fontSize = 14.sp,
                    color = captionColor,
                )
                SdeUpdateSheetActions(
                    primaryLabel = stringResource(R.string.sde_update_ok),
                    onPrimaryClick = onDismiss,
                    secondaryLabel = null,
                    onSecondaryClick = {},
                )
            }

            is SdeUpdateSheetContent.Info -> {
                SdeUpdateBuildInfo(
                    installedBuild = content.installedBuild,
                    remoteBuild = null,
                    installedIconVersion = content.installedIconVersion,
                    remoteIconVersion = null,
                    versionVisibility = SdeUpdateVersionVisibility.All,
                )
                Text(
                    text = stringResource(R.string.sde_update_up_to_date),
                    fontSize = 14.sp,
                    color = captionColor,
                )
                SdeUpdateSheetActions(
                    primaryLabel = stringResource(R.string.sde_update_ok),
                    onPrimaryClick = onDismiss,
                    secondaryLabel = stringResource(R.string.sde_update_check_manually),
                    onSecondaryClick = onRetryCheck,
                    sideBySide = true,
                )
            }

            is SdeUpdateSheetContent.DownloadFailed -> {
                SdeUpdateBuildInfo(
                    installedBuild = content.installedBuild,
                    remoteBuild = content.remoteBuild,
                    installedIconVersion = content.installedIconVersion,
                    remoteIconVersion = content.remoteIconVersion,
                    versionVisibility = content.versionVisibility,
                )
                Text(
                    text = resolveDownloadFailedMessage(content),
                    fontSize = 14.sp,
                    color = captionColor,
                )
                if (content.cancelled) {
                    SdeUpdateSheetActions(
                        primaryLabel = stringResource(R.string.sde_update_ok),
                        onPrimaryClick = onDismiss,
                        secondaryLabel = null,
                        onSecondaryClick = {},
                    )
                } else {
                    SdeUpdateSheetActions(
                        primaryLabel = stringResource(R.string.sde_update_retry),
                        onPrimaryClick = onRetryDownload,
                        secondaryLabel = stringResource(R.string.sde_update_cancel),
                        onSecondaryClick = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun SdeUpdateBuildInfo(
    installedBuild: String?,
    remoteBuild: String?,
    installedIconVersion: Int?,
    remoteIconVersion: Int?,
    versionVisibility: SdeUpdateVersionVisibility,
) {
    val textColor = colorResource(R.color.text_primary)
    val captionColor = colorResource(R.color.text_caption)
    val accentColor = colorResource(R.color.sde_update_accent)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (versionVisibility.showDatabase) {
            SdeUpdateVersionRow(
                label = stringResource(R.string.sde_update_installed_build_label),
                value = formatBuildNumber(installedBuild),
                valueColor = textColor,
                captionColor = captionColor,
            )
            if (remoteBuild != null) {
                SdeUpdateVersionRow(
                    label = stringResource(R.string.sde_update_remote_build_label),
                    value = formatBuildNumber(remoteBuild),
                    valueColor = accentColor,
                    captionColor = captionColor,
                    emphasized = true,
                )
            }
        }
        if (versionVisibility.showIcons) {
            SdeUpdateVersionRow(
                label = stringResource(R.string.sde_update_installed_icon_version_label),
                value = formatIconVersion(installedIconVersion),
                valueColor = textColor,
                captionColor = captionColor,
            )
            if (remoteIconVersion != null) {
                SdeUpdateVersionRow(
                    label = stringResource(R.string.sde_update_remote_icon_version_label),
                    value = formatIconVersion(remoteIconVersion),
                    valueColor = accentColor,
                    captionColor = captionColor,
                    emphasized = true,
                )
            }
        }
    }
}

@Composable
private fun SdeUpdateVersionRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    captionColor: androidx.compose.ui.graphics.Color,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = captionColor,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            color = valueColor,
        )
    }
}

@Composable
private fun SdeUpdateSheetActions(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String?,
    onSecondaryClick: () -> Unit,
    sideBySide: Boolean = false,
) {
    if (sideBySide && secondaryLabel != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onSecondaryClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = secondaryLabel)
            }
            Button(
                onClick = onPrimaryClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = primaryLabel)
            }
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = primaryLabel)
        }
        if (secondaryLabel != null) {
            OutlinedButton(
                onClick = onSecondaryClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = secondaryLabel)
            }
        }
    }
}

private fun SdeUpdateSheetContent.isDismissLocked(): Boolean {
    return this is SdeUpdateSheetContent.Checking || this is SdeUpdateSheetContent.Downloading
}

@Composable
private fun sheetTitle(content: SdeUpdateSheetContent): String {
    return when (content) {
        is SdeUpdateSheetContent.Prompt -> stringResource(R.string.sde_update_sheet_title)
        is SdeUpdateSheetContent.Downloading -> {
            if (content.applying) {
                stringResource(R.string.sde_update_applying_title)
            } else {
                stringResource(R.string.sde_update_downloading_title)
            }
        }
        is SdeUpdateSheetContent.Completed -> stringResource(R.string.sde_update_complete)
        is SdeUpdateSheetContent.DownloadFailed -> {
            if (content.cancelled) {
                stringResource(R.string.sde_update_cancelled_title)
            } else {
                stringResource(R.string.sde_update_failed_title)
            }
        }
        is SdeUpdateSheetContent.Checking -> stringResource(R.string.sde_update_checking_title)
        is SdeUpdateSheetContent.CheckFailed -> stringResource(R.string.sde_update_check_failed_title)
        is SdeUpdateSheetContent.Info -> stringResource(R.string.sde_update_sheet_title)
    }
}

@Composable
private fun formatBuildNumber(build: String?): String {
    return build?.takeIf { it.isNotBlank() } ?: stringResource(R.string.sde_update_build_unknown)
}

@Composable
private fun formatIconVersion(version: Int?): String {
    return version?.toString() ?: stringResource(R.string.sde_update_build_unknown)
}

@Composable
private fun resolveCheckFailedMessage(message: String): String {
    return message.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.sde_update_network_error)
}

@Composable
private fun resolveDownloadFailedMessage(content: SdeUpdateSheetContent.DownloadFailed): String {
    if (content.cancelled) {
        return stringResource(R.string.sde_update_cancelled_message)
    }
    return if (content.message.isBlank()) {
        stringResource(R.string.sde_update_network_error)
    } else {
        stringResource(R.string.sde_update_error, content.message)
    }
}
