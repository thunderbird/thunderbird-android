package com.fsck.k9.notification

import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import net.thunderbird.core.android.account.LegacyAccountDto

interface NotificationStrategy {

    fun shouldNotifyForMessage(
        account: LegacyAccountDto,
        localFolder: LocalFolder,
        message: LocalMessage,
        isOldMessage: Boolean,
    ): Boolean
}
