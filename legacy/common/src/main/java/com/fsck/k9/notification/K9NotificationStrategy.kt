package com.fsck.k9.notification

import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.legacy.di.DI
import com.fsck.k9.K9
import com.fsck.k9.QuietTimeChecker
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.K9MailLib
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.mail.toEmailAddressOrNull
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.notification.NotificationPreference

class K9NotificationStrategy(
    private val contactRepository: ContactRepository,
    private val generalSettingsManager: GeneralSettingsManager,
) : NotificationStrategy {

    @Suppress("ReturnCount")
    override fun shouldNotifyForMessage(
        account: LegacyAccount,
        localFolder: LocalFolder,
        message: LocalMessage,
        isOldMessage: Boolean,
    ): Boolean {
        if (!K9.isNotificationDuringQuietTimeEnabled && generalSettingsManager.getConfig().notification.isQuietTime) {
            Log.v("No notification: Quiet time is active")
            return false
        }

        if (!account.isNotifyNewMail) {
            Log.v("No notification: Notifications are disabled")
            return false
        }

        if (!localFolder.isVisible) {
            Log.v("No notification: Message is in folder not being displayed")
            return false
        }

        if (!localFolder.isNotificationsEnabled) {
            Log.v("No notification: Notifications are not enabled for this folder")
            return false
        }

        if (isOldMessage) {
            Log.v("No notification: Message is old")
            return false
        }

        if (message.isSet(Flag.SEEN)) {
            Log.v("No notification: Message is marked as read")
            return false
        }

        if (account.isIgnoreChatMessages && message.isChatMessage) {
            Log.v("No notification: Notifications for chat messages are disabled")
            return false
        }

        if (!account.isNotifySelfNewMail && account.isAnIdentity(message.from)) {
            Log.v("No notification: Notifications for messages from yourself are disabled")
            return false
        }

        if (account.isNotifyContactsMailOnly &&
            !contactRepository.hasAnyContactFor(message.from.asList().mapNotNull { it.address.toEmailAddressOrNull() })
        ) {
            Log.v("No notification: Message is not from a known contact")
            return false
        }

        return true
    }

    private val Message.isChatMessage: Boolean
        get() = getHeader(K9MailLib.CHAT_HEADER).isNotEmpty()

    @OptIn(ExperimentalTime::class)
    private val NotificationPreference.isQuietTime: Boolean
        get() {
            val clock = DI.get<Clock>()
            val quietTimeChecker = QuietTimeChecker(
                clock = clock,
                quietTimeStart = quietTimeStarts,
                quietTimeEnd = quietTimeEnds,
            )
            return quietTimeChecker.isQuietTime
        }
}
