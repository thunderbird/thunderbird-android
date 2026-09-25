package net.thunderbird.feature.mail.folder.api

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderType

// TODO move to ???
public interface SpecialFolderUpdater {
    /**
     * Updates all account's special folders. If POP3, only Inbox is updated.
     */
    public fun updateSpecialFolders()

    /**
     * Updates all account's special folders synchronously. If POP3, only Inbox is updated.
     */
    public fun updateSpecialFoldersSync()

    public fun setSpecialFolder(type: FolderType, folderId: Long?, selection: SpecialFolderSelection)

    public interface Factory {
        public fun create(accountId: AccountId): SpecialFolderUpdater
    }
}
