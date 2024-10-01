package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.MigrationsHelper

@Suppress("MagicNumber")
object Migrations {
    @JvmStatic
    fun upgradeDatabase(db: SQLiteDatabase, migrationsHelper: MigrationsHelper) {
        val oldVersion = db.version

        if (oldVersion < 62) MigrationTo62.addServerIdColumnToFoldersTable(db)
        if (oldVersion < 64) MigrationTo64.addExtraValuesTables(db)
        if (oldVersion < 65) MigrationTo65.addLocalOnlyColumnToFoldersTable(db, migrationsHelper)
        if (oldVersion < 66) MigrationTo66.addEncryptionTypeColumnToMessagesTable(db)
        if (oldVersion < 67) MigrationTo67.addTypeColumnToFoldersTable(db, migrationsHelper)
        if (oldVersion < 68) MigrationTo68.addOutboxStateTable(db)
        if (oldVersion < 69) MigrationTo69(db).createPendingDelete()
        if (oldVersion < 70) MigrationTo70(db).removePushState()
        if (oldVersion < 71) MigrationTo71(db).cleanUpFolderClass()
        if (oldVersion < 72) MigrationTo72(db).createMessagePartsRootIndex()
        if (oldVersion < 73) MigrationTo73(db).rewritePendingCommandsToUseFolderIds()
        if (oldVersion < 74) MigrationTo74(db, migrationsHelper.account).removeDeletedMessages()
        if (oldVersion < 75) MigrationTo75(db, migrationsHelper).updateAccountWithSpecialFolderIds()
        if (oldVersion < 76) MigrationTo76(db, migrationsHelper).cleanUpSpecialLocalFolders()
        // 77: No longer necessary
        if (oldVersion < 78) MigrationTo78(db).removeServerIdFromLocalFolders()
        if (oldVersion < 79) MigrationTo79(db).updateDeleteMessageTrigger()
        if (oldVersion < 80) MigrationTo80(db).rewriteLastUpdatedColumn()
        if (oldVersion < 81) MigrationTo81(db).addNotificationsTable()
        if (oldVersion < 82) MigrationTo82(db).addNewMessageColumn()
        if (oldVersion < 83) MigrationTo83(db, migrationsHelper).rewriteHighestKnownUid()
        if (oldVersion < 84) MigrationTo84(db).rewriteAddresses()
        if (oldVersion < 85) MigrationTo85(db, migrationsHelper).addFoldersNotificationsEnabledColumn()
        if (oldVersion < 86) MigrationTo86(db, migrationsHelper).addFoldersPushEnabledColumn()
        if (oldVersion < 87) MigrationTo87(db, migrationsHelper).addFoldersSyncEnabledColumn()
    }
}
