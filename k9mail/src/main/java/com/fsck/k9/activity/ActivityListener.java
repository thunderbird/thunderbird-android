package com.fsck.k9.activity;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.DateUtils;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.service.MailService;
import net.jcip.annotations.GuardedBy;


public class ActivityListener extends SimpleMessagingListener {
    private final Object lock = new Object();

    @GuardedBy("lock") private Account account = null;
    @GuardedBy("lock") private String loadingFolderName = null;
    @GuardedBy("lock") private String loadingHeaderFolderName = null;
    @GuardedBy("lock") private String loadingAccountDescription = null;
    @GuardedBy("lock") private String sendingAccountDescription = null;
    @GuardedBy("lock") private int folderCompleted = 0;
    @GuardedBy("lock") private int folderTotal = 0;
    @GuardedBy("lock") private String processingAccountDescription = null;
    @GuardedBy("lock") private String processingCommandTitle = null;

    private BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            informUserOfStatus();
        }
    };


    public String getOperation(Context context) {
        synchronized (lock) {
            if (loadingAccountDescription != null ||
                    sendingAccountDescription != null ||
                    loadingHeaderFolderName != null ||
                    processingAccountDescription != null) {
                return getActionInProgressOperation(context);
            }
        }

        long nextPollTime = MailService.getNextPollTime();
        if (nextPollTime != -1) {
            CharSequence relativeTimeSpanString = DateUtils.getRelativeTimeSpanString(
                    nextPollTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, 0);
            return context.getString(R.string.status_next_poll, relativeTimeSpanString);
        } else if (K9.isDebug() && MailService.isSyncDisabled()) {
            if (MailService.hasNoConnectivity()) {
                return context.getString(R.string.status_no_network);
            } else if (MailService.isSyncNoBackground()) {
                return context.getString(R.string.status_no_background);
            } else if (MailService.isSyncBlocked()) {
                return context.getString(R.string.status_syncing_blocked);
            } else if (MailService.isPollAndPushDisabled()) {
                return context.getString(R.string.status_poll_and_push_disabled);
            } else {
                return context.getString(R.string.status_syncing_off);
            }
        } else if (MailService.isSyncDisabled()) {
            return context.getString(R.string.status_syncing_off);
        } else {
            return "";
        }
    }

    @GuardedBy("lock")
    private String getActionInProgressOperation(Context context) {
        String progress = folderTotal > 0 ?
                context.getString(R.string.folder_progress, folderCompleted, folderTotal) : "";

        if (loadingFolderName != null || loadingHeaderFolderName != null) {
            String displayName;
            if (loadingHeaderFolderName != null) {
                displayName = loadingHeaderFolderName;
            } else {
                displayName = loadingFolderName;
            }

            if (account != null) {
                if (displayName.equalsIgnoreCase(account.getInboxFolderName())) {
                    displayName = context.getString(R.string.special_mailbox_name_inbox);
                } else if (displayName.equalsIgnoreCase(account.getOutboxFolderName())) {
                    displayName = context.getString(R.string.special_mailbox_name_outbox);
                }
            }

            if (loadingHeaderFolderName != null) {
                return context.getString(R.string.status_loading_account_folder_headers,
                        loadingAccountDescription, displayName, progress);
            } else {
                return context.getString(R.string.status_loading_account_folder,
                        loadingAccountDescription, displayName, progress);
            }
        } else if (sendingAccountDescription != null) {
            return context.getString(R.string.status_sending_account, sendingAccountDescription, progress);
        } else if (processingAccountDescription != null) {
            return context.getString(R.string.status_processing_account, processingAccountDescription,
                    processingCommandTitle != null ? processingCommandTitle : "", progress);
        } else {
            return "";
        }
    }

    public void onResume(Context context) {
        context.registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    public void onPause(Context context) {
        context.unregisterReceiver(tickReceiver);
    }

    public void informUserOfStatus() {
    }

    @Override
    public void synchronizeMailboxFinished(Account account, String folder, int totalMessagesInMailbox,
            int numNewMessages) {
        synchronized (lock) {
            loadingAccountDescription = null;
            loadingFolderName = null;
            this.account = null;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxStarted(Account account, String folder) {
        synchronized (lock) {
            loadingAccountDescription = account.getDescription();
            loadingFolderName = folder;
            this.account = account;
            folderCompleted = 0;
            folderTotal = 0;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxHeadersStarted(Account account, String folder) {
        synchronized (lock) {
            loadingAccountDescription = account.getDescription();
            loadingHeaderFolderName = folder;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxHeadersProgress(Account account, String folder, int completed, int total) {
        synchronized (lock) {
            folderCompleted = completed;
            folderTotal = total;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxHeadersFinished(Account account, String folder, int total, int completed) {
        synchronized (lock) {
            loadingHeaderFolderName = null;
            folderCompleted = 0;
            folderTotal = 0;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxProgress(Account account, String folder, int completed, int total) {
        synchronized (lock) {
            folderCompleted = completed;
            folderTotal = total;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxFailed(Account account, String folder, String message) {
        synchronized (lock) {
            loadingAccountDescription = null;
            loadingHeaderFolderName = null;
            loadingFolderName = null;
            this.account = null;
        }
        informUserOfStatus();
    }

    @Override
    public void sendPendingMessagesStarted(Account account) {
        synchronized (lock) {
            sendingAccountDescription = account.getDescription();
        }

        informUserOfStatus();
    }

    @Override
    public void sendPendingMessagesCompleted(Account account) {
        synchronized (lock) {
            sendingAccountDescription = null;
        }

        informUserOfStatus();
    }

    @Override
    public void sendPendingMessagesFailed(Account account) {
        synchronized (lock) {
            sendingAccountDescription = null;
        }

        informUserOfStatus();
    }

    @Override
    public void pendingCommandsProcessing(Account account) {
        synchronized (lock) {
            processingAccountDescription = account.getDescription();
            folderCompleted = 0;
            folderTotal = 0;
        }

        informUserOfStatus();
    }

    @Override
    public void pendingCommandsFinished(Account account) {
        synchronized (lock) {
            processingAccountDescription = null;
        }

        informUserOfStatus();
    }

    @Override
    public void pendingCommandStarted(Account account, String commandTitle) {
        synchronized (lock) {
            processingCommandTitle = commandTitle;

        }
        informUserOfStatus();
    }

    @Override
    public void pendingCommandCompleted(Account account, String commandTitle) {
        synchronized (lock) {
            processingCommandTitle = null;
        }

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
        synchronized (lock) {
            return folderCompleted;
        }
    }

    public int getFolderTotal() {
        synchronized (lock) {
            return folderTotal;
        }
    }
}
