package net.thunderbird.legacy.core

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.LocalDeleteOperationDecider

/**
 * A [LocalDeleteOperationDecider] that always returns false.
 */
class StubLocalDeleteOperationDecider : LocalDeleteOperationDecider {
    override fun isDeleteImmediately(
        accountId: AccountId,
        folderId: Long,
    ): Boolean = false
}
