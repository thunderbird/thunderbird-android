package com.fsck.k9.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.DateUtils;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.service.MailService;

public class ActivityListener extends MessagingListener {
    private Account mAccount = null;
    private String mLoadingFolderName = null;
    private String mLoadingHeaderFolderName = null;
    private String mLoadingAccountDescription = null;
    private String mSendingAccountDescription = null;
    private int mFolderCompleted = 0;
    private int mFolderTotal = 0;
    private String mProcessingAccountDescription = null;
    private String mProcessingCommandTitle = null;

    private BroadcastReceiver mTickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            informUserOfStatus();
        }
    };

    public String getOperation(Context context) {
        String operation;
        String progress = null;
        if (mLoadingAccountDescription  != null
                || mSendingAccountDescription != null
                || mLoadingHeaderFolderName != null
                || mProcessingAccountDescription != null) {
            progress = (mFolderTotal > 0 ?
                        context.getString(R.string.folder_progress, mFolderCompleted, mFolderTotal) : "");

            if (mLoadingFolderName != null || mLoadingHeaderFolderName != null) {
                String displayName = mLoadingFolderName;
                if ((mAccount != null) && (mAccount.getInboxFolderName() != null) && mAccount.getInboxFolderName().equalsIgnoreCase(displayName)) {
                    displayName = context.getString(R.string.special_mailbox_name_inbox);
                } else if ((mAccount != null) && mAccount.getOutboxFolderName().equals(displayName)) {
                    displayName = context.getString(R.string.special_mailbox_name_outbox);
                }

                if (mLoadingHeaderFolderName != null) {

                    operation = context.getString(R.string.status_loading_account_folder_headers, mLoadingAccountDescription, displayName, progress);
                } else {
                    operation = context.getString(R.string.status_loading_account_folder, mLoadingAccountDescription, displayName, progress);
                }
            }

            else if (mSendingAccountDescription != null) {
                operation = context.getString(R.string.status_sending_account, mSendingAccountDescription, progress);
            } else if (mProcessingAccountDescription != null) {
                operation = context.getString(R.string.status_processing_account, mProcessingAccountDescription,
                                              mProcessingCommandTitle != null ? mProcessingCommandTitle : "",
                                              progress);
            } else {
                operation = "";
            }
        } else {
            long nextPollTime = MailService.getNextPollTime();
            if (nextPollTime != -1) {
                operation = context.getString(R.string.status_next_poll,
                        DateUtils.getRelativeTimeSpanString(nextPollTime, System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS, 0));
            } else if (MailService.isSyncDisabled()) {
                operation = context.getString(R.string.status_syncing_off);
            } else {
                operation = "";
            }
        }

        return operation;
    }

    public void onResume(Context context) {
        context.registerReceiver(mTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    public void onPause(Context context) {
        context.unregisterReceiver(mTickReceiver);
    }

    public void informUserOfStatus() {
    }

    @Override
    public void synchronizeMailboxFinished(
        Account account,
        String folder,
        int totalMessagesInMailbox,
        int numNewMessages) {
        mLoadingAccountDescription = null;
        mLoadingFolderName = null;
        mAccount = null;
        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxStarted(Account account, String folder) {
        mLoadingAccountDescription = account.getDescription();
        mLoadingFolderName = folder;
        mAccount = account;
        mFolderCompleted = 0;
        mFolderTotal = 0;
        informUserOfStatus();
    }


    @Override
    public void synchronizeMailboxHeadersStarted(Account account, String folder) {
        mLoadingHeaderFolderName = folder;
        informUserOfStatus();
    }


    @Override
    public void synchronizeMailboxHeadersProgress(Account account, String folder, int completed, int total) {
        mFolderCompleted = completed;
        mFolderTotal = total;
        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxHeadersFinished(Account account, String folder,
            int total, int completed) {
        mLoadingHeaderFolderName = null;
        mFolderCompleted = 0;
        mFolderTotal = 0;
        informUserOfStatus();
    }


    @Override
    public void synchronizeMailboxProgress(Account account, String folder, int completed, int total) {
        mFolderCompleted = completed;
        mFolderTotal = total;
        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxFailed(Account account, String folder,
                                         String message) {
        mLoadingAccountDescription = null;
        mLoadingFolderName = null;
        mAccount = null;
        informUserOfStatus();

    }

    @Override
    public void sendPendingMessagesStarted(Account account) {
        mSendingAccountDescription = account.getDescription();
        informUserOfStatus();
    }

    @Override
    public void sendPendingMessagesCompleted(Account account) {
        mSendingAccountDescription = null;
        informUserOfStatus();
    }

    @Override
    public void sendPendingMessagesFailed(Account account) {
        mSendingAccountDescription = null;
        informUserOfStatus();
    }

    @Override
    public void pendingCommandsProcessing(Account account) {
        mProcessingAccountDescription = account.getDescription();
        mFolderCompleted = 0;
        mFolderTotal = 0;
        informUserOfStatus();
    }

    @Override
    public void pendingCommandsFinished(Account account) {
        mProcessingAccountDescription = null;
        informUserOfStatus();
    }

    @Override
    public void pendingCommandStarted(Account account, String commandTitle) {
        mProcessingCommandTitle = commandTitle;
        informUserOfStatus();
    }

    @Override
    public void pendingCommandCompleted(Account account, String commandTitle) {
        mProcessingCommandTitle = null;
        informUserOfStatus();
    }

    @Override
    public void searchStats(AccountStats stats) {
        informUserOfStatus();
    }

    @Override
    public void systemStatusChanged() {
        informUserOfStatus();
    }
    @Override
    public void folderStatusChanged(Account account, String folder, int unreadMessageCount) {
        informUserOfStatus();
    }

    public int getFolderCompleted() {
        return mFolderCompleted;
    }


    public int getFolderTotal() {
        return mFolderTotal;
    }

}
