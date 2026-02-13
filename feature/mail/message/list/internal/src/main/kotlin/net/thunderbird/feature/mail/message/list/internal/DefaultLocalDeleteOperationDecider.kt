package net.thunderbird.feature.mail.message.list.internal

import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.message.list.LocalDeleteOperationDecider

class DefaultLocalDeleteOperationDecider : LocalDeleteOperationDecider {
    override fun isDeleteImmediately(
        account: LegacyAccountDto,
        folderId: Long,
    ): Boolean {
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
