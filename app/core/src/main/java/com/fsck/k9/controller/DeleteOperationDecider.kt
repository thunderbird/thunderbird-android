package com.fsck.k9.controller

import com.fsck.k9.Account

/**
 * Decides whether deleting a message in the app moves it to the trash folder or deletes it immediately.
 *
 * Note: This only applies to local messages. What remote operation is performed when deleting a message is controlled
 * by [Account.deletePolicy].
 */
internal class DeleteOperationDecider {
    fun isDeleteImmediately(account: Account, folderId: Long): Boolean {
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
