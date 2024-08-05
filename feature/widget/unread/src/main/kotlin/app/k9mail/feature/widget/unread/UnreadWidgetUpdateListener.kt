package app.k9mail.feature.widget.unread

import app.k9mail.legacy.account.Account
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mail.Message
import timber.log.Timber

class UnreadWidgetUpdateListener(private val unreadWidgetUpdater: UnreadWidgetUpdater) : SimpleMessagingListener() {

    @Suppress("TooGenericExceptionCaught")
    private fun updateUnreadWidget() {
        try {
            unreadWidgetUpdater.updateAll()
        } catch (e: Exception) {
            Timber.e(e, "Error while updating unread widget(s)")
        }
    }

    override fun synchronizeMailboxRemovedMessage(account: Account, folderServerId: String, messageServerId: String) {
        updateUnreadWidget()
    }

    override fun synchronizeMailboxNewMessage(account: Account, folderServerId: String, message: Message) {
        updateUnreadWidget()
    }

    override fun folderStatusChanged(account: Account, folderId: Long) {
        updateUnreadWidget()
    }
}
