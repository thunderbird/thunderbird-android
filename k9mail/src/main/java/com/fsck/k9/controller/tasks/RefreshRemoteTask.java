package com.fsck.k9.controller.tasks;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.ControllerUtils;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import timber.log.Timber;


public class RefreshRemoteTask implements Runnable {
    private final Account account;
    private final MessagingListener taskListener;
    private final Set<MessagingListener> listeners;

    public RefreshRemoteTask(Account account, final MessagingListener taskListener,
            Set<MessagingListener> listeners) {
        this.account = account;
        this.taskListener = taskListener;
        this.listeners = listeners;
    }

    @Override
    public void run() {
        refreshRemoteSynchronous();
    }

    void refreshRemoteSynchronous() {
        Set<MessagingListener> listeners = new HashSet<>(this.listeners);
        listeners.add(taskListener);

        List<LocalFolder> localFolders = null;
        try {
            Store store = account.getRemoteStore();

            List<? extends Folder> remoteFolders = store.getPersonalNamespaces(false);

            LocalStore localStore = account.getLocalStore();
            Set<String> remoteFolderNames = new HashSet<>();
            List<LocalFolder> foldersToCreate = new LinkedList<>();

            localFolders = localStore.getPersonalNamespaces(false);
            Set<String> localFolderNames = new HashSet<>();
            for (Folder localFolder : localFolders) {
                localFolderNames.add(localFolder.getName());
            }

            for (Folder remoteFolder : remoteFolders) {
                if (!localFolderNames.contains(remoteFolder.getName())) {
                    LocalFolder localFolder = localStore.getFolder(remoteFolder.getName());
                    foldersToCreate.add(localFolder);
                }
                remoteFolderNames.add(remoteFolder.getName());
            }
            localStore.createFolders(foldersToCreate, account.getDisplayCount());

            localFolders = localStore.getPersonalNamespaces(false);

            /*
             * Clear out any folders that are no longer on the remote store.
             */
            for (Folder localFolder : localFolders) {
                String localFolderName = localFolder.getName();

                // FIXME: This is a hack used to clean up when we accidentally created the
                //        special placeholder folder "-NONE-".
                if (K9.FOLDER_NONE.equals(localFolderName)) {
                    localFolder.delete(false);
                }

                if (!account.isSpecialFolder(localFolderName) &&
                        !remoteFolderNames.contains(localFolderName)) {
                    localFolder.delete(false);
                }
            }

            localFolders = localStore.getPersonalNamespaces(false);

            for (MessagingListener l : listeners) {
                l.listFolders(account, localFolders);
            }
            for (MessagingListener l : listeners) {
                l.listFoldersFinished(account);
            }
        } catch (Exception e) {
            for (MessagingListener l : listeners) {
                l.listFoldersFailed(account, "");
            }
            Timber.e(e);
        } finally {
            if (localFolders != null) {
                for (Folder localFolder : localFolders) {
                    ControllerUtils.closeFolder(localFolder);
                }
            }
        }
    }
}
