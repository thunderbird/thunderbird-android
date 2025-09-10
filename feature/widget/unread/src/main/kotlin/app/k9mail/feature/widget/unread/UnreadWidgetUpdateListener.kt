package app.k9mail.feature.widget.unread

import app.k9mail.legacy.message.controller.SimpleMessagingListener
import com.fsck.k9.mail.Message
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.legacy.Log

class UnreadWidgetUpdateListener(
    private val unreadWidgetUpdater: UnreadWidgetUpdater,
) : SimpleMessagingListener() {

    @Suppress("TooGenericExceptionCaught")
    private fun updateUnreadWidget() {
        try {
            unreadWidgetUpdater.updateAll()
        } catch (e: Exception) {
            Log.e(e, "Error while updating unread widget(s)")
        }
    }

    override fun synchronizeMailboxRemovedMessage(
        account: LegacyAccountDto,
        folderServerId: String,
        messageServerId: String,
    ) {
        updateUnreadWidget()
    }

    override fun synchronizeMailboxNewMessage(account: LegacyAccountDto, folderServerId: String, message: Message) {
        updateUnreadWidget()
    }

    override fun folderStatusChanged(account: LegacyAccountDto, folderId: Long) {
        updateUnreadWidget()
    }
}
