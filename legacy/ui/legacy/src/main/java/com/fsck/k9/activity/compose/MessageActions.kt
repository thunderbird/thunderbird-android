package com.fsck.k9.activity.compose

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget.AccountSetup
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageCompose
import net.thunderbird.core.android.account.LegacyAccountDto

object MessageActions {
    /**
     * Compose a new message using the given account. If account is null the default account
     * will be used. If there is no default account set, user will be sent to AccountSetup
     * activity.
     */
    @JvmStatic
    fun actionCompose(context: Context, account: LegacyAccountDto?) {
        val defaultAccount = Preferences.getPreferences().defaultAccount
        if (account == null && defaultAccount == null) {
            FeatureLauncherActivity.launch(context, AccountSetup)
        } else {
            val accountUuid = account?.uuid ?: requireNotNull(defaultAccount?.uuid) {
                "Unexpected state. At this point, either account ($account) or defaultAccount " +
                    "($defaultAccount) must have a value."
            }
            val intent = Intent(context, MessageCompose::class.java).apply {
                putExtra(MessageCompose.EXTRA_ACCOUNT, accountUuid)
                action = MessageCompose.ACTION_COMPOSE
            }
            context.startActivity(intent)
        }
    }

    /**
     * Get intent for composing a new message as a reply to the given message. If replyAll is true
     * the function is reply all instead of simply reply.
     */
    @JvmStatic
    fun getActionReplyIntent(
        context: Context,
        messageReference: MessageReference,
        replyAll: Boolean,
        decryptionResult: Parcelable?,
    ): Intent {
        return Intent(context, MessageCompose::class.java).apply {
            putExtra(MessageCompose.EXTRA_MESSAGE_DECRYPTION_RESULT, decryptionResult)
            putExtra(MessageCompose.EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString())
            action = if (replyAll) {
                MessageCompose.ACTION_REPLY_ALL
            } else {
                MessageCompose.ACTION_REPLY
            }
        }
    }

    @JvmStatic
    fun getActionReplyIntent(context: Context, messageReference: MessageReference): Intent {
        return Intent(context, MessageCompose::class.java).apply {
            action = MessageCompose.ACTION_REPLY
            putExtra(MessageCompose.EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Compose a new message as a reply to the given message. If replyAll is true the function
     * is reply all instead of simply reply.
     */
    @JvmStatic
    fun actionReply(
        context: Context,
        messageReference: MessageReference,
        replyAll: Boolean,
        decryptionResult: Parcelable?,
    ) {
        context.startActivity(getActionReplyIntent(context, messageReference, replyAll, decryptionResult))
    }

    /**
     * Compose a new message as a forward of the given message.
     */
    @JvmStatic
    fun actionForward(context: Context, messageReference: MessageReference, decryptionResult: Parcelable?) {
        val intent = Intent(context, MessageCompose::class.java).apply {
            putExtra(MessageCompose.EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString())
            putExtra(MessageCompose.EXTRA_MESSAGE_DECRYPTION_RESULT, decryptionResult)
            action = MessageCompose.ACTION_FORWARD
        }
        context.startActivity(intent)
    }

    /**
     * Compose a new message as a forward of the given message.
     */
    @JvmStatic
    fun actionForwardAsAttachment(context: Context, messageReference: MessageReference, decryptionResult: Parcelable?) {
        val intent = Intent(context, MessageCompose::class.java).apply {
            putExtra(MessageCompose.EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString())
            putExtra(MessageCompose.EXTRA_MESSAGE_DECRYPTION_RESULT, decryptionResult)
            action = MessageCompose.ACTION_FORWARD_AS_ATTACHMENT
        }
        context.startActivity(intent)
    }

    /**
     * Continue composition of the given message. This action modifies the way this Activity
     * handles certain actions.
     * Save will attempt to replace the message in the given folder with the updated version.
     * Discard will delete the message from the given folder.
     */
    @JvmStatic
    fun actionEditDraft(context: Context, messageReference: MessageReference) {
        val intent = Intent(context, MessageCompose::class.java).apply {
            putExtra(MessageCompose.EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString())
            action = MessageCompose.ACTION_EDIT_DRAFT
        }
        context.startActivity(intent)
    }
}
