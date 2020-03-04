package com.fsck.k9.ui.managefolders

import androidx.preference.PreferenceDataStore
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mailstore.LocalFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FolderSettingsDataStore(private val folder: LocalFolder) : PreferenceDataStore() {
    private val saveScope = CoroutineScope(GlobalScope.coroutineContext + Dispatchers.IO)

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return when (key) {
            "folder_settings_in_top_group" -> folder.isInTopGroup
            "folder_settings_include_in_integrated_inbox" -> folder.isIntegrate
            else -> error("Unknown key: $key")
        }
    }

    override fun putBoolean(key: String?, value: Boolean) {
        return when (key) {
            "folder_settings_in_top_group" -> updateFolder { isInTopGroup = value }
            "folder_settings_include_in_integrated_inbox" -> updateFolder { isIntegrate = value }
            else -> error("Unknown key: $key")
        }
    }

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "folder_settings_folder_display_mode" -> folder.displayClass.name
            "folder_settings_folder_sync_mode" -> folder.rawSyncClass.name
            "folder_settings_folder_notify_mode" -> folder.rawNotifyClass.name
            else -> error("Unknown key: $key")
        }
    }

    override fun putString(key: String?, value: String?) {
        val newValue = requireNotNull(value) { "'value' can't be null" }

        when (key) {
            "folder_settings_folder_display_mode" -> updateFolder { displayClass = FolderClass.valueOf(newValue) }
            "folder_settings_folder_sync_mode" -> updateFolder { syncClass = FolderClass.valueOf(newValue) }
            "folder_settings_folder_notify_mode" -> updateFolder { notifyClass = FolderClass.valueOf(newValue) }
            else -> error("Unknown key: $key")
        }
    }

    private fun updateFolder(block: LocalFolder.() -> Unit) {
        saveScope.launch {
            block(folder)
            folder.save()
        }
    }
}
