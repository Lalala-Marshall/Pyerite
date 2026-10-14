package com.marshall.pyerite.sdeModule

import com.marshall.pyerite.infra.network.createApi
import com.marshall.pyerite.sdeModule.network.SdeRemoteApi
import com.marshall.pyerite.sdeModule.network.SdeRemoteDataSource
import com.marshall.pyerite.sdeModule.network.createSdeOkHttpClient
import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.sdeModule.update.SdeRemoteConfig
import com.marshall.pyerite.sdeModule.update.SdeUpdateController
import com.marshall.pyerite.sdeModule.update.SdeUpdateRepository
import com.marshall.pyerite.sdeModule.update.SdeUpdateViewModel
import com.marshall.pyerite.sdeModule.update.SdeVersionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val sdeModule = module {
    single { RoomProvider(androidContext(), get()) }
    single { SdeVersionStore(androidContext()) }
    single {
        val base = get<OkHttpClient>()
        val metadataClient = createSdeOkHttpClient(
            connectTimeoutSeconds = SdeRemoteConfig.METADATA_CONNECT_TIMEOUT_SECONDS,
            readTimeout = SdeRemoteConfig.METADATA_READ_TIMEOUT_SECONDS,
            base = base,
        )
        val downloadClient = createSdeOkHttpClient(
            connectTimeoutSeconds = SdeRemoteConfig.DOWNLOAD_CONNECT_TIMEOUT_SECONDS,
            readTimeout = SdeRemoteConfig.DOWNLOAD_READ_TIMEOUT_MINUTES,
            readTimeoutUnit = TimeUnit.MINUTES,
            base = base,
        )
        SdeRemoteDataSource(
            metadataApi = metadataClient.createApi<SdeRemoteApi>(SdeRemoteConfig.RETROFIT_BASE_URL),
            downloadClient = downloadClient,
        )
    }
    single {
        SdeUpdateRepository(
            context = androidContext(),
            remoteDataSource = get(),
            versionStore = get(),
            roomProvider = get(),
            iconManager = get(),
        )
    }
    single {
        SdeUpdateController(
            repository = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
    }
    viewModel { SdeUpdateViewModel(get()) }
}
