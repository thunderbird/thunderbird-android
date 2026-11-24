package net.thunderbird.feature.mail.message.list.internal

import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.LocalDeleteOperationDecider

internal class DefaultLocalDeleteOperationDecider(
    private val accountManager: LegacyAccountManager,
) : LocalDeleteOperationDecider {
    override fun isDeleteImmediately(
        accountId: AccountId,
        folderId: Long,
    ): Boolean {
        val account = accountManager.findById(accountId) ?: error("Account not found $accountId")

        // If there's no trash folder configured, all messages are deleted immediately.
        if (!account.hasTrashFolder()) {
            return true
        }

        // Deleting messages from the trash folder will delete them immediately.
        val isTrashFolder = folderId == account.trashFolderId

        // Messages deleted from the spam folder are deleted immediately.
        val isSpamFolder = folderId == account.spamFolderId

        return isTrashFolder || isSpamFolder
    }
}
