package app.k9mail.feature.widget.unread

import android.content.SharedPreferences
import androidx.core.content.edit
import app.k9mail.feature.widget.unread.UnreadWidgetRepository.Companion.PREFS_VERSION
import app.k9mail.feature.widget.unread.UnreadWidgetRepository.Companion.PREF_VERSION_KEY
import net.thunderbird.components.core.outcome.fold
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.UnifiedAccountId
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository

internal class UnreadWidgetMigrations(
    private val accountManager: LegacyAccountManager,
    private val folderQueryRepository: FolderQueryRepository,
) {
    @Suppress("MagicNumber")
    suspend fun upgradePreferences(preferences: SharedPreferences, version: Int) {
        if (version < 2) rewriteFolderNameToFolderId(preferences)
        if (version < 3) rewriteLegacyUnifiedAccountId(preferences)

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
                val accountId = runCatching { AccountIdFactory.of(accountUuid) }.getOrNull() ?: continue
                val account = accountManager.findById(accountId) ?: continue

                val folderServerId = preferences.getString("unread_widget.$widgetId.folder_name", null)
                if (folderServerId != null) {
                    val folderId = folderQueryRepository.findIdByServerId(account.id, FolderServerId(folderServerId))
                        .fold(
                            onSuccess = { it },
                            onFailure = { error ->
                                when (val throwable = error.throwable) {
                                    null -> null
                                    else -> throw throwable
                                }
                            },
                        )
                    putString("unread_widget.$widgetId.folder_id", folderId?.toString())
                }

                remove("unread_widget.$widgetId.folder_name")
            }
        }
    }

    /**
     * Before the switch to [net.thunderbird.feature.account.AccountId], the unified folders widget stored a
     * special string instead of an account UUID. Rewrite it to [UnifiedAccountId].
     */
    private fun rewriteLegacyUnifiedAccountId(preferences: SharedPreferences) {
        val legacyUnifiedAccountKeys = preferences.all
            .filter { (key, value) -> WIDGET_ACCOUNT_KEY.matches(key) && value in LEGACY_UNIFIED_ACCOUNT_IDS }
            .keys

        preferences.edit {
            for (key in legacyUnifiedAccountKeys) {
                putString(key, UnifiedAccountId.toString())
            }
        }
    }

    private companion object {
        val WIDGET_ACCOUNT_KEY = Regex("unread_widget\\.\\d+")
        val LEGACY_UNIFIED_ACCOUNT_IDS = setOf("unified_folders", "unified_inbox")
    }
}
