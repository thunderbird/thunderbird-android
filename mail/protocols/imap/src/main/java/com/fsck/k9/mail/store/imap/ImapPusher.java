package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.mail.power.PowerManager;
import timber.log.Timber;


public class ImapPusher implements Pusher {
    private final ImapStore store;
    private final PushReceiver pushReceiver;
    private final PowerManager powerManager;

    private final List<ImapFolderPusher> folderPushers = new ArrayList<>();

    private long lastRefresh = -1;


    public ImapPusher(ImapStore store, PushReceiver pushReceiver, PowerManager powerManager) {
        this.store = store;
        this.pushReceiver = pushReceiver;
        this.powerManager = powerManager;
    }

    @Override
    public void start(List<String> folderServerIds) {
        synchronized (folderPushers) {
            stop();

            setLastRefresh(currentTimeMillis());

            for (String folderName : folderServerIds) {
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
                    Timber.e(e, "Got exception while refreshing for %s", folderPusher.getServerId());
                }
            }
        }
    }

    @Override
    public void stop() {
        if (K9MailLib.isDebug()) {
            Timber.i("Requested stop of IMAP pusher");
        }

        synchronized (folderPushers) {
            for (ImapFolderPusher folderPusher : folderPushers) {
                try {
                    if (K9MailLib.isDebug()) {
                        Timber.i("Requesting stop of IMAP folderPusher %s", folderPusher.getServerId());
                    }

                    folderPusher.stop();
                } catch (Exception e) {
                    Timber.e(e, "Got exception while stopping %s", folderPusher.getServerId());
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
        return new ImapFolderPusher(store, folderName, pushReceiver, powerManager);
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
