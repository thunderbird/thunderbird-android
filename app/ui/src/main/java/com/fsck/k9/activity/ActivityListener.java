package com.fsck.k9.activity;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.fsck.k9.Account;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.service.CoreService;
import com.fsck.k9.ui.R;

import net.jcip.annotations.GuardedBy;


public class ActivityListener extends SimpleMessagingListener {
    private final Object lock = new Object();

    @GuardedBy("lock") private Account account = null;
    @GuardedBy("lock") private String loadingFolderServerId = null;
    @GuardedBy("lock") private String loadingFolderName = null;
    @GuardedBy("lock") private String loadingHeaderFolderServerId = null;
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
                    loadingHeaderFolderServerId != null ||
                    processingAccountDescription != null) {
                return getActionInProgressOperation(context);
            }
        }

        if (CoreService.isMailSyncDisabled(context)) {
            return context.getString(R.string.status_syncing_off);
        } else {
            return "";
        }
    }

    @GuardedBy("lock")
    private String getActionInProgressOperation(Context context) {
        String progress = folderTotal > 0 ?
                context.getString(R.string.folder_progress, folderCompleted, folderTotal) : "";

        if (loadingFolderServerId != null || loadingHeaderFolderServerId != null) {
            String folderServerId;
            String folderName;
            if (loadingHeaderFolderServerId != null) {
                folderServerId = loadingHeaderFolderServerId;
                folderName = loadingHeaderFolderName;
            } else {
                folderServerId = loadingFolderServerId;
                folderName = loadingFolderName;
            }

            String displayName;
            if (account != null) {
                if (folderServerId.equals(account.getInboxFolder())) {
                    displayName = context.getString(R.string.special_mailbox_name_inbox);
                } else if (folderServerId.equals(account.getOutboxFolder())) {
                    displayName = context.getString(R.string.special_mailbox_name_outbox);
                } else {
                    displayName = folderName;
                }
            } else {
                displayName = folderName;
            }

            if (loadingHeaderFolderServerId != null) {
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
    public void synchronizeMailboxFinished(Account account, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages) {
        synchronized (lock) {
            loadingAccountDescription = null;
            loadingFolderServerId = null;
            loadingFolderName = null;
            this.account = null;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxStarted(Account account, String folderServerId, String folderName) {
        synchronized (lock) {
            loadingAccountDescription = account.getDescription();
            loadingFolderServerId = folderServerId;
            loadingFolderName = folderName;
            this.account = account;
            folderCompleted = 0;
            folderTotal = 0;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxHeadersStarted(Account account, String folderServerId, String folderName) {
        synchronized (lock) {
            loadingAccountDescription = account.getDescription();
            loadingHeaderFolderServerId = folderServerId;
            loadingHeaderFolderName = folderName;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxHeadersProgress(Account account, String folderServerId, int completed, int total) {
        synchronized (lock) {
            folderCompleted = completed;
            folderTotal = total;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxHeadersFinished(Account account, String folderServerId, int total, int completed) {
        synchronized (lock) {
            loadingHeaderFolderServerId = null;
            loadingHeaderFolderName = null;
            folderCompleted = 0;
            folderTotal = 0;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxProgress(Account account, String folderServerId, int completed, int total) {
        synchronized (lock) {
            folderCompleted = completed;
            folderTotal = total;
        }

        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxFailed(Account account, String folderServerId, String message) {
        synchronized (lock) {
            loadingAccountDescription = null;
            loadingHeaderFolderServerId = null;
            loadingHeaderFolderName = null;
            loadingFolderServerId = null;
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
    public void systemStatusChanged() {
        informUserOfStatus();
    }

    @Override
    public void folderStatusChanged(Account account, String folderServerId, int unreadMessageCount) {
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
