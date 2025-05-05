package net.thunderbird.core.mail.folder.api

import app.k9mail.core.mail.folder.api.FolderType
import net.thunderbird.core.account.BaseAccount

interface SpecialFolderUpdater {
    /**
     * Updates all account's special folders. If POP3, only Inbox is updated.
     */
    fun updateSpecialFolders()

    fun setSpecialFolder(type: FolderType, folderId: Long?, selection: SpecialFolderSelection)

    interface Factory<TAccount : BaseAccount> {
        fun create(account: TAccount): SpecialFolderUpdater
    }
}
