package app.k9mail.feature.widget.unread

import android.content.SharedPreferences
import androidx.core.content.edit
import app.k9mail.feature.widget.unread.UnreadWidgetRepository.Companion.PREFS_VERSION
import app.k9mail.feature.widget.unread.UnreadWidgetRepository.Companion.PREF_VERSION_KEY
import com.fsck.k9.Preferences
import net.thunderbird.core.outcome.fold
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository

internal class UnreadWidgetMigrations(
    private val accountRepository: Preferences,
    private val folderQueryRepository: FolderQueryRepository,
) {
    suspend fun upgradePreferences(preferences: SharedPreferences, version: Int) {
        if (version < 2) rewriteFolderNameToFolderId(preferences)

        preferences.setVersion(PREFS_VERSION)
    }

    private fun SharedPreferences.setVersion(version: Int) {
        edit { putInt(PREF_VERSION_KEY, version) }
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private suspend fun rewriteFolderNameToFolderId(preferences: SharedPreferences) {
        val widgetIds = preferences.all.keys
            .filter { it.endsWith(".folder_name") }
            .map { it.split(".")[1] }

        preferences.edit {
            for (widgetId in widgetIds) {
                val accountUuid = preferences.getString("unread_widget.$widgetId", null) ?: continue
                val account = accountRepository.getAccount(accountUuid) ?: continue

                val folderServerId = preferences.getString("unread_widget.$widgetId.folder_name", null)
                if (folderServerId != null) {
                    val folderId = folderQueryRepository.findIdByServerId(account.id, FolderServerId(folderServerId))
                        .fold(onSuccess = { it }, onFailure = { null })
                    putString("unread_widget.$widgetId.folder_id", folderId?.toString())
                }

                remove("unread_widget.$widgetId.folder_name")
            }
        }
    }
}
