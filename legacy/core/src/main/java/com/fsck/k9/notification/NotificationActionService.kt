package com.fsck.k9.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessageReferenceHelper
import com.fsck.k9.controller.MessagingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class NotificationActionService : Service() {
    private val preferences: Preferences by inject()
    private val messagingController: MessagingController by inject()
    private val coroutineScope: CoroutineScope by inject(named("AppCoroutineScope"))
    private val interactionPreferences: InteractionSettingsPreferenceManager by inject()
    private val interactionSettings get() = interactionPreferences.getConfig()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("NotificationActionService started with startId = %d", startId)

        startHandleCommand(intent, startId)

        return START_NOT_STICKY
    }

    private fun startHandleCommand(intent: Intent, startId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            handleCommand(intent)
            stopSelf(startId)
        }
    }

    private fun handleCommand(intent: Intent) {
        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID)
        if (accountUuid == null) {
            Log.w("Missing account UUID.")
            return
        }

        val account = preferences.getAccount(accountUuid)
        if (account == null) {
            Log.w("Could not find account for notification action.")
            return
        }

        when (intent.action) {
            ACTION_MARK_AS_READ -> markMessagesAsRead(intent, account)
            ACTION_DELETE -> deleteMessages(intent)
            ACTION_ARCHIVE -> archiveMessages(intent)
            ACTION_SPAM -> markMessageAsSpam(intent, account)
            ACTION_STAR -> markMessagesAsStarred(intent, account)
            ACTION_DISMISS -> Log.i("Notification dismissed")
        }

        cancelNotifications(intent, account)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun markMessagesAsRead(intent: Intent, account: LegacyAccountDto) {
        Log.i("NotificationActionService marking messages as read")

        val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
        val messageReferences = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

        for (messageReference in messageReferences) {
            val folderId = messageReference.folderId
            val uid = messageReference.uid
            messagingController.setFlag(account, folderId, uid, Flag.SEEN, true)
        }
    }

    private fun deleteMessages(intent: Intent) {
        Log.i("NotificationActionService deleting messages")

        val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
        val messageReferences = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

        messagingController.deleteMessages(messageReferences)
    }

    private fun archiveMessages(intent: Intent) {
        Log.i("NotificationActionService archiving messages")

        val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
        val messageReferences = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

        messagingController.archiveMessages(messageReferences)
    }

    private fun markMessageAsSpam(intent: Intent, account: LegacyAccountDto) {
        Log.i("NotificationActionService moving messages to spam")

        val messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE)
        val messageReference = MessageReference.parse(messageReferenceString)

        if (messageReference == null) {
            Log.w("Invalid message reference: %s", messageReferenceString)
            return
        }

        val spamFolderId = account.spamFolderId
        if (spamFolderId == null) {
            Log.w("No spam folder configured")
            return
        }

        if (!interactionSettings.isConfirmSpam && messagingController.isMoveCapable(account)) {
            val sourceFolderId = messageReference.folderId
            messagingController.moveMessage(account, sourceFolderId, messageReference, spamFolderId)
        }
    }

    private fun markMessagesAsStarred(intent: Intent, account: LegacyAccountDto) {
        Log.i("NotificationActionService starring messages")

        val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
        val messageReferences = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

        for (messageReference in messageReferences) {
            val folderId = messageReference.folderId
            val uid = messageReference.uid
            messagingController.setFlag(account, folderId, uid, Flag.FLAGGED, true)
        }
    }

    private fun cancelNotifications(intent: Intent, account: LegacyAccountDto) {
        if (intent.hasExtra(EXTRA_MESSAGE_REFERENCE)) {
            val messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE)
            val messageReference = MessageReference.parse(messageReferenceString)

            if (messageReference != null) {
                messagingController.cancelNotificationForMessage(account, messageReference)
            } else {
                Log.w("Invalid message reference: %s", messageReferenceString)
            }
        } else if (intent.hasExtra(EXTRA_MESSAGE_REFERENCES)) {
            val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
            val messageReferences = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

            for (messageReference in messageReferences) {
                messagingController.cancelNotificationForMessage(account, messageReference)
            }
        } else {
            messagingController.cancelNotificationsForAccount(account)
        }
    }

    companion object {
        private const val ACTION_MARK_AS_READ = "ACTION_MARK_AS_READ"
        private const val ACTION_DELETE = "ACTION_DELETE"
        private const val ACTION_ARCHIVE = "ACTION_ARCHIVE"
        private const val ACTION_SPAM = "ACTION_SPAM"
        private const val ACTION_STAR = "ACTION_STAR"
        private const val ACTION_DISMISS = "ACTION_DISMISS"
        private const val EXTRA_ACCOUNT_UUID = "accountUuid"
        private const val EXTRA_MESSAGE_REFERENCE = "messageReference"
        private const val EXTRA_MESSAGE_REFERENCES = "messageReferences"

        fun createMarkMessageAsReadIntent(context: Context, messageReference: MessageReference): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_MARK_AS_READ
                putExtra(EXTRA_ACCOUNT_UUID, messageReference.accountUuid)
                putExtra(EXTRA_MESSAGE_REFERENCES, createSingleItemArrayList(messageReference))
            }
        }

        fun createMarkAllAsReadIntent(
            context: Context,
            accountUuid: String,
            messageReferences: List<MessageReference>,
        ): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_MARK_AS_READ
                putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
                putExtra(
                    EXTRA_MESSAGE_REFERENCES,
                    MessageReferenceHelper.toMessageReferenceStringList(messageReferences),
                )
            }
        }

        fun createDismissMessageIntent(context: Context, messageReference: MessageReference): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_ACCOUNT_UUID, messageReference.accountUuid)
                putExtra(EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString())
            }
        }

        fun createDismissAllMessagesIntent(context: Context, account: LegacyAccountDto): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_ACCOUNT_UUID, account.uuid)
            }
        }

        fun createDeleteMessageIntent(context: Context, messageReference: MessageReference): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_DELETE
                putExtra(EXTRA_ACCOUNT_UUID, messageReference.accountUuid)
                putExtra(EXTRA_MESSAGE_REFERENCES, createSingleItemArrayList(messageReference))
            }
        }

        fun createDeleteAllMessagesIntent(
            context: Context,
            accountUuid: String,
            messageReferences: List<MessageReference>,
        ): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_DELETE
                putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
                putExtra(
                    EXTRA_MESSAGE_REFERENCES,
                    MessageReferenceHelper.toMessageReferenceStringList(messageReferences),
                )
            }
        }

        fun createArchiveMessageIntent(context: Context, messageReference: MessageReference): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_ARCHIVE
                putExtra(EXTRA_ACCOUNT_UUID, messageReference.accountUuid)
                putExtra(EXTRA_MESSAGE_REFERENCES, createSingleItemArrayList(messageReference))
            }
        }

        fun createArchiveAllIntent(
            context: Context,
            account: LegacyAccountDto,
            messageReferences: List<MessageReference>,
        ): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_ARCHIVE
                putExtra(EXTRA_ACCOUNT_UUID, account.uuid)
                putExtra(
                    EXTRA_MESSAGE_REFERENCES,
                    MessageReferenceHelper.toMessageReferenceStringList(messageReferences),
                )
            }
        }

        fun createMarkMessageAsSpamIntent(context: Context, messageReference: MessageReference): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_SPAM
                putExtra(EXTRA_ACCOUNT_UUID, messageReference.accountUuid)
                putExtra(EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString())
            }
        }

        fun createMarkMessageAsStarIntent(context: Context, messageReference: MessageReference): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                action = ACTION_STAR
                putExtra(EXTRA_ACCOUNT_UUID, messageReference.accountUuid)
                putExtra(EXTRA_MESSAGE_REFERENCES, createSingleItemArrayList(messageReference))
            }
        }

        private fun createSingleItemArrayList(messageReference: MessageReference): ArrayList<String> {
            return ArrayList<String>(1).apply {
                add(messageReference.toIdentityString())
            }
        }
    }
}
