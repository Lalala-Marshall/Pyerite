package com.marshall.pyerite.application

import android.app.Application
import com.marshall.pyerite.infra.appModule
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.settingsModule.settingsModule
import com.marshall.pyerite.ui.theme.ThemeModeStore
import com.marshall.pyerite.infra.network.networkModule
import com.marshall.pyerite.aboutModule.aboutModule
import com.marshall.pyerite.sdeModule.update.BundledSdeUpdater
import com.marshall.pyerite.sdeModule.update.SdeUpdateController
import com.marshall.pyerite.sdeModule.update.SdeUpdateLog
import com.marshall.pyerite.sdeModule.sdeModule
import com.marshall.pyerite.characterCalendarModule.characterCalendarModule
import com.marshall.pyerite.characterIndustryModule.characterIndustryModule
import com.marshall.pyerite.characterTransactionsModule.characterTransactionsModule
import com.marshall.pyerite.characterTransactionsModule.data.CharacterTransactionsDiskCache
import com.marshall.pyerite.characterWalletJournalModule.characterWalletJournalModule
import com.marshall.pyerite.characterWalletJournalModule.data.CharacterWalletJournalDiskCache
import com.marshall.pyerite.characterClonesModule.characterClonesModule
import com.marshall.pyerite.loyaltyPointsModule.loyaltyPointsModule
import com.marshall.pyerite.peoplePlacesModule.peoplePlacesModule
import com.marshall.pyerite.characterMailModule.characterMailModule
import com.marshall.pyerite.characterMasteryModule.characterMasteryModule
import com.marshall.pyerite.characterSheetModule.characterSheetModule
import com.marshall.pyerite.entityProfileModule.entityProfileModule
import com.marshall.pyerite.contractsCommon.contractsCommonModule
import com.marshall.pyerite.contractListModule.contractListModule
import com.marshall.pyerite.corporationModule.corporationModule
import com.marshall.pyerite.personalAssetsModule.personalAssetsModule
import com.marshall.pyerite.personalPropertyModule.personalPropertyModule
import com.marshall.pyerite.characterSkillsModule.characterSkillsModule
import com.marshall.pyerite.charactersListModule.charactersListModule
import com.marshall.pyerite.databaseHierarchyModule.databaseHierarchyModule
import com.marshall.pyerite.esiModule.esiModule
import com.marshall.pyerite.eveAuthModule.eveAuthModule
import com.marshall.pyerite.iconModule.iconModule
import com.marshall.pyerite.industryCommon.industryCommonModule
import com.marshall.pyerite.regionMarketModule.regionMarketModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class PyeriteApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ThemeModeStore(this).applyCurrent()
        LocaleController(this).applyAppLanguage()

        // SDE must exist before auth restore (createdAtStart) resolves skill/location names.
        runBlocking(Dispatchers.IO) {
            BundledSdeUpdater.ensureUpToDate(this@PyeriteApp)
        }

        startKoin {
            androidContext(this@PyeriteApp)
            modules(
                appModule,
                networkModule,
                iconModule,
                sdeModule,
                eveAuthModule,
                esiModule,
                databaseHierarchyModule,
                charactersListModule,
                characterSheetModule,
                characterClonesModule,
                characterSkillsModule,
                characterMasteryModule,
                characterMailModule,
                characterCalendarModule,
                personalPropertyModule,
                personalAssetsModule,
                contractsCommonModule,
                industryCommonModule,
                contractListModule,
                characterTransactionsModule,
                characterWalletJournalModule,
                characterIndustryModule,
                corporationModule,
                loyaltyPointsModule,
                peoplePlacesModule,
                entityProfileModule,
                regionMarketModule,
                settingsModule,
                aboutModule,
            )
        }

        applicationScope.launch {
            SdeUpdateLog.d("PyeriteApp", "starting background update check")
            GlobalContext.get().get<SdeUpdateController>().checkForUpdates()
        }
        applicationScope.launch(Dispatchers.IO) {
            val koin = GlobalContext.get()
            koin.get<CharacterTransactionsDiskCache>().removeExpiredTransactions()
            koin.get<CharacterWalletJournalDiskCache>().removeExpiredJournal()
        }
    }
}
