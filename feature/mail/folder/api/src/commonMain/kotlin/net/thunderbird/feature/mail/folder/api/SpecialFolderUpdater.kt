package net.thunderbird.feature.mail.folder.api

import net.thunderbird.feature.account.AccountId

// TODO move to ???
interface SpecialFolderUpdater {
    /**
     * Updates all account's special folders. If POP3, only Inbox is updated.
     */
    fun updateSpecialFolders()

    /**
     * Updates all account's special folders synchronously. If POP3, only Inbox is updated.
     */
    fun updateSpecialFoldersSync()

    fun setSpecialFolder(type: FolderType, folderId: Long?, selection: SpecialFolderSelection)

    interface Factory {
        fun create(accountId: AccountId): SpecialFolderUpdater
    }
}
