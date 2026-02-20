package net.thunderbird.legacy.core

import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.message.list.LocalDeleteOperationDecider

/**
 * A [LocalDeleteOperationDecider] that always returns false.
 */
class StubLocalDeleteOperationDecider : LocalDeleteOperationDecider {
    override fun isDeleteImmediately(
        account: LegacyAccountDto,
        folderId: Long,
    ): Boolean = false
}
