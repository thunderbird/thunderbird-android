package net.thunderbird.core.mail.folder.api

import net.thunderbird.core.account.BaseAccount

fun interface SpecialFolderUpdater {
    /**
     * Updates all account's special folders. If POP3, only Inbox is updated.
     */
    fun updateSpecialFolders()

    interface Factory<TAccount : BaseAccount> {
        fun create(account: TAccount): SpecialFolderUpdater
    }
}
