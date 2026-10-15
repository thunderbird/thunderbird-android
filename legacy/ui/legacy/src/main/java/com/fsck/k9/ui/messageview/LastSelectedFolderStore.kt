package com.fsck.k9.ui.messageview

import java.util.concurrent.ConcurrentHashMap
import net.thunderbird.feature.account.AccountId

/**
 * Remembers the folder last chosen when moving or copying messages, so the folder chooser in the message view can
 * scroll to it next time.
 *
 * Kept in memory only, so it resets when the app restarts.
 */
internal class LastSelectedFolderStore {
    private val folderIds = ConcurrentHashMap<AccountId, Long>()

    fun get(accountId: AccountId): Long? = folderIds[accountId]

    fun set(accountId: AccountId, folderId: Long) {
        folderIds[accountId] = folderId
    }
}
