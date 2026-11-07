package com.fsck.k9.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.controller.MessageReferenceHelper
import com.fsck.k9.controller.MessagingController
import kotlin.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

private const val TAG = "NotificationActionService"

class NotificationActionService : Service() {
    private val accountManager: LegacyAccountManager by inject()
    private val messagingController: MessagingController by inject()
    private val coroutineScope: CoroutineScope by inject(named("AppCoroutineScope"))
    private val interactionPreferences: InteractionSettingsPreferenceManager by inject()
    private val logger: Logger by inject()
    private val interactionSettings get() = interactionPreferences.getConfig()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        logger.info(TAG) { "NotificationActionService started with startId = $startId" }

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
        val rawId = intent.getStringExtra(EXTRA_ACCOUNT_UUID)
        val accountId = rawId?.let { rawId ->
            try {
                AccountIdFactory.of(rawId)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        if (accountId == null) {
            logger.warn(TAG) { "Missing account id: $rawId" }
            return
        }

        when (intent.action) {
            ACTION_MARK_AS_READ -> markMessagesAsRead(intent, accountId)
            ACTION_DELETE -> deleteMessages(intent)
            ACTION_ARCHIVE -> archiveMessages(intent)
            ACTION_SPAM -> markMessageAsSpam(intent, accountId)
            ACTION_STAR -> markMessagesAsStarred(intent, accountId)
            ACTION_DISMISS -> logger.info(TAG) { "Notification dismissed for account: $accountId" }
        }

        cancelNotifications(intent, accountId)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun markMessagesAsRead(intent: Intent, accountId: AccountId) {
        logger.info(TAG) { "NotificationActionService marking messages as read for account: $accountId" }

        val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
        val messageReferences = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

        for (messageReference in messageReferences) {
            val folderId = messageReference.folderId
            val uid = messageReference.uid
            messagingController.setFlag(accountId, folderId, uid, Flag.SEEN, true)
        }
    }

    private fun deleteMessages(intent: Intent) {
        logger.info(TAG) { "NotificationActionService deleting messages" }

        val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
        val messageReferences = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

        messagingController.deleteMessages(messageReferences)
    }

    private fun archiveMessages(intent: Intent) {
        logger.info(TAG) { "NotificationActionService archiving messages" }

        val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
        val messageReferences = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

        messagingController.archiveMessages(messageReferences)
    }

    private fun markMessageAsSpam(intent: Intent, accountId: AccountId) {
        logger.info(TAG) { "NotificationActionService moving messages to spam for account: $accountId" }

        val messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE)
        val messageReference = MessageReference.parse(messageReferenceString)

        if (messageReference == null) {
            logger.warn(TAG) { "Invalid message reference: $messageReferenceString" }
            return
        }

        val account = accountManager.findById(accountId)
        val spamFolderId = account?.spamFolderId

        when {
            account == null -> {
                logger.warn(TAG) { "Could not find account for notification action: $accountId" }
            }

            spamFolderId == null -> {
                logger.warn(TAG) { "No spam folder configured for account: $accountId" }
            }

            !interactionSettings.isConfirmSpam && messagingController.isMoveCapable(accountId) -> {
                val sourceFolderId = messageReference.folderId
                messagingController.moveMessage(accountId, sourceFolderId, messageReference, spamFolderId)
            }
        }
    }

    private fun markMessagesAsStarred(intent: Intent, accountId: AccountId) {
        logger.info(TAG) { "NotificationActionService starring messages for account: $accountId" }

        val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
        val messageReferences = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

        for (messageReference in messageReferences) {
            val folderId = messageReference.folderId
            val uid = messageReference.uid
            messagingController.setFlag(accountId, folderId, uid, Flag.FLAGGED, true)
        }
    }

    private fun cancelNotifications(intent: Intent, accountId: AccountId) {
        if (intent.hasExtra(EXTRA_MESSAGE_REFERENCE)) {
            val messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE)
            val messageReference = MessageReference.parse(messageReferenceString)

            if (messageReference != null) {
                messagingController.cancelNotificationForMessage(accountId, messageReference)
            } else {
                logger.warn(TAG) { "Invalid message reference: $messageReferenceString" }
            }
        } else if (intent.hasExtra(EXTRA_MESSAGE_REFERENCES)) {
            val messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES)
            val messageReferences = MessageReferenceHelper.toMessageReferenceList(messageReferenceStrings)

            for (messageReference in messageReferences) {
                messagingController.cancelNotificationForMessage(accountId, messageReference)
            }
        } else {
            messagingController.cancelNotificationsForAccount(accountId)
        }
    }
}
