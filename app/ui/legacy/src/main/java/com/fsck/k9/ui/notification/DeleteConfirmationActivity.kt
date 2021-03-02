package com.fsck.k9.ui.notification

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.ConfirmationDialog
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.controller.MessageReferenceHelper
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.notification.NotificationActionService
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.K9ActivityCommon
import com.fsck.k9.ui.base.ThemeType

class DeleteConfirmationActivity : AppCompatActivity() {
    private val base = K9ActivityCommon(this, ThemeType.DIALOG)

    private lateinit var account: Account
    private lateinit var messagesToDelete: List<MessageReference>

    public override fun onCreate(savedInstanceState: Bundle?) {
        base.preOnCreate()
        super.onCreate(savedInstanceState)

        extractExtras()

        showDialog(DIALOG_CONFIRM)
    }

    override fun onResume() {
        base.preOnResume()
        super.onResume()
    }

    private fun extractExtras() {
        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID)
        val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
        val messagesToDelete = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

        requireNotNull(accountUuid) { "$EXTRA_ACCOUNT_UUID can't be null" }
        requireNotNull(messagesToDelete) { "$EXTRA_MESSAGE_REFERENCES can't be null" }
        require(messagesToDelete.isNotEmpty()) { "$EXTRA_MESSAGE_REFERENCES can't be empty" }

        val account = getAccountFromUuid(accountUuid) ?: error("$EXTRA_ACCOUNT_UUID couldn't be resolved to an account")

        this.account = account
        this.messagesToDelete = messagesToDelete
    }

    public override fun onCreateDialog(dialogId: Int): Dialog {
        return when (dialogId) {
            DIALOG_CONFIRM -> createDeleteConfirmationDialog(dialogId)
            else -> super.onCreateDialog(dialogId)
        }
    }

    public override fun onPrepareDialog(dialogId: Int, dialog: Dialog) {
        when (dialogId) {
            DIALOG_CONFIRM -> {
                val alertDialog = dialog as AlertDialog
                val messageCount = messagesToDelete.size
                alertDialog.setMessage(
                    resources.getQuantityString(R.plurals.dialog_confirm_delete_messages, messageCount, messageCount)
                )
            }
            else -> super.onPrepareDialog(dialogId, dialog)
        }
    }

    private fun getAccountFromUuid(accountUuid: String): Account? {
        val preferences = Preferences.getPreferences(this)
        return preferences.getAccount(accountUuid)
    }

    private fun createDeleteConfirmationDialog(dialogId: Int): Dialog {
        return ConfirmationDialog.create(
            this,
            dialogId,
            R.string.dialog_confirm_delete_title,
            "",
            R.string.dialog_confirm_delete_confirm_button,
            R.string.dialog_confirm_delete_cancel_button,
            { deleteAndFinish() },
            { finish() }
        )
    }

    private fun deleteAndFinish() {
        cancelNotifications()
        triggerDelete()
        finish()
    }

    private fun cancelNotifications() {
        val controller = MessagingController.getInstance(this)
        for (messageReference in messagesToDelete) {
            controller.cancelNotificationForMessage(account, messageReference)
        }
    }

    private fun triggerDelete() {
        val intent = NotificationActionService.createDeleteAllMessagesIntent(this, account.uuid, messagesToDelete)
        startService(intent)
    }

    companion object {
        private const val EXTRA_ACCOUNT_UUID = "accountUuid"
        private const val EXTRA_MESSAGE_REFERENCES = "messageReferences"
        private const val DIALOG_CONFIRM = 1

        @JvmStatic
        fun getIntent(context: Context, messageReference: MessageReference): Intent {
            return getIntent(context, listOf(messageReference))
        }

        @JvmStatic
        fun getIntent(context: Context, messageReferences: List<MessageReference>): Intent {
            val accountUuid = messageReferences[0].accountUuid
            val messageReferenceStrings = MessageReferenceHelper.toMessageReferenceStringList(messageReferences)

            return Intent(context, DeleteConfirmationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
                putExtra(EXTRA_MESSAGE_REFERENCES, messageReferenceStrings)
            }
        }
    }
}
