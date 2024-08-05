package com.fsck.k9.notification

import app.k9mail.legacy.account.Account
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage

interface NotificationStrategy {

    fun shouldNotifyForMessage(
        account: Account,
        localFolder: LocalFolder,
        message: LocalMessage,
        isOldMessage: Boolean,
    ): Boolean
}
