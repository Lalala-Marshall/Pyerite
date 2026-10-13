package com.marshall.pyerite.charactersListModule

import androidx.lifecycle.ProcessLifecycleOwner
import com.marshall.pyerite.charactersListModule.data.CharacterOrderStore
import com.marshall.pyerite.charactersListModule.data.CharacterProfileCache
import com.marshall.pyerite.charactersListModule.data.CharacterProfileLoader
import com.marshall.pyerite.charactersListModule.data.CharacterSelectionStore
import com.marshall.pyerite.charactersListModule.data.EveSsoAuthRepository
import com.marshall.pyerite.charactersListModule.data.EveTokenLifecycleObserver
import com.marshall.pyerite.charactersListModule.viewModel.CharacterRepository
import com.marshall.pyerite.charactersListModule.viewModel.CharacterViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val charactersListModule = module {
    single { CharacterSelectionStore(androidContext()) }
    single { CharacterOrderStore(androidContext()) }
    single { CharacterProfileCache(androidContext()) }
    singleOf(::CharacterProfileLoader)
    single {
        CharacterRepository(
            tokenManager = get(),
            selectionStore = get(),
            orderStore = get(),
        )
    }
    single {
        EveSsoAuthRepository(
            remote = get(),
            tokenManager = get(),
            profileLoader = get(),
            characterRepository = get(),
            callbackBus = get(),
            pendingLoginStore = get(),
            profileCache = get(),
            skillQueueExpiryScheduler = get(),
        )
    }
    single(createdAtStart = true) {
        EveTokenLifecycleObserver(get()).also { observer ->
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        }
    }
    viewModelOf(::CharacterViewModel)
}
