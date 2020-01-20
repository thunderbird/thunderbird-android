package com.fsck.k9.activity;


import com.fsck.k9.Account;
import com.fsck.k9.controller.SimpleMessagingListener;

import net.jcip.annotations.GuardedBy;


public class ActivityListener extends SimpleMessagingListener {
    private final Object lock = new Object();

    @GuardedBy("lock") private int folderCompleted = 0;
    @GuardedBy("lock") private int folderTotal = 0;


    public void informUserOfStatus() {
    }

    @Override
    public void synchronizeMailboxFinished(Account account, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages) {
        informUserOfStatus();
    }

    @Override
    public void synchronizeMailboxStarted(Account account, String folderServerId, String folderName) {
        synchronized (lock) {
            folderCompleted = 0;
            folderTotal = 0;
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
