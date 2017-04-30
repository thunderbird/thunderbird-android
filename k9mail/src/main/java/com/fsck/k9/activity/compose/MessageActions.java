package com.fsck.k9.activity.compose;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageReference;

public class MessageActions {
    /**
     * Compose a new message using the given account. If account is null the default account
     * will be used.
     */
    public static void actionCompose(Context context, Account account) {
        String accountUuid = (account == null) ?
                Preferences.getPreferences(context).getDefaultAccount().getUuid() :
                account.getUuid();

        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(MessageCompose.EXTRA_ACCOUNT, accountUuid);
        i.setAction(MessageCompose.ACTION_COMPOSE);
        context.startActivity(i);
    }

    /**
     * Get intent for composing a new message as a reply to the given message. If replyAll is true
     * the function is reply all instead of simply reply.
     */
    public static Intent getActionReplyIntent(
            Context context, MessageReference messageReference, boolean replyAll, Parcelable decryptionResult) {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(MessageCompose.EXTRA_MESSAGE_DECRYPTION_RESULT, decryptionResult);
        i.putExtra(MessageCompose.EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString());
        if (replyAll) {
            i.setAction(MessageCompose.ACTION_REPLY_ALL);
        } else {
            i.setAction(MessageCompose.ACTION_REPLY);
        }
        return i;
    }

    public static Intent getActionReplyIntent(Context context, MessageReference messageReference) {
        Intent intent = new Intent(context, MessageCompose.class);
        intent.setAction(MessageCompose.ACTION_REPLY);
        intent.putExtra(MessageCompose.EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    /**
     * Compose a new message as a reply to the given message. If replyAll is true the function
     * is reply all instead of simply reply.
     */
    public static void actionReply(
            Context context, MessageReference messageReference, boolean replyAll, Parcelable decryptionResult) {
        context.startActivity(getActionReplyIntent(context, messageReference, replyAll, decryptionResult));
    }

    /**
     * Compose a new message as a forward of the given message.
     */
    public static void actionForward(Context context, MessageReference messageReference, Parcelable decryptionResult) {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(MessageCompose.EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString());
        i.putExtra(MessageCompose.EXTRA_MESSAGE_DECRYPTION_RESULT, decryptionResult);
        i.setAction(MessageCompose.ACTION_FORWARD);
        context.startActivity(i);
    }

    /**
     * Continue composition of the given message. This action modifies the way this Activity
     * handles certain actions.
     * Save will attempt to replace the message in the given folder with the updated version.
     * Discard will delete the message from the given folder.
     */
    public static void actionEditDraft(Context context, MessageReference messageReference) {
        Intent i = new Intent(context, MessageCompose.class);
        i.putExtra(MessageCompose.EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString());
        i.setAction(MessageCompose.ACTION_EDIT_DRAFT);
        context.startActivity(i);
    }
}
