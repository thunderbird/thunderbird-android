package app.k9mail.feature.widget.unread

import android.content.SharedPreferences
import androidx.core.content.edit
import app.k9mail.feature.widget.unread.UnreadWidgetRepository.Companion.PREFS_VERSION
import app.k9mail.feature.widget.unread.UnreadWidgetRepository.Companion.PREF_VERSION_KEY
import app.k9mail.legacy.mailstore.FolderRepository
import com.fsck.k9.Preferences

internal class UnreadWidgetMigrations(
    private val accountRepository: Preferences,
    private val folderRepository: FolderRepository,
) {
    fun upgradePreferences(preferences: SharedPreferences, version: Int) {
        if (version < 2) rewriteFolderNameToFolderId(preferences)

        preferences.setVersion(PREFS_VERSION)
    }

    private fun SharedPreferences.setVersion(version: Int) {
        edit { putInt(PREF_VERSION_KEY, version) }
    }

    @Suppress("LoopWithTooManyJumpStatements")
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
                    val folderId = folderRepository.getFolderId(account.id, folderServerId)
                    putString("unread_widget.$widgetId.folder_id", folderId?.toString())
                }

                remove("unread_widget.$widgetId.folder_name")
            }
        }
    }
}
