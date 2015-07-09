package com.fsck.k9.notification;


import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.search.LocalSearch;


class NotificationActionCreator {
    private final Context context;


    public NotificationActionCreator(Context context) {
        this.context = context;
    }

    public PendingIntent createSummaryNotificationActionPendingIntent(Account account, LocalMessage message,
            int newMessagesCount, int unreadMessagesCount,
            ArrayList<MessageReference> allNotificationMessageReferences) {

        TaskStackBuilder stack = buildNotificationNavigationStack(account, message, newMessagesCount,
                unreadMessagesCount, allNotificationMessageReferences);

        return stack.getPendingIntent(account.getAccountNumber(),
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createViewInboxPendingIntent(Account account) {
        String inboxFolderName = account.getInboxFolderName();
        TaskStackBuilder stack = buildMessageListBackStack(account, inboxFolderName);
        return stack.getPendingIntent(0, 0);
    }

    public PendingIntent createViewFolderListPendingIntent(Account account) {
        TaskStackBuilder stack = buildFolderListBackStack(account);
        return stack.getPendingIntent(0, 0);
    }

    private TaskStackBuilder buildNotificationNavigationStack(Account account, LocalMessage message, int newMessages,
            int unreadCount, ArrayList<MessageReference> allRefs) {

        TaskStackBuilder stack;
        boolean treatAsSingleMessageNotification;

        if (NotificationController.platformSupportsExtendedNotifications()) {
            // in the new-style notifications, we focus on the new messages, not the unread ones
            treatAsSingleMessageNotification = newMessages == 1;
        } else {
            // in the old-style notifications, we focus on unread messages, as we don't have a
            // good way to express the new message count
            treatAsSingleMessageNotification = unreadCount == 1;
        }

        if (treatAsSingleMessageNotification) {
            stack = buildMessageViewBackStack(message.makeMessageReference());
        } else if (account.goToUnreadMessageSearch()) {
            stack = buildUnreadBackStack(account);
        } else {
            String initialFolder = message.getFolder().getName();
            /* only go to folder if all messages are in the same folder, else go to folder list */
            for (MessageReference ref : allRefs) {
                if (!TextUtils.equals(initialFolder, ref.getFolderName())) {
                    initialFolder = null;
                    break;
                }
            }

            stack = buildMessageListBackStack(account, initialFolder);
        }
        return stack;
    }

    private TaskStackBuilder buildAccountsBackStack() {
        TaskStackBuilder stack = TaskStackBuilder.create(context);
        if (!skipAccountsInBackStack()) {
            stack.addNextIntent(new Intent(context, Accounts.class).putExtra(Accounts.EXTRA_STARTUP, false));
        }
        return stack;
    }

    private TaskStackBuilder buildFolderListBackStack(Account account) {
        TaskStackBuilder stack = buildAccountsBackStack();
        stack.addNextIntent(FolderList.actionHandleAccountIntent(context, account, false));
        return stack;
    }

    private TaskStackBuilder buildUnreadBackStack(final Account account) {
        TaskStackBuilder stack = buildAccountsBackStack();
        LocalSearch search = Accounts.createUnreadSearch(context, account);
        stack.addNextIntent(MessageList.intentDisplaySearch(context, search, true, false, false));
        return stack;
    }

    private TaskStackBuilder buildMessageListBackStack(Account account, String folder) {
        TaskStackBuilder stack = skipFolderListInBackStack(account, folder) ?
                buildAccountsBackStack() : buildFolderListBackStack(account);

        if (folder != null) {
            LocalSearch search = new LocalSearch(folder);
            search.addAllowedFolder(folder);
            search.addAccountUuid(account.getUuid());
            stack.addNextIntent(MessageList.intentDisplaySearch(context, search, false, true, true));
        }
        return stack;
    }

    private TaskStackBuilder buildMessageViewBackStack(MessageReference message) {
        Account account = Preferences.getPreferences(context).getAccount(message.getAccountUuid());
        TaskStackBuilder stack = buildMessageListBackStack(account, message.getFolderName());
        stack.addNextIntent(MessageList.actionDisplayMessageIntent(context, message));
        return stack;
    }

    private boolean skipFolderListInBackStack(Account account, String folder) {
        return folder != null && folder.equals(account.getAutoExpandFolderName());
    }

    private boolean skipAccountsInBackStack() {
        return Preferences.getPreferences(context).getAccounts().size() == 1;
    }
}
