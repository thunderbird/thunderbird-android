package net.thunderbird.feature.mail.folder.api

import net.thunderbird.feature.mail.account.api.BaseAccount

// TODO move to ???
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
