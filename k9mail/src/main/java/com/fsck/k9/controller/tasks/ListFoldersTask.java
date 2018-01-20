package com.fsck.k9.controller.tasks;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.controller.ControllerUtils;
import com.fsck.k9.controller.IMessageController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import timber.log.Timber;

public class ListFoldersTask implements Runnable {

    private final IMessageController controller;
    private final Account account;
    private final boolean refreshRemote;
    private final Set<MessagingListener> listeners;
    private final MessagingListener taskListener;
    private final Context context;

    public ListFoldersTask(IMessageController controller,
            Context context,
            Account account, boolean refreshRemote,
            MessagingListener taskListener, Set<MessagingListener> listeners) {
        this.controller = controller;
        this.context = context;
        this.account = account;
        this.refreshRemote = refreshRemote;
        this.listeners = listeners;
        this.taskListener = taskListener;
    }

    @Override
    public void run() {
        listFoldersSynchronous();
    }

    /**
     * Lists folders that are available locally and remotely. This method calls
     * listFoldersCallback for local folders before it returns, and then for
     * remote folders at some later point. If there are no local folders
     * includeRemote is forced by this method. This method is called in the
     * foreground.
     * TODO this needs to cache the remote folder list
     */
    private void listFoldersSynchronous() {
        Set<MessagingListener> listeners = new HashSet<>(this.listeners);
        listeners.add(taskListener);

        for (MessagingListener l : listeners) {
            l.listFoldersStarted(account);
        }
        List<LocalFolder> localFolders = null;
        if (!account.isAvailable(context)) {
            Timber.i("not listing folders of unavailable account");
        } else {
            try {
                LocalStore localStore = account.getLocalStore();
                localFolders = localStore.getPersonalNamespaces(false);

                if (refreshRemote || localFolders.isEmpty()) {
                    controller.doRefreshRemote(account, taskListener);
                    return;
                }

                for (MessagingListener l : listeners) {
                    l.listFolders(account, localFolders);
                }
            } catch (Exception e) {
                for (MessagingListener l : listeners) {
                    l.listFoldersFailed(account, e.getMessage());
                }

                Timber.e(e);
                return;
            } finally {
                if (localFolders != null) {
                    for (Folder localFolder : localFolders) {
                        ControllerUtils.closeFolder(localFolder);
                    }
                }
            }
        }

        for (MessagingListener l : listeners) {
            l.listFoldersFinished(account);
        }
    }
}
