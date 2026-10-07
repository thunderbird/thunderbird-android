package com.fsck.k9.notification

import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import net.thunderbird.feature.account.AccountId

interface NotificationStrategy {

    fun shouldNotifyForMessage(
        accountId: AccountId,
        localFolder: LocalFolder,
        message: LocalMessage,
        isOldMessage: Boolean,
    ): Boolean
}
