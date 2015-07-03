package com.fsck.k9.notification;


import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.service.CoreService;


public class NotificationActionService extends CoreService {
    private final static String ACTION_REPLY = "ACTION_REPLY";
    private final static String ACTION_MARK_AS_READ = "ACTION_MARK_AS_READ";
    private final static String ACTION_DELETE = "ACTION_DELETE";
    private final static String ACTION_ARCHIVE = "ACTION_ARCHIVE";
    private final static String ACTION_SPAM = "ACTION_SPAM";
    private final static String ACTION_DISMISS = "ACTION_DISMISS";

    private final static String EXTRA_ACCOUNT_UUID = "accountUuid";
    private final static String EXTRA_MESSAGE_REFERENCE = "messageReference";
    private final static String EXTRA_MESSAGE_REFERENCES = "messageReferences";
    public final static String EXTRA_NOTIFICATION_ID = "notificationId";


    public static PendingIntent createReplyPendingIntent(Context context, Account account,
            MessageReference messageReference, int notificationId) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_REPLY);
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        return PendingIntent.getService(context, account.getAccountNumber(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent createMarkAsReadPendingIntent(Context context, Account account,
            ArrayList<MessageReference> messageReferences, int notificationId) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_MARK_AS_READ);
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, messageReferences);
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        return PendingIntent.getService(context, account.getAccountNumber(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent createDismissPendingIntent(Context context, Account account, int notificationId) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_DISMISS);
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        return PendingIntent.getService(context, account.getAccountNumber(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static Intent createDeletePendingIntent(Context context, Account account,
            ArrayList<MessageReference> messageReferences, int notificationID) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_DELETE);
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, messageReferences);
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationID);

        return intent;
    }

    public static PendingIntent createArchivePendingIntent(Context context, Account account,
            ArrayList<MessageReference> messageReferences, int notificationId) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_ARCHIVE);
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, messageReferences);
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        return PendingIntent.getService(context, account.getAccountNumber(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    public static PendingIntent createMarkAsSpamPendingIntent(Context context, Account account,
            ArrayList<MessageReference> messageReferences, int notificationId) {
        Intent intent = new Intent(context, NotificationActionService.class);
        intent.setAction(ACTION_SPAM);
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, messageReferences);
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        return PendingIntent.getService(context, account.getAccountNumber(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static boolean isMovePossible(MessagingController controller, Account account,
            String destinationFolderName) {
        boolean isSpecialFolderConfigured = !K9.FOLDER_NONE.equalsIgnoreCase(destinationFolderName);

        return isSpecialFolderConfigured && controller.isMoveCapable(account);
    }

    @Override
    public int startService(Intent intent, int startId) {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "NotificationActionService started with startId = " + startId);
        }

        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID);
        Preferences preferences = Preferences.getPreferences(this);
        Account account = preferences.getAccount(accountUuid);

        if (account == null) {
            Log.w(K9.LOG_TAG, "Could not find account for notification action.");
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
            markMessagesAsSpam(intent, account, controller);
        } else if (ACTION_REPLY.equals(action)) {
            reply(intent);
        } else if (ACTION_DISMISS.equals(action)) {
            Log.i(K9.LOG_TAG, "notification acknowledged");
        }

        if (intent.hasExtra(EXTRA_NOTIFICATION_ID)) {
            int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, account.getAccountNumber());
            controller.cancelNotification(notificationId);
        } else {
            controller.cancelNotificationsForAccount(account);
        }

        return START_NOT_STICKY;
    }

    private void markMessagesAsRead(Intent intent, Account account, MessagingController controller) {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "NotificationActionService marking messages as read");
        }

        List<MessageReference> messageReferences = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES);
        for (MessageReference messageReference : messageReferences) {
            controller.setFlag(account, messageReference.getFolderName(), messageReference.getUid(), Flag.SEEN, true);
        }
    }

    private void deleteMessages(Intent intent, MessagingController controller) {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "NotificationActionService deleting messages");
        }

        List<MessageReference> messageReferences = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES);
        List<LocalMessage> messages = getLocalMessages(messageReferences);

        controller.deleteMessages(messages, null);
    }

    private void archiveMessages(Intent intent, Account account, MessagingController controller) {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "NotificationActionService archiving messages");
        }

        List<MessageReference> messageReferences = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES);
        List<LocalMessage> messages = getLocalMessages(messageReferences);

        String archiveFolderName = account.getArchiveFolderName();
        if (archiveFolderName != null &&
                !(archiveFolderName.equals(account.getSpamFolderName()) && K9.confirmSpam()) &&
                isMovePossible(controller, account, archiveFolderName)) {
            for (LocalMessage messageToMove : messages) {
                if (controller.isMoveCapable(messageToMove)) {
                    String sourceFolderName = messageToMove.getFolder().getName();
                    controller.moveMessage(account, sourceFolderName, messageToMove, archiveFolderName, null);
                }
            }
        }
    }

    private void markMessagesAsSpam(Intent intent, Account account, MessagingController controller) {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "NotificationActionService moving messages to spam");
        }

        List<MessageReference> messageReferences = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES);
        List<LocalMessage> messages = getLocalMessages(messageReferences);

        String spamFolderName = account.getSpamFolderName();
        if (spamFolderName != null && !K9.confirmSpam() &&
                isMovePossible(controller, account, spamFolderName)) {
            for (LocalMessage messageToMove : messages) {
                String sourceFolderName = messageToMove.getFolder().getName();
                controller.moveMessage(account, sourceFolderName, messageToMove, spamFolderName, null);
            }
        }
    }

    private void reply(Intent intent) {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "NotificationActionService initiating reply");
        }

        MessageReference messageReference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
        LocalMessage message = messageReference.restoreToLocalMessage(this);
        if (message == null) {
            Log.i(K9.LOG_TAG, "Could not execute reply action.");
            return;
        }

        Intent replyIntent = MessageCompose.getActionReplyIntent(this, message, false, null);
        replyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(replyIntent);
    }

    private List<LocalMessage> getLocalMessages(List<MessageReference> messageReferences) {
        List<LocalMessage> messages = new ArrayList<LocalMessage>(messageReferences.size());

        for (MessageReference messageReference : messageReferences) {
            LocalMessage message = messageReference.restoreToLocalMessage(this);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }
}
