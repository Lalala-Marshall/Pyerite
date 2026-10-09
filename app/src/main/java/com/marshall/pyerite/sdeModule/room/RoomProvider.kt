package com.marshall.pyerite.sdeModule.room

import android.content.Context
import androidx.room.Room
import com.marshall.pyerite.localization.LocaleController
import com.marshall.pyerite.localization.SdeDatabase
import com.marshall.pyerite.sdeModule.room.mastery.SdeMasteryTableBootstrap

/**
 * Opens the language-specific SDE sqlite (Room).
 *
 * All SDE Entity/Dao types live under [com.marshall.pyerite.sdeModule.room]
 * (same pattern as ESI: data module holds, business modules only call).
 * Cross-feature entry points: [AppDatabase.mapDao], [AppDatabase.sdeTypeDao]; hierarchy uses type/dogma/industry/skill DAOs.
 * Browser queries: [AppDatabase.typeDao], category/group/trait/metaGroup DAOs.
 */
class RoomProvider(
    private val context: Context,
    private val localeController: LocaleController,
) {

    @Volatile
    private var cachedDbName: String? = null

    @Volatile
    private var cachedDatabase: AppDatabase? = null

    @Synchronized
    fun closeAndInvalidate() {
        cachedDatabase?.close()
        cachedDatabase = null
        cachedDbName = null
    }

    @Synchronized
    fun getDatabase(): AppDatabase {
        val dbName = resolveDatabaseFileName()
        val existing = cachedDatabase
        if (existing != null && cachedDbName == dbName) return existing

        val dbFile = context.getDatabasePath(dbName)
        check(dbFile.exists()) {
            "SDE database missing: $dbName. BundledSdeUpdater must run before Room opens."
        }
        SdeMasteryTableBootstrap.ensurePresent(dbFile)
        val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, dbName)
            .createFromFile(dbFile)
            .addMigrations(
                AppDatabaseMigrations.MIGRATION_1_2,
                AppDatabaseMigrations.MIGRATION_2_3,
                AppDatabaseMigrations.MIGRATION_3_4,
                AppDatabaseMigrations.MIGRATION_4_5,
                AppDatabaseMigrations.MIGRATION_5_6,
                AppDatabaseMigrations.MIGRATION_6_7,
                AppDatabaseMigrations.MIGRATION_7_8,
                AppDatabaseMigrations.MIGRATION_8_9,
            )
            .build()
        cachedDbName = dbName
        cachedDatabase = database
        return database
    }

    private fun resolveDatabaseFileName(): String {
        val preferred = localeController.resolveDatabaseFileName()
        if (context.getDatabasePath(preferred).exists()) return preferred
        return SdeDatabase.ZH_FILE_NAME
    }
}
