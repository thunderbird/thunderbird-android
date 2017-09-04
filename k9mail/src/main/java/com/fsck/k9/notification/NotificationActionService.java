package com.fsck.k9.notification;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import timber.log.Timber;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.service.CoreService;

import static com.fsck.k9.activity.MessageReferenceHelper.toMessageReferenceList;
import static com.fsck.k9.activity.MessageReferenceHelper.toMessageReferenceStringList;


public class NotificationActionService extends CoreService {
    private static final String ACTION_MARK_AS_READ = "ACTION_MARK_AS_READ";
    private static final String ACTION_DELETE = "ACTION_DELETE";
    private static final String ACTION_ARCHIVE = "ACTION_ARCHIVE";
    private static final String ACTION_SPAM = "ACTION_SPAM";
    private static final String ACTION_DISMISS = "ACTION_DISMISS";

    private static final String EXTRA_ACCOUNT_UUID = "accountUuid";
    private static final String EXTRA_MESSAGE_REFERENCE = "messageReference";
    private static final String EXTRA_MESSAGE_REFERENCES = "messageReferences";


    static Intent createMarkMessageAsReadIntent(Context context, MessageReference messageReference) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_MARK_AS_READ);
        intent.putExtra(EXTRA_ACCOUNT_UUID, messageReference.getAccountUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, createSingleItemArrayList(messageReference));

        return intent;
    }

    static Intent createMarkAllAsReadIntent(Context context, String accountUuid,
            List<MessageReference> messageReferences) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_MARK_AS_READ);
        intent.putExtra(EXTRA_ACCOUNT_UUID, accountUuid);
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, toMessageReferenceStringList(messageReferences));

        return intent;
    }

    static Intent createDismissMessageIntent(Context context, MessageReference messageReference) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_DISMISS);
        intent.putExtra(EXTRA_ACCOUNT_UUID, messageReference.getAccountUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString());

        return intent;
    }

    static Intent createDismissAllMessagesIntent(Context context, Account account) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_DISMISS);
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());

        return intent;
    }

    static Intent createDeleteMessageIntent(Context context, MessageReference messageReference) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_DELETE);
        intent.putExtra(EXTRA_ACCOUNT_UUID, messageReference.getAccountUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, createSingleItemArrayList(messageReference));

        return intent;
    }

    public static Intent createDeleteAllMessagesIntent(Context context, String accountUuid,
            List<MessageReference> messageReferences) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_DELETE);
        intent.putExtra(EXTRA_ACCOUNT_UUID, accountUuid);
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, toMessageReferenceStringList(messageReferences));

        return intent;
    }

    static Intent createArchiveMessageIntent(Context context, MessageReference messageReference) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_ARCHIVE);
        intent.putExtra(EXTRA_ACCOUNT_UUID, messageReference.getAccountUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, createSingleItemArrayList(messageReference));

        return intent;
    }

    static Intent createArchiveAllIntent(Context context, Account account, List<MessageReference> messageReferences) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_ARCHIVE);
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, toMessageReferenceStringList(messageReferences));

        return intent;
    }

    static Intent createMarkMessageAsSpamIntent(Context context, MessageReference messageReference) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_SPAM);
        intent.putExtra(EXTRA_ACCOUNT_UUID, messageReference.getAccountUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference.toIdentityString());

        return intent;
    }

    private static ArrayList<String> createSingleItemArrayList(MessageReference messageReference) {
        ArrayList<String> messageReferenceStrings = new ArrayList<>(1);
        messageReferenceStrings.add(messageReference.toIdentityString());
        return messageReferenceStrings;
    }

    @Override
    public int startService(Intent intent, int startId) {
        Timber.i("NotificationActionService started with startId = %d", startId);

        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID);
        Preferences preferences = Preferences.getPreferences(this);
        Account account = preferences.getAccount(accountUuid);

        if (account == null) {
            Timber.w("Could not find account for notification action.");
            return START_NOT_STICKY;
        }

        MessagingController controller = MessagingController.getInstance(getApplication());

        String action = intent.getAction();
        if (ACTION_MARK_AS_READ.equals(action)) {
            markMessagesAsRead(intent, account, controller);
        } else if (ACTION_DELETE.equals(action)) {
            deleteMessages(intent, controller);
        } else if (ACTION_ARCHIVE.equals(action)) {
            archiveMessages(intent, account, controller);
        } else if (ACTION_SPAM.equals(action)) {
            markMessageAsSpam(intent, account, controller);
        } else if (ACTION_DISMISS.equals(action)) {
            Timber.i("Notification dismissed");
        }

        cancelNotifications(intent, account, controller);

        return START_NOT_STICKY;
    }

    private void markMessagesAsRead(Intent intent, Account account, MessagingController controller) {
        Timber.i("NotificationActionService marking messages as read");

        List<String> messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES);
        List<MessageReference> messageReferences = toMessageReferenceList(messageReferenceStrings);
        for (MessageReference messageReference : messageReferences) {
            String folderName = messageReference.getFolderId();
            String uid = messageReference.getUid();
            controller.setFlag(account, folderName, uid, Flag.SEEN, true);
        }
    }

    private void deleteMessages(Intent intent, MessagingController controller) {
        Timber.i("NotificationActionService deleting messages");

        List<String> messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES);
        List<MessageReference> messageReferences = toMessageReferenceList(messageReferenceStrings);
        controller.deleteMessages(messageReferences, null);
    }

    private void archiveMessages(Intent intent, Account account, MessagingController controller) {
        Timber.i("NotificationActionService archiving messages");

        String archiveFolderName = account.getArchiveFolderId();
        if (archiveFolderName == null ||
                (archiveFolderName.equals(account.getSpamFolderId()) && K9.confirmSpam()) ||
                !isMovePossible(controller, account, archiveFolderName)) {
            Timber.w("Can not archive messages");
            return;
        }

        List<String> messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES);
        List<MessageReference> messageReferences = toMessageReferenceList(messageReferenceStrings);
        for (MessageReference messageReference : messageReferences) {
            if (controller.isMoveCapable(messageReference)) {
                String sourceFolderName = messageReference.getFolderId();
                controller.moveMessage(account, sourceFolderName, messageReference, archiveFolderName);
            }
        }
    }

    private void markMessageAsSpam(Intent intent, Account account, MessagingController controller) {
        Timber.i("NotificationActionService moving messages to spam");

        String messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE);
        MessageReference messageReference = MessageReference.parse(messageReferenceString);
        if (messageReference == null) {
            Timber.w("Invalid message reference: %s", messageReferenceString);
            return;
        }

        String spamFolderName = account.getSpamFolderId();
        if (spamFolderName != null && !K9.confirmSpam() && isMovePossible(controller, account, spamFolderName)) {
            String sourceFolderName = messageReference.getFolderId();
            controller.moveMessage(account, sourceFolderName, messageReference, spamFolderName);
        }
    }

    private void cancelNotifications(Intent intent, Account account, MessagingController controller) {
        if (intent.hasExtra(EXTRA_MESSAGE_REFERENCE)) {
            String messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE_REFERENCE);
            MessageReference messageReference = MessageReference.parse(messageReferenceString);
            if (messageReference != null) {
                controller.cancelNotificationForMessage(account, messageReference);
            } else {
                Timber.w("Invalid message reference: %s", messageReferenceString);
            }
        } else if (intent.hasExtra(EXTRA_MESSAGE_REFERENCES)) {
            List<String> messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES);
            List<MessageReference> messageReferences = toMessageReferenceList(messageReferenceStrings);
            for (MessageReference messageReference : messageReferences) {
                controller.cancelNotificationForMessage(account, messageReference);
            }
        } else {
            controller.cancelNotificationsForAccount(account);
        }
    }

    private boolean isMovePossible(MessagingController controller, Account account,
            String destinationFolderName) {
        boolean isSpecialFolderConfigured = !K9.FOLDER_NONE.equalsIgnoreCase(destinationFolderName);

        return isSpecialFolderConfigured && controller.isMoveCapable(account);
    }
}
