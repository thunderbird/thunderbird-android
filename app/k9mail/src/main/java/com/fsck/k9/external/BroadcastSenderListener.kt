package com.fsck.k9.external

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fsck.k9.Account
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import timber.log.Timber

class BroadcastSenderListener(private val context: Context) : SimpleMessagingListener() {

    private fun broadcastIntent(action: String, account: Account, folder: String, message: Message) {
        val uri = Uri.parse("email://messages/" + account.accountNumber + "/" + Uri.encode(folder) + "/" + Uri.encode(message.uid))
        val intent = Intent(action, uri)
        intent.putExtra(BroadcastIntents.EXTRA_ACCOUNT, account.description)
        intent.putExtra(BroadcastIntents.EXTRA_FOLDER, folder)
        intent.putExtra(BroadcastIntents.EXTRA_SENT_DATE, message.sentDate)
        intent.putExtra(BroadcastIntents.EXTRA_FROM, Address.toString(message.from))
        intent.putExtra(BroadcastIntents.EXTRA_TO, Address.toString(message.getRecipients(Message.RecipientType.TO)))
        intent.putExtra(BroadcastIntents.EXTRA_CC, Address.toString(message.getRecipients(Message.RecipientType.CC)))
        intent.putExtra(BroadcastIntents.EXTRA_BCC, Address.toString(message.getRecipients(Message.RecipientType.BCC)))
        intent.putExtra(BroadcastIntents.EXTRA_SUBJECT, message.subject)
        intent.putExtra(BroadcastIntents.EXTRA_FROM_SELF, account.isAnIdentity(message.from))
        context.sendBroadcast(intent)

        Timber.d("Broadcasted: action=%s account=%s folder=%s message uid=%s",
                action,
                account.description,
                folder,
                message.uid)
    }

    override fun synchronizeMailboxRemovedMessage(account: Account, folderServerId: String, message: Message) {
        broadcastIntent(BroadcastIntents.ACTION_EMAIL_DELETED, account, folderServerId, message)
    }

    override fun messageDeleted(account: Account, folderServerId: String, message: Message) {
        broadcastIntent(BroadcastIntents.ACTION_EMAIL_DELETED, account, folderServerId, message)
    }

    override fun synchronizeMailboxNewMessage(account: Account, folderServerId: String, message: Message) {
        broadcastIntent(BroadcastIntents.ACTION_EMAIL_RECEIVED, account, folderServerId, message)
    }

    override fun folderStatusChanged(account: Account, folderServerId: String, unreadMessageCount: Int) {
        // let observers know a change occurred
        val intent = Intent(BroadcastIntents.ACTION_REFRESH_OBSERVER, null)
        intent.putExtra(BroadcastIntents.EXTRA_ACCOUNT, account.description)
        intent.putExtra(BroadcastIntents.EXTRA_FOLDER, folderServerId)
        context.sendBroadcast(intent)
    }
}
