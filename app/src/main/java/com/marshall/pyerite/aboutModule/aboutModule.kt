package com.marshall.pyerite.aboutModule

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.marshall.pyerite.aboutModule.viewModel.AboutViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val aboutModule = module {
    viewModel {
        AboutViewModel(
            versionName = androidContext().appVersionName(),
            repository = get(),
            controller = get(),
        )
    }
}

private fun Context.appVersionName(): String {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    return info.versionName.orEmpty()
}
