package com.fsck.k9.ui.managefolders

import androidx.preference.PreferenceDataStore
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.folder.FolderDetails
import app.k9mail.legacy.mailstore.FolderRepository
import com.fsck.k9.mail.FolderClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FolderSettingsDataStore(
    private val folderRepository: FolderRepository,
    private val account: Account,
    private var folder: FolderDetails,
    private val saveScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : PreferenceDataStore() {

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return when (key) {
            "folder_settings_in_top_group" -> folder.isInTopGroup
            "folder_settings_include_in_integrated_inbox" -> folder.isIntegrate
            "folder_settings_notifications" -> folder.isNotificationsEnabled
            else -> error("Unknown key: $key")
        }
    }

    override fun putBoolean(key: String?, value: Boolean) {
        return when (key) {
            "folder_settings_in_top_group" -> updateFolder(folder.copy(isInTopGroup = value))
            "folder_settings_include_in_integrated_inbox" -> updateFolder(folder.copy(isIntegrate = value))
            "folder_settings_notifications" -> updateFolder(folder.copy(isNotificationsEnabled = value))
            else -> error("Unknown key: $key")
        }
    }

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "folder_settings_folder_display_mode" -> folder.displayClass.name
            "folder_settings_folder_sync_mode" -> folder.syncClass.name
            "folder_settings_folder_push_mode" -> folder.pushClass.name
            else -> error("Unknown key: $key")
        }
    }

    override fun putString(key: String?, value: String?) {
        val newValue = requireNotNull(value) { "'value' can't be null" }

        when (key) {
            "folder_settings_folder_display_mode" -> {
                updateFolder(folder.copy(displayClass = FolderClass.valueOf(newValue)))
            }

            "folder_settings_folder_sync_mode" -> {
                updateFolder(folder.copy(syncClass = FolderClass.valueOf(newValue)))
            }

            "folder_settings_folder_push_mode" -> {
                updateFolder(folder.copy(pushClass = FolderClass.valueOf(newValue)))
            }

            else -> error("Unknown key: $key")
        }
    }

    private fun updateFolder(newFolder: FolderDetails) {
        folder = newFolder
        saveScope.launch {
            folderRepository.updateFolderDetails(account, newFolder)
        }
    }
}
