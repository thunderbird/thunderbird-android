package com.fsck.k9.notification

import android.content.Context
import android.content.Intent
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.controller.MessageReferenceHelper
import net.thunderbird.core.android.account.LegacyAccountDto

internal const val ACTION_MARK_AS_READ = "ACTION_MARK_AS_READ"
internal const val ACTION_DELETE = "ACTION_DELETE"
internal const val ACTION_ARCHIVE = "ACTION_ARCHIVE"
internal const val ACTION_SPAM = "ACTION_SPAM"
internal const val ACTION_STAR = "ACTION_STAR"
internal const val ACTION_DISMISS = "ACTION_DISMISS"
internal const val EXTRA_ACCOUNT_UUID = "accountUuid"
internal const val EXTRA_MESSAGE_REFERENCE = "messageReference"
internal const val EXTRA_MESSAGE_REFERENCES = "messageReferences"

object NotificationActionIntents {
    fun createMarkMessageAsReadIntent(context: Context, messageReference: MessageReference): Intent {
        return Intent(context, NotificationActionService::class.java).apply {
            action = ACTION_MARK_AS_READ
            putExtra(EXTRA_ACCOUNT_UUID, messageReference.accountUuid)
            putExtra(EXTRA_MESSAGE_REFERENCES, arrayListOf(messageReference.toIdentityString()))
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
            putExtra(EXTRA_MESSAGE_REFERENCES, arrayListOf(messageReference.toIdentityString()))
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
            putExtra(EXTRA_MESSAGE_REFERENCES, arrayListOf(messageReference.toIdentityString()))
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
            putExtra(EXTRA_MESSAGE_REFERENCES, arrayListOf(messageReference.toIdentityString()))
        }
    }
}
