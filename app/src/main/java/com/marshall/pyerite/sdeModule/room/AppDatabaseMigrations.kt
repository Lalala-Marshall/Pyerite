package com.marshall.pyerite.sdeModule.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.marshall.pyerite.sdeModule.room.mastery.SdeMasteryTableBootstrap

/**
 * Room identity-hash migrations for the prepackaged SDE file.
 * Tables already exist in the SQLite asset — migrations are no-ops that only
 * advance [AppDatabase] version so Room accepts newly registered / aligned entities.
 */
internal object AppDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // StationEntity registered; `stations` already present in the SDE file.
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Map entities aligned to full SDE column sets (extra locale / universe cols).
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // StationEntity: nullable stationTypeID + idx_stations_solarSystemID.
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // NPC corp / faction / agent / loyalty offer tables already in the SDE file.
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // LoyaltyOfferOutputEntity: SDE DEFAULT 1/0 and nullable INTEGER PRIMARY KEY.
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // NPC / loyalty entities declare existing SDE indexes (createFromFile validates).
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Empty tables on the installed copy only; bundled EveSDE is not modified.
            db.execSQL(SdeMasteryTableBootstrap.CREATE_CERTIFICATE_SKILLS)
            db.execSQL(SdeMasteryTableBootstrap.CREATE_MASTERIES)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // MarketGroupEntity registered; `marketGroups` already present in the SDE file.
        }
    }
}
