package net.thunderbird.feature.mail.message.list

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountId

// TODO move to sync or message use case
/**
 * Decides whether deleting a message in the app moves it to the trash folder or deletes it immediately.
 *
 * Note: This only applies to local messages. What remote operation is performed when deleting a message is controlled
 * by [LegacyAccount.deletePolicy].
 */
interface LocalDeleteOperationDecider {
    /**
     * Returns `true` if messages in the given folder should be deleted immediately
     * rather than moved to the trash folder.
     *
     * @param accountId The [AccountId] the message belongs to.
     * @param folderId The ID of the folder the message is being deleted from.
     */
    fun isDeleteImmediately(accountId: AccountId, folderId: Long): Boolean
}
