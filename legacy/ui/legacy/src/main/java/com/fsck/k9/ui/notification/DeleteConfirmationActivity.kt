package com.fsck.k9.ui.notification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.controller.MessageReferenceHelper
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.fragment.ConfirmationDialogFragment
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.fsck.k9.notification.NotificationActionIntents
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.fsck.k9.ui.base.ThemeType
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import org.koin.android.ext.android.inject

class DeleteConfirmationActivity : BaseActivity(ThemeType.DIALOG), ConfirmationDialogFragmentListener {
    private val messagingController: MessagingController by inject()

    private lateinit var accountId: AccountId
    private lateinit var messagesToDelete: List<MessageReference>

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        extractExtras()

        if (savedInstanceState == null) {
            val dialogFragment = createConfirmationDialogFragment()
            dialogFragment.show(supportFragmentManager, DIALOG_TAG)
        }
    }

    private fun extractExtras() {
        val accountId = intent.getStringExtra(EXTRA_ACCOUNT_UUID)?.let { AccountIdFactory.of(it) }
        val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
        val messagesToDelete = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

        requireNotNull(accountId) { "$EXTRA_ACCOUNT_UUID can't be null" }
        requireNotNull(messagesToDelete) { "$EXTRA_MESSAGE_REFERENCES can't be null" }
        require(messagesToDelete.isNotEmpty()) { "$EXTRA_MESSAGE_REFERENCES can't be empty" }

        this.accountId = accountId
        this.messagesToDelete = messagesToDelete
    }

    private fun createConfirmationDialogFragment(): DialogFragment {
        val messageCount = messagesToDelete.size
        val message = resources.getQuantityString(R.plurals.dialog_confirm_delete_messages, messageCount, messageCount)

        return ConfirmationDialogFragment.newInstance(
            DIALOG_ID,
            getString(R.string.dialog_confirm_delete_title),
            message,
            getString(R.string.dialog_confirm_delete_confirm_button),
            getString(R.string.dialog_confirm_delete_cancel_button),
        )
    }

    override fun doPositiveClick(dialogId: Int) {
        deleteAndFinish()
    }

    override fun doNegativeClick(dialogId: Int) {
        finish()
    }

    override fun dialogCancelled(dialogId: Int) {
        finish()
    }

    private fun deleteAndFinish() {
        cancelNotifications()
        triggerDelete()
        finish()
    }

    private fun cancelNotifications() {
        for (messageReference in messagesToDelete) {
            messagingController.cancelNotificationForMessage(accountId, messageReference)
        }
    }

    private fun triggerDelete() {
        val intent = NotificationActionIntents.createDeleteAllMessagesIntent(
            this,
            accountId,
            messagesToDelete,
        )
        startService(intent)
    }

    companion object {
        private const val EXTRA_ACCOUNT_UUID = "accountUuid"
        private const val EXTRA_MESSAGE_REFERENCES = "messageReferences"
        private const val DIALOG_ID = 1
        private const val DIALOG_TAG = "dialog"

        fun getIntent(context: Context, messageReference: MessageReference): Intent {
            return getIntent(context, listOf(messageReference))
        }

        fun getIntent(context: Context, messageReferences: List<MessageReference>): Intent {
            val accountId = messageReferences[0].accountId
            val messageReferenceStrings = MessageReferenceHelper.toMessageReferenceStringList(messageReferences)

            return Intent(context, DeleteConfirmationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ACCOUNT_UUID, accountId.toString())
                putExtra(EXTRA_MESSAGE_REFERENCES, messageReferenceStrings)
            }
        }
    }
}
