package com.fsck.k9.notification

import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.core.common.mail.toEmailAddressOrNull
import app.k9mail.legacy.account.Account
import com.fsck.k9.K9
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.K9MailLib
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import timber.log.Timber

class K9NotificationStrategy(
    private val contactRepository: ContactRepository,
) : NotificationStrategy {

    override fun shouldNotifyForMessage(
        account: Account,
        localFolder: LocalFolder,
        message: LocalMessage,
        isOldMessage: Boolean,
    ): Boolean {
        if (!K9.isNotificationDuringQuietTimeEnabled && K9.isQuietTime) {
            Timber.v("No notification: Quiet time is active")
            return false
        }

        if (!account.isNotifyNewMail) {
            Timber.v("No notification: Notifications are disabled")
            return false
        }

        if (!localFolder.isVisible) {
            Timber.v("No notification: Message is in folder not being displayed")
            return false
        }

        if (!localFolder.isNotificationsEnabled) {
            Timber.v("No notification: Notifications are not enabled for this folder")
            return false
        }

        if (isOldMessage) {
            Timber.v("No notification: Message is old")
            return false
        }

        if (message.isSet(Flag.SEEN)) {
            Timber.v("No notification: Message is marked as read")
            return false
        }

        if (account.isIgnoreChatMessages && message.isChatMessage) {
            Timber.v("No notification: Notifications for chat messages are disabled")
            return false
        }

        if (!account.isNotifySelfNewMail && account.isAnIdentity(message.from)) {
            Timber.v("No notification: Notifications for messages from yourself are disabled")
            return false
        }

        if (account.isNotifyContactsMailOnly &&
            !contactRepository.hasAnyContactFor(message.from.asList().mapNotNull { it.address.toEmailAddressOrNull() })
        ) {
            Timber.v("No notification: Message is not from a known contact")
            return false
        }

        return true
    }

    private val Message.isChatMessage: Boolean
        get() = getHeader(K9MailLib.CHAT_HEADER).isNotEmpty()
}
