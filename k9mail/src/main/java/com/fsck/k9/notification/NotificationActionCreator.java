package com.fsck.k9.notification;


import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.NotificationDeleteConfirmation;
import com.fsck.k9.activity.compose.MessageActions;
import com.fsck.k9.search.LocalSearch;


/**
 * This class contains methods to create the {@link PendingIntent}s for the actions of new mail notifications.
 * <p/>
 * <strong>Note:</strong>
 * We need to take special care to ensure the {@code PendingIntent}s are unique as defined in the documentation of
 * {@link PendingIntent}. Otherwise selecting a notification action might perform the action on the wrong message.
 * <p/>
 * We use the notification ID as {@code requestCode} argument to ensure each notification/action pair gets a unique
 * {@code PendingIntent}.
 */
class NotificationActionCreator {
    private final Context context;


    public NotificationActionCreator(Context context) {
        this.context = context;
    }

    public PendingIntent createViewMessagePendingIntent(MessageReference messageReference, int notificationId) {
        TaskStackBuilder stack = buildMessageViewBackStack(messageReference);
        return stack.getPendingIntent(notificationId, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createViewFolderPendingIntent(Account account, String folderId, int notificationId) {
        TaskStackBuilder stack = buildMessageListBackStack(account, folderId);
        return stack.getPendingIntent(notificationId, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createViewMessagesPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {

        TaskStackBuilder stack;
        if (account.goToUnreadMessageSearch()) {
            stack = buildUnreadBackStack(account);
        } else {
            String folderId = getFolderIdOfAllMessages(messageReferences);

            if (folderId == null) {
                stack = buildFolderListBackStack(account);
            } else {
                stack = buildMessageListBackStack(account, folderId);
            }
        }

        return stack.getPendingIntent(notificationId, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createViewFolderListPendingIntent(Account account, int notificationId) {
        TaskStackBuilder stack = buildFolderListBackStack(account);
        return stack.getPendingIntent(notificationId, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createDismissAllMessagesPendingIntent(Account account, int notificationId) {
        Intent intent = NotificationActionService.createDismissAllMessagesIntent(context, account);

        return PendingIntent.getService(context, notificationId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createDismissMessagePendingIntent(Context context, MessageReference messageReference,
            int notificationId) {

        Intent intent = NotificationActionService.createDismissMessageIntent(context, messageReference);

        return PendingIntent.getService(context, notificationId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createReplyPendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = MessageActions.getActionReplyIntent(context, messageReference);

        return PendingIntent.getActivity(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createMarkMessageAsReadPendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = NotificationActionService.createMarkMessageAsReadIntent(context, messageReference);

        return PendingIntent.getService(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createMarkAllAsReadPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {
        return getMarkAsReadPendingIntent(account, messageReferences, notificationId, context,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent getMarkAllAsReadPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {
        return getMarkAsReadPendingIntent(account, messageReferences, notificationId, context,
                PendingIntent.FLAG_NO_CREATE);
    }

    private PendingIntent getMarkAsReadPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId, Context context, int flags) {
        String accountUuid = account.getUuid();
        Intent intent = NotificationActionService.createMarkAllAsReadIntent(context, accountUuid, messageReferences);

        return PendingIntent.getService(context, notificationId, intent, flags);
    }

    public PendingIntent createDeleteMessagePendingIntent(MessageReference messageReference, int notificationId) {
        if (K9.confirmDeleteFromNotification()) {
            return createDeleteConfirmationPendingIntent(messageReference, notificationId);
        } else {
            return createDeleteServicePendingIntent(messageReference, notificationId);
        }
    }

    private PendingIntent createDeleteServicePendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = NotificationActionService.createDeleteMessageIntent(context, messageReference);

        return PendingIntent.getService(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    private PendingIntent createDeleteConfirmationPendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = NotificationDeleteConfirmation.getIntent(context, messageReference);

        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public PendingIntent createDeleteAllPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {
        if (K9.confirmDeleteFromNotification()) {
            return getDeleteAllConfirmationPendingIntent(messageReferences, notificationId,
                    PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            return getDeleteAllServicePendingIntent(account, messageReferences, notificationId,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        }
    }

    public PendingIntent getDeleteAllPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {
        if (K9.confirmDeleteFromNotification()) {
            return getDeleteAllConfirmationPendingIntent(messageReferences, notificationId,
                    PendingIntent.FLAG_NO_CREATE);
        } else {
            return getDeleteAllServicePendingIntent(account, messageReferences, notificationId,
                    PendingIntent.FLAG_NO_CREATE);
        }
    }

    private PendingIntent getDeleteAllConfirmationPendingIntent(List<MessageReference> messageReferences,
            int notificationId, int flags) {
        Intent intent = NotificationDeleteConfirmation.getIntent(context, messageReferences);

        return PendingIntent.getActivity(context, notificationId, intent, flags);
    }

    private PendingIntent getDeleteAllServicePendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId, int flags) {
        String accountUuid = account.getUuid();
        Intent intent = NotificationActionService.createDeleteAllMessagesIntent(
                context, accountUuid, messageReferences);

        return PendingIntent.getService(context, notificationId, intent, flags);
    }

    public PendingIntent createArchiveMessagePendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = NotificationActionService.createArchiveMessageIntent(context, messageReference);

        return PendingIntent.getService(context, notificationId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createArchiveAllPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {
        Intent intent = NotificationActionService.createArchiveAllIntent(context, account, messageReferences);

        return PendingIntent.getService(context, notificationId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createMarkMessageAsSpamPendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = NotificationActionService.createMarkMessageAsSpamIntent(context, messageReference);

        return PendingIntent.getService(context, notificationId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    private TaskStackBuilder buildAccountsBackStack() {
        TaskStackBuilder stack = TaskStackBuilder.create(context);
        if (!skipAccountsInBackStack()) {
            Intent intent = new Intent(context, Accounts.class);
            intent.putExtra(Accounts.EXTRA_STARTUP, false);

            stack.addNextIntent(intent);
        }
        return stack;
    }

    private TaskStackBuilder buildFolderListBackStack(Account account) {
        TaskStackBuilder stack = buildAccountsBackStack();

        Intent intent = FolderList.actionHandleAccountIntent(context, account, false);

        stack.addNextIntent(intent);

        return stack;
    }

    private TaskStackBuilder buildUnreadBackStack(final Account account) {
        TaskStackBuilder stack = buildAccountsBackStack();

        LocalSearch search = Accounts.createUnreadSearch(context, account);
        Intent intent = MessageList.intentDisplaySearch(context, search, true, false, false);

        stack.addNextIntent(intent);

        return stack;
    }

    private TaskStackBuilder buildMessageListBackStack(Account account, String folderId) {
        TaskStackBuilder stack = skipFolderListInBackStack(account, folderId) ?
                buildAccountsBackStack() : buildFolderListBackStack(account);

        LocalSearch search = new LocalSearch(folderId);
        search.addAllowedFolder(folderId);
        search.addAccountUuid(account.getUuid());
        Intent intent = MessageList.intentDisplaySearch(context, search, false, true, true);

        stack.addNextIntent(intent);

        return stack;
    }

    private TaskStackBuilder buildMessageViewBackStack(MessageReference message) {
        Account account = Preferences.getPreferences(context).getAccount(message.getAccountUuid());
        String folderId = message.getFolderId();
        TaskStackBuilder stack = buildMessageListBackStack(account, folderId);

        Intent intent = MessageList.actionDisplayMessageIntent(context, message);

        stack.addNextIntent(intent);

        return stack;
    }

    private String getFolderIdOfAllMessages(List<MessageReference> messageReferences) {
        MessageReference firstMessage = messageReferences.get(0);
        String folderId = firstMessage.getFolderId();

        for (MessageReference messageReference : messageReferences) {
            if (!TextUtils.equals(folderId, messageReference.getFolderId())) {
                return null;
            }
        }

        return folderId;
    }

    private boolean skipFolderListInBackStack(Account account, String folderId) {
        return folderId != null && folderId.equals(account.getAutoExpandFolderId());
    }

    private boolean skipAccountsInBackStack() {
        return Preferences.getPreferences(context).getAccounts().size() == 1;
    }
}
