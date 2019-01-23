package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Folder
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mail.Message

interface NotificationStrategy {

    fun shouldNotifyForMessage(account: Account,
                               localFolder: LocalFolder,
                               message: Message,
                               isOldMessage:Boolean):Boolean
}