package com.marshall.pyerite.settingsModule.ui

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.localization.AppLanguagePreference
import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.settingsModule.viewModel.SettingsViewModel
import com.marshall.pyerite.ui.golbalComponents.BaseContainer
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun LanguageSettingsPage(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val databaseLanguage by viewModel.databaseLanguage.collectAsState()
    val scrollState = rememberScrollState()
    val pageTitle = stringResource(R.string.settings_language_page)
    val onBack = navController.rememberNavigateUpAction()
    val showCollapsedTitle = rememberScrollTitleCollapsed(scrollState)
    val sectionGap = dimensionResource(R.dimen.type_detail_section_gap)
    val bottomPadding = dimensionResource(R.dimen.type_detail_bottom_padding)
    val context = LocalContext.current

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
            BaseContainer(
                title = stringResource(R.string.settings_language_section_app),
                useSystemBarsPadding = false,
            ) {
                SettingsCheckRow(
                    title = stringResource(R.string.settings_language_english),
                    selected = appLanguage == AppLanguagePreference.ENGLISH,
                    showDivider = true,
                    onClick = { viewModel.setAppLanguage(AppLanguagePreference.ENGLISH) },
                )
                SettingsCheckRow(
                    title = stringResource(R.string.settings_language_chinese),
                    selected = appLanguage == AppLanguagePreference.CHINESE,
                    showDivider = true,
                    onClick = { viewModel.setAppLanguage(AppLanguagePreference.CHINESE) },
                )
                SettingsCheckRow(
                    title = stringResource(R.string.settings_language_follow),
                    selected = appLanguage == AppLanguagePreference.FOLLOW_SYSTEM,
                    showDivider = false,
                    onClick = { viewModel.setAppLanguage(AppLanguagePreference.FOLLOW_SYSTEM) },
                )
            }
            Spacer(modifier = Modifier.height(sectionGap))
            BaseContainer(
                title = stringResource(R.string.settings_language_section_database),
                useSystemBarsPadding = false,
            ) {
                SettingsCheckRow(
                    title = stringResource(R.string.settings_language_english),
                    selected = databaseLanguage == ContentLanguage.ENGLISH,
                    showDivider = true,
                    onClick = {
                        if (viewModel.setDatabaseLanguage(ContentLanguage.ENGLISH)) {
                            (context as? Activity)?.recreate()
                        }
                    },
                )
                SettingsCheckRow(
                    title = stringResource(R.string.settings_language_chinese),
                    selected = databaseLanguage == ContentLanguage.CHINESE,
                    showDivider = false,
                    onClick = {
                        if (viewModel.setDatabaseLanguage(ContentLanguage.CHINESE)) {
                            (context as? Activity)?.recreate()
                        }
                    },
                )
            }
        }
    }
}
