package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;


class ImapPusher implements Pusher {
    private final ImapStore store;
    private final PushReceiver pushReceiver;

    private final List<ImapFolderPusher> folderPushers = new ArrayList<>();

    private long lastRefresh = -1;


    public ImapPusher(ImapStore store, PushReceiver pushReceiver) {
        this.store = store;
        this.pushReceiver = pushReceiver;
    }

    @Override
    public void start(List<String> folderNames) {
        synchronized (folderPushers) {
            stop();

            setLastRefresh(currentTimeMillis());

            for (String folderName : folderNames) {
                ImapFolderPusher pusher = createImapFolderPusher(folderName);
                folderPushers.add(pusher);

                pusher.start();
            }
        }
    }

    @Override
    public void refresh() {
        synchronized (folderPushers) {
            for (ImapFolderPusher folderPusher : folderPushers) {
                try {
                    folderPusher.refresh();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Got exception while refreshing for " + folderPusher.getName(), e);
                }
            }
        }
    }

    @Override
    public void stop() {
        if (K9MailLib.isDebug()) {
            Log.i(LOG_TAG, "Requested stop of IMAP pusher");
        }

        synchronized (folderPushers) {
            for (ImapFolderPusher folderPusher : folderPushers) {
                try {
                    if (K9MailLib.isDebug()) {
                        Log.i(LOG_TAG, "Requesting stop of IMAP folderPusher " + folderPusher.getName());
                    }

                    folderPusher.stop();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Got exception while stopping " + folderPusher.getName(), e);
                }
            }

            folderPushers.clear();
        }
    }

    @Override
    public int getRefreshInterval() {
        return (store.getStoreConfig().getIdleRefreshMinutes() * 60 * 1000);
    }

    @Override
    public long getLastRefresh() {
        return lastRefresh;
    }

    @Override
    public void setLastRefresh(long lastRefresh) {
        this.lastRefresh = lastRefresh;
    }

    ImapFolderPusher createImapFolderPusher(String folderName) {
        return new ImapFolderPusher(store, folderName, pushReceiver);
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
