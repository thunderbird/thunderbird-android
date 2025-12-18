package net.thunderbird.core.preference

import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

private const val OLD_KEY = "account_setup_auto_expand_folder"
private const val NEW_KEY = "auto_select_folder"

class FolderListPreferenceMigration(
    private val storage: Storage,
    private val editor: StorageEditor,
) {

    fun apply() {
        if (storage.contains(OLD_KEY)) {
            val value = storage.getString(OLD_KEY)
            editor.putString(NEW_KEY, value)
            editor.remove(OLD_KEY)
        }
    }
}
