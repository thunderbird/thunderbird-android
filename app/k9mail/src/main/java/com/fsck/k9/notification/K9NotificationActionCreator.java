package com.fsck.k9.notification;


import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.activity.NotificationDeleteConfirmation;
import com.fsck.k9.activity.compose.MessageActions;
import com.fsck.k9.activity.setup.AccountSetupIncoming;
import com.fsck.k9.activity.setup.AccountSetupOutgoing;
import com.fsck.k9.search.AccountSearchConditions;
import com.fsck.k9.search.LocalSearch;


/**
 * This class contains methods to create the {@link PendingIntent}s for the actions of our notifications.
 * <p/>
 * <strong>Note:</strong>
 * We need to take special care to ensure the {@code PendingIntent}s are unique as defined in the documentation of
 * {@link PendingIntent}. Otherwise selecting a notification action might perform the action on the wrong message.
 * <p/>
 * We use the notification ID as {@code requestCode} argument to ensure each notification/action pair gets a unique
 * {@code PendingIntent}.
 */
class K9NotificationActionCreator implements NotificationActionCreator {
    private final Context context;
    private final AccountSearchConditions accountSearchConditions = DI.get(AccountSearchConditions.class);


    public K9NotificationActionCreator(Context context) {
        this.context = context;
    }

    @Override
    public PendingIntent createViewMessagePendingIntent(MessageReference messageReference, int notificationId) {
        TaskStackBuilder stack = buildMessageViewBackStack(messageReference);
        return stack.getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createViewFolderPendingIntent(Account account, String folderServerId, int notificationId) {
        TaskStackBuilder stack = buildMessageListBackStack(account, folderServerId);
        return stack.getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createViewMessagesPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {

        TaskStackBuilder stack;
        if (account.isGoToUnreadMessageSearch()) {
            stack = buildUnreadBackStack(account);
        } else {
            String folderServerId = getFolderServerIdOfAllMessages(messageReferences);

            if (folderServerId == null) {
                stack = buildFolderListBackStack(account);
            } else {
                stack = buildMessageListBackStack(account, folderServerId);
            }
        }

        return stack.getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createViewFolderListPendingIntent(Account account, int notificationId) {
        TaskStackBuilder stack = buildFolderListBackStack(account);
        return stack.getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createDismissAllMessagesPendingIntent(Account account, int notificationId) {
        Intent intent = NotificationActionService.createDismissAllMessagesIntent(context, account);

        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createDismissMessagePendingIntent(Context context, MessageReference messageReference,
            int notificationId) {

        Intent intent = NotificationActionService.createDismissMessageIntent(context, messageReference);

        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createReplyPendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = MessageActions.getActionReplyIntent(context, messageReference);

        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createMarkMessageAsReadPendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = NotificationActionService.createMarkMessageAsReadIntent(context, messageReference);

        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createMarkAllAsReadPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {
        String accountUuid = account.getUuid();
        Intent intent = NotificationActionService.createMarkAllAsReadIntent(context, accountUuid, messageReferences);

        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent getEditIncomingServerSettingsIntent(Account account) {
        Intent intent = AccountSetupIncoming.intentActionEditIncomingSettings(context, account);

        return PendingIntent.getActivity(context, account.getAccountNumber(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent getEditOutgoingServerSettingsIntent(Account account) {
        Intent intent = AccountSetupOutgoing.intentActionEditOutgoingSettings(context, account);

        return PendingIntent.getActivity(context, account.getAccountNumber(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createDeleteMessagePendingIntent(MessageReference messageReference, int notificationId) {
        if (K9.confirmDeleteFromNotification()) {
            return createDeleteConfirmationPendingIntent(messageReference, notificationId);
        } else {
            return createDeleteServicePendingIntent(messageReference, notificationId);
        }
    }

    private PendingIntent createDeleteServicePendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = NotificationActionService.createDeleteMessageIntent(context, messageReference);

        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createDeleteConfirmationPendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = NotificationDeleteConfirmation.getIntent(context, messageReference);

        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createDeleteAllPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {
        if (K9.confirmDeleteFromNotification()) {
            return getDeleteAllConfirmationPendingIntent(messageReferences, notificationId);
        } else {
            return getDeleteAllServicePendingIntent(account, messageReferences, notificationId);
        }
    }

    private PendingIntent getDeleteAllConfirmationPendingIntent(List<MessageReference> messageReferences,
            int notificationId) {
        Intent intent = NotificationDeleteConfirmation.getIntent(context, messageReferences);

        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getDeleteAllServicePendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {
        String accountUuid = account.getUuid();
        Intent intent = NotificationActionService.createDeleteAllMessagesIntent(
                context, accountUuid, messageReferences);

        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createArchiveMessagePendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = NotificationActionService.createArchiveMessageIntent(context, messageReference);

        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createArchiveAllPendingIntent(Account account, List<MessageReference> messageReferences,
            int notificationId) {
        Intent intent = NotificationActionService.createArchiveAllIntent(context, account, messageReferences);

        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public PendingIntent createMarkMessageAsSpamPendingIntent(MessageReference messageReference, int notificationId) {
        Intent intent = NotificationActionService.createMarkMessageAsSpamIntent(context, messageReference);

        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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

        String searchTitle = context.getString(R.string.search_title, account.getDescription(), context.getString(R.string.unread_modifier));
        LocalSearch search = accountSearchConditions.createUnreadSearch(account, searchTitle);
        Intent intent = MessageList.intentDisplaySearch(context, search, true, false, false);

        stack.addNextIntent(intent);

        return stack;
    }

    private TaskStackBuilder buildMessageListBackStack(Account account, String folderServerId) {
        TaskStackBuilder stack = skipFolderListInBackStack(account, folderServerId) ?
                buildAccountsBackStack() : buildFolderListBackStack(account);

        LocalSearch search = new LocalSearch(folderServerId);
        search.addAllowedFolder(folderServerId);
        search.addAccountUuid(account.getUuid());
        Intent intent = MessageList.intentDisplaySearch(context, search, false, true, true);

        stack.addNextIntent(intent);

        return stack;
    }

    private TaskStackBuilder buildMessageViewBackStack(MessageReference message) {
        Account account = Preferences.getPreferences(context).getAccount(message.getAccountUuid());
        String folderServerId = message.getFolderServerId();
        TaskStackBuilder stack = buildMessageListBackStack(account, folderServerId);

        Intent intent = MessageList.actionDisplayMessageIntent(context, message);

        stack.addNextIntent(intent);

        return stack;
    }

    private String getFolderServerIdOfAllMessages(List<MessageReference> messageReferences) {
        MessageReference firstMessage = messageReferences.get(0);
        String folderServerId = firstMessage.getFolderServerId();

        for (MessageReference messageReference : messageReferences) {
            if (!TextUtils.equals(folderServerId, messageReference.getFolderServerId())) {
                return null;
            }
        }

        return folderServerId;
    }

    private boolean skipFolderListInBackStack(Account account, String folderServerId) {
        return folderServerId != null && folderServerId.equals(account.getAutoExpandFolder());
    }

    private boolean skipAccountsInBackStack() {
        return Preferences.getPreferences(context).getAccounts().size() == 1;
    }
}
