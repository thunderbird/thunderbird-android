package com.fsck.k9.mailstore

import app.k9mail.legacy.account.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mail.FolderClass

/**
 * Provides imported folder settings if available, otherwise default values.
 */
class FolderSettingsProvider(val preferences: Preferences, val account: Account) {
    fun getFolderSettings(folderServerId: String): FolderSettings {
        val storage = preferences.storage
        val prefix = "${account.uuid}.$folderServerId"

        return FolderSettings(
            visibleLimit = account.displayCount,
            displayClass = storage.getString("$prefix.displayMode", null).toFolderClass(FolderClass.NO_CLASS),
            syncClass = storage.getString("$prefix.syncMode", null).toFolderClass(FolderClass.INHERITED),
            notifyClass = storage.getString("$prefix.notifyMode", null).toFolderClass(FolderClass.INHERITED),
            pushClass = storage.getString("$prefix.pushMode", null).toFolderClass(FolderClass.SECOND_CLASS),
            inTopGroup = storage.getBoolean("$prefix.inTopGroup", false),
            integrate = storage.getBoolean("$prefix.integrate", false),
        ).also {
            removeImportedFolderSettings(prefix)
        }
    }

    private fun removeImportedFolderSettings(prefix: String) {
        val editor = preferences.createStorageEditor()

        editor.remove("$prefix.displayMode")
        editor.remove("$prefix.syncMode")
        editor.remove("$prefix.notifyMode")
        editor.remove("$prefix.pushMode")
        editor.remove("$prefix.inTopGroup")
        editor.remove("$prefix.integrate")

        editor.commit()
    }

    private fun String?.toFolderClass(defaultValue: FolderClass): FolderClass {
        return if (this == null) defaultValue else FolderClass.valueOf(this)
    }
}
