package com.fsck.k9.widget.unread

import android.content.SharedPreferences
import androidx.core.content.edit
import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.FolderRepositoryManager
import com.fsck.k9.widget.unread.UnreadWidgetRepository.Companion.PREFS_VERSION
import com.fsck.k9.widget.unread.UnreadWidgetRepository.Companion.PREF_VERSION_KEY

internal class UnreadWidgetMigrations(
    private val accountRepository: Preferences,
    private val folderRepositoryManager: FolderRepositoryManager
) {
    fun upgradePreferences(preferences: SharedPreferences, version: Int) {
        if (version < 2) rewriteFolderNameToFolderId(preferences)

        preferences.setVersion(PREFS_VERSION)
    }

    private fun SharedPreferences.setVersion(version: Int) {
        edit { putInt(PREF_VERSION_KEY, version) }
    }

    private fun rewriteFolderNameToFolderId(preferences: SharedPreferences) {
        val widgetIds = preferences.all.keys
            .filter { it.endsWith(".folder_name") }
            .map { it.split(".")[1] }

        preferences.edit {
            for (widgetId in widgetIds) {
                val accountUuid = preferences.getString("unread_widget.$widgetId", null) ?: continue
                val account = accountRepository.getAccount(accountUuid) ?: continue

                val folderServerId = preferences.getString("unread_widget.$widgetId.folder_name", null)
                if (folderServerId != null) {
                    val folderRepository = folderRepositoryManager.getFolderRepository(account)
                    val folderId = folderRepository.getFolderId(folderServerId)
                    putString("unread_widget.$widgetId.folder_id", folderId?.toString())
                }

                remove("unread_widget.$widgetId.folder_name")
            }
        }
    }
}
