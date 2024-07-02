package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

internal object StorageMigrations {
    @Suppress("MagicNumber", "CyclomaticComplexMethod")
    @JvmStatic
    fun upgradeDatabase(db: SQLiteDatabase, migrationsHelper: StorageMigrationHelper) {
        val oldVersion = db.version

        if (oldVersion < 2) StorageMigrationTo2.urlEncodeUserNameAndPassword(db, migrationsHelper)
        if (oldVersion < 3) StorageMigrationTo3(db, migrationsHelper).rewriteFolderNone()
        if (oldVersion < 4) StorageMigrationTo4(db, migrationsHelper).insertSpecialFolderSelectionValues()
        if (oldVersion < 5) StorageMigrationTo5(db, migrationsHelper).fixMailCheckFrequencies()
        if (oldVersion < 6) StorageMigrationTo6(db, migrationsHelper).performLegacyMigrations()
        if (oldVersion < 7) StorageMigrationTo7(db, migrationsHelper).rewriteEnumOrdinalsToNames()
        if (oldVersion < 8) StorageMigrationTo8(db, migrationsHelper).rewriteTheme()
        // 9: "Temporarily disable Push" is no longer necessary
        if (oldVersion < 10) StorageMigrationTo10(db, migrationsHelper).removeSavedFolderSettings()
        if (oldVersion < 11) StorageMigrationTo11(db, migrationsHelper).upgradeMessageViewContentFontSize()
        if (oldVersion < 12) StorageMigrationTo12(db, migrationsHelper).removeStoreAndTransportUri()
        if (oldVersion < 13) StorageMigrationTo13(db, migrationsHelper).renameHideSpecialAccounts()
        if (oldVersion < 14) StorageMigrationTo14(db, migrationsHelper).disablePushFoldersForNonImapAccounts()
        if (oldVersion < 15) StorageMigrationTo15(db, migrationsHelper).rewriteIdleRefreshInterval()
        if (oldVersion < 16) StorageMigrationTo16(db, migrationsHelper).changeDefaultRegisteredNameColor()
        if (oldVersion < 17) StorageMigrationTo17(db, migrationsHelper).rewriteNotificationLightSettings()
        if (oldVersion < 18) StorageMigrationTo18(db, migrationsHelper).rewriteImapCompressionSettings()
        if (oldVersion < 19) StorageMigrationTo19(db, migrationsHelper).markGmailAccounts()
        if (oldVersion < 20) StorageMigrationTo20(db, migrationsHelper).fixIdentities()
        if (oldVersion < 21) StorageMigrationTo21(db, migrationsHelper).createPostRemoveNavigationSetting()
        if (oldVersion < 22) StorageMigrationTo22(db, migrationsHelper).fixServerSettings()
        if (oldVersion < 23) StorageMigrationTo23(db, migrationsHelper).renameSendClientId()
        if (oldVersion < 24) StorageMigrationTo24(db, migrationsHelper).removeLegacyAuthenticationModes()
        if (oldVersion < 25) StorageMigrationTo25(db, migrationsHelper).convertToAuthTypeNone()
    }
}
