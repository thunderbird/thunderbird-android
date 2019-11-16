package com.fsck.k9.preferences.migrations

import android.database.sqlite.SQLiteDatabase

internal object StorageMigrations {
    @JvmStatic
    fun upgradeDatabase(db: SQLiteDatabase, migrationsHelper: StorageMigrationsHelper) {
        val oldVersion = db.version

        if (oldVersion < 2) StorageMigrationTo2.urlEncodeUserNameAndPassword(db, migrationsHelper)
        if (oldVersion < 3) StorageMigrationTo3(db, migrationsHelper).rewriteFolderNone()
        if (oldVersion < 4) StorageMigrationTo4(db, migrationsHelper).insertSpecialFolderSelectionValues()
        if (oldVersion < 5) StorageMigrationTo5(db, migrationsHelper).fixMailCheckFrequencies()
        if (oldVersion < 6) StorageMigrationTo6(db, migrationsHelper).performLegacyMigrations()
        if (oldVersion < 7) StorageMigrationTo7(db, migrationsHelper).rewriteEnumOrdinalsToNames()
        if (oldVersion < 8) StorageMigrationTo8(db, migrationsHelper).rewriteTheme()
        if (oldVersion < 9) StorageMigrationTo9(db, migrationsHelper).disablePush()
    }
}
