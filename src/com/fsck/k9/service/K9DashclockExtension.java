package com.fsck.k9.service;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.DashclockSettingsActivity;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.StringUtils;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.search.SearchAccount;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

/**
 * K9DashclockExtension is a K-9 extension for DashClock widget -
 *  https://play.google.com/store/apps/details?id=net.nurik.roman.dashclock
 * It displays unread mail counts in the widget.
 */
public class K9DashclockExtension extends DashClockExtension {

    @Override
    protected void onUpdateData(int reason) {
        try {
            SearchAccount unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(this);
            MessagingController controller = MessagingController.getInstance(K9.app);
            AccountStats stats = controller.getSearchAccountStatsSynchronous(unifiedInboxAccount, null);

            int unreadEmailCount = getUnreadEmailCount();
            if (unreadEmailCount > 0) {
                publishUpdate(new ExtensionData()
                        .visible(true)
                        .icon(R.drawable.ic_unread_widget)
                        .status(String.valueOf(unreadEmailCount))
                        .expandedTitle(getResources().getQuantityString(R.plurals.dashclock_title,
                                                        unreadEmailCount, unreadEmailCount))
                        .expandedBody(constructBody())
                        .clickIntent(new Intent(Intent.ACTION_VIEW).setType(MimeUtility.K9_SETTINGS_MIME_TYPE)));

            } else {
                publishUpdate(new ExtensionData().visible(false));
            }
        } catch (Exception e) {
            Log.d(K9.LOG_TAG, e.getMessage());
        }

    }

    private int getUnreadEmailCount() {

        Account[] selectedAccounts = loadSelectedAccounts();

        if (selectedAccounts != null) {
            int count = 0;
            for(Account account : selectedAccounts) {
                try {
                    count += account.getStats(getApplicationContext()).unreadMessageCount;
                }catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, e.getMessage());
                }
            }

            return count;

        } else {
            SearchAccount unifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(this);
            MessagingController controller = MessagingController.getInstance(K9.app);
            AccountStats stats = controller.getSearchAccountStatsSynchronous(unifiedInboxAccount, null);

            return stats.unreadMessageCount;
        }

    }
    private String constructBody() {

        StringBuilder sb = new StringBuilder();
        final String template = "%s (%d) ";

        Account[] accounts = loadSelectedAccounts();
        if (accounts == null) {
            accounts = Preferences.getPreferences(getApplicationContext()).getAccounts();
        }
        if(accounts != null) {
            //MessagingController controller = MessagingController.getInstance(K9.app);
            for(Account account : accounts) {
                Log.d(K9.LOG_TAG, "dashclock loaded account: "+account.getEmail());
                try {
                    AccountStats stats = account.getStats(this);
                    if(stats.unreadMessageCount > 0) {
                        String accountDescription = TextUtils.isEmpty(account.getDescription()) ?
                                account.getEmail() : account.getDescription().trim();

                        sb.append(String.format(template, accountDescription, stats.unreadMessageCount))
                            .append('\n');
                    }

                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }

    private Account[] loadSelectedAccounts() {
        String uuids = Preferences.getPreferences(getApplicationContext()).getPreferences()
                                  .getString(DashclockSettingsActivity.PREFERENCE_DASHCLOCK_SELECTED_ACCOUNTS, null);

        if (!StringUtils.isNullOrEmpty(uuids)) {
            String[] accountUuids = uuids.split(",");
            Account[] accounts = new Account[accountUuids.length];
            int i=0;
            for(String uuid : accountUuids) {
                Account account = Preferences.getPreferences(getApplicationContext()).getAccount(uuid);
                if (account != null) {
                    accounts[i++] = account;
                }
            }
            return accounts;

        }

        return null;

    }

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        setUpdateWhenScreenOn(true);
    }
}
