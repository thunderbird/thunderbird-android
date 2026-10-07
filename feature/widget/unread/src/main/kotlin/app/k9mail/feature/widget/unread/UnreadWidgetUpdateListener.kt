package app.k9mail.feature.widget.unread

import app.k9mail.legacy.message.controller.SimpleMessagingListener
import com.fsck.k9.mail.Message
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId

private const val TAG = "UnreadWidgetUpdateListener"

class UnreadWidgetUpdateListener(
    private val unreadWidgetUpdater: UnreadWidgetUpdater,
    private val logger: Logger,
) : SimpleMessagingListener() {

    @Suppress("TooGenericExceptionCaught")
    private fun updateUnreadWidget() {
        try {
            unreadWidgetUpdater.updateAll()
        } catch (e: Exception) {
            logger.error(TAG, e) { "Error while updating unread widget(s)" }
        }
    }

    override fun synchronizeMailboxRemovedMessage(
        accountId: AccountId,
        folderServerId: String,
        messageServerId: String,
    ) {
        updateUnreadWidget()
    }

    override fun synchronizeMailboxNewMessage(accountId: AccountId, folderServerId: String, message: Message) {
        updateUnreadWidget()
    }

    override fun folderStatusChanged(accountId: AccountId, folderId: Long) {
        updateUnreadWidget()
    }
}
