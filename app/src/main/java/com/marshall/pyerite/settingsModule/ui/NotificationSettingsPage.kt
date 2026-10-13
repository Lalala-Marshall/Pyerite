package com.marshall.pyerite.settingsModule.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.infra.notification.NotificationAccess
import com.marshall.pyerite.infra.notification.NotificationMasterChoice
import com.marshall.pyerite.settingsModule.viewModel.SettingsViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun NotificationSettingsPage(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val master by viewModel.notificationMaster.collectAsState()
    val calendarEnabled by viewModel.calendarNotificationsEnabled.collectAsState()
    val skillQueueEnabled by viewModel.skillQueueNotificationsEnabled.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = NotificationAccess.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val masterOn = when (master) {
        NotificationMasterChoice.DISABLED -> false
        NotificationMasterChoice.FOLLOW_PERMISSION,
        NotificationMasterChoice.ENABLED,
        -> permissionGranted
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        if (granted) viewModel.setNotificationMaster(NotificationMasterChoice.ENABLED)
    }
    val scrollState = rememberScrollState()
    val pageTitle = stringResource(R.string.settings_notifications)
    val onBack = navController.rememberNavigateUpAction()
    val showCollapsedTitle = rememberScrollTitleCollapsed(scrollState)
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)

    fun onMasterChange(wantOn: Boolean) {
        if (!wantOn) {
            viewModel.setNotificationMaster(NotificationMasterChoice.DISABLED)
            return
        }
        if (NotificationAccess.isGranted(context)) {
            viewModel.setNotificationMaster(NotificationMasterChoice.ENABLED)
            return
        }
        val activity = context as? Activity ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            viewModel.setNotificationMaster(NotificationMasterChoice.ENABLED)
            return
        }
        val asked = viewModel.hasRequestedPostNotifications()
        val rationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        if (!asked || rationale) {
            viewModel.markPostNotificationsRequested()
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(NotificationAccess.settingsIntent(context))
        }
    }

    PyeritePageScaffold(
        title = pageTitle,
        showCollapsedTitle = showCollapsedTitle,
        onBack = onBack,
    ) { topBarPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(topBarPadding)
                .verticalScroll(scrollState)
                .padding(bottom = bottomPadding),
        ) {
            PageTitle(text = pageTitle)
            Spacer(modifier = Modifier.height(sectionGap))
            BaseContainer(useSystemBarsPadding = false) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_notifications_master),
                    hint = "",
                    checked = masterOn,
                    showDivider = false,
                    onCheckedChange = ::onMasterChange,
                )
            }
            if (masterOn) {
                Spacer(modifier = Modifier.height(sectionGap))
                BaseContainer(
                    title = stringResource(R.string.settings_notification_items),
                    useSystemBarsPadding = false,
                ) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_notification_calendar),
                        hint = stringResource(R.string.settings_notification_calendar_hint),
                        checked = calendarEnabled,
                        showDivider = true,
                        onCheckedChange = viewModel::setCalendarNotificationsEnabled,
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_notification_skill_queue),
                        hint = stringResource(R.string.settings_notification_skill_queue_hint),
                        checked = skillQueueEnabled,
                        showDivider = false,
                        wrapHint = true,
                        onCheckedChange = viewModel::setSkillQueueNotificationsEnabled,
                    )
                }
            }
        }
    }
}
