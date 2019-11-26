package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalFolder

interface NotificationStrategy {

    fun shouldNotifyForMessage(
        account: Account,
        localFolder: LocalFolder,
        message: Message,
        isOldMessage: Boolean
    ): Boolean
}
