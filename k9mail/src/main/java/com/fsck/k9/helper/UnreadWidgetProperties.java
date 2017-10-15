package com.fsck.k9.helper;

import android.content.Context;
import android.content.Intent;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;

public class UnreadWidgetProperties {

    private int appWidgetId;
    private String accountUuid;
    private String folderName;
    private Type type;

    public UnreadWidgetProperties(int appWidgetId, String accountUuid, String folderName) {
        this.appWidgetId = appWidgetId;
        this.accountUuid = accountUuid;
        this.folderName = folderName;
        calculateType();
    }

    public String getTitle(Context context) {
        String accountName = getAccount(context).getDescription();
        switch (type) {
            case SEARCH_ACCOUNT:
            case ACCOUNT:
                return accountName;
            case FOLDER:
                return context.getString(R.string.unread_widget_title, accountName, folderName);
            default:
                return null;
        }
    }

    public int getUnreadCount(Context context) throws MessagingException {
        BaseAccount baseAccount = getAccount(context);
        AccountStats stats;
        switch (type) {
            case SEARCH_ACCOUNT:
                MessagingController controller = MessagingController.getInstance(context);
                stats = controller.getSearchAccountStatsSynchronous((SearchAccount) baseAccount, null);
                return stats.unreadMessageCount;
            case ACCOUNT:
                Account account = (Account) baseAccount;
                stats = account.getStats(context);
                return stats.unreadMessageCount;
            case FOLDER:
                return ((Account) baseAccount).getFolderUnreadCount(context, folderName);
            default:
                return -1;
        }
    }

    public Intent getClickIntent(Context context) {
        switch (type) {
            case SEARCH_ACCOUNT:
                SearchAccount searchAccount = (SearchAccount) getAccount(context);
                return MessageList.intentDisplaySearch(context,
                        searchAccount.getRelatedSearch(), false, true, true);
            case ACCOUNT:
                return getClickIntentForAccount(context);
            case FOLDER:
                return getClickIntentForFolder(context);
            default:
                return null;
        }
    }

    public int getAppWidgetId() {
        return appWidgetId;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public String getFolderName() {
        return folderName;
    }

    private void calculateType() {
        if (SearchAccount.UNIFIED_INBOX.equals(accountUuid) ||
                SearchAccount.ALL_MESSAGES.equals(accountUuid)) {
            type = Type.SEARCH_ACCOUNT;
        } else if(folderName != null) {
            type = Type.FOLDER;
        } else {
            type = Type.ACCOUNT;
        }
    }

    private BaseAccount getAccount(Context context) {
        if (SearchAccount.UNIFIED_INBOX.equals(accountUuid)) {
            return SearchAccount.createUnifiedInboxAccount(context);
        } else if (SearchAccount.ALL_MESSAGES.equals(accountUuid)) {
            return SearchAccount.createAllMessagesAccount(context);
        }
        return Preferences.getPreferences(context).getAccount(accountUuid);
    }

    private Intent getClickIntentForAccount(Context context) {
        Account account = Preferences.getPreferences(context).getAccount(accountUuid);
        if (K9.FOLDER_NONE.equals(account.getAutoExpandFolderName())) {
            return FolderList.actionHandleAccountIntent(context, account, false);
        }
        LocalSearch search = new LocalSearch(account.getAutoExpandFolderName());
        search.addAllowedFolder(account.getAutoExpandFolderName());
        search.addAccountUuid(account.getUuid());
        return MessageList.intentDisplaySearch(context, search, false, true, true);
    }

    private Intent getClickIntentForFolder(Context context) {
        Account account = Preferences.getPreferences(context).getAccount(accountUuid);
        LocalSearch search = new LocalSearch(folderName);
        search.addAllowedFolder(folderName);
        search.addAccountUuid(account.getUuid());
        Intent clickIntent = MessageList.intentDisplaySearch(context, search, false, true, true);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return clickIntent;
    }

    public enum Type {
        SEARCH_ACCOUNT,
        ACCOUNT,
        FOLDER
    }
}
